package com.lifestyle.nestly.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        listOf(
            "photoUri TEXT",
            "comboGroupId TEXT",
            "diaper_hasRash INTEGER",
            "solids_mealType TEXT",
            "solids_foods TEXT",
            "routine_name TEXT",
            "babyFirst_title TEXT",
            "medical_temperatureC REAL",
            "medical_medicationName TEXT",
            "medical_dose REAL",
            "medical_doseUnit TEXT",
            "vaccine_vaccines TEXT",
        ).forEach { db.execSQL("ALTER TABLE activity ADD COLUMN $it") }
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reminder` (
                `id` TEXT NOT NULL,
                `familyId` TEXT NOT NULL,
                `profileId` TEXT,
                `kind` TEXT NOT NULL,
                `intervalMinutes` INTEGER NOT NULL,
                `enabled` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`familyId`) REFERENCES `family`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`profileId`) REFERENCES `profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_reminder_profileId_kind` ON `reminder` (`profileId`, `kind`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_familyId` ON `reminder` (`familyId`)")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
