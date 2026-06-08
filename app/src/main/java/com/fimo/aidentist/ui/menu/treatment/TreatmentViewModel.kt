package com.fimo.aidentist.ui.menu.treatment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fimo.aidentist.data.model.Resource
import com.fimo.aidentist.data.repository.AuthRepository
import com.fimo.aidentist.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TreatmentViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val authRepository = AuthRepository()

    private val _diseaseState = MutableStateFlow<Resource<Map<String, Any?>>?>(null)
    val diseaseState: StateFlow<Resource<Map<String, Any?>>?> = _diseaseState.asStateFlow()

    fun checkDisease() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _diseaseState.value = Resource.Loading()
            _diseaseState.value = userRepository.getUserDiseaseData(uid)
        }
    }

    fun loadUserData() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            val result = userRepository.getUserDiseaseData(uid)
            _diseaseState.value = result
        }
    }
}
