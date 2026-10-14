"""Baseline training pipeline for WasteClassifier."""

from pathlib import Path
from typing import Sized, cast

import torch
import torch.nn as nn
from torch.optim import AdamW
from torch.optim.lr_scheduler import CosineAnnealingLR
from torch.utils.data import DataLoader
from wastesort_ml.dataset.loader import create_dataloader
from wastesort_ml.models.classifier import WasteClassifier


def train_epoch(
    model: nn.Module,
    loader: DataLoader,
    criterion: nn.Module,
    optimizer: torch.optim.Optimizer,
    device: torch.device,
) -> float:
    model.train()
    running_loss = 0.0
    for images, targets in loader:
        images, targets = images.to(device), targets.to(device)
        optimizer.zero_grad()
        outputs = model(images)
        loss = criterion(outputs, targets)
        loss.backward()
        optimizer.step()
        running_loss += loss.item() * images.size(0)

    dataset_size = len(cast(Sized, loader.dataset))
    return running_loss / dataset_size if dataset_size > 0 else 0.0


def evaluate_loss(
    model: nn.Module,
    loader: DataLoader,
    criterion: nn.Module,
    device: torch.device,
) -> float:
    model.eval()
    running_loss = 0.0
    with torch.no_grad():
        for images, targets in loader:
            images, targets = images.to(device), targets.to(device)
            outputs = model(images)
            loss = criterion(outputs, targets)
            running_loss += loss.item() * images.size(0)

    dataset_size = len(cast(Sized, loader.dataset))
    return running_loss / dataset_size if dataset_size > 0 else 0.0


def train_baseline(
    train_manifest: Path,
    val_manifest: Path,
    output_dir: Path,
    epochs: int = 5,
    lr: float = 1e-4,
    batch_size: int = 8,
) -> Path:
    output_dir.mkdir(parents=True, exist_ok=True)
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

    model = WasteClassifier(pretrained=False).to(device)
    criterion = nn.CrossEntropyLoss()
    optimizer = AdamW(model.parameters(), lr=lr, weight_decay=1e-2)
    scheduler = CosineAnnealingLR(optimizer, T_max=epochs)

    train_loader = create_dataloader(
        train_manifest, batch_size=batch_size, is_training=True, num_workers=0
    )
    val_loader = create_dataloader(
        val_manifest, batch_size=batch_size, is_training=False, num_workers=0
    )

    best_val_loss = float("inf")
    best_checkpoint = output_dir / "baseline_model.pth"

    for epoch in range(epochs):
        _ = train_epoch(model, train_loader, criterion, optimizer, device)
        val_loss = evaluate_loss(model, val_loader, criterion, device)
        scheduler.step()

        if val_loss < best_val_loss:
            best_val_loss = val_loss
            torch.save(
                {
                    "epoch": epoch,
                    "model_state_dict": model.state_dict(),
                    "val_loss": val_loss,
                },
                best_checkpoint,
            )

    return best_checkpoint
