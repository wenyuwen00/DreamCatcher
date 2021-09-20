package edu.vt.cs.cs5254.dreamcatcher

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import java.io.File
import java.util.UUID

class DreamDetailViewModel : ViewModel() {
    private val dreamRepository = DreamRepository.get()
    private var dreamIdLiveData = MutableLiveData<UUID>()

    var dreamLiveData: LiveData<DreamWithEntries> =
        Transformations.switchMap(dreamIdLiveData) { dreamId ->
            dreamRepository.getDreamWithEntries(dreamId)
        }

    fun loadDream(dreamId: UUID) {
        dreamIdLiveData.value = dreamId
    }

    fun saveDream(dream: DreamWithEntries) {
        dreamRepository.updateDreamWithEntries(dream)
    }

    fun getPhotoFile(dream: DreamWithEntries): File {
        return dreamRepository.getPhotoFile(dream.dream)
    }
}