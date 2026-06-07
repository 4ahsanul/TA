package com.fimo.aidentist.ui.home

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

class HomeViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    private val _diseaseState = MutableStateFlow<Resource<Map<String, Any?>>?>(null)
    val diseaseState: StateFlow<Resource<Map<String, Any?>>?> = _diseaseState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        _userProfile.value = authRepository.getUserProfile()
    }

    fun loadDiseaseData() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _diseaseState.value = Resource.Loading()
            _diseaseState.value = userRepository.getUserDiseaseData(uid)
        }
    }
}
