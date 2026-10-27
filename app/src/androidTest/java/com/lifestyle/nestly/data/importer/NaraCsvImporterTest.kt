package com.lifestyle.nestly.data.importer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lifestyle.nestly.data.db.NestlyDatabase
import com.lifestyle.nestly.data.db.entity.ActivityType
import com.lifestyle.nestly.data.db.entity.MilkType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NaraCsvImporterTest {
    private lateinit var db: NestlyDatabase
    private lateinit var importer: NaraCsvImporter

    private val expected = mapOf(
        ActivityType.BOTTLE_FEED to 1125,
        ActivityType.PUMP to 1107,
        ActivityType.BREASTFEED to 65,
        ActivityType.GROWTH to 22,
        ActivityType.DIAPER to 9,
        ActivityType.MILESTONE to 3,
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NestlyDatabase::class.java).build()
        importer = NaraCsvImporter(db)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun importFixture() =
        InstrumentationRegistry.getInstrumentation().context.assets.open("nara_export.csv").use { importer.import(it) }

    private suspend fun dbCounts() = db.activityDao().countsByType().associate { it.type to it.count }

    @Test
    fun importsAllActivitiesAndIsIdempotent() = runTest {
        val first = importFixture()
        assertEquals(expected, first.activityCounts)
        assertEquals(expected, dbCounts())

        importFixture()
        assertEquals(expected, dbCounts())

        val profiles = db.profileDao().observeAll().first()
        assertEquals(listOf("Rithan"), profiles.map { it.name })
        val familyId = profiles.single().familyId
        assertEquals(setOf("Gouthan", "Pravalika"), db.familyDao().observeCaregivers(familyId).first().map { it.name }.toSet())
    }

    @Test
    fun storesTypedDetails() = runTest {
        importFixture()
        val bottle = db.activityDao().get("t-P2tkIJaWIjbcK9jl8gD")!!
        assertEquals(MilkType.BREAST_MILK, bottle.bottle!!.milkType)
        assertEquals(150.0, bottle.bottle!!.totalMl!!, 0.0)
        assertNotNull(bottle.createdById)
        assertNull(bottle.pump)

        val pump = db.activityDao().get("t-P2te5BCg8HT9c-qr3r9")!!
        assertNull(pump.profileId)
        assertEquals(130.0, pump.pump!!.totalMl!!, 0.0)
        assertNull(pump.bottle)
    }
}
