package io.github.achmalearning.scootercompanion.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import io.github.achmalearning.scootercompanion.ui.screens.onboarding.OnboardingScreen
import io.github.achmalearning.scootercompanion.ui.theme.ScooterTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The first screen a new owner sees, in each language the app ships. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class LocalizedScreensTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun renderWelcome(): LayoutDirection {
        var direction = LayoutDirection.Ltr
        compose.setContent {
            direction = LocalLayoutDirection.current
            ScooterTheme { Surface(color = MaterialTheme.colorScheme.background) { OnboardingScreen(onDone = {}) } }
        }
        compose.waitForIdle()
        return direction
    }

    @Test @Config(qualifiers = "en")
    fun english() {
        assertEquals(LayoutDirection.Ltr, renderWelcome())
        compose.onNodeWithText("Add my scooter").assertExists()
    }

    @Test @Config(qualifiers = "fr")
    fun french() {
        renderWelcome()
        compose.onNodeWithText("Ajouter mon scooter").assertExists()
        compose.onNodeWithText("J'ai un fichier de sauvegarde").assertExists()
    }

    @Test @Config(qualifiers = "ar-rMA-ldrtl")
    fun arabicIsRightToLeft() {
        assertEquals(LayoutDirection.Rtl, renderWelcome())
        compose.onNodeWithText("إضافة السكوتر").assertExists()
    }
}
