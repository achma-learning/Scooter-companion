package io.github.achmalearning.scootercompanion.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import io.github.achmalearning.scootercompanion.AppContainer
import io.github.achmalearning.scootercompanion.R
import io.github.achmalearning.scootercompanion.data.db.BikeProfileEntity
import io.github.achmalearning.scootercompanion.data.db.ExpenseEntity
import io.github.achmalearning.scootercompanion.data.db.FuelEntity
import io.github.achmalearning.scootercompanion.data.db.InsurancePolicyEntity
import io.github.achmalearning.scootercompanion.domain.Dates
import io.github.achmalearning.scootercompanion.ui.components.formatMoney
import io.github.achmalearning.scootercompanion.widget.ScooterWidget
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The home-screen widget, drawn at its default 4 x 2 size in light, dark and Arabic. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val outDir = System.getProperty("screenshots.dir").orEmpty().takeIf { it.isNotBlank() }?.let { File(it).apply { mkdirs() } }

    private fun seed() = runBlocking {
        val c = AppContainer.getInstance(context)
        val today = Dates.today()
        c.garageRepository.saveProfile(
            BikeProfileEntity(nickname = "Zitoun", brand = "Kymco", model = "Agility 50 4T", engineCc = 50, purchaseDate = Dates.addMonths(today, -6), monthlyBudget = 400.0)
        )
        c.garageRepository.addOdometer(2890, today)
        c.garageRepository.saveFuel(FuelEntity(date = today, km = 2890, liters = 3.0, cost = 48.0, full = true))
        c.garageRepository.saveExpense(ExpenseEntity(date = today, category = "WASH", title = "Wash", amount = 30.0))
        c.garageRepository.saveInsurance(InsurancePolicyEntity(startDate = Dates.addMonths(today, -11), months = 12, totalPaid = 1500.0))
    }

    private fun draw(name: String): View = runBlocking {
        seed()
        val snapshot = AppContainer.getInstance(context).garageRepository.current()
        val view = ScooterWidget.render(context, snapshot).apply(context, FrameLayout(context))
        val d = context.resources.displayMetrics.density
        val w = (320 * d).toInt()
        val h = (170 * d).toInt()
        view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, w, h)
        outDir?.let { dir ->
            // A grey "wallpaper" margin so the rounded corners show.
            val pad = (12 * d).toInt()
            val bitmap = Bitmap.createBitmap(w + 2 * pad, h + 2 * pad, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.rgb(96, 112, 128))
            canvas.translate(pad.toFloat(), pad.toFloat())
            view.draw(canvas)
            File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        // What the owner reads, checked against the data it comes from.
        assertEquals("Zitoun", view.findViewById<TextView>(R.id.widget_title).text.toString())
        assertEquals(formatMoney(snapshot.ledger.thisMonth), view.findViewById<TextView>(R.id.widget_spent).text.toString())
        assertTrue(view.findViewById<TextView>(R.id.widget_insurance).text.contains(snapshot.coverage.daysLeft.toString()))
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.widget_service).visibility)
        view
    }

    @Test
    @Config(sdk = [34], qualifiers = "xxhdpi")
    fun light() {
        val view = draw("w1-widget")
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.widget_budget).visibility)
    }

    @Test
    @Config(sdk = [34], qualifiers = "night-xxhdpi")
    fun dark() {
        draw("w2-widget-dark")
    }

    @Test
    @Config(sdk = [34], qualifiers = "ar-rMA-xxhdpi")
    fun arabic() {
        val view = draw("w3-widget-ar")
        assertTrue(view.findViewById<TextView>(R.id.widget_service).text.any { it in '؀'..'ۿ' })
    }
}
