package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.CardsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OperationsScreen
import com.example.ui.screens.PinLockScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmsParserLabScreen
import com.example.ui.screens.UtilitiesScreen
import com.example.ui.theme.TransfermovilTheme
import com.example.ui.viewmodel.BankViewModel

enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("Inicio", Icons.Default.Home),
    SMS_LAB("SMS Detector", Icons.Default.MarkEmailUnread),
    OPERATIONS("Operaciones", Icons.AutoMirrored.Filled.Send),
    CARDS("Tarjetas", Icons.Default.CreditCard),
    UTILITIES("Servicios", Icons.Default.Receipt),
    SETTINGS("Ajustes", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: BankViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TransfermovilTheme {
                val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
                var currentTab by remember { mutableStateOf(AppTab.HOME) }

                BackHandler(enabled = isUnlocked && currentTab != AppTab.HOME) {
                    currentTab = AppTab.HOME
                }

                if (!isUnlocked) {
                    PinLockScreen(
                        onUnlock = { pin -> viewModel.attemptUnlock(pin) }
                    )
                } else {
                    val cards by viewModel.cards.collectAsStateWithLifecycle()
                    val totalCup by viewModel.totalCupBalance.collectAsStateWithLifecycle()
                    val totalMlc by viewModel.totalMlcBalance.collectAsStateWithLifecycle()
                    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
                    val utilities by viewModel.utilities.collectAsStateWithLifecycle()

                    val smsInput by viewModel.manualSmsInput.collectAsStateWithLifecycle()
                    val lastParsedResult by viewModel.lastParsedResult.collectAsStateWithLifecycle()

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            NavigationBar(
                                modifier = Modifier
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                                    .testTag("bottom_nav_bar")
                            ) {
                                AppTab.entries.forEach { tab ->
                                    NavigationBarItem(
                                        selected = currentTab == tab,
                                        onClick = { currentTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.label
                                            )
                                        },
                                        label = { Text(tab.label) },
                                        modifier = Modifier.testTag("nav_tab_${tab.name}")
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            Crossfade(targetState = currentTab, label = "ScreenTransition") { tab ->
                                when (tab) {
                                    AppTab.HOME -> HomeScreen(
                                        cards = cards,
                                        totalCup = totalCup,
                                        totalMlc = totalMlc,
                                        transactions = transactions,
                                        onNavigateToSmsLab = { currentTab = AppTab.SMS_LAB },
                                        onNavigateToOperations = { currentTab = AppTab.OPERATIONS },
                                        onNavigateToCards = { currentTab = AppTab.CARDS },
                                        onNavigateToUtilities = { currentTab = AppTab.UTILITIES },
                                        onLockApp = { viewModel.lockApp() }
                                    )

                                    AppTab.SMS_LAB -> SmsParserLabScreen(
                                        inputText = smsInput,
                                        onInputChange = { viewModel.updateManualSmsInput(it) },
                                        onParseSms = { viewModel.parseAndProcessSms() },
                                        lastResult = lastParsedResult,
                                        sampleSmsList = viewModel.sampleBankSms,
                                        cards = cards
                                    )

                                    AppTab.OPERATIONS -> OperationsScreen(
                                        bankKey = viewModel.bankKey,
                                        onLaunchUssd = { ctx, ussd -> viewModel.launchUssdCall(ctx, ussd) }
                                    )

                                    AppTab.CARDS -> CardsScreen(
                                        cards = cards,
                                        onAddCard = { bank, last4, curr, bal ->
                                            viewModel.addNewCard(bank, last4, curr, bal)
                                        },
                                        onUpdateBalance = { last4, newBal ->
                                            viewModel.updateCardBalance(last4, newBal)
                                        },
                                        onDeleteCard = { id -> viewModel.deleteCard(id) }
                                    )

                                    AppTab.UTILITIES -> UtilitiesScreen(
                                        utilities = utilities,
                                        onAddUtility = { type, alias, acc, amt ->
                                            viewModel.addUtility(type, alias, acc, amt)
                                        },
                                        onDeleteUtility = { id -> viewModel.deleteUtility(id) },
                                        onPayUtility = { util ->
                                            currentTab = AppTab.OPERATIONS
                                        }
                                    )

                                    AppTab.SETTINGS -> SettingsScreen(
                                        currentPin = viewModel.appPin,
                                        currentBankKey = viewModel.bankKey,
                                        onSavePin = { viewModel.appPin = it },
                                        onSaveBankKey = { viewModel.bankKey = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
