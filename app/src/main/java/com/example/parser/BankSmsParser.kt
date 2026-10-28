package com.example.parser

import com.example.data.model.BankType
import com.example.data.model.Currency
import com.example.data.model.TransactionType
import java.util.regex.Pattern

data class ParsedSmsResult(
    val success: Boolean,
    val bankType: BankType,
    val cardLast4: String?,
    val currency: Currency,
    val remainingBalance: Double?,
    val transactionAmount: Double?,
    val transactionType: TransactionType,
    val concept: String,
    val referenceCode: String?,
    val rawText: String,
    val errorMessage: String? = null
)

object BankSmsParser {

    fun parse(smsBody: String, sender: String = ""): ParsedSmsResult {
        val upperText = smsBody.uppercase()
        val upperSender = sender.uppercase()

        // 1. Detect Bank
        val bankType = when {
            upperText.contains("BPA") || upperSender.contains("BPA") || upperText.contains("POPULAR DE AHORRO") -> BankType.BPA
            upperText.contains("BANDEC") || upperSender.contains("BANDEC") || upperText.contains("CREDITO Y COMERCIO") -> BankType.BANDEC
            upperText.contains("BANMET") || upperSender.contains("BANMET") || upperText.contains("METROPOLITANO") -> BankType.BANMET
            upperText.contains("PAGOX") || upperSender.contains("PAGOX") || upperText.contains("TRANSFERMOVIL") -> BankType.OTHER
            else -> BankType.OTHER
        }

        // 2. Detect Currency
        val currency = when {
            upperText.contains("MLC") -> Currency.MLC
            upperText.contains("USD") -> Currency.USD
            else -> Currency.CUP
        }

        // 3. Extract Card Last 4 Digits
        val cardLast4 = extractCardLast4(upperText)

        // 4. Extract Remaining Balance (Saldo)
        val remainingBalance = extractRemainingBalance(upperText)

        // 5. Extract Transaction Amount
        val transactionAmount = extractTransactionAmount(upperText)

        // 6. Extract Reference Code
        val referenceCode = extractReferenceCode(upperText)

        // 7. Determine Transaction Type & Concept
        val (transactionType, concept) = extractTypeAndConcept(upperText)

        val isSuccess = remainingBalance != null || cardLast4 != null || transactionAmount != null

        return ParsedSmsResult(
            success = isSuccess,
            bankType = bankType,
            cardLast4 = cardLast4,
            currency = currency,
            remainingBalance = remainingBalance,
            transactionAmount = transactionAmount ?: 0.0,
            transactionType = transactionType,
            concept = concept,
            referenceCode = referenceCode,
            rawText = smsBody,
            errorMessage = if (!isSuccess) "No se pudieron extraer datos financieros clave del mensaje." else null
        )
    }

    private fun extractCardLast4(text: String): String? {
        // Match patterns like: "terminada en 9224", "xxxx9224", "tarjeta 9224", "tarjeta: 9224", "4521xxxx9224"
        val regexes = listOf(
            Regex("""(?:TARJETA|CUENTA|TERMINADA EN|XXXX|TARJETA:)\s*(?:[X\d]{8,12})?(\d{4})"""),
            Regex("""\b(\d{4})\b""")
        )

        for (regex in regexes) {
            val match = regex.find(text)
            if (match != null) {
                val groupVal = match.groupValues.lastOrNull { it.length == 4 }
                if (groupVal != null) return groupVal
            }
        }
        return null
    }

    private fun extractRemainingBalance(text: String): Double? {
        // Patterns for remaining balance
        val balancePatterns = listOf(
            Regex("""SALDO\s*(?:ACTUAL|RESTANTE|DISPONIBLE|ES|EN LA TARJETA|:)?\s*(?:ES\s+DE\s+|ES\s+|DE\s+|:)?\s*\$?([0-9.,]+)"""),
            Regex("""SALDO:\s*\$?([0-9.,]+)"""),
            Regex("""ES\s+\$?([0-9.,]+)\s*(?:CUP|MLC|USD)""")
        )

        for (pattern in balancePatterns) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size >= 2) {
                val rawNum = match.groupValues[1]
                val parsed = parseCubanNumber(rawNum)
                if (parsed != null && parsed >= 0) return parsed
            }
        }
        return null
    }

    private fun extractTransactionAmount(text: String): Double? {
        val amountPatterns = listOf(
            Regex("""(?:MONTO|POR|DE|CANTIDAD|IMPORTE)\s*[:\s]*\$?([0-9.,]+)\s*(?:CUP|MLC|USD)"""),
            Regex("""TRANSACCION:\s*.*?\s+POR\s+\$?([0-9.,]+)"""),
            Regex("""(\d+[.,]\d{2})\s*(?:CUP|MLC|USD)""")
        )

        for (pattern in amountPatterns) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size >= 2) {
                val rawNum = match.groupValues[1]
                val parsed = parseCubanNumber(rawNum)
                if (parsed != null && parsed > 0) return parsed
            }
        }
        return null
    }

    private fun extractReferenceCode(text: String): String? {
        val refPatterns = listOf(
            Regex("""(?:REF|REFERENCIA|CODIGO|TRANSACCION)\s*[:\s]*([0-9A-Z]{5,12})"""),
            Regex("""REF:\s*([0-9A-Z]+)""")
        )

        for (pattern in refPatterns) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size >= 2) {
                return match.groupValues[1]
            }
        }
        return null
    }

    private fun extractTypeAndConcept(text: String): Pair<TransactionType, String> {
        return when {
            text.contains("TRANSFERENCIA RECIBIDA") || text.contains("RECIBIÓ") || text.contains("CREDITO") ->
                Pair(TransactionType.TRANSFER_IN, "Transferencia Recibida")
            text.contains("TRANSFERENCIA ENVIADA") || text.contains("TRANSFERENCIA") || text.contains("ENVIARON") ->
                Pair(TransactionType.TRANSFER_OUT, "Transferencia de Dinero")
            text.contains("PAGO DE") || text.contains("PAGO DE SERVICIO") || text.contains("ELECTRICA") || text.contains("TELEFONO") || text.contains("GAS") || text.contains("ONAT") ->
                Pair(TransactionType.UTILITY_PAYMENT, "Pago de Servicio / Factura")
            text.contains("EXTRACCION") || text.contains("CAJERO") || text.contains("CAJERO AUTOMATICO") ->
                Pair(TransactionType.ATM_WITHDRAWAL, "Extracción de Efectivo")
            text.contains("RECARGA") || text.contains("NAUTA") || text.contains("ETECSA") ->
                Pair(TransactionType.RECHARGE, "Recarga Telefónica/Nauta")
            text.contains("CONSULTA") || text.contains("CONSULTAR") ->
                Pair(TransactionType.BALANCE_INQUIRY, "Consulta de Saldo")
            text.contains("DEPOSITO") ->
                Pair(TransactionType.DEPOSIT, "Depósito Bancario")
            else ->
                Pair(TransactionType.UTILITY_PAYMENT, "Operación Bancaria")
        }
    }

    private fun parseCubanNumber(raw: String): Double? {
        try {
            // Cuban numbers usually use comma as decimal or dot as thousand separator
            // E.g. "15,250.50" or "15250.50" or "15.250,50"
            var clean = raw.trim()
            if (clean.contains(",") && clean.contains(".")) {
                if (clean.lastIndexOf(",") > clean.lastIndexOf(".")) {
                    // "15.250,50" -> "15250.50"
                    clean = clean.replace(".", "").replace(",", ".")
                } else {
                    // "15,250.50" -> "15250.50"
                    clean = clean.replace(",", "")
                }
            } else if (clean.contains(",")) {
                // If it ends with ,XX assume decimal separator e.g. "15250,50"
                if (clean.length - clean.lastIndexOf(",") == 3) {
                    clean = clean.replace(",", ".")
                } else {
                    clean = clean.replace(",", "")
                }
            }
            return clean.toDoubleOrNull()
        } catch (e: Exception) {
            return null
        }
    }
}
