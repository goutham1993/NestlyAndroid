package com.lifestyle.nestly.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lifestyle.nestly.data.db.entity.ActivityType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NestlyDatabase::class.java,
    )

    @Test
    fun migrate1To2KeepsRows() = runTest {
        helper.createDatabase(dbName, 1).apply {
            execSQL("INSERT INTO family (id, name, createdAt, updatedAt, syncState) VALUES ('f', NULL, 0, 0, 'SYNCED')")
            execSQL(
                """
                INSERT INTO activity (id, familyId, profileId, type, startTime, timeZone, note,
                    bottle_milkType, bottle_breastMilkMl, bottle_totalMl, createdAt, updatedAt, syncState)
                VALUES ('a', 'f', NULL, 'BOTTLE_FEED', 1000, 'UTC', 'hello', 'BREAST_MILK', 150.0, 150.0, 1000, 1000, 'SYNCED')
                """,
            )
            execSQL(
                """
                INSERT INTO activity (id, familyId, profileId, type, startTime, timeZone,
                    diaper_diaperType, createdAt, updatedAt, syncState)
                VALUES ('d', 'f', NULL, 'DIAPER', 2000, 'UTC', 'DIRTY', 2000, 2000, 'SYNCED')
                """,
            )
            close()
        }

        helper.runMigrationsAndValidate(dbName, 2, true, *ALL_MIGRATIONS).close()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.databaseBuilder(context, NestlyDatabase::class.java, dbName)
            .addMigrations(*ALL_MIGRATIONS)
            .build()
        try {
            val bottle = db.activityDao().get("a")!!
            assertEquals(ActivityType.BOTTLE_FEED, bottle.type)
            assertEquals(150.0, bottle.bottle!!.totalMl!!, 0.0)
            assertEquals("hello", bottle.note)
            assertNull(bottle.photoUri)
            assertNull(bottle.solids)

            val diaper = db.activityDao().get("d")!!
            assertNull(diaper.diaper!!.hasRash)
        } finally {
            db.close()
        }
    }

    @Test
    fun migrate2To3AddsReminderTable() = runTest {
        helper.createDatabase(dbName + "-v2", 2).apply {
            execSQL("INSERT INTO family (id, name, createdAt, updatedAt, syncState) VALUES ('f', NULL, 0, 0, 'SYNCED')")
            execSQL(
                """
                INSERT INTO activity (id, familyId, profileId, type, startTime, timeZone,
                    bottle_milkType, bottle_totalMl, createdAt, updatedAt, syncState)
                VALUES ('a', 'f', NULL, 'BOTTLE_FEED', 1000, 'UTC', 'BREAST_MILK', 120.0, 1000, 1000, 'SYNCED')
                """,
            )
            close()
        }

        helper.runMigrationsAndValidate(dbName + "-v2", 3, true, *ALL_MIGRATIONS).close()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.databaseBuilder(context, NestlyDatabase::class.java, dbName + "-v2")
            .addMigrations(*ALL_MIGRATIONS)
            .build()
        try {
            val bottle = db.activityDao().get("a")!!
            assertEquals(120.0, bottle.bottle!!.totalMl!!, 0.0)
            assertEquals(0, db.reminderDao().getEnabled().size)
        } finally {
            db.close()
        }
    }
}
