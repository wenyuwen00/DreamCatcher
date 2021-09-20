package edu.vt.cs.cs5254.dreamcatcher

import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.*

@Dao
interface DreamDao {

// ----------------------------------------------------------
// Dream functions
// ----------------------------------------------------------

    @Query("SELECT * FROM dream")
    fun getDreams(): LiveData<List<Dream>>

    @Query("SELECT * FROM dream WHERE id=(:id)")
    fun getDream(id: UUID): LiveData<Dream?>

    @Insert
    fun addDream(dream: Dream)

    @Update
    fun updateDream(dream: Dream)


// ----------------------------------------------------------
// DreamEntry functions
// ----------------------------------------------------------

    @Query("SELECT * FROM dream_entry")
    fun getDreamEntries(): LiveData<List<DreamEntry>>

    @Query("SELECT * FROM dream_entry WHERE id=(:id)")
    fun getDreamEntry(id: UUID): LiveData<DreamEntry?>

    @Insert
    fun addDreamEntry(dreamEntry: DreamEntry)

    @Update
    fun updateDreamEntry(dreamEntry: DreamEntry)

    @Query("DELETE FROM dream_entry WHERE dreamId=(:dreamId)")
    fun deleteDreamEntries(dreamId: UUID)


// ----------------------------------------------------------
// DreamWithEntries functions
// ----------------------------------------------------------

    @Query("SELECT * FROM dream WHERE id=(:dreamId)")
    fun getDreamWithEntries(dreamId: UUID): LiveData<DreamWithEntries>

    @Transaction
    fun updateDreamWithEntries(dreamWithEntries: DreamWithEntries) {
        val theDream = dreamWithEntries.dream
        val theEntries = dreamWithEntries.dreamEntries
        updateDream(dreamWithEntries.dream)
        deleteDreamEntries(theDream.id)
        theEntries.forEach { e -> addDreamEntry(e) }
    }

    @Transaction
    fun addDreamWithEntries(dreamWithEntries: DreamWithEntries) {
        addDream(dreamWithEntries.dream)
        dreamWithEntries.dreamEntries.forEach { e -> addDreamEntry(e) }
    }

// ----------------------------------------------------------
// Nuclear Options!
// ----------------------------------------------------------

    @Query("DELETE FROM dream")
    fun deleteAllDreamsInDatabase()

    @Query("DELETE FROM dream_entry")
    fun deleteAllDreamEntriesInDatabase()

}