package com.easystyleshop.massengerapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.easystyleshop.massengerapp.data.local.UserDao
import com.easystyleshop.massengerapp.ui.screens.*
import com.easystyleshop.massengerapp.ui.theme.MessengerAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()

            MessengerAppTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NavHost(navController = navController, startDestination = "splash") {
                            composable("splash") {
                                SplashScreen(navController = navController, userDao = userDao)
                            }
                            composable("register") {
                                RegisterScreen(navController = navController)
                            }
                            composable("profile/{token}") { backStackEntry ->
                                val token = backStackEntry.arguments?.getString("token") ?: ""
                                ProfileScreen(token = token)
                            }
                            composable("message/{token}/{phoneNumber}") { backStackEntry ->
                                val token = backStackEntry.arguments?.getString("token") ?: ""
                                val phoneNumber =
                                    backStackEntry.arguments?.getString("phoneNumber") ?: ""
                                MessageScreen(
                                    navController = navController,
                                    token = token,
                                    phoneNumber = phoneNumber
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

