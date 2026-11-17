package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.data.repository.LumaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isSignUpMode: Boolean = false,
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val username: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val repository: LumaRepository
) : ViewModel() {

    val currentUserId: StateFlow<String?> = repository.currentUserId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val isDarkMode: StateFlow<Boolean> = repository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initialize()
        }
    }

    fun toggleMode() {
        _uiState.update {
            it.copy(
                isSignUpMode = !it.isSignUpMode,
                errorMessage = null
            )
        }
    }

    fun updateEmail(value: String) = _uiState.update { it.copy(email = value, errorMessage = null) }
    fun updatePassword(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }
    fun updateDisplayName(value: String) = _uiState.update { it.copy(displayName = value, errorMessage = null) }
    fun updateUsername(value: String) = _uiState.update { it.copy(username = value, errorMessage = null) }

    fun submit() {
        val state = _uiState.value
        if (state.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = if (state.isSignUpMode) {
                repository.signUp(
                    email = state.email,
                    password = state.password,
                    displayName = state.displayName,
                    username = state.username
                )
            } else {
                repository.login(
                    email = state.email,
                    password = state.password
                )
            }
            result.fold(
                onSuccess = {
                    _uiState.update { AuthUiState(isSignUpMode = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Authentication failed."
                        )
                    }
                }
            )
        }
    }

    fun continueWithDemoAccount() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.loginDemoAccount().fold(
                onSuccess = { _uiState.update { AuthUiState() } },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = err.message ?: "Demo sign-in failed.")
                    }
                }
            )
        }
    }

    companion object {
        fun factory(repository: LumaRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    return AuthViewModel(repository) as T
                }
            }
    }
}
