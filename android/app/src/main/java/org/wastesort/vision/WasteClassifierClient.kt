package org.wastesort.vision

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.wastesort.model.ClassificationOutput
import org.wastesort.model.DecisionStatus
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class WasteClassifierClient(
    context: Context,
    modelAssetPath: String = "models/waste_classifier_v1.tflite",
) {
    private val interpreter: Interpreter
    private val inputBuffer: ByteBuffer
    private val outputBuffer: Array<FloatArray>

    init {
        val assetFileDescriptor = context.assets.openFd(modelAssetPath)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }
        interpreter = Interpreter(modelBuffer, options)

        // 1 * 224 * 224 * 3 channels * 4 bytes per float
        inputBuffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
        }
        outputBuffer = Array(1) { FloatArray(CLASS_NAMES.size) }
    }

    @Synchronized
    fun classify(bitmap: Bitmap): ClassificationOutput {
        val scaled = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        inputBuffer.rewind()

        val intValues = IntArray(224 * 224)
        scaled.getPixels(intValues, 0, 224, 0, 0, 224, 224)

        for (pixel in intValues) {
            val r = (pixel shr 16 and 0xFF) / 255.0f
            val g = (pixel shr 8 and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            // Channel-wise ImageNet normalization
            inputBuffer.putFloat((r - 0.485f) / 0.229f)
            inputBuffer.putFloat((g - 0.456f) / 0.224f)
            inputBuffer.putFloat((b - 0.406f) / 0.225f)
        }

        interpreter.run(inputBuffer, outputBuffer)
        val probabilities = outputBuffer[0]

        return evaluateUncertainty(probabilities)
    }

    private fun evaluateUncertainty(probabilities: FloatArray): ClassificationOutput {
        var maxIdx = 0
        var maxProb = 0f
        for (i in probabilities.indices) {
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i]
                maxIdx = i
            }
        }

        val topClass = CLASS_NAMES[maxIdx]
        val bgIdx = CLASS_NAMES.indexOf("background_no_item")

        // 1. Background Suppression Check
        if (maxIdx == bgIdx || (bgIdx in probabilities.indices && probabilities[bgIdx] >= 0.40f)) {
            return ClassificationOutput("background_no_item", probabilities[bgIdx], DecisionStatus.NO_OBJECT_DETECTED)
        }

        // 2. Hazard Safety Override Check
        for (hazardName in HAZARD_CLASSES) {
            val hIdx = CLASS_NAMES.indexOf(hazardName)
            if (hIdx >= 0 && probabilities[hIdx] >= 0.50f) {
                return ClassificationOutput(hazardName, probabilities[hIdx], DecisionStatus.HAZARD_FLAGGED)
            }
        }

        // 3. Confidence Threshold Check
        if (maxProb < 0.70f) {
            return ClassificationOutput(topClass, maxProb, DecisionStatus.NOT_SURE)
        }

        return ClassificationOutput(topClass, maxProb, DecisionStatus.CONFIDENT)
    }

    fun close() {
        interpreter.close()
    }

    companion object {
        val CLASS_NAMES = listOf(
            "food_organic", "garden_waste", "paper", "cardboard",
            "plastic_bottle_container", "plastic_film_flexible", "glass_bottle_jar",
            "metal_can_tin", "textile", "e_waste", "battery", "sanitary_medical",
            "hazardous_chemical", "mixed_composite_packaging",
            "unknown_household_item", "background_no_item",
        )

        val HAZARD_CLASSES = listOf(
            "battery",
            "sanitary_medical",
            "hazardous_chemical",
        )
    }
}
