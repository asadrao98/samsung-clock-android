package com.asadrao.clock.ui.worldclock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.asadrao.clock.di.AppContainer
import com.asadrao.clock.domain.repository.WorldClockRepository
import com.asadrao.clock.domain.worldclock.City
import com.asadrao.clock.domain.worldclock.CityCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorldClockUiState(
    val cities: List<City> = emptyList(),
    val loaded: Boolean = false,
) {
    val isEmpty: Boolean get() = loaded && cities.isEmpty()
}

data class CitySearchUiState(
    val query: String = "",
    val results: List<City> = emptyList(),
    val alreadyAdded: Set<String> = emptySet(),
)

class WorldClockViewModel(
    private val repository: WorldClockRepository,
    private val catalog: CityCatalog,
) : ViewModel() {

    val uiState: StateFlow<WorldClockUiState> = repository.observeCities()
        .map { WorldClockUiState(cities = it, loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorldClockUiState())

    private val query = MutableStateFlow("")

    val searchState: StateFlow<CitySearchUiState> =
        combine(query, repository.observeCities()) { text, added ->
            CitySearchUiState(
                query = text,
                results = catalog.search(text),
                alreadyAdded = added.map { it.zoneId }.toSet(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CitySearchUiState())

    fun setQuery(text: String) {
        query.value = text
    }

    fun addCity(city: City) {
        viewModelScope.launch { repository.addCity(city) }
    }

    fun removeCity(zoneId: String) {
        viewModelScope.launch { repository.removeCity(zoneId) }
    }

    /** Moves a city and persists the whole new order. */
    fun move(fromIndex: Int, toIndex: Int) {
        val current = uiState.value.cities.map { it.zoneId }.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        current.add(toIndex, current.removeAt(fromIndex))
        viewModelScope.launch { repository.reorder(current) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                WorldClockViewModel(
                    repository = container.worldClockRepository,
                    catalog = container.cityCatalog,
                )
            }
        }
    }
}
