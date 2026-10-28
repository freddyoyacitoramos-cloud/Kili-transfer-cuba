package com.example.data.repository

import com.example.data.local.BankCardDao
import com.example.data.local.TransactionDao
import com.example.data.local.UtilityDao
import com.example.data.model.BankCard
import com.example.data.model.BankType
import com.example.data.model.Currency
import com.example.data.model.SavedUtility
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.parser.BankSmsParser
import com.example.parser.ParsedSmsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class BankRepository(
    private val cardDao: BankCardDao,
    private val transactionDao: TransactionDao,
    private val utilityDao: UtilityDao
) {

    val allCards: Flow<List<BankCard>> = cardDao.getAllCards()
    val allTransactions: Flow<List<TransactionRecord>> = transactionDao.getAllTransactions()
    val allUtilities: Flow<List<SavedUtility>> = utilityDao.getAllUtilities()

    suspend fun seedSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingCards = cardDao.getAllCards().firstOrNull()
        if (existingCards.isNullOrEmpty()) {
            // Seed BPA CUP Card
            cardDao.insertCard(
                BankCard(
                    bankType = BankType.BPA,
                    cardLast4 = "9224",
                    maskedCardNumber = "9225 3410 8890 9224",
                    cardHolder = "JUAN PEREZ GONZALEZ",
                    currency = Currency.CUP,
                    balance = 15450.50,
                    isDefault = true,
                    cardColorHex = "#1E3A8A"
                )
            )

            // Seed BANDEC MLC Card
            cardDao.insertCard(
                BankCard(
                    bankType = BankType.BANDEC,
                    cardLast4 = "4321",
                    maskedCardNumber = "9228 1045 7711 4321",
                    cardHolder = "JUAN PEREZ GONZALEZ",
                    currency = Currency.MLC,
                    balance = 8320.00,
                    isDefault = false,
                    cardColorHex = "#065F46"
                )
            )

            // Seed BANMET CUP Card
            cardDao.insertCard(
                BankCard(
                    bankType = BankType.BANMET,
                    cardLast4 = "8877",
                    maskedCardNumber = "9226 9081 2244 8877",
                    cardHolder = "JUAN PEREZ GONZALEZ",
                    currency = Currency.CUP,
                    balance = 42100.00,
                    isDefault = false,
                    cardColorHex = "#831843"
                )
            )

            // Seed sample transactions
            transactionDao.insertTransaction(
                TransactionRecord(
                    cardLast4 = "9224",
                    bankType = BankType.BPA,
                    type = TransactionType.TRANSFER_IN,
                    amount = 1200.00,
                    currency = Currency.CUP,
                    balanceAfter = 15450.50,
                    concept = "Transferencia Recibida de xxxx1122",
                    reference = "882019",
                    timestamp = System.currentTimeMillis() - 3600000 * 2,
                    rawSms = "BPA: Confirmacion de operacion. Su saldo actual en la tarjeta 9224 es 15450.50 CUP. Transferencia recibida por 1200.00 CUP. Ref: 882019."
                )
            )

            transactionDao.insertTransaction(
                TransactionRecord(
                    cardLast4 = "4321",
                    bankType = BankType.BANDEC,
                    type = TransactionType.UTILITY_PAYMENT,
                    amount = 45.00,
                    currency = Currency.MLC,
                    balanceAfter = 8320.00,
                    concept = "Pago en Tienda MLC",
                    reference = "449201",
                    timestamp = System.currentTimeMillis() - 3600000 * 24,
                    rawSms = "BANDEC: Transaccion completada. Saldo restante en tarjeta xxxx4321: 8320.00 MLC. Pago realizado de 45.00 MLC. Ref: 449201."
                )
            )

            // Seed sample utilities
            utilityDao.insertUtility(
                SavedUtility(
                    utilityType = "ELECTRICIDAD",
                    alias = "Casa Particular (Habana)",
                    accountNumber = "10982345120",
                    lastAmount = 145.80
                )
            )

            utilityDao.insertUtility(
                SavedUtility(
                    utilityType = "TELEFONO",
                    alias = "Móvil Personal ETECSA",
                    accountNumber = "5352109843",
                    lastAmount = 250.00
                )
            )

            utilityDao.insertUtility(
                SavedUtility(
                    utilityType = "GAS",
                    alias = "Gas Manufacturado Plaza",
                    accountNumber = "0402198",
                    lastAmount = 12.00
                )
            )
        }
    }

    suspend fun processIncomingSms(smsBody: String, sender: String = ""): ParsedSmsResult = withContext(Dispatchers.IO) {
        val parsed = BankSmsParser.parse(smsBody, sender)

        if (parsed.success) {
            val cardLast4 = parsed.cardLast4 ?: "9224" // Fallback to primary if unlisted
            var existingCard = cardDao.getCardByLast4(cardLast4)

            // If card exists, update balance
            if (existingCard != null) {
                val newBal = parsed.remainingBalance ?: existingCard.balance
                cardDao.updateCardBalanceByLast4(cardLast4, newBal)
            } else {
                // Auto create card if not found
                val newCard = BankCard(
                    bankType = parsed.bankType,
                    cardLast4 = cardLast4,
                    maskedCardNumber = "XXXX XXXX XXXX $cardLast4",
                    cardHolder = "CUBA USER",
                    currency = parsed.currency,
                    balance = parsed.remainingBalance ?: 0.0,
                    isDefault = false,
                    cardColorHex = parsed.bankType.primaryColorHex
                )
                cardDao.insertCard(newCard)
                existingCard = newCard
            }

            // Create Transaction Record
            val txRecord = TransactionRecord(
                cardLast4 = cardLast4,
                bankType = parsed.bankType,
                type = parsed.transactionType,
                amount = parsed.transactionAmount ?: 0.0,
                currency = parsed.currency,
                balanceAfter = parsed.remainingBalance ?: (existingCard?.balance ?: 0.0),
                concept = parsed.concept,
                reference = parsed.referenceCode ?: "AUTO-${System.currentTimeMillis() % 100000}",
                timestamp = System.currentTimeMillis(),
                rawSms = smsBody
            )
            transactionDao.insertTransaction(txRecord)
        }

        parsed
    }

    suspend fun addNewCard(card: BankCard) = withContext(Dispatchers.IO) {
        cardDao.insertCard(card)
    }

    suspend fun updateCardBalance(last4: String, newBalance: Double) = withContext(Dispatchers.IO) {
        cardDao.updateCardBalanceByLast4(last4, newBalance)
    }

    suspend fun deleteCard(cardId: Long) = withContext(Dispatchers.IO) {
        cardDao.deleteCardById(cardId)
    }

    suspend fun addUtility(utility: SavedUtility) = withContext(Dispatchers.IO) {
        utilityDao.insertUtility(utility)
    }

    suspend fun deleteUtility(id: Long) = withContext(Dispatchers.IO) {
        utilityDao.deleteUtility(id)
    }

    suspend fun clearTransactions() = withContext(Dispatchers.IO) {
        transactionDao.clearAll()
    }
}
