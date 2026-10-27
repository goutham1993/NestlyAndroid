package com.lifestyle.nestly.data.db

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun listToString(value: List<String>?): String? = value?.joinToString(SEPARATOR)

    @TypeConverter
    fun stringToList(value: String?): List<String>? =
        value?.let { if (it.isEmpty()) emptyList() else it.split(SEPARATOR) }

    private companion object {
        /** ASCII unit separator; never typed by users, so items can contain commas. */
        const val SEPARATOR = "\u001F"
    }
}
