package org.wastesort.model

enum class WasteStream(val displayName: String, val colorHex: String) {
    WET("Wet Waste", "#2E7D32"),
    DRY("Dry Waste", "#1565C0"),
    OTHER("Other / Special Handling", "#D84315"),
    NOT_SURE("Not Sure", "#757575"),
}

data class ClassificationOutput(
    val classId: String,
    val confidence: Float,
    val status: DecisionStatus,
)

enum class DecisionStatus {
    CONFIDENT,
    NOT_SURE,
    NO_OBJECT_DETECTED,
    HAZARD_FLAGGED,
}

data class DisposalRecommendation(
    val stream: WasteStream,
    val identifiedItem: String,
    val instruction: String,
    val confidence: Float,
    val status: DecisionStatus,
)
