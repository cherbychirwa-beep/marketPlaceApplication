package com.shopizzo.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.shopizzo.data.model.UserProfile
import com.shopizzo.data.repository.AuthRepository
import com.shopizzo.data.repository.UserPreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle    : AuthUiState()
    object Loading : AuthUiState()
    object OtpSent : AuthUiState()
    data class Success(val message: String) : AuthUiState()
    data class Error(val message: String)   : AuthUiState()
}

class AuthViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val repository = AuthRepository()
    private val preferencesRepository = UserPreferencesRepository(context)

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    val isLoggedIn: Boolean get() = repository.isLoggedIn
    
    // Greeting state management
    val currentUserName: String? get() = _userProfile.value?.fullName ?: repository.currentUser?.displayName ?: (savedStateHandle["user_name"] as? String)

    private val _appTheme = MutableStateFlow("SYSTEM")
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    private val _biometricEnabled = MutableStateFlow(false)
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    private var verificationId: String? = null
    private var pendingAction: (() -> Unit)? = null
    private var profileJob: Job? = null

    init {
        val uid = repository.currentUser?.uid
        if (uid != null) {
            observeUserProfile(uid)
        }
        
        viewModelScope.launch {
            _userProfile.collectLatest { profile ->
                profile?.fullName?.let { name ->
                    savedStateHandle["user_name"] = name
                }
            }
        }

        viewModelScope.launch {
            preferencesRepository.themeFlow.collectLatest { theme ->
                _appTheme.value = theme
            }
        }
        viewModelScope.launch {
            preferencesRepository.biometricEnabledFlow.collectLatest { enabled ->
                _biometricEnabled.value = enabled
            }
        }
    }

    private fun observeUserProfile(uid: String) {
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            repository.getUserProfileFlow(uid).collect { profile ->
                _userProfile.value = profile
            }
        }
    }

    fun setPendingAction(action: () -> Unit) { pendingAction = action }
    fun consumePendingAction() { pendingAction?.invoke(); pendingAction = null }

    fun register(email: String, password: String, fullName: String, phoneNumber: String, country: String) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            val result = repository.register(email, password, fullName, phoneNumber, country)
            if (result.isSuccess) {
                val profile = result.getOrNull()
                profile?.uid?.let { observeUserProfile(it) }
                _authState.value = AuthUiState.Success("Account created successfully")
                consumePendingAction()
            } else {
                _authState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Registration failed.")
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            val result = repository.login(email, password)
            if (result.isSuccess) {
                val user = result.getOrNull()
                user?.uid?.let { observeUserProfile(it) }
                _authState.value = AuthUiState.Success("Success! Getting things ready...")
                consumePendingAction()
            } else {
                _authState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Login failed.")
            }
        }
    }

    fun startPhoneLogin(phoneNumber: String, activity: Activity) {
        _authState.value = AuthUiState.Loading
        repository.startPhoneVerification(phoneNumber, activity, object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                viewModelScope.launch {
                    val result = repository.signInWithCredential(credential)
                    if (result.isSuccess) {
                        val user = result.getOrNull()
                        user?.uid?.let { observeUserProfile(it) }
                        _authState.value = AuthUiState.Success("Phone login successful")
                        consumePendingAction()
                    } else {
                        _authState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Auto-verification failed")
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _authState.value = AuthUiState.Error(e.message ?: "Verification failed")
            }

            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = id
                _authState.value = AuthUiState.OtpSent
            }
        })
    }

    fun verifyOtp(code: String) {
        val vid = verificationId ?: return
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            val result = repository.signInWithPhone(vid, code)
            if (result.isSuccess) {
                val user = result.getOrNull()
                user?.uid?.let { observeUserProfile(it) }
                _authState.value = AuthUiState.Success("Phone login successful")
                consumePendingAction()
            } else {
                _authState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Invalid code")
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            val result = repository.signInWithGoogle(idToken)
            if (result.isSuccess) {
                val user = result.getOrNull()
                user?.uid?.let { observeUserProfile(it) }
                _authState.value = AuthUiState.Success("Signed in with Google")
                consumePendingAction()
            } else {
                _authState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Google Sign-In failed.")
            }
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            val result = repository.sendPasswordReset(email)
            if (result.isSuccess) {
                _authState.value = AuthUiState.Success("Password reset link sent to your email")
            } else {
                _authState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Failed to send reset link")
            }
        }
    }

    fun logout() {
        repository.logout()
        profileJob?.cancel()
        _userProfile.value = null
        savedStateHandle["user_name"] = null
        _authState.value = AuthUiState.Idle
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            val result = repository.updateProfile(profile)
            if (result.isSuccess) {
                _authState.value = AuthUiState.Success("Profile updated successfully")
            } else {
                _authState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Failed to update profile")
            }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            val result = repository.updateNotificationSetting(enabled)
            if (!result.isSuccess) {
                _snackbarMessage.emit("Failed to update notification settings")
            }
        }
    }

    fun saveThemePreference(theme: String) {
        viewModelScope.launch {
            preferencesRepository.saveTheme(theme)
        }
    }

    fun saveBiometricPreference(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.saveBiometricEnabled(enabled)
        }
    }

    fun toggleFavourite(productId: String) {
        val currentProfile = _userProfile.value ?: return
        val currentFavourites = currentProfile.favouriteIds
        
        // Optimistic UI Update: Toggle locally immediately
        val isAdding = !currentFavourites.contains(productId)
        val updatedFavourites = if (isAdding) {
            currentFavourites + productId
        } else {
            currentFavourites - productId
        }
        
        _userProfile.update { it?.copy(favouriteIds = updatedFavourites) }

        viewModelScope.launch {
            val result = repository.toggleFavourite(productId, currentFavourites)
            if (result.isSuccess) {
                _snackbarMessage.emit(if (isAdding) "Liked" else "Removed from Favourites")
            } else {
                // Revert to original state if the network call fails
                _userProfile.update { currentProfile }
                _snackbarMessage.emit("Failed to update favourites")
            }
        }
    }

    fun isFavourite(productId: String): Boolean =
        _userProfile.value?.favouriteIds?.contains(productId) == true

    fun resetState() {
        _authState.value = AuthUiState.Idle
    }

    fun updatePaymentPin(pin: String) {
        val uid = repository.currentUser?.uid ?: return
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            val result = repository.setPaymentPin(uid, pin)
            if (result.isSuccess) {
                _userProfile.update { it?.copy(paymentPin = pin) }
                _authState.value = AuthUiState.Success("Payment PIN updated successfully")
            } else {
                _authState.value = AuthUiState.Error("Failed to update payment PIN")
            }
        }
    }

    fun updatePreferredAuthMethod(method: String) {
        val uid = repository.currentUser?.uid ?: return
        viewModelScope.launch {
            val result = repository.updatePreferredAuthMethod(uid, method)
            if (result.isSuccess) {
                _userProfile.update { it?.copy(preferredAuthMethod = method) }
                _snackbarMessage.emit("Security preference updated")
            }
        }
    }
}
