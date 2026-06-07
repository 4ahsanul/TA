package com.fimo.aidentist.ui.profile

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fimo.aidentist.data.model.Resource
import com.fimo.aidentist.data.model.User
import com.fimo.aidentist.data.repository.AuthRepository
import com.fimo.aidentist.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    private val _updateState = MutableStateFlow<Resource<Unit>?>(null)
    val updateState: StateFlow<Resource<Unit>?> = _updateState.asStateFlow()

    private val _imageUploadState = MutableStateFlow<Resource<android.net.Uri>?>(null)
    val imageUploadState: StateFlow<Resource<android.net.Uri>?> = _imageUploadState.asStateFlow()

    private val _emailVerificationState = MutableStateFlow<Resource<Unit>?>(null)
    val emailVerificationState: StateFlow<Resource<Unit>?> = _emailVerificationState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        _userProfile.value = authRepository.getUserProfile()
    }

    fun updateProfile(displayName: String, photoUri: android.net.Uri?) {
        viewModelScope.launch {
            _updateState.value = Resource.Loading()
            _updateState.value = userRepository.updateProfile(displayName, photoUri)
            // Refresh profile after update
            loadUserProfile()
        }
    }

    fun uploadProfileImage(bitmap: Bitmap) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _imageUploadState.value = Resource.Loading()
            _imageUploadState.value = userRepository.uploadProfileImage(uid, bitmap)
        }
    }

    fun sendEmailVerification() {
        viewModelScope.launch {
            _emailVerificationState.value = Resource.Loading()
            _emailVerificationState.value = authRepository.sendEmailVerification()
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun resetUpdateState() {
        _updateState.value = null
    }

    fun resetImageUploadState() {
        _imageUploadState.value = null
    }

    fun resetEmailVerificationState() {
        _emailVerificationState.value = null
    }
}
