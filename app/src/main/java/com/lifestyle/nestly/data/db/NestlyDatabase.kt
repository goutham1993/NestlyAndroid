package com.lifestyle.nestly.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lifestyle.nestly.data.db.dao.ActivityDao
import com.lifestyle.nestly.data.db.dao.FamilyDao
import com.lifestyle.nestly.data.db.dao.ProfileDao
import com.lifestyle.nestly.data.db.dao.ReminderDao
import com.lifestyle.nestly.data.db.entity.ActivityEntity
import com.lifestyle.nestly.data.db.entity.CaregiverEntity
import com.lifestyle.nestly.data.db.entity.FamilyEntity
import com.lifestyle.nestly.data.db.entity.ProfileEntity
import com.lifestyle.nestly.data.db.entity.ReminderEntity

@Database(
    entities = [
        FamilyEntity::class,
        CaregiverEntity::class,
        ProfileEntity::class,
        ActivityEntity::class,
        ReminderEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class NestlyDatabase : RoomDatabase() {
    abstract fun familyDao(): FamilyDao
    abstract fun profileDao(): ProfileDao
    abstract fun activityDao(): ActivityDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var instance: NestlyDatabase? = null

        fun get(context: Context): NestlyDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NestlyDatabase::class.java,
                    "nestly.db",
                ).addMigrations(*ALL_MIGRATIONS).build().also { instance = it }
            }
    }
}
