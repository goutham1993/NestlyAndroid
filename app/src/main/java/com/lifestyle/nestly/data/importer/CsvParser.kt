package com.lifestyle.nestly.data.importer

import java.io.Reader

/** Minimal RFC 4180 parser: quoted fields, escaped quotes (`""`), and newlines inside quotes. */
object CsvParser {
    fun parse(reader: Reader): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var fieldStarted = false

        fun endField() {
            row.add(field.toString())
            field.setLength(0)
            fieldStarted = false
        }

        fun endRow() {
            endField()
            if (!(row.size == 1 && row[0].isEmpty())) rows.add(row)
            row = mutableListOf()
        }

        val text = reader.readText().removePrefix("\uFEFF")
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> if (!fieldStarted && field.isEmpty()) inQuotes = true else field.append(c)
                    ',' -> endField()
                    '\r' -> if (i + 1 < text.length && text[i + 1] == '\n') {
                        i++
                        endRow()
                    } else {
                        endRow()
                    }
                    '\n' -> endRow()
                    else -> {
                        field.append(c)
                        fieldStarted = true
                    }
                }
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) endRow()
        return rows
    }

    /** Parses with the first row as header; each record maps header name to value (blank values dropped). */
    fun parseRecords(reader: Reader): List<Map<String, String>> {
        val rows = parse(reader)
        if (rows.isEmpty()) return emptyList()
        val header = rows.first()
        return rows.drop(1).map { values ->
            buildMap {
                header.forEachIndexed { index, name ->
                    val value = values.getOrNull(index)
                    if (!value.isNullOrBlank()) put(name, value)
                }
            }
        }
    }
}
