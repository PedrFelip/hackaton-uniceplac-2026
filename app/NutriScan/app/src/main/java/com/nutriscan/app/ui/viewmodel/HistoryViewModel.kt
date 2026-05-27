package com.nutriscan.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.app.data.local.HistoryItem
import com.nutriscan.app.data.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HistoryRepository(application)

    private val _items = MutableStateFlow<List<HistoryItem>>(emptyList())
    val items: StateFlow<List<HistoryItem>> = _items.asStateFlow()

    fun loadHistory() {
        viewModelScope.launch {
            try {
                _items.value = repository.getAll()
            } catch (_: Exception) {}
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
            _items.value = emptyList()
        }
    }
}
