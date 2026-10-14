"""Quality gate unit tests for Phase 3: Dataset, Taxonomy, and Leakage Prevention."""

from pathlib import Path
import pytest
from wastesort_ml.dataset.taxonomy import (
    TAXONOMY_CLASSES,
    CLASS_TO_INDEX,
    INDEX_TO_CLASS,
    NUM_CLASSES,
)
from wastesort_ml.dataset.prepare_fixture import create_fixture_dataset


def test_taxonomy_count_and_sync():
    """Verify taxonomy has exactly 16 classes and matches MODEL_CONTRACT index constraints."""
    assert NUM_CLASSES == 16, f"Expected 16 taxonomy classes, found {NUM_CLASSES}"
    assert len(CLASS_TO_INDEX) == 16
    assert len(INDEX_TO_CLASS) == 16

    # Verify first and last indices match Model Contract
    assert INDEX_TO_CLASS[0] == "food_organic"
    assert INDEX_TO_CLASS[15] == "background_no_item"
    assert CLASS_TO_INDEX["battery"] == 10
    assert CLASS_TO_INDEX["sanitary_medical"] == 11


def test_zero_split_leakage(tmp_path: Path):
    """Verify that images and capture sessions never leak across train, val, and test splits."""
    fixture_dir = tmp_path / "dataset"
    splits = create_fixture_dataset(fixture_dir, samples_per_class=6)

    train_hashes = {s.sha256 for s in splits["train"]}
    val_hashes = {s.sha256 for s in splits["val"]}
    test_hashes = {s.sha256 for s in splits["test"]}

    # Image hash disjoint check
    assert train_hashes.isdisjoint(val_hashes), "Leakage detected between Train and Val hashes"
    assert train_hashes.isdisjoint(test_hashes), "Leakage detected between Train and Test hashes"
    assert val_hashes.isdisjoint(test_hashes), "Leakage detected between Val and Test hashes"

    # Session disjoint check
    train_sessions = {s.session_id for s in splits["train"]}
    val_sessions = {s.session_id for s in splits["val"]}
    test_sessions = {s.session_id for s in splits["test"]}

    assert train_sessions.isdisjoint(val_sessions), "Session leakage between Train and Val"
    assert train_sessions.isdisjoint(test_sessions), "Session leakage between Train and Test"
    assert val_sessions.isdisjoint(test_sessions), "Session leakage between Val and Test"


def test_dataset_coverage(tmp_path: Path):
    """Verify all 16 classes are represented across dataset splits."""
    fixture_dir = tmp_path / "dataset"
    splits = create_fixture_dataset(fixture_dir, samples_per_class=6)

    total_samples = len(splits["train"]) + len(splits["val"]) + len(splits["test"])
    assert total_samples == 16 * 6

    # Verify all classes exist in the combined dataset
    covered_classes = {s.class_name for split in splits.values() for s in split}
    assert covered_classes == set(TAXONOMY_CLASSES)
