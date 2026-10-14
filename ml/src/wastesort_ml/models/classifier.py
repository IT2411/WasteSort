"""WasteSort Mobile Classifier Backbone based on MobileNetV3-Small."""

import torch
import torch.nn as nn
from torchvision.models import mobilenet_v3_small, MobileNet_V3_Small_Weights
from wastesort_ml.dataset.taxonomy import NUM_CLASSES


class WasteClassifier(nn.Module):
    def __init__(self, num_classes: int = NUM_CLASSES, pretrained: bool = True):
        super().__init__()
        weights = MobileNet_V3_Small_Weights.DEFAULT if pretrained else None
        self.backbone = mobilenet_v3_small(weights=weights)

        # Replace classification head
        in_features = self.backbone.classifier[3].in_features
        self.backbone.classifier[3] = nn.Linear(in_features, num_classes)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        """
        Input: Tensor of shape [B, 3, 224, 224]
        Output: Logits tensor of shape [B, 16]
        """
        return self.backbone(x)

    def predict_probabilities(self, x: torch.Tensor) -> torch.Tensor:
        """Outputs Softmax probabilities [B, 16] per Model Contract."""
        logits = self.forward(x)
        return torch.softmax(logits, dim=-1)
