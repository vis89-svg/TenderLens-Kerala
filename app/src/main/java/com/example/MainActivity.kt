package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TenderViewModel

sealed class Screen {
    object Login : Screen()
    object SignUp : Screen()
    object Onboarding : Screen()
    object Dashboard : Screen()
    data class TenderDetails(val tenderId: String) : Screen()
    object Nearby : Screen()
    object Analytics : Screen()
    object SmartAlerts : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: TenderViewModel = viewModel()

                // State backstack list for secure, crash-free history backpress navigation
                val backStack = remember { mutableStateListOf<Screen>() }

                val currentUserEmail by viewModel.currentUserEmail.collectAsState()
                val userPreference by viewModel.userPreference.collectAsState()

                // Initialize backstack state when user Prefs/Session are loaded
                LaunchedEffect(currentUserEmail, userPreference) {
                    val email = currentUserEmail
                    val pref = userPreference
                    if (email == null) {
                        if (backStack.isEmpty() || !backStack.contains(Screen.Login)) {
                            backStack.clear()
                            backStack.add(Screen.Login)
                        }
                    } else {
                        if (pref != null) {
                            if (!pref.onboardingCompleted) {
                                if (!backStack.contains(Screen.Onboarding)) {
                                    backStack.clear()
                                    backStack.add(Screen.Onboarding)
                                }
                            } else {
                                if (backStack.isEmpty() || backStack.contains(Screen.Login) || backStack.contains(Screen.Onboarding) || backStack.contains(Screen.SignUp)) {
                                    backStack.clear()
                                    backStack.add(Screen.Dashboard)
                                }
                            }
                        }
                    }
                }

                val currentScreen = backStack.lastOrNull() ?: Screen.Login

                // Auto intercept Android physical backpress gestures
                BackHandler(enabled = backStack.size > 1) {
                    if (backStack.size > 1) {
                        backStack.removeAt(backStack.lastIndex)
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                        when (screen) {
                            is Screen.Login -> {
                                LoginScreen(
                                    viewModel = viewModel,
                                    onNavigateToSignUp = {
                                        backStack.add(Screen.SignUp)
                                    },
                                    onLoginSuccess = {
                                        // Re-routed by LaunchedEffect
                                    }
                                )
                            }
                            is Screen.SignUp -> {
                                SignUpScreen(
                                    viewModel = viewModel,
                                    onNavigateToLogin = {
                                        backStack.removeAt(backStack.lastIndex)
                                    },
                                    onSignUpSuccess = {
                                        // Re-routed by LaunchedEffect
                                    }
                                )
                            }
                            is Screen.Onboarding -> {
                                OnboardingScreen(
                                    viewModel = viewModel,
                                    onOnboardingComplete = {
                                        backStack.clear()
                                        backStack.add(Screen.Dashboard)
                                    }
                                )
                            }
                            is Screen.Dashboard -> {
                                DashboardScreen(
                                    viewModel = viewModel,
                                    onTenderClick = { id ->
                                        viewModel.selectTender(id)
                                        backStack.add(Screen.TenderDetails(id))
                                    },
                                    onNavigateToNearby = {
                                        backStack.add(Screen.Nearby)
                                    },
                                    onNavigateToAnalytics = {
                                        backStack.add(Screen.Analytics)
                                    },
                                    onNavigateToAlerts = {
                                        backStack.add(Screen.SmartAlerts)
                                    }
                                )
                            }
                            is Screen.TenderDetails -> {
                                TenderDetailsScreen(
                                    viewModel = viewModel,
                                    onBackClick = {
                                        backStack.removeAt(backStack.lastIndex)
                                    }
                                )
                            }
                            is Screen.Nearby -> {
                                NearbyTendersScreen(
                                    viewModel = viewModel,
                                    onBackClick = {
                                        backStack.removeAt(backStack.lastIndex)
                                    },
                                    onTenderClick = { id ->
                                        viewModel.selectTender(id)
                                        backStack.add(Screen.TenderDetails(id))
                                    }
                                )
                            }
                            is Screen.Analytics -> {
                                AnalyticsScreen(
                                    viewModel = viewModel,
                                    onBackClick = {
                                        backStack.removeAt(backStack.lastIndex)
                                    }
                                )
                            }
                            is Screen.SmartAlerts -> {
                                SmartAlertsScreen(
                                    viewModel = viewModel,
                                    onBackClick = {
                                        backStack.removeAt(backStack.lastIndex)
                                    },
                                    onTenderClick = { id ->
                                        viewModel.selectTender(id)
                                        backStack.add(Screen.TenderDetails(id))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
