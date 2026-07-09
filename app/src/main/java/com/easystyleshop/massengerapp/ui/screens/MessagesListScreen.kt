package com.easystyleshop.massengerapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.easystyleshop.massengerapp.util.Resource
import com.easystyleshop.massengerapp.viewmodel.MessageViewModel

@Composable
fun MessagesListScreen(
    phoneNumber: String,
    viewModel: MessageViewModel = hiltViewModel()
) {
    val messagesState by viewModel.messagesState.collectAsState()

    LaunchedEffect(phoneNumber) {  // Depend on phoneNumber, reload if it changes
        viewModel.getMessages(phoneNumber)
    }

    // Use Box to center loading or error messages
    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = messagesState) {
            is Resource.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is Resource.Success -> {
                val messages = state.data ?: emptyList()
                if (messages.isEmpty()) {
                    Text(
                        text = "هیچ پیامی موجود نیست",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(messages) { message ->
                            Card(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = "ایمیل: ${message.email}", style = MaterialTheme.typography.bodyMedium)
                                    Text(text = "موضوع: ${message.subject}", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "محتوا: ${message.content}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
            is Resource.Error -> {
                Text(
                    text = state.message ?: "خطا در دریافت پیام‌ها",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            }
            is Resource.Neutral<*> -> {
                // Optionally handle neutral state
            }
        }
    }
}
