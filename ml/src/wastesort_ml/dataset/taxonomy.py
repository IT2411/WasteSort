"""Centralized taxonomy and class index definitions for WasteSort."""

from typing import List, Dict

TAXONOMY_CLASSES: List[str] = [
    "food_organic",
    "garden_waste",
    "paper",
    "cardboard",
    "plastic_bottle_container",
    "plastic_film_flexible",
    "glass_bottle_jar",
    "metal_can_tin",
    "textile",
    "e_waste",
    "battery",
    "sanitary_medical",
    "hazardous_chemical",
    "mixed_composite_packaging",
    "unknown_household_item",
    "background_no_item",
]

CLASS_TO_INDEX: Dict[str, int] = {name: idx for idx, name in enumerate(TAXONOMY_CLASSES)}
INDEX_TO_CLASS: Dict[int, str] = {idx: name for idx, name in enumerate(TAXONOMY_CLASSES)}

HAZARDOUS_CLASSES: List[str] = [
    "battery",
    "sanitary_medical",
    "hazardous_chemical",
]

NUM_CLASSES: int = len(TAXONOMY_CLASSES)
