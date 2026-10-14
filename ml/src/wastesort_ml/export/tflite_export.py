"""Export WasteClassifier PyTorch model to ONNX and TensorFlow Lite (TFLite)."""

import tempfile
from pathlib import Path
from typing import Tuple

import onnx
import tensorflow as tf
import torch
from onnx_tf.backend import prepare


def export_to_onnx(
    model: torch.nn.Module,
    output_onnx_path: Path,
    input_shape: Tuple[int, ...] = (1, 3, 224, 224),
) -> Path:
    """Exports PyTorch model to ONNX format."""
    model.eval()
    dummy_input = torch.randn(*input_shape, requires_grad=False)
    output_onnx_path.parent.mkdir(parents=True, exist_ok=True)

    torch.onnx.export(
        model,
        dummy_input,
        str(output_onnx_path),
        export_params=True,
        opset_version=13,
        do_constant_folding=True,
        input_names=["input"],
        output_names=["output"],
        dynamic_axes={"input": {0: "batch_size"}, "output": {0: "batch_size"}},
    )
    onnx_model = onnx.load(str(output_onnx_path))
    onnx.checker.check_model(onnx_model)
    return output_onnx_path


def export_to_tflite(
    model: torch.nn.Module,
    output_tflite_path: Path,
    quantize_fp16: bool = True,
) -> Path:
    """
    Exports PyTorch model directly to TFLite with NCHW -> NHWC conversion.
    """
    output_tflite_path.parent.mkdir(parents=True, exist_ok=True)
    model.eval()

    with tempfile.TemporaryDirectory() as tmp_dir:
        onnx_path = Path(tmp_dir) / "model.onnx"
        tf_saved_model_dir = Path(tmp_dir) / "tf_saved_model"

        # 1. PyTorch -> ONNX
        export_to_onnx(model, onnx_path)

        # 2. ONNX -> TF SavedModel
        onnx_model = onnx.load(str(onnx_path))
        tf_rep = prepare(onnx_model)
        tf_rep.export_graph(str(tf_saved_model_dir))

        # 3. TF SavedModel -> TFLite
        converter = tf.lite.TFLiteConverter.from_saved_model(str(tf_saved_model_dir))
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        if quantize_fp16:
            converter.target_spec.supported_types = [tf.float16]

        tflite_model = converter.convert()

        with open(output_tflite_path, "wb") as f:
            f.write(tflite_model)

    return output_tflite_path
