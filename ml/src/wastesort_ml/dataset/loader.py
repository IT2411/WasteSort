"""PyTorch Dataset and Dataloaders for WasteSort manifests."""

import json
from pathlib import Path
from typing import List, Dict, Tuple
from PIL import Image
import torch
from torch.utils.data import Dataset, DataLoader
from torchvision import transforms

# Normalization constants defined in docs/MODEL_CONTRACT.md
IMAGENET_MEAN = [0.485, 0.456, 0.406]
IMAGENET_STD = [0.229, 0.224, 0.225]


def get_transforms(is_training: bool = False) -> transforms.Compose:
    if is_training:
        return transforms.Compose([
            transforms.Resize(256),
            transforms.RandomResizedCrop(224, scale=(0.8, 1.0)),
            transforms.RandomHorizontalFlip(),
            transforms.ColorJitter(brightness=0.2, contrast=0.2),
            transforms.ToTensor(),
            transforms.Normalize(mean=IMAGENET_MEAN, std=IMAGENET_STD),
        ])
    else:
        return transforms.Compose([
            transforms.Resize(256),
            transforms.CenterCrop(224),
            transforms.ToTensor(),
            transforms.Normalize(mean=IMAGENET_MEAN, std=IMAGENET_STD),
        ])


class WasteDataset(Dataset):
    def __init__(self, manifest_path: Path, is_training: bool = False):
        with open(manifest_path, "r") as f:
            self.samples: List[Dict] = json.load(f)
        self.transform = get_transforms(is_training=is_training)

    def __len__(self) -> int:
        return len(self.samples)

    def __getitem__(self, idx: int) -> Tuple[torch.Tensor, int]:
        item = self.samples[idx]
        image_path = item["image_path"]
        class_index = item["class_index"]

        image = Image.open(image_path).convert("RGB")
        tensor = self.transform(image)
        return tensor, class_index


def create_dataloader(
    manifest_path: Path,
    batch_size: int = 16,
    is_training: bool = False,
    num_workers: int = 2
) -> DataLoader:
    dataset = WasteDataset(manifest_path, is_training=is_training)
    return DataLoader(
        dataset,
        batch_size=batch_size,
        shuffle=is_training,
        num_workers=num_workers,
        pin_memory=torch.cuda.is_available()
    )
