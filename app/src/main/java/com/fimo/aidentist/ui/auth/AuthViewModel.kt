package com.fimo.aidentist.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fimo.aidentist.data.model.Resource
import com.fimo.aidentist.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _signInState = MutableStateFlow<Resource<FirebaseUser>?>(null)
    val signInState: StateFlow<Resource<FirebaseUser>?> = _signInState.asStateFlow()

    private val _signUpState = MutableStateFlow<Resource<FirebaseUser>?>(null)
    val signUpState: StateFlow<Resource<FirebaseUser>?> = _signUpState.asStateFlow()

    val currentUser: FirebaseUser?
        get() = repository.currentUser

    val isUserLoggedIn: Boolean
        get() = repository.isUserLoggedIn

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _signInState.value = Resource.Loading()
            _signInState.value = repository.signIn(email, password)
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _signUpState.value = Resource.Loading()
            _signUpState.value = repository.signUp(email, password)
        }
    }

    fun signOut() {
        repository.signOut()
    }

    fun resetSignInState() {
        _signInState.value = null
    }

    fun resetSignUpState() {
        _signUpState.value = null
    }
}
