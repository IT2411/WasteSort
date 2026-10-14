"""Inference post-processing, probability calibration, and uncertainty thresholds."""

from dataclasses import dataclass
from typing import List, Tuple

import numpy as np
from wastesort_ml.dataset.taxonomy import (
    CLASS_TO_INDEX,
    HAZARDOUS_CLASSES,
    TAXONOMY_CLASSES,
)


@dataclass
class DecisionOutput:
    status: str  # "CONFIDENT", "NOT_SURE", "NO_OBJECT_DETECTED", "HAZARD_FLAGGED"
    predicted_class: str
    confidence: float
    is_safe_to_recommend: bool
    top_k: List[Tuple[str, float]]


class UncertaintyPostProcessor:
    def __init__(
        self,
        default_threshold: float = 0.70,
        hazard_threshold: float = 0.50,
        background_threshold: float = 0.40,
    ):
        self.default_threshold = default_threshold
        self.hazard_threshold = hazard_threshold
        self.background_threshold = background_threshold

        self.hazard_indices = [CLASS_TO_INDEX[name] for name in HAZARDOUS_CLASSES]
        self.bg_index = CLASS_TO_INDEX["background_no_item"]

    def process_probabilities(self, probs: np.ndarray, top_k_count: int = 3) -> DecisionOutput:
        """
        Takes a 1D probability distribution of length 16 and applies safety & uncertainty gates.
        """
        assert len(probs) == len(
            TAXONOMY_CLASSES
        ), f"Expected {len(TAXONOMY_CLASSES)} classes, got {len(probs)}"

        # Compute Top-K
        ranked_indices = np.argsort(probs)[::-1]
        top_k = [(TAXONOMY_CLASSES[idx], float(probs[idx])) for idx in ranked_indices[:top_k_count]]

        top_idx = ranked_indices[0]
        top_prob = float(probs[top_idx])
        top_class = TAXONOMY_CLASSES[top_idx]

        # 1. Background / No object check
        if top_idx == self.bg_index or probs[self.bg_index] >= self.background_threshold:
            return DecisionOutput(
                status="NO_OBJECT_DETECTED",
                predicted_class="background_no_item",
                confidence=float(probs[self.bg_index]),
                is_safe_to_recommend=False,
                top_k=top_k,
            )

        # 2. Hazard safety check
        # If any hazardous class exceeds hazard_threshold, force HAZARD_FLAGGED
        for h_idx in self.hazard_indices:
            if probs[h_idx] >= self.hazard_threshold:
                h_name = TAXONOMY_CLASSES[h_idx]
                return DecisionOutput(
                    status="HAZARD_FLAGGED",
                    predicted_class=h_name,
                    confidence=float(probs[h_idx]),
                    is_safe_to_recommend=True,
                    top_k=top_k,
                )

        # 3. Default confidence threshold check
        if top_prob < self.default_threshold:
            return DecisionOutput(
                status="NOT_SURE",
                predicted_class=top_class,
                confidence=top_prob,
                is_safe_to_recommend=False,
                top_k=top_k,
            )

        # 4. Confident prediction
        return DecisionOutput(
            status="CONFIDENT",
            predicted_class=top_class,
            confidence=top_prob,
            is_safe_to_recommend=True,
            top_k=top_k,
        )
