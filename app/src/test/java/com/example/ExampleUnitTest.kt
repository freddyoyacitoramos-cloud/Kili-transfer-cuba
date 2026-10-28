package com.example

import com.example.data.model.BankType
import com.example.data.model.Currency
import com.example.parser.BankSmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testBpaSmsParsing() {
        val sms = "BPA: Confirmacion de operacion. Su saldo actual en la tarjeta 9224 es 18450.00 CUP. Transferencia recibida por 3000.00 CUP. Ref: 994012."
        val result = BankSmsParser.parse(sms)

        assertTrue(result.success)
        assertEquals(BankType.BPA, result.bankType)
        assertEquals("9224", result.cardLast4)
        assertEquals(18450.00, result.remainingBalance!!, 0.01)
        assertEquals(Currency.CUP, result.currency)
        assertEquals("994012", result.referenceCode)
    }

    @Test
    fun testBandecSmsParsing() {
        val sms = "BANDEC: Transaccion completada. Saldo restante en tarjeta xxxx4321: 9150.00 MLC. Deposito acreditado de 830.00 MLC. Ref: 220193."
        val result = BankSmsParser.parse(sms)

        assertTrue(result.success)
        assertEquals(BankType.BANDEC, result.bankType)
        assertEquals("4321", result.cardLast4)
        assertEquals(9150.00, result.remainingBalance!!, 0.01)
        assertEquals(Currency.MLC, result.currency)
        assertEquals("220193", result.referenceCode)
    }

    @Test
    fun testBanmetSmsParsing() {
        val sms = "BANMET: Operacion efectuada. Saldo disponible: 40100.00 CUP en tarjeta xxxx8877. Extraccion en cajero automatico por 2000.00 CUP. Ref: 338102."
        val result = BankSmsParser.parse(sms)

        assertTrue(result.success)
        assertEquals(BankType.BANMET, result.bankType)
        assertEquals("8877", result.cardLast4)
        assertEquals(40100.00, result.remainingBalance!!, 0.01)
        assertEquals(Currency.CUP, result.currency)
        assertEquals("338102", result.referenceCode)
    }
}
