package com.bakertelekom.portugaltowers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bakertelekom.portugaltowers.data.TowerRepository
import com.bakertelekom.portugaltowers.domain.Tower
import kotlinx.coroutines.Dispatchers
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
            val result = repository.loadTowers()
            mutableState.value = result.fold(
                onSuccess = { towers ->
                    if (towers.isEmpty()) AppState.Empty else AppState.Ready(towers)
                },
                onFailure = { error ->
                    AppState.Error(error.message ?: "Nao foi possivel carregar a base local.")
                },
            )
        }
    }
}

sealed interface AppState {
    data object Loading : AppState
    data object Empty : AppState
    data class Ready(val towers: List<Tower>) : AppState
    data class Error(val message: String) : AppState
}
