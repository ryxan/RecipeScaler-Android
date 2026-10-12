package com.ryxan.recipescaler.feature.pro

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryxan.recipescaler.core.data.billing.ProProduct
import com.ryxan.recipescaler.core.data.billing.ProRepository
import com.ryxan.recipescaler.core.data.billing.PurchaseLaunch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class ProUiState(
    val isPro: Boolean = false,
    val isPending: Boolean = false,
    val products: List<ProProduct> = emptyList(),
    /** True until the first product refresh completes. */
    val loading: Boolean = true,
)

@HiltViewModel
class ProViewModel @Inject constructor(
    private val proRepository: ProRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProUiState())
    val uiState: StateFlow<ProUiState> = _uiState

    /** One-shot user-visible error messages (purchase/restore failures). */
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    init {
        viewModelScope.launch { proRepository.isPro.collect { p -> _uiState.value = _uiState.value.copy(isPro = p) } }
        viewModelScope.launch { proRepository.isPending.collect { p -> _uiState.value = _uiState.value.copy(isPending = p) } }
        viewModelScope.launch { proRepository.products.collect { p -> _uiState.value = _uiState.value.copy(products = p) } }
        viewModelScope.launch {
            proRepository.refreshProducts()
            _uiState.value = _uiState.value.copy(loading = false)
        }
    }

    fun purchase(activity: Activity, productId: String) {
        viewModelScope.launch {
            when (val result = proRepository.launchPurchase(activity, productId)) {
                is PurchaseLaunch.Error -> _messages.trySend(result.message)
                else -> Unit // Launched / Cancelled — nothing to report
            }
        }
    }

    fun restore() {
        viewModelScope.launch {
            if (!proRepository.restore()) {
                _messages.trySend("Couldn't check purchases. Try again later.")
            }
        }
    }
}
