"""Manifest generation, deduplication, and session-aware data splitting."""

import hashlib
import json
import random
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Dict, List

from wastesort_ml.dataset.taxonomy import CLASS_TO_INDEX, TAXONOMY_CLASSES


@dataclass
class ImageSample:
    image_path: str
    class_name: str
    class_index: int
    session_id: str
    sha256: str


class DatasetManifest:
    def __init__(self, seed: int = 42):
        self.seed = seed
        self.samples: List[ImageSample] = []

    @staticmethod
    def compute_sha256(file_path: Path) -> str:
        hasher = hashlib.sha256()
        with open(file_path, "rb") as f:
            for chunk in iter(lambda: f.read(65536), b""):
                hasher.update(chunk)
        return hasher.hexdigest()

    def add_sample(self, file_path: Path, class_name: str, session_id: str) -> bool:
        if class_name not in CLASS_TO_INDEX:
            raise ValueError(f"Unknown class: {class_name}. Must be one of {TAXONOMY_CLASSES}")

        sha = self.compute_sha256(file_path)
        if any(s.sha256 == sha for s in self.samples):
            return False

        sample = ImageSample(
            image_path=str(file_path.resolve()),
            class_name=class_name,
            class_index=CLASS_TO_INDEX[class_name],
            session_id=session_id,
            sha256=sha,
        )
        self.samples.append(sample)
        return True

    def split(
        self, train_ratio: float = 0.70, val_ratio: float = 0.15, test_ratio: float = 0.15
    ) -> Dict[str, List[ImageSample]]:
        assert abs((train_ratio + val_ratio + test_ratio) - 1.0) < 1e-5, "Ratios must sum to 1.0"

        sessions: Dict[str, List[ImageSample]] = {}
        for sample in self.samples:
            sessions.setdefault(sample.session_id, []).append(sample)

        session_keys = list(sessions.keys())
        rng = random.Random(self.seed)
        rng.shuffle(session_keys)

        total_sessions = len(session_keys)
        train_end = int(total_sessions * train_ratio)
        val_end = train_end + int(total_sessions * val_ratio)

        train_sessions = set(session_keys[:train_end])
        val_sessions = set(session_keys[train_end:val_end])

        splits: Dict[str, List[ImageSample]] = {"train": [], "val": [], "test": []}

        for session_id, sample_list in sessions.items():
            if session_id in train_sessions:
                splits["train"].extend(sample_list)
            elif session_id in val_sessions:
                splits["val"].extend(sample_list)
            else:
                splits["test"].extend(sample_list)

        return splits

    def save_split_manifests(self, splits: Dict[str, List[ImageSample]], output_dir: Path) -> None:
        output_dir.mkdir(parents=True, exist_ok=True)
        for split_name, items in splits.items():
            manifest_file = output_dir / f"{split_name}_manifest.json"
            with open(manifest_file, "w") as f:
                json.dump([asdict(item) for item in items], f, indent=2)
