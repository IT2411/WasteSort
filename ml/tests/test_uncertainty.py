"""Phase 5 Quality Gate: Uncertainty handling, hazard safety, and background suppression."""

import numpy as np
import pytest
from wastesort_ml.dataset.taxonomy import (
    CLASS_TO_INDEX,
    TAXONOMY_CLASSES,
)
from wastesort_ml.evaluation.thresholds import evaluate_selective_coverage
from wastesort_ml.evaluation.uncertainty import UncertaintyPostProcessor


@pytest.fixture
def post_processor():
    return UncertaintyPostProcessor(
        default_threshold=0.70,
        hazard_threshold=0.50,
        background_threshold=0.40,
    )


def test_confident_prediction(post_processor):
    """High confidence prediction (> 0.70) must return CONFIDENT."""
    probs = np.zeros(len(TAXONOMY_CLASSES))
    probs[CLASS_TO_INDEX["food_organic"]] = 0.88
    probs[CLASS_TO_INDEX["paper"]] = 0.12

    decision = post_processor.process_probabilities(probs)
    assert decision.status == "CONFIDENT"
    assert decision.predicted_class == "food_organic"
    assert decision.confidence == pytest.approx(0.88)
    assert decision.is_safe_to_recommend is True


def test_uncertainty_threshold_routing(post_processor):
    """Low confidence (< 0.70) must route to NOT_SURE and reject safe recommendation."""
    probs = np.zeros(len(TAXONOMY_CLASSES))
    probs[CLASS_TO_INDEX["plastic_bottle_container"]] = 0.62
    probs[CLASS_TO_INDEX["glass_bottle_jar"]] = 0.38

    decision = post_processor.process_probabilities(probs)
    assert decision.status == "NOT_SURE"
    assert decision.confidence == pytest.approx(0.62)
    assert decision.is_safe_to_recommend is False


def test_hazard_sensitivity_override(post_processor):
    """If a hazard class is >= 0.50, it MUST flag HAZARD_FLAGGED to prevent safety violations."""
    probs = np.zeros(len(TAXONOMY_CLASSES))
    probs[CLASS_TO_INDEX["battery"]] = 0.52
    probs[CLASS_TO_INDEX["metal_can_tin"]] = 0.48

    decision = post_processor.process_probabilities(probs)
    assert decision.status == "HAZARD_FLAGGED"
    assert decision.predicted_class == "battery"
    assert decision.confidence == pytest.approx(0.52)
    assert decision.is_safe_to_recommend is True


def test_background_suppression(post_processor):
    """If background exceeds threshold or is top-1, output NO_OBJECT_DETECTED."""
    probs = np.zeros(len(TAXONOMY_CLASSES))
    probs[CLASS_TO_INDEX["background_no_item"]] = 0.45
    probs[CLASS_TO_INDEX["cardboard"]] = 0.35

    decision = post_processor.process_probabilities(probs)
    assert decision.status == "NO_OBJECT_DETECTED"
    assert decision.predicted_class == "background_no_item"
    assert decision.is_safe_to_recommend is False


def test_selective_coverage_evaluation(post_processor):
    """Verify coverage and selective accuracy calculation with rejection of uncertain items."""
    # 3 samples: 1 confident correct, 1 uncertain, 1 confident incorrect
    p1 = np.zeros(len(TAXONOMY_CLASSES))
    p1[CLASS_TO_INDEX["paper"]] = 0.90  # Confident

    p2 = np.zeros(len(TAXONOMY_CLASSES))
    p2[CLASS_TO_INDEX["paper"]] = 0.55  # Uncertain (<0.70)

    p3 = np.zeros(len(TAXONOMY_CLASSES))
    p3[CLASS_TO_INDEX["paper"]] = 0.85  # Confident

    probs = np.array([p1, p2, p3])
    targets = np.array(["paper", "cardboard", "glass_bottle_jar"])

    metrics = evaluate_selective_coverage(probs, targets, post_processor)

    assert metrics["total_samples"] == 3
    assert metrics["accepted_samples"] == 2  # 1 was rejected as NOT_SURE
    assert metrics["coverage"] == pytest.approx(2 / 3)
    assert metrics["selective_accuracy"] == pytest.approx(1 / 2)  # 1 correct out of 2 accepted
