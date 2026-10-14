"""Coverage vs Risk evaluation for calibrating operational thresholds."""

from typing import Any, Dict

import numpy as np
from wastesort_ml.evaluation.uncertainty import UncertaintyPostProcessor


def evaluate_selective_coverage(
    probabilities: np.ndarray,
    ground_truth: np.ndarray,
    post_processor: UncertaintyPostProcessor,
) -> Dict[str, Any]:
    """
    Evaluates accuracy and coverage when the uncertainty policy rejects uncertain predictions.
    """
    total = len(ground_truth)
    accepted = 0
    correct = 0

    for probs, target in zip(probabilities, ground_truth):
        decision = post_processor.process_probabilities(probs)
        if decision.is_safe_to_recommend:
            accepted += 1
            if decision.predicted_class == target:
                correct += 1

    coverage = accepted / total if total > 0 else 0.0
    selective_accuracy = correct / accepted if accepted > 0 else 0.0

    return {
        "total_samples": total,
        "accepted_samples": accepted,
        "coverage": coverage,
        "selective_accuracy": selective_accuracy,
    }
