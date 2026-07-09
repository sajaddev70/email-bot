package com.easystyleshop.massengerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.easystyleshop.massengerapp.R
import com.easystyleshop.massengerapp.util.Resource
import com.easystyleshop.massengerapp.viewmodel.RegisterViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var phoneNumberError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    val registerState by viewModel.registerState.collectAsState()
    val maxEmailLength = 50
    val maxPhoneLength = 11

    fun validateFields(): Boolean {
        var isValid = true

        if (phoneNumber.isBlank()) {
            phoneNumberError = "شماره تلفن الزامی است"
            isValid = false
        } else if (!phoneNumber.matches(Regex("^09\\d{9}$"))) {
            phoneNumberError = "شماره تلفن معتبر نیست"
            isValid = false
        } else {
            phoneNumberError = null
        }

        if (email.isBlank()) {
            emailError = "ایمیل الزامی است"
            isValid = false
        } else if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))) {
            emailError = "ایمیل معتبر نیست"
            isValid = false
        } else {
            emailError = null
        }

        if (password.isBlank()) {
            passwordError = "رمز عبور الزامی است"
            isValid = false
        } else if (password.length < 6) {
            passwordError = "رمز عبور باید حداقل ۶ کاراکتر باشد"
            isValid = false
        } else {
            passwordError = null
        }

        return isValid
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.register_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.enter_your_info),
                        style = MaterialTheme.typography.bodyLarge
                    )


                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = {
                            if (it.length <= maxPhoneLength) phoneNumber = it
                        },
                        label = {
                            Text(
                                stringResource(R.string.phone_number),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = phoneNumberError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        supportingText = {
                            if (phoneNumberError != null) {
                                Text(text = phoneNumberError ?: "")
                            }
                        }
                    )



                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            if (it.length <= maxEmailLength) email = it
                        },
                        label = {
                            Text(
                                stringResource(R.string.email),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = emailError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        supportingText = {
                            if (emailError != null) {
                                Text(text = emailError ?: "")
                            }
                        }
                    )


                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = {
                            Text(
                                stringResource(R.string.password),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        isError = passwordError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            val image = if (passwordVisible)
                                Icons.Default.Visibility
                            else
                                Icons.Default.VisibilityOff

                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                            }
                        },
                        supportingText = {
                            if (passwordError != null) {
                                Text(text = passwordError ?: "")
                            }
                        }
                    )

                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (registerState is Resource.Error) {
                        Text(
                            text = (registerState as Resource.Error).message
                                ?: stringResource(R.string.register_error),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = {
                            if (validateFields()) {
                                viewModel.register(phoneNumber, password, email)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        enabled = registerState !is Resource.Loading
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            if (registerState is Resource.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.register_button),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }

            if (registerState is Resource.Success) {
                LaunchedEffect(registerState) {
                    navController.navigate("profile/${(registerState as Resource.Success).data}")
                }
            }
        }
    }
}
