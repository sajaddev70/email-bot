package com.easystyleshop.massengerapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.easystyleshop.massengerapp.data.local.UserDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SplashScreen(navController: NavController, userDao: UserDao) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator() // نمایش یک لودر ساده

        LaunchedEffect(Unit) {
            // چون عملیات دیتابیس باید توی دیسپچر IO باشه:
            val userCount = withContext(Dispatchers.IO) {
                userDao.getUsersCount()
            }
            if (userCount > 0) {
                val user = withContext(Dispatchers.IO) {
                    userDao.getUser()
                }
                val token = user?.token ?: ""
                navController.navigate("profile/$token") {
                    popUpTo("splash") { inclusive = true }
                }
            } else {
                navController.navigate("register") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        }
    }
}
