package com.easystyleshop.massengerapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easystyleshop.massengerapp.repository.UserRepository
import com.easystyleshop.massengerapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ViewModel برای ثبت‌نام
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    // مقدار پیش‌فرض خنثی است تا در شروع اپ چیزی نمایش داده نشود
    private val _registerState = MutableStateFlow<Resource<String>>(Resource.Neutral())
    val registerState: StateFlow<Resource<String>> = _registerState

    init {
        resetRegisterState()
    }

    fun register(phoneNumber: String, password: String, email: String) {
        viewModelScope.launch {
            _registerState.value = Resource.Loading()
            _registerState.value = repository.register(phoneNumber, password, email)
        }
    }

    // متد اختیاری برای ریست به حالت خنثی
    fun resetRegisterState() {
        _registerState.value = Resource.Neutral()
    }
}
