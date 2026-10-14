package org.wastesort.model

data class ManualWasteOption(
    val classId: String,
    val title: String,
    val examples: String,
    val expectedStream: WasteStream,
)

object ManualWasteCatalog {
    val items = listOf(
        ManualWasteOption(
            classId = "food_organic",
            title = "Food & Kitchen Scraps",
            examples = "Fruit peels, leftovers, tea grounds, eggshells",
            expectedStream = WasteStream.WET,
        ),
        ManualWasteOption(
            classId = "garden_waste",
            title = "Garden & Plant Waste",
            examples = "Leaves, grass, twigs, dried flowers",
            expectedStream = WasteStream.WET,
        ),
        ManualWasteOption(
            classId = "mixed_composite_packaging",
            title = "Chips & Snack Packets (Kurkure, Lays)",
            examples = "Multi-layer foil packets, Tetra Paks, biscuit wrappers",
            expectedStream = WasteStream.DRY,
        ),
        ManualWasteOption(
            classId = "plastic_bottle_container",
            title = "Plastic Bottles & Jars",
            examples = "Water/soda bottles, shampoo bottles, milk jugs",
            expectedStream = WasteStream.DRY,
        ),
        ManualWasteOption(
            classId = "plastic_film_flexible",
            title = "Plastic Bags & Wraps",
            examples = "Grocery bags, bubble wrap, cling film",
            expectedStream = WasteStream.DRY,
        ),
        ManualWasteOption(
            classId = "paper",
            title = "Paper & Documents",
            examples = "Newspapers, flyers, notebooks, envelopes",
            expectedStream = WasteStream.DRY,
        ),
        ManualWasteOption(
            classId = "cardboard",
            title = "Cardboard & Cartons",
            examples = "Amazon delivery boxes, cereal boxes, egg cartons",
            expectedStream = WasteStream.DRY,
        ),
        ManualWasteOption(
            classId = "metal_can_tin",
            title = "Metal Cans & Foil",
            examples = "Soda cans, food tins, clean aluminum trays",
            expectedStream = WasteStream.DRY,
        ),
        ManualWasteOption(
            classId = "glass_bottle_jar",
            title = "Glass Bottles & Jars",
            examples = "Sauce jars, beverage bottles",
            expectedStream = WasteStream.DRY,
        ),
        ManualWasteOption(
            classId = "textile",
            title = "Clothes & Fabric",
            examples = "Old clothes, towels, cloth rags",
            expectedStream = WasteStream.DRY,
        ),
        ManualWasteOption(
            classId = "battery",
            title = "Batteries (Hazardous)",
            examples = "AA/AAA cells, phone batteries, button cells",
            expectedStream = WasteStream.OTHER,
        ),
        ManualWasteOption(
            classId = "e_waste",
            title = "Electronic Waste",
            examples = "Cables, chargers, broken remotes, small gadgets",
            expectedStream = WasteStream.OTHER,
        ),
        ManualWasteOption(
            classId = "sanitary_medical",
            title = "Sanitary & Medical Items",
            examples = "Used masks, bandages, diapers, wipes",
            expectedStream = WasteStream.OTHER,
        ),
        ManualWasteOption(
            classId = "hazardous_chemical",
            title = "Chemicals & Aerosols",
            examples = "Pesticide cans, spray paints, cleaning chemicals",
            expectedStream = WasteStream.OTHER,
        ),
    )
}
