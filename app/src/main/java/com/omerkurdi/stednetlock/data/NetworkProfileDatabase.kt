package com.omerkurdi.stednetlock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [NetworkProfile::class], version = 2, exportSchema = false)
abstract class NetworkProfileDatabase : RoomDatabase() {
    abstract fun networkProfileDao(): NetworkProfileDao

    companion object {
        @Volatile
        private var INSTANCE: NetworkProfileDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): NetworkProfileDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NetworkProfileDatabase::class.java,
                    "network_profile_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.networkProfileDao())
                    }
                }
            }

            suspend fun populateDatabase(dao: NetworkProfileDao) {
                // Pre-populate system default network profiles
                dao.insertProfile(
                    NetworkProfile(
                        name = "Auto / Reset",
                        is2gEnabled = true,
                        is3gEnabled = true,
                        is4gEnabled = true,
                        is5gEnabled = true,
                        isDefaultAuto = true,
                        isSystemDefault = true,
                        showOnWidget = true
                    )
                )
                dao.insertProfile(
                    NetworkProfile(
                        name = "5G Only",
                        is2gEnabled = false,
                        is3gEnabled = false,
                        is4gEnabled = false,
                        is5gEnabled = true,
                        isDefaultAuto = false,
                        isSystemDefault = true,
                        showOnWidget = true
                    )
                )
                dao.insertProfile(
                    NetworkProfile(
                        name = "5G Preferred",
                        is2gEnabled = false,
                        is3gEnabled = false,
                        is4gEnabled = true,
                        is5gEnabled = true,
                        isDefaultAuto = false,
                        isSystemDefault = true,
                        showOnWidget = false
                    )
                )
                dao.insertProfile(
                    NetworkProfile(
                        name = "4G Only (LTE)",
                        is2gEnabled = false,
                        is3gEnabled = false,
                        is4gEnabled = true,
                        is5gEnabled = false,
                        isDefaultAuto = false,
                        isSystemDefault = true,
                        showOnWidget = true
                    )
                )
                dao.insertProfile(
                    NetworkProfile(
                        name = "3G Only",
                        is2gEnabled = false,
                        is3gEnabled = true,
                        is4gEnabled = false,
                        is5gEnabled = false,
                        isDefaultAuto = false,
                        isSystemDefault = true
                    )
                )
                dao.insertProfile(
                    NetworkProfile(
                        name = "2G Only (Battery Saver)",
                        is2gEnabled = true,
                        is3gEnabled = false,
                        is4gEnabled = false,
                        is5gEnabled = false,
                        isDefaultAuto = false,
                        isSystemDefault = true
                    )
                )
            }
        }
    }
}
