package com.easystyleshop.massengerapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easystyleshop.massengerapp.data.model.User
import com.easystyleshop.massengerapp.repository.UserRepository
import com.easystyleshop.massengerapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ViewModel برای پروفایل کاربر
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {
    private val _profileState = MutableStateFlow<Resource<User>>(Resource.Loading())
    val profileState: StateFlow<Resource<User>> = _profileState

    private val _emailSendState = MutableStateFlow<Resource<Unit>>(Resource.Loading())
    val emailSendState: StateFlow<Resource<Unit>> = _emailSendState

    fun getProfile(token: String) {
        viewModelScope.launch {
            _profileState.value = Resource.Loading()
            _profileState.value = repository.getUserProfile(token)
        }
    }

    fun sendEmailViaZoho(
        cookie: String,
        csrfToken: String,
        accId: String,
        from: String,
        to: String,
        subject: String,
        content: String
    ) {
        viewModelScope.launch {
            _emailSendState.value = Resource.Loading()
            _emailSendState.value = repository.sendEmailViaZoho(
                cookie = cookie,
                csrfToken = csrfToken,
                accId = accId,
                from = from,
                to = to,
                subject = subject,
                content = content
            )
        }
    }
}