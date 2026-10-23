package io.github.achmalearning.scootercompanion.data.db

import androidx.room.TypeConverter
import io.github.achmalearning.scootercompanion.data.model.OcrStatus
import io.github.achmalearning.scootercompanion.data.model.PdfSource

class Converters {
    @TypeConverter
    fun fromOcrStatus(status: OcrStatus): String = status.name

    @TypeConverter
    fun toOcrStatus(value: String): OcrStatus = OcrStatus.valueOf(value)

    @TypeConverter
    fun fromPdfSource(pdf: PdfSource): String = pdf.name

    @TypeConverter
    fun toPdfSource(value: String): PdfSource = PdfSource.valueOf(value)
}
