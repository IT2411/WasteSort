"""Phase 6 Quality Gate: Mobile Model Export, Size <= 15MB, Input/Output Tensor Contracts."""

from pathlib import Path

import numpy as np
import tensorflow as tf
from wastesort_ml.dataset.taxonomy import NUM_CLASSES
from wastesort_ml.export.export_mobile import benchmark_tflite, export_tflite_direct


def test_tflite_export_and_contract(tmp_path: Path):
    """Verify exported TFLite adheres to SRS size <= 15MB and [1, 224, 224, 3] -> [1, 16] contract."""
    tflite_path = tmp_path / "waste_classifier_v1.tflite"
    export_tflite_direct(tflite_path, quantize=True)

    assert tflite_path.exists(), "TFLite artifact was not created"

    # 1. Model size gate: SRS specifies <= 15 MB
    size_mb = tflite_path.stat().st_size / (1024 * 1024)
    assert size_mb <= 15.0, f"Model size {size_mb:.2f} MB exceeds 15 MB limit"

    # 2. Inspect TFLite tensor dimensions
    interpreter = tf.lite.Interpreter(model_path=str(tflite_path))
    interpreter.allocate_tensors()

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    assert input_details[0]["shape"].tolist() == [
        1,
        224,
        224,
        3,
    ], f"Input tensor mismatch: {input_details[0]['shape']}"
    assert output_details[0]["shape"].tolist() == [
        1,
        NUM_CLASSES,
    ], f"Output tensor mismatch: {output_details[0]['shape']}"

    # 3. Test execution and probability properties
    dummy_input = np.random.randn(1, 224, 224, 3).astype(np.float32)
    interpreter.set_tensor(input_details[0]["index"], dummy_input)
    interpreter.invoke()
    output_data = interpreter.get_tensor(output_details[0]["index"])

    assert output_data.shape == (1, NUM_CLASSES)
    assert np.isclose(np.sum(output_data), 1.0, atol=1e-4), "TFLite output does not sum to 1.0"


def test_tflite_benchmark(tmp_path: Path):
    """Verify inference latency and performance metrics."""
    tflite_path = tmp_path / "waste_classifier_v1.tflite"
    export_tflite_direct(tflite_path, quantize=True)

    metrics = benchmark_tflite(tflite_path, iterations=5)
    assert metrics["model_size_mb"] <= 15.0
    assert metrics["mean_latency_ms"] > 0
