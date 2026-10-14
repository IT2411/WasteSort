package org.wastesort.rules

import android.content.Context
import org.json.JSONObject
import org.wastesort.model.ClassificationOutput
import org.wastesort.model.DecisionStatus
import org.wastesort.model.DisposalRecommendation
import org.wastesort.model.WasteStream

class RulesEngine(private val profileJson: JSONObject) {

    fun resolve(classification: ClassificationOutput): DisposalRecommendation {
        if (classification.status == DecisionStatus.NO_OBJECT_DETECTED) {
            return DisposalRecommendation(
                stream = WasteStream.NOT_SURE,
                identifiedItem = "No object detected",
                instruction = "Point the camera reticle directly at a single waste item.",
                confidence = classification.confidence,
                status = classification.status,
            )
        }

        if (classification.status == DecisionStatus.NOT_SURE) {
            return DisposalRecommendation(
                stream = WasteStream.NOT_SURE,
                identifiedItem = formatItemName(classification.classId),
                instruction = "Could not identify with high confidence. Please retake or select category manually.",
                confidence = classification.confidence,
                status = classification.status,
            )
        }

        val mappings = profileJson.optJSONObject("mappings")
        val itemRule = mappings?.optJSONObject(classification.classId)

        val streamStr = itemRule?.optString("stream", "NOT_SURE") ?: "NOT_SURE"
        val instruction = itemRule?.optString("instruction", "Follow local municipal disposal guidelines.")
            ?: "Follow local municipal disposal guidelines."

        val stream = try {
            WasteStream.valueOf(streamStr)
        } catch (_: IllegalArgumentException) {
            WasteStream.NOT_SURE
        }

        return DisposalRecommendation(
            stream = stream,
            identifiedItem = formatItemName(classification.classId),
            instruction = instruction,
            confidence = classification.confidence,
            status = classification.status,
        )
    }

    private fun formatItemName(rawClassId: String): String {
        return rawClassId.replace("_", " ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    companion object {
        fun fromAssets(context: Context, assetPath: String = "rules/urban_general_v1.json"): RulesEngine {
            val jsonString = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            return RulesEngine(JSONObject(jsonString))
        }
    }
}
