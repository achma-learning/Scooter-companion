package com.example.kymcoagility50.data.db

import androidx.room.TypeConverter
import com.example.kymcoagility50.data.model.OcrStatus
import com.example.kymcoagility50.data.model.PdfSource

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
