"""Phase 4 Quality Gate: Model contract dimensions, inference, and training loop validation."""

from pathlib import Path
import torch
from wastesort_ml.models.classifier import WasteClassifier
from wastesort_ml.dataset.prepare_fixture import create_fixture_dataset
from wastesort_ml.training.train import train_baseline
from wastesort_ml.evaluation.evaluate import evaluate_model
from wastesort_ml.dataset.taxonomy import NUM_CLASSES


def test_model_contract_dimensions():
    """Verify input shape [B, 3, 224, 224] maps to exactly [B, 16] logits and probabilities."""
    model = WasteClassifier(pretrained=False)
    model.eval()

    batch_size = 4
    dummy_input = torch.randn(batch_size, 3, 224, 224)

    logits = model(dummy_input)
    assert logits.shape == (batch_size, NUM_CLASSES), f"Logits shape mismatch: {logits.shape}"

    probs = model.predict_probabilities(dummy_input)
    assert probs.shape == (batch_size, NUM_CLASSES), f"Probabilities shape mismatch: {probs.shape}"

    # Softmax probabilities must sum to 1.0 along the class dimension
    sums = probs.sum(dim=-1)
    assert torch.allclose(sums, torch.ones(batch_size), atol=1e-5), "Probabilities do not sum to 1.0"


def test_end_to_end_training_and_evaluation(tmp_path: Path):
    """Verify training and evaluation executes cleanly and yields a valid checkpoint."""
    fixture_dir = tmp_path / "data"
    create_fixture_dataset(fixture_dir, samples_per_class=4)

    train_manifest = fixture_dir / "splits" / "train_manifest.json"
    val_manifest = fixture_dir / "splits" / "val_manifest.json"
    test_manifest = fixture_dir / "splits" / "test_manifest.json"
    output_dir = tmp_path / "checkpoint"

    # Run a 2-epoch training run
    checkpoint_path = train_baseline(
        train_manifest=train_manifest,
        val_manifest=val_manifest,
        output_dir=output_dir,
        epochs=2,
        lr=1e-3,
        batch_size=8,
    )

    assert checkpoint_path.exists(), "Model checkpoint was not created"
    assert checkpoint_path.stat().st_size > 0, "Model checkpoint is empty"

    # Run evaluation
    results = evaluate_model(checkpoint_path, test_manifest, batch_size=8)
    assert "accuracy" in results
    assert "macro_f1" in results
    assert "confusion_matrix" in results
    assert len(results["confusion_matrix"]) == NUM_CLASSES
