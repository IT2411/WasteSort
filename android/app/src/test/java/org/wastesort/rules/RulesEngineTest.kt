package org.wastesort.rules

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.wastesort.model.ClassificationOutput
import org.wastesort.model.DecisionStatus
import org.wastesort.model.WasteStream

class RulesEngineTest {

    private lateinit var rulesEngine: RulesEngine

    @Before
    fun setUp() {
        val mockProfileJson = JSONObject(
            """
            {
              "profile_id": "test_profile",
              "mappings": {
                "food_organic": { "stream": "WET", "instruction": "Compostable organic" },
                "plastic_bottle_container": { "stream": "DRY", "instruction": "Recycle clean" },
                "battery": { "stream": "OTHER", "instruction": "Hazardous disposal" }
              }
            }
            """.trimIndent(),
        )
        rulesEngine = RulesEngine(mockProfileJson)
    }

    @Test
    fun testWetWasteResolution() {
        val output = ClassificationOutput("food_organic", 0.95f, DecisionStatus.CONFIDENT)
        val result = rulesEngine.resolve(output)
        assertEquals(WasteStream.WET, result.stream)
        assertEquals("Compostable organic", result.instruction)
    }

    @Test
    fun testDryWasteResolution() {
        val output = ClassificationOutput("plastic_bottle_container", 0.85f, DecisionStatus.CONFIDENT)
        val result = rulesEngine.resolve(output)
        assertEquals(WasteStream.DRY, result.stream)
        assertEquals("Recycle clean", result.instruction)
    }

    @Test
    fun testHazardousOtherResolution() {
        val output = ClassificationOutput("battery", 0.75f, DecisionStatus.HAZARD_FLAGGED)
        val result = rulesEngine.resolve(output)
        assertEquals(WasteStream.OTHER, result.stream)
        assertEquals("Hazardous disposal", result.instruction)
    }

    @Test
    fun testNotSureResolution() {
        val output = ClassificationOutput("unknown_household_item", 0.40f, DecisionStatus.NOT_SURE)
        val result = rulesEngine.resolve(output)
        assertEquals(WasteStream.NOT_SURE, result.stream)
    }

    @Test
    fun testNoObjectDetectedResolution() {
        val output = ClassificationOutput("background_no_item", 0.50f, DecisionStatus.NO_OBJECT_DETECTED)
        val result = rulesEngine.resolve(output)
        assertEquals(WasteStream.NOT_SURE, result.stream)
        assertEquals("No object detected", result.identifiedItem)
    }
}
