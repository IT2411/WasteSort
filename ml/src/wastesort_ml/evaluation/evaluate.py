"""Model evaluation: Accuracy, Macro-F1, per-class Precision/Recall and Confusion Matrix."""

from pathlib import Path
from typing import Any, Dict, List
import numpy as np
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix, f1_score
import torch
from wastesort_ml.dataset.loader import create_dataloader
from wastesort_ml.dataset.taxonomy import TAXONOMY_CLASSES
from wastesort_ml.models.classifier import WasteClassifier


def evaluate_model(
    model_checkpoint: Path,
    test_manifest: Path,
    batch_size: int = 8,
) -> Dict[str, Any]:
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model = WasteClassifier(pretrained=False).to(device)

    checkpoint = torch.load(model_checkpoint, map_location=device)
    model.load_state_dict(checkpoint["model_state_dict"])
    model.eval()

    test_loader = create_dataloader(
        test_manifest, batch_size=batch_size, is_training=False, num_workers=0
    )

    preds_list: List[int] = []
    targets_list: List[int] = []

    with torch.no_grad():
        for images, targets in test_loader:
            images = images.to(device)
            probs = model.predict_probabilities(images)
            preds = torch.argmax(probs, dim=-1).cpu().numpy()
            preds_list.extend(preds.tolist())
            targets_list.extend(targets.numpy().tolist())

    all_preds = np.array(preds_list)
    all_targets = np.array(targets_list)

    acc = accuracy_score(all_targets, all_preds)
    macro_f1 = f1_score(all_targets, all_preds, average="macro", zero_division=0)
    matrix = confusion_matrix(all_targets, all_preds, labels=list(range(len(TAXONOMY_CLASSES))))
    report = classification_report(
        all_targets,
        all_preds,
        target_names=TAXONOMY_CLASSES,
        labels=list(range(len(TAXONOMY_CLASSES))),
        output_dict=True,
        zero_division=0,
    )

    return {
        "accuracy": float(acc),
        "macro_f1": float(macro_f1),
        "confusion_matrix": matrix.tolist(),
        "report": report,
    }
