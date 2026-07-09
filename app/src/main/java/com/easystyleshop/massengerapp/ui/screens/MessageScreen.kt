package com.easystyleshop.massengerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.easystyleshop.massengerapp.util.Resource
import com.easystyleshop.massengerapp.viewmodel.MessageViewModel

// صفحه ارسال پیام
@Composable
fun MessageScreen(
    navController: NavController,
    token: String,
    phoneNumber: String,
    viewModel: MessageViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val messageState by viewModel.messageState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("ایمیل مقصد") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("موضوع") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("محتوا") }
        )
        Spacer(modifier = Modifier.height(16.dp))


        when (val state = messageState) {
            is Resource.Loading -> CircularProgressIndicator()
            is Resource.Success -> Text("پیام با موفقیت ارسال شد")
            is Resource.Error -> Text(state.message ?: "خطا در ارسال پیام")
            is Resource.Neutral<*> -> {
                Button(onClick = {
                    viewModel.sendMessage(
                        token,
                        email,
                        subject,
                        content,
                        phoneNumber
                    )
                }) {
                    Text("ارسال")
                }
            }
        }
    }
}