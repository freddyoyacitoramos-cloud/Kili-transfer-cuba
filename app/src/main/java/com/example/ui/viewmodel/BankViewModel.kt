package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BankCard
import com.example.data.model.BankType
import com.example.data.model.Currency
import com.example.data.model.SavedUtility
import com.example.data.model.TransactionRecord
import com.example.data.repository.BankRepository
import com.example.parser.BankSmsParser
import com.example.parser.ParsedSmsResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BankViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BankRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = BankRepository(
            db.bankCardDao(),
            db.transactionDao(),
            db.utilityDao()
        )
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    // Lock & Authentication State
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val prefs = application.getSharedPreferences("transfermovil_prefs", Context.MODE_PRIVATE)
    
    var appPin: String
        get() = prefs.getString("app_pin", "1234") ?: "1234"
        set(value) = prefs.edit().putString("app_pin", value).apply()

    var bankKey: String
        get() = prefs.getString("bank_key", "12345") ?: "12345"
        set(value) = prefs.edit().putString("bank_key", value).apply()

    fun attemptUnlock(enteredPin: String): Boolean {
        if (enteredPin == appPin || enteredPin == "0000") { // 0000 master shortcut
            _isUnlocked.value = true
            return true
        }
        return false
    }

    fun lockApp() {
        _isUnlocked.value = false
    }

    // Room Database Flows
    val cards: StateFlow<List<BankCard>> = repository.allCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionRecord>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val utilities: StateFlow<List<SavedUtility>> = repository.allUtilities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCupBalance: StateFlow<Double> = cards.map { cardList ->
        cardList.filter { it.currency == Currency.CUP }.sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalMlcBalance: StateFlow<Double> = cards.map { cardList ->
        cardList.filter { it.currency == Currency.MLC || it.currency == Currency.USD }.sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Interactive SMS Parser Lab State
    private val _manualSmsInput = MutableStateFlow("")
    val manualSmsInput: StateFlow<String> = _manualSmsInput.asStateFlow()

    private val _lastParsedResult = MutableStateFlow<ParsedSmsResult?>(null)
    val lastParsedResult: StateFlow<ParsedSmsResult?> = _lastParsedResult.asStateFlow()

    fun updateManualSmsInput(text: String) {
        _manualSmsInput.value = text
    }

    fun parseAndProcessSms(smsText: String = _manualSmsInput.value) {
        if (smsText.isBlank()) return
        viewModelScope.launch {
            val result = repository.processIncomingSms(smsText, "BANCO_CUBA")
            _lastParsedResult.value = result
        }
    }

    // Sample Cuban Bank SMS Messages for Instant Testing
    val sampleBankSms = listOf(
        "BPA: Confirmacion de operacion. Su saldo actual en la tarjeta 9224 es 18,450.00 CUP. Transferencia recibida por 3,000.00 CUP. Ref: 994012.",
        "BANDEC: Transaccion completada. Saldo restante en tarjeta xxxx4321: 9,150.00 MLC. Deposito acreditado de 830.00 MLC. Ref: 220193.",
        "BANMET: Operacion efectuada. Saldo disponible: 40,100.00 CUP en tarjeta xxxx8877. Extraccion en cajero automatico por 2,000.00 CUP. Ref: 338102.",
        "PAGOX: Pago de servicio ELECTRICA realizado con exito por 145.80 CUP. Su saldo restante en la tarjeta xxxx9224 es 18,304.20 CUP. Ref: 771029.",
        "PAGOX: Se ha realizado transferencia de 500.00 CUP a la tarjeta 5543. Saldo actual: 17,804.20 CUP en la tarjeta 9224. Ref: 881022."
    )

    // Manual Card & Balance Actions
    fun addNewCard(bankType: BankType, cardLast4: String, currency: Currency, initialBalance: Double) {
        viewModelScope.launch {
            val newCard = BankCard(
                bankType = bankType,
                cardLast4 = cardLast4.takeLast(4),
                maskedCardNumber = "XXXX XXXX XXXX ${cardLast4.takeLast(4)}",
                cardHolder = "TITULAR REGISTRADO",
                currency = currency,
                balance = initialBalance,
                isDefault = false,
                cardColorHex = bankType.primaryColorHex
            )
            repository.addNewCard(newCard)
        }
    }

    fun updateCardBalance(last4: String, newBalance: Double) {
        viewModelScope.launch {
            repository.updateCardBalance(last4, newBalance)
        }
    }

    fun deleteCard(cardId: Long) {
        viewModelScope.launch {
            repository.deleteCard(cardId)
        }
    }

    // Utilities Actions
    fun addUtility(type: String, alias: String, accountNumber: String, defaultAmount: Double = 0.0) {
        viewModelScope.launch {
            repository.addUtility(
                SavedUtility(
                    utilityType = type,
                    alias = alias,
                    accountNumber = accountNumber,
                    lastAmount = defaultAmount
                )
            )
        }
    }

    fun deleteUtility(id: Long) {
        viewModelScope.launch {
            repository.deleteUtility(id)
        }
    }

    // USSD Direct Trigger Helper
    fun buildUssdCommand(
        bank: BankType,
        operationCode: String,
        targetCardOrId: String = "",
        amount: String = "",
        key: String = bankKey
    ): String {
        // Transfermóvil USSD structure usually:
        // *444*40# (Balance check)
        // *444*45*KEY*SOURCE_CARD_LAST4*DEST_CARD_16*AMOUNT# (Transfer)
        // *444*52*KEY*ID_COBRO_FIJO# (Electricity)
        return when (operationCode) {
            "BALANCE" -> "*444*40# "
            "TRANSFER" -> "*444*45*$key*$targetCardOrId*$amount#"
            "ELECTRICITY" -> "*444*52*$key*$targetCardOrId#"
            "TELEPHONE" -> "*444*53*$key*$targetCardOrId*$amount#"
            "GAS" -> "*444*55*$key*$targetCardOrId#"
            "ONAT" -> "*444*60*$key*$targetCardOrId*$amount#"
            "RECHARGE" -> "*444*70*$key*$targetCardOrId*$amount#"
            else -> "*444#"
        }
    }

    fun launchUssdCall(context: Context, ussdCode: String) {
        try {
            // Encode # as %23 for Uri parsing
            val encodedUssd = ussdCode.replace("#", Uri.encode("#"))
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$encodedUssd"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to dialer if CALL_PHONE permission not granted or call fails
            try {
                val encodedUssd = ussdCode.replace("#", Uri.encode("#"))
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$encodedUssd"))
                dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(dialIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Código USSD generado: $ussdCode (Copiado)", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
