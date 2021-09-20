package edu.vt.cs.cs5254.dreamcatcher

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

private const val DATABASE_NAME = "dream-database"

class DreamRepository private constructor(context: Context) {

    private val initializeDreamDatabaseCallback: RoomDatabase.Callback =
        object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                executor.execute {
                    deleteAllDreamsInDatabase()
                    deleteAllDreamEntriesInDatabase()
                }
            }
        }

    private val database: DreamDatabase =
        Room.databaseBuilder(context.applicationContext, DreamDatabase::class.java, DATABASE_NAME)
            .addCallback(initializeDreamDatabaseCallback).build()

    private val dreamDao = database.dreamDao()
    private val executor = Executors.newSingleThreadExecutor()
    private val filesDir = context.applicationContext.filesDir
    fun getPhotoFile(dream: Dream): File = File(filesDir, dream.photoFileName)


    fun deleteAllDreamsInDatabase() {
        executor.execute {
            dreamDao.deleteAllDreamsInDatabase()
        }
    }

    fun getDreams(): LiveData<List<Dream>> {
        val dreams = dreamDao.getDreams()
        return dreams
    }

    fun addDreamWithEntries(dream: DreamWithEntries) {
        executor.execute {
            dreamDao.addDreamWithEntries(dream)
        }
    }

    fun getDreamWithEntries(dreamId: UUID): LiveData<DreamWithEntries> =
        dreamDao.getDreamWithEntries(dreamId)

    fun updateDreamWithEntries(dream: DreamWithEntries) {
        executor.execute { dreamDao.updateDreamWithEntries(dream) }
    }

    fun deleteAllDreamEntriesInDatabase() {
        executor.execute {
            dreamDao.deleteAllDreamEntriesInDatabase()
        }
    }

    companion object {

        private var INSTANCE: DreamRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = DreamRepository(context)
            }
        }

        fun get(): DreamRepository {
            return INSTANCE ?: throw IllegalStateException("DreamRepository must be initialized")
        }
    }
}
