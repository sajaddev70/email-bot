package com.easystyleshop.massengerapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easystyleshop.massengerapp.data.model.Message
import com.easystyleshop.massengerapp.repository.MessageRepository
import com.easystyleshop.massengerapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val repository: MessageRepository
) : ViewModel() {

    // Existing message sending states
    private val _messageState = MutableStateFlow<Resource<Message>>(Resource.Neutral())
    val messageState: StateFlow<Resource<Message>> = _messageState

    private val _sendToEmailsState = MutableStateFlow<Resource<List<Message>>>(Resource.Neutral())
    val sendToEmailsState: StateFlow<Resource<List<Message>>> = _sendToEmailsState

    // New state for messages list
    private val _messagesState = MutableStateFlow<Resource<List<Message>>>(Resource.Neutral())
    val messagesState: StateFlow<Resource<List<Message>>> = _messagesState

    val isSendingMessage = messageState.map { it is Resource.Loading }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = false
    )

    // Existing sendMessage function
    fun sendMessage(
        token: String,
        email: String,
        subject: String,
        content: String,
        phoneNumber: String
    ) {
        viewModelScope.launch {
            _messageState.value = Resource.Loading()
            try {
                val result = repository.sendMessage(token, email, subject, content, phoneNumber)
                _messageState.value = result
            } catch (e: Exception) {
                _messageState.value = Resource.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    // New function to fetch messages list
    fun getMessages(phoneNumber: String) {
        viewModelScope.launch {
            _messagesState.value = Resource.Loading()
            try {
                val result = repository.getMessages(phoneNumber)
                _messagesState.value = result
            } catch (e: Exception) {
                _messagesState.value = Resource.Error(e.localizedMessage ?: "خطا در دریافت پیام‌ها")
            }
        }
    }

    // Existing sendToEmails function
    fun sendToEmails(
        token: String,
        subject: String,
        content: String,
        phoneNumber: String,
        emails: List<String>
    ) {
        viewModelScope.launch {
            _sendToEmailsState.value = Resource.Loading()
            try {
                val result = repository.sendToEmails(token, subject, content, phoneNumber, emails)
                _sendToEmailsState.value = result
            } catch (e: Exception) {
                _sendToEmailsState.value = Resource.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }
}

