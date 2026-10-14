"""Direct, self-contained Mobile / TFLite compatible flatbuffer exporter."""

import os
from pathlib import Path

import numpy as np
import tensorflow as tf
from wastesort_ml.dataset.taxonomy import NUM_CLASSES


def export_tflite_direct(output_tflite_path: Path, quantize: bool = True) -> Path:
    """
    Builds and exports the equivalent MobileNetV3-Small architecture in TensorFlow/Keras
    and converts to optimized TFLite to guarantee Android LiteRT runtime compatibility.
    """
    output_tflite_path.parent.mkdir(parents=True, exist_ok=True)

    # Construct Keras MobileNetV3-Small with matching 16 classes and ImageNet normalization
    base_model = tf.keras.applications.MobileNetV3Small(
        input_shape=(224, 224, 3),
        include_top=False,
        weights="imagenet",
        pooling="avg",
    )
    inputs = tf.keras.Input(shape=(224, 224, 3), name="input")
    x = base_model(inputs, training=False)
    outputs = tf.keras.layers.Dense(NUM_CLASSES, activation="softmax", name="output")(x)
    keras_model = tf.keras.Model(inputs=inputs, outputs=outputs)

    converter = tf.lite.TFLiteConverter.from_keras_model(keras_model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    if quantize:
        converter.target_spec.supported_types = [tf.float16]

    tflite_buffer = converter.convert()
    with open(output_tflite_path, "wb") as f:
        f.write(tflite_buffer)

    return output_tflite_path


def benchmark_tflite(tflite_path: Path, iterations: int = 20) -> dict:
    """Benchmarks cold start and warm latency on host."""
    import time

    interpreter = tf.lite.Interpreter(model_path=str(tflite_path))

    # Measure cold start
    t0 = time.perf_counter()
    interpreter.allocate_tensors()
    cold_start_ms = (time.perf_counter() - t0) * 1000

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    input_shape = input_details[0]["shape"]

    # Warmup
    dummy = np.random.randn(*input_shape).astype(np.float32)
    interpreter.set_tensor(input_details[0]["index"], dummy)
    interpreter.invoke()

    # Benchmark warm iterations
    latencies = []
    for _ in range(iterations):
        dummy = np.random.randn(*input_shape).astype(np.float32)
        t_start = time.perf_counter()
        interpreter.set_tensor(input_details[0]["index"], dummy)
        interpreter.invoke()
        _ = interpreter.get_tensor(output_details[0]["index"])
        latencies.append((time.perf_counter() - t_start) * 1000)

    return {
        "cold_start_ms": cold_start_ms,
        "mean_latency_ms": float(np.mean(latencies)),
        "p95_latency_ms": float(np.percentile(latencies, 95)),
        "model_size_mb": os.path.getsize(tflite_path) / (1024 * 1024),
    }
