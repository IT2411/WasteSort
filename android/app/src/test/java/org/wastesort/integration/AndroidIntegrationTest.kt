package org.wastesort.integration

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wastesort.model.ClassificationOutput
import org.wastesort.model.DecisionStatus
import org.wastesort.model.WasteStream
import org.wastesort.rules.RulesEngine
import org.wastesort.vision.WasteClassifierClient
import java.io.File
import java.util.zip.ZipFile

class AndroidIntegrationTest {

    @Test
    fun testManifestAirGapCompliance() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())

        val content = manifestFile.readText()

        // Core Privacy Gate: Internet permission must NEVER be present
        assertFalse(
            "Air-Gap Violation: INTERNET permission found in AndroidManifest.xml",
            content.contains("android.permission.INTERNET"),
        )
        assertFalse(
            "Air-Gap Violation: ACCESS_NETWORK_STATE found in AndroidManifest.xml",
            content.contains("android.permission.ACCESS_NETWORK_STATE"),
        )
        assertTrue(
            "Camera permission must be requested",
            content.contains("android.permission.CAMERA"),
        )
    }

    @Test
    fun testApkAssetPackagingAndUncompressedTflite() {
        val apkFile = File("build/outputs/apk/debug/app-debug.apk")
        assertTrue("Debug APK must exist. Run ./gradlew assembleDebug first", apkFile.exists())

        ZipFile(apkFile).use { zip ->
            val tfliteEntry = zip.getEntry("assets/models/waste_classifier_v1.tflite")
            assertNotNull("waste_classifier_v1.tflite must be present in APK assets", tfliteEntry)

            // Critical performance check: TFLite model must be STORED (uncompressed) for direct mmap
            assertEquals(
                "TFLite asset must be STORED (uncompressed) for zero-copy memory mapping",
                java.util.zip.ZipEntry.STORED,
                tfliteEntry.method,
            )

            val rulesEntry = zip.getEntry("assets/rules/urban_general_v1.json")
            assertNotNull("urban_general_v1.json must be present in APK assets", rulesEntry)
        }
    }

    @Test
    fun testAllTaxonomyClassesMapInProductionProfile() {
        val rulesFile = File("src/main/assets/rules/urban_general_v1.json")
        assertTrue("Production rules file must exist in assets", rulesFile.exists())

        val profileJson = JSONObject(rulesFile.readText())
        val rulesEngine = RulesEngine(profileJson)

        // Verify all 16 classes from taxonomy map cleanly without crashing
        for (classId in WasteClassifierClient.CLASS_NAMES) {
            val status = when (classId) {
                "background_no_item" -> DecisionStatus.NO_OBJECT_DETECTED
                in WasteClassifierClient.HAZARD_CLASSES -> DecisionStatus.HAZARD_FLAGGED
                else -> DecisionStatus.CONFIDENT
            }

            val output = ClassificationOutput(classId = classId, confidence = 0.90f, status = status)
            val recommendation = rulesEngine.resolve(output)

            assertNotNull("Recommendation cannot be null for $classId", recommendation)
            assertTrue("Instruction must not be empty for $classId", recommendation.instruction.isNotBlank())
            assertTrue("Identified item must not be blank for $classId", recommendation.identifiedItem.isNotBlank())

            when (classId) {
                "food_organic", "garden_waste" -> {
                    assertEquals("$classId should map to WET", WasteStream.WET, recommendation.stream)
                }
                "battery", "sanitary_medical", "hazardous_chemical", "e_waste" -> {
                    assertEquals("$classId should map to OTHER", WasteStream.OTHER, recommendation.stream)
                }
                "paper", "cardboard", "plastic_bottle_container", "glass_bottle_jar", "metal_can_tin" -> {
                    assertEquals("$classId should map to DRY", WasteStream.DRY, recommendation.stream)
                }
            }
        }
    }

    @Test
    fun testResilienceToCorruptedProfileJson() {
        val corruptedJson = JSONObject("{}")
        val rulesEngine = RulesEngine(corruptedJson)

        val output = ClassificationOutput("plastic_bottle_container", 0.95f, DecisionStatus.CONFIDENT)
        val recommendation = rulesEngine.resolve(output)

        // Should gracefully fall back to NOT_SURE without crashing
        assertEquals(WasteStream.NOT_SURE, recommendation.stream)
    }

    @Test
    fun testUnknownItemFallback() {
        val rulesFile = File("src/main/assets/rules/urban_general_v1.json")
        val rulesEngine = RulesEngine(JSONObject(rulesFile.readText()))

        val output = ClassificationOutput("alien_space_junk_xyz", 0.99f, DecisionStatus.CONFIDENT)
        val recommendation = rulesEngine.resolve(output)

        assertEquals(WasteStream.NOT_SURE, recommendation.stream)
    }
}
