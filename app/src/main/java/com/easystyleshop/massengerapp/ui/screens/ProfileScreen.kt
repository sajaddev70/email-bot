package com.easystyleshop.massengerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.easystyleshop.massengerapp.util.Resource
import com.easystyleshop.massengerapp.viewmodel.MessageViewModel
import com.easystyleshop.massengerapp.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    token: String,
    viewModel: ProfileViewModel = hiltViewModel(),
    messageViewModel: MessageViewModel = hiltViewModel()
) {
    val profileState by viewModel.profileState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getProfile(token)
    }

    var selectedItem by remember { mutableStateOf("sendToEmails") } // Default tab

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        when (val state = profileState) {
                            is Resource.Success -> {
                                Text(
                                    text = "ایمیل: ${state.data?.email ?: "نامشخص"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Start
                                )
                                Text(
                                    text = "شماره تلفن: ${state.data?.phoneNumber ?: "نامشخص"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Start
                                )
                            }
                            else -> {
                                Text(
                                    text = "در حال بارگذاری...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Message, contentDescription = "مشاهده پیام‌ها") },
                    label = { Text("پیام‌ها") },
                    selected = selectedItem == "messages",
                    onClick = { selectedItem = "messages" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Email, contentDescription = "ارسال ایمیل") },
                    label = { Text("ارسال ایمیل") },
                    selected = selectedItem == "sendToEmails",
                    onClick = { selectedItem = "sendToEmails" }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = profileState) {
                is Resource.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is Resource.Error -> Text(
                    text = state.message ?: "خطا در دریافت پروفایل",
                    modifier = Modifier.align(Alignment.Center)
                )
                is Resource.Success -> {
                    when (selectedItem) {
                        "messages" -> {
                            MessagesListScreen(
                                phoneNumber=state.data?.email ?: "نامشخص"
                            )
                        }
                        "sendToEmails" -> SendToEmailsContent(
                            onSend = { senderEmail, senderPassword, email, subject, content ->
                                messageViewModel.sendMessage(token, email, subject, content, state.data?.phoneNumber ?: "نامشخص")
                                true
                            }
                        )
                    }
                }
                is Resource.Neutral<*> -> { /* no UI */ }
            }
        }
    }
}
