package com.bakertelekom.portugaltowers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bakertelekom.portugaltowers.data.TowerRepository
import com.bakertelekom.portugaltowers.domain.MapTowerCluster
import com.bakertelekom.portugaltowers.domain.Tower
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TowerRepository(application.applicationContext)
    private val mutableState = MutableStateFlow<AppState>(AppState.Loading)
    val state: StateFlow<AppState> = mutableState.asStateFlow()

    init {
        loadTowers()
    }

    fun loadTowers() {
        mutableState.value = AppState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.prepareDatabase()
            mutableState.value = result.fold(
                onSuccess = { count ->
                    if (count == 0) AppState.Empty else AppState.Ready(count)
                },
                onFailure = { error ->
                    AppState.Error(error.message ?: "Nao foi possivel carregar a base local.")
                },
            )
        }
    }

    suspend fun allTowers(): List<Tower> = withContext(Dispatchers.IO) {
        repository.loadTowers().getOrThrow()
    }

    suspend fun towersInBounds(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        limit: Int,
    ): List<Tower> = withContext(Dispatchers.IO) {
        repository.loadTowersInBounds(minLat, maxLat, minLon, maxLon, limit).getOrThrow()
    }

    suspend fun macroClusters(cellSize: Double): List<MapTowerCluster> = withContext(Dispatchers.IO) {
        repository.loadMacroClusters(cellSize).getOrThrow()
    }
}

sealed interface AppState {
    data object Loading : AppState
    data object Empty : AppState
    data class Ready(val towerCount: Int) : AppState
    data class Error(val message: String) : AppState
}
