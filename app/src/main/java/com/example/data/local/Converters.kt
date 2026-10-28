package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.BankType
import com.example.data.model.Currency
import com.example.data.model.TransactionType

class Converters {
    @TypeConverter
    fun fromBankType(value: BankType): String = value.name

    @TypeConverter
    fun toBankType(value: String): BankType = try {
        BankType.valueOf(value)
    } catch (e: Exception) {
        BankType.OTHER
    }

    @TypeConverter
    fun fromCurrency(value: Currency): String = value.name

    @TypeConverter
    fun toCurrency(value: String): Currency = try {
        Currency.valueOf(value)
    } catch (e: Exception) {
        Currency.CUP
    }

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = try {
        TransactionType.valueOf(value)
    } catch (e: Exception) {
        TransactionType.BALANCE_INQUIRY
    }
}
