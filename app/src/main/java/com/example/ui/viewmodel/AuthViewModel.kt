package com.example.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthRepository
import com.example.data.local.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val recoveryCodeNotice: String? = null,
    val verificationCodeNotice: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUser: StateFlow<UserEntity?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val isFirebaseGoogleReady: Boolean
        get() = authRepository.isFirebaseGoogleAuthConfigured()

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun loginWithEmail(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            authRepository.loginWithEmailAndPassword(email, password)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Bem-vindo ao Code X, ${it.name}!"
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Falha ao entrar."
                    )
                }
        }
    }

    fun registerAccount(name: String, email: String, password: String, confirm: String, onSuccess: () -> Unit) {
        if (password != confirm) {
            _uiState.value = _uiState.value.copy(errorMessage = "As senhas não coincidem.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            authRepository.registerLocalAccount(name, email, password)
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Conta criada! Código de verificação: ${user.verificationCode}",
                        verificationCodeNotice = user.verificationCode
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Falha no cadastro."
                    )
                }
        }
    }

    fun signInWithGoogle(activity: Activity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            authRepository.signInWithGoogle(activity)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Conectado com Google (${it.email})."
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Falha no login com Google."
                    )
                }
        }
    }

    fun requestRecoveryCode(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            authRepository.requestPasswordRecoveryCode(email)
                .onSuccess { code ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        recoveryCodeNotice = code,
                        successMessage = "Código de recuperação gerado: $code"
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Não foi possível gerar código."
                    )
                }
        }
    }

    fun resetPassword(email: String, code: String, newPassword: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            authRepository.resetPasswordWithCode(email, code, newPassword)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        recoveryCodeNotice = null,
                        successMessage = "Senha redefinida com sucesso! Faça login."
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Erro ao redefinir senha."
                    )
                }
        }
    }

    fun verifyEmail(userId: String, code: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            authRepository.verifyEmailCode(userId, code)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        verificationCodeNotice = null,
                        successMessage = "E-mail verificado com sucesso!"
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Código inválido."
                    )
                }
        }
    }

    fun resendVerificationCode(userId: String) {
        viewModelScope.launch {
            authRepository.generateNewEmailVerificationCode(userId)
                .onSuccess { code ->
                    _uiState.value = _uiState.value.copy(
                        verificationCodeNotice = code,
                        successMessage = "Novo código de verificação: $code"
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
                }
        }
    }

    fun toggleCloudSync(userId: String, enabled: Boolean) {
        viewModelScope.launch {
            authRepository.toggleCloudSync(userId, enabled)
                .onSuccess {
                    val note = if (enabled && !isFirebaseGoogleReady) {
                        "Preferência salva. Adicione google-services.json para ativar a sincronização Firestore."
                    } else if (enabled) {
                        "Sincronização na nuvem ativada."
                    } else {
                        "Sincronização na nuvem desativada."
                    }
                    _uiState.value = _uiState.value.copy(successMessage = note)
                }
        }
    }

    fun updateDisplayName(userId: String, newName: String) {
        viewModelScope.launch {
            authRepository.updateProfileName(userId, newName)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(successMessage = "Nome atualizado.")
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
                }
        }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = AuthUiState()
            onDone()
        }
    }
}
