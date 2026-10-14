"""Generates a reproducible dataset fixture across all 16 classes to validate the ML pipeline."""

from pathlib import Path

from PIL import Image, ImageDraw
from wastesort_ml.dataset.manifest import DatasetManifest
from wastesort_ml.dataset.taxonomy import TAXONOMY_CLASSES


def create_fixture_dataset(base_dir: Path, samples_per_class: int = 10):
    base_dir.mkdir(parents=True, exist_ok=True)
    manifest = DatasetManifest(seed=42)

    for class_idx, class_name in enumerate(TAXONOMY_CLASSES):
        class_dir = base_dir / class_name
        class_dir.mkdir(parents=True, exist_ok=True)

        for i in range(samples_per_class):
            # 2 sessions per class to test session-aware splitting
            session_id = f"session_{class_name}_{i % 2}"
            img_path = class_dir / f"{class_name}_{i}.png"

            # Create synthetic 224x224 RGB image with distinct color patterns
            img = Image.new(
                "RGB",
                (224, 224),
                color=((class_idx * 15) % 255, (i * 25) % 255, (class_idx * 20 + i * 10) % 255),
            )
            draw = ImageDraw.Draw(img)
            draw.rectangle([40, 40, 184, 184], outline="white", width=2)
            draw.text((50, 100), f"{class_name}\n#{i}", fill="white")
            img.save(img_path)

            manifest.add_sample(img_path, class_name, session_id)

    splits = manifest.split(0.70, 0.15, 0.15)
    manifest.save_split_manifests(splits, base_dir / "splits")
    return splits


if __name__ == "__main__":
    out = Path("ml/data/fixture")
    create_fixture_dataset(out)
    print(f"Fixture dataset successfully generated at {out}")
