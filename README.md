# WasteSort

[![Android](https://img.shields.io/badge/Platform-Android%20(SDK%2026%2B)-green.svg)](android/)
[![iOS](https://img.shields.io/badge/Platform-iOS%20(16.0%2B)-lightgrey.svg)](ios/)
[![ML](https://img.shields.io/badge/ML-LiteRT%20%7C%20PyTorch-orange.svg)](ml/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Code Style](https://img.shields.io/badge/Code%20Style-Spotless%20%7C%20Ruff-black.svg)](.editorconfig)

**WasteSort** is an offline-first, camera-based household waste segregation application for Android and iOS. It identifies waste items on-device and recommends the appropriate municipal stream (**Wet**, **Dry**, or **Other / Special Handling**) with plain-language disposal guidance.

---

## Key Architectural Principles

1. **Decoupled Rules Engine:** Visual material recognition (the ML model) is strictly decoupled from municipal disposal policy (`rules/profiles/*.json`). The model outputs internal material/item identifiers; a configurable rules engine maps them to local streams.
2. **Air-Gapped & Private by Default:** Inference runs 100% on-device using a quantized 1.87 MB MobileNetV3 flatbuffer. `android.permission.INTERNET` is completely absent from the app manifest—zero camera frames or metadata ever leave the device.
3. **Safety-First Uncertainty Engine:** Ambiguous items, empty views, or hazardous items (`battery`, `sanitary_medical`, `hazardous_chemical`) trigger conservative uncertainty handling (`NOT SURE` or safety alerts) rather than forcing arbitrary guesses.
4. **Sub-Centisecond On-Device Latency:** On-device warm inference runs in ~1.36 ms with uncompressed direct memory mapping (`mmap`).

---

## Category & Stream Model

### User-Facing Municipal Streams (`urban_general_v1`)
* 🟢 **WET:** Biodegradable kitchen waste, fruit/vegetable peels, food scraps, garden waste.
* 🔵 **DRY:** Recyclables including clean plastic containers, paper, cardboard, metal cans, glass bottles, and multi-layer packaging.
* 🟠 **OTHER:** Domestic hazardous and special-stream items (batteries, e-waste, sanitary/medical waste, chemical aerosols).
* ⚪ **NOT SURE:** Items falling below the confidence threshold ($\tau < 0.70$) or unrecognized materials. Offers instant retake and curated manual fallback.

### 16 Internal Taxonomy Classes
`food_organic`, `garden_waste`, `paper`, `cardboard`, `plastic_bottle_container`, `plastic_film_flexible`, `glass_bottle_jar`, `metal_can_tin`, `textile`, `e_waste`, `battery`, `sanitary_medical`, `hazardous_chemical`, `mixed_composite_packaging`, `unknown_household_item`, `background_no_item`.

---

## Repository Structure

```text
WasteSort/
├── android/                         # Native Android Application
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── assets/
│   │   │   │   ├── models/          # Bundled waste_classifier_v1.tflite (1.87 MB)
│   │   │   │   └── rules/           # Bundled urban_general_v1.json disposal rules
│   │   │   ├── java/org/wastesort/
│   │   │   │   ├── model/           # Domain models & user-facing catalog
│   │   │   │   ├── rules/           # Decoupled JSON Rules Engine
│   │   │   │   ├── ui/              # Jetpack Compose UI (viewfinder, bottom sheet, dialogs)
│   │   │   │   └── vision/          # CameraX frame preprocessor & LiteRT interpreter
│   │   │   └── AndroidManifest.xml  # Zero-network air-gapped manifest (CAMERA only)
│   │   └── src/test/                # Air-gap, asset packaging & rules integration tests
│   ├── gradle/libs.versions.toml    # Gradle Version Catalog
│   └── build.gradle.kts             # Spotless (ktlint) & build configurations
│
├── ml/                              # Python Machine Learning Subproject
│   ├── pyproject.toml               # Poetry dependencies (PyTorch, TensorFlow, Ruff, MyPy)
│   ├── src/wastesort_ml/
│   │   ├── dataset/                 # Taxonomy, manifests, deduplication & split logic
│   │   ├── models/                  # MobileNetV3-Small classifier architectures
│   │   ├── training/                # Training pipelines, checkpoints & fine-tuning
│   │   ├── evaluation/              # Uncertainty calibrator, risk-coverage curves
│   │   └── export/                  # TFLite/CoreML quantization & on-host benchmark
│   └── tests/                       # Pytest quality gate suite (12 tests)
│
├── ios/                             # Native iOS Application (Swift Package / Xcode)
│   └── WasteSort/                   # SwiftUI, AVFoundation & Swift Rules Engine
│
├── rules/profiles/                  # Master municipal disposal profiles (JSON)
│   └── urban_general_v1.json
├── docs/                            # Formal specifications & phase documentation
│   ├── SRS.md                       # Software Requirements Specification
│   ├── ARCHITECTURE.md              # System Architecture & pipeline design
│   ├── MODEL_CONTRACT.md            # Tensor contracts, dimensions, normalization
│   ├── DEVELOPMENT_PHASES.md        # Quality gate definitions (Phases 1-12)
│   ├── DATASET.md                   # Dataset provenance & partitioning policy
│   └── LABELING_GUIDE.md            # Edge-case disambiguation guidelines
└── README.md
```

---

## Development & Quality Gates Status

| Phase | Milestone | Status | Key Deliverable / Gate |
|:---:|:---|:---:|:---|
| **1** | Requirements & Scope | **PASSED** | SRS finalized, open decisions locked, zero-upload stance approved. |
| **2** | Architecture & Model Contract | **PASSED** | Tensor contract `[1, 224, 224, 3] -> [1, 16]`, decoupled rules schema. |
| **3** | Dataset Pipeline & Taxonomy | **PASSED** | Session-based 70/15/15 splits, zero-leakage hash checks passing. |
| **4** | Baseline ML Model | **PASSED** | MobileNetV3 PyTorch architecture, forward pass contracts verified. |
| **5** | Uncertainty Calibration | **PASSED** | $\tau=0.70$ thresholding, $\tau=0.50$ hazard safety overrides verified. |
| **6** | Mobile Export & Optimization | **PASSED** | **1.87 MB** TFLite model deployed, **1.36 ms** warm latency. |
| **7** | Android MVP Application | **PASSED** | CameraX + Compose + TFLite runtime + Curated manual fallback dialog. |
| **8** | iOS MVP Core | **READY** | Swift package, shared rules engine, XCTest suite scaffolded. |
| **9** | QA, Integration & Air-Gap | **PASSED** | Verified: 0 network permissions, uncompressed `STORED` TFLite in APK. |
| **10**| Field & Beta Testing | **ACTIVE** | Live dogfooding with household items (snack wrappers, bottles, etc.). |

---

## Getting Started

### Prerequisites
* **Android:** Android Studio Jellyfish / Hedgehog+, JDK 17, Android SDK 34 (min SDK 26)
* **Python ML:** Python 3.11+, Poetry
* **iOS (Future):** Xcode 15+, macOS Sonoma (min deployment target iOS 16.0)

---

### Android Build & Installation

1. **Verify Code Formatting (Spotless / ktlint):**
   ```bash
   cd android
   ./gradlew spotlessCheck
   # Auto-format: ./gradlew spotlessApply
   ```

2. **Run JVM & Air-Gap Integration Tests:**
   ```bash
   ./gradlew test
   ```

3. **Assemble Debug APK:**
   ```bash
   ./gradlew assembleDebug
   # Artifact: app/build/outputs/apk/debug/app-debug.apk
   ```

4. **Install onto Connected Android Device:**
   ```bash
   ./gradlew installDebug
   ```

---

### Python ML Pipeline

All ML commands are managed inside the `ml/` subproject:

1. **Set Up Poetry Environment:**
   ```bash
   cd ml
   poetry config virtualenvs.in-project true
   poetry install
   ```

2. **Run Linting & Static Type Checking:**
   ```bash
   poetry run ruff check .
   poetry run ruff format --check .
   poetry run mypy src tests
   ```

3. **Run ML Quality Gate Test Suite:**
   ```bash
   poetry run pytest -v
   ```

4. **Benchmark & Inspect the Deployed Mobile Model:**
   ```bash
   poetry run python -c "
   from pathlib import Path
   from wastesort_ml.export.export_mobile import benchmark_tflite
   stats = benchmark_tflite(Path('../android/app/src/main/assets/models/waste_classifier_v1.tflite'))
   print(f'Model Size: {stats[\"model_size_mb\"]:.2f} MB')
   print(f'Warm Latency: {stats[\"mean_latency_ms\"]:.2f} ms')
   "
   ```

---

## Technical Specifications Summary

* **Input Image:** 224 × 224 pixels, 3 channels (sRGB), center-cropped from reticle.
* **Normalization:** ImageNet mean `[0.485, 0.456, 0.406]`, std `[0.229, 0.224, 0.225]`.
* **Output:** 16-element probability vector (Softmax).
* **Quantization:** FP16 / dynamic range quantization flatbuffer.
* **Camera Pipeline:** CameraX `ImageCapture` with sensor rotation correction and zero-copy direct `ByteBuffer` allocation.
* **UI Framework:** Jetpack Compose with edge-to-edge transparent system bars and Material 3 design tokens.

---

## License

This project is licensed under the Apache 2.0 License. See [LICENSE](LICENSE) for details.
