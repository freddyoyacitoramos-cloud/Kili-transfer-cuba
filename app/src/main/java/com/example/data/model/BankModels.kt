package com.example.data.model

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BankType(
    val displayName: String,
    val shortName: String,
    val primaryColorHex: String,
    val ussdCode: String
) {
    BPA(
        displayName = "Banco Popular de Ahorro",
        shortName = "BPA",
        primaryColorHex = "#1E3A8A", // Royal Navy Blue
        ussdCode = "*444#"
    ),
    BANDEC(
        displayName = "Banco de Crédito y Comercio",
        shortName = "BANDEC",
        primaryColorHex = "#065F46", // Deep Emerald
        ussdCode = "*444#"
    ),
    BANMET(
        displayName = "Banco Metropolitano",
        shortName = "BANMET",
        primaryColorHex = "#831843", // Deep Wine / Gold Accent
        ussdCode = "*444#"
    ),
    OTHER(
        displayName = "Otra Entidad Financiera",
        shortName = "OTROS",
        primaryColorHex = "#334155", // Slate
        ussdCode = "*444#"
    );

    fun getColor(): Color {
        return try {
            Color(android.graphics.Color.parseColor(primaryColorHex))
        } catch (e: Exception) {
            Color(0xFF1E3A8A)
        }
    }
}

enum class Currency(val symbol: String, val code: String) {
    CUP("₱", "CUP"),
    MLC("$", "MLC"),
    USD("$", "USD")
}

enum class TransactionType(val label: String, val isExpense: Boolean) {
    TRANSFER_OUT("Transferencia Enviada", true),
    TRANSFER_IN("Transferencia Recibida", false),
    UTILITY_PAYMENT("Pago de Servicio", true),
    ATM_WITHDRAWAL("Extracción en Cajero", true),
    BALANCE_INQUIRY("Consulta de Saldo", false),
    RECHARGE("Recarga Telefónica/Nauta", true),
    DEPOSIT("Depósito / Crédito", false)
}

@Entity(tableName = "bank_cards")
data class BankCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bankType: BankType,
    val cardLast4: String,
    val maskedCardNumber: String,
    val cardHolder: String = "CUBA USER",
    val currency: Currency,
    val balance: Double,
    val isDefault: Boolean = false,
    val cardColorHex: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transaction_records")
data class TransactionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardLast4: String,
    val bankType: BankType,
    val type: TransactionType,
    val amount: Double,
    val currency: Currency,
    val balanceAfter: Double,
    val concept: String,
    val reference: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val rawSms: String = ""
)

@Entity(tableName = "saved_utilities")
data class SavedUtility(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val utilityType: String, // "ELECTRICITY", "TELEPHONE", "GAS", "ONAT", "NAUTA"
    val alias: String,
    val accountNumber: String,
    val lastAmount: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)
