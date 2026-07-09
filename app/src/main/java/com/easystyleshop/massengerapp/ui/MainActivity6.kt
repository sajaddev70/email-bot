package com.easystyleshop.massengerapp.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.easystyleshop.massengerapp.ui.theme.MessengerAppTheme

class MainActivity6 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MessengerAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ButtonScreen()
                }
            }
        }
    }

    @Composable
    fun ButtonScreen() {
        val context = this@MainActivity6

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    val intent = Intent(context, MainActivity4::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "فرم اطلاعات")
                Spacer(modifier = Modifier.width(8.dp))
                Text("فرم اطلاعات")
            }

            Button(
                onClick = {
                    val intent = Intent(context, MainActivity5::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "عضویت")
                Spacer(modifier = Modifier.width(8.dp))
                Text("عضویت")
            }
        }
    }
}
