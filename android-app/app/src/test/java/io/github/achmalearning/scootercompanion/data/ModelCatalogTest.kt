package io.github.achmalearning.scootercompanion.data

import io.github.achmalearning.scootercompanion.data.models.ModelCatalog
import io.github.achmalearning.scootercompanion.data.models.ScooterModel
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Every scooter-data model parses in the app, and owners' typing finds the right one. */
class ModelCatalogTest {
    private val root = listOf("../../scooter-data", "../scooter-data").map(::File).first { it.isDirectory }
    private val models: List<ScooterModel> = root.walk().filter { it.name == "model.json" }.map {
        Json { ignoreUnknownKeys = true }.decodeFromString(ScooterModel.serializer(), it.readText())
    }.toList()

    private fun match(brand: String, model: String, cc: Int) = ModelCatalog.match(models, brand, model, cc)?.id

    @Test fun everyModelParsesWithAUniqueId() {
        assertTrue(models.size >= 20)
        assertEquals(models.size, models.map { it.id }.toSet().size)
    }

    @Test fun theWaysOwnersTypeAModelFindIt() {
        assertEquals("honda/sh125i", match("Honda", "SH 125i", 125))
        assertEquals("honda/sh125i", match("honda", "sh125", 125))
        assertEquals("honda/sh150i", match("Honda", "SH150i", 157))
        assertEquals("kymco/agility-50-4t", match("Kymco", "Agility 50 4T", 50))
        assertEquals("yamaha/nmax-125", match("Yamaha", "N-MAX", 125))
        assertEquals("honda/x-adv-750", match("Honda", "X-ADV", 745))
    }

    @Test fun theLongestNameWinsBetweenSiblings() {
        assertEquals("sym/symphony-st-125-lc", match("SYM", "Symphony ST 125 LC", 125))
        assertEquals("sym/symphony-st-125-lc", match("SYM", "Symphony ST 125 EFI L/C", 125))
        assertEquals("sym/symphony-st-125-ac", match("SYM", "Symphony ST 125", 125))
    }

    @Test fun lookalikeNamesStayApart() {
        assertEquals("sym/orbit-iii-50", match("SYM", "Orbit III", 50))
        assertEquals("sym/orbit-ii-50", match("SYM", "Orbit II", 50))
        assertEquals("sym/orbit-iii-125", match("SYM", "Orbit 3", 125))
        assertEquals("sym/jet-14-125", match("SYM", "Jet 14", 125))
        assertEquals("sym/jet-14-125-lc", match("SYM", "Jet 14 LC", 125))
        assertEquals("sym/jet-14-50", match("SYM", "Jet 14", 50))
        assertEquals("sym/symphony-125", match("SYM", "Symphony", 125))
        assertEquals("sym/symphony-sr-125", match("SYM", "Symphony SR", 125))
        assertEquals("sym/symphony-sr-50", match("SYM", "Symphony SR", 50))
        assertEquals("sym/fiddle-v-125", match("SYM", "Fiddle 125", 125))
        assertEquals("yamaha/neos-50", match("Yamaha", "Neo's", 50))
        assertEquals("cfmoto/150sc", match("CFMOTO", "150 SC", 149))
        assertEquals("kymco/super-dink-gt-125", match("Kymco", "Superdink 125", 125))
        assertEquals("kymco/agility-s-125", match("Kymco", "Agility S 125", 125))
        assertEquals("kymco/agility-nx-125", match("KYMCO", "agility nx", 125))
        assertEquals("kymco/agility-50-4t", match("Kymco", "Agility 50 4T", 50))
        assertEquals("bmw/c-400-gt", match("BMW", "C400GT", 350))
    }

    @Test fun aDifferentEngineSizeRulesAModelOut() {
        assertEquals("yamaha/xmax-300", match("Yamaha", "XMAX", 292))
        assertEquals("yamaha/xmax-125", match("Yamaha", "XMAX", 125))
        assertNull(match("Kymco", "Agility 125", 125))
        assertNull(match("Honda", "Vision 110", 110))
    }

    @Test fun derivedFiguresAreRight() {
        val sh = models.first { it.id == "honda/sh125i" }.figures
        assertEquals(7.0 / 2.2 * 100, sh.rangeKm!!, 0.01)
        assertEquals(9.6 * 1000 / 138, sh.powerToWeight!!, 0.01)
    }
}
