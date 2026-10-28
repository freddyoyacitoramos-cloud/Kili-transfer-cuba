package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BankCard
import com.example.data.model.SavedUtility
import com.example.data.model.TransactionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface BankCardDao {
    @Query("SELECT * FROM bank_cards ORDER BY isDefault DESC, updatedAt DESC")
    fun getAllCards(): Flow<List<BankCard>>

    @Query("SELECT * FROM bank_cards WHERE cardLast4 = :last4 LIMIT 1")
    suspend fun getCardByLast4(last4: String): BankCard?

    @Query("SELECT * FROM bank_cards WHERE id = :id LIMIT 1")
    suspend fun getCardById(id: Long): BankCard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: BankCard): Long

    @Update
    suspend fun updateCard(card: BankCard)

    @Query("UPDATE bank_cards SET balance = :newBalance, updatedAt = :timestamp WHERE cardLast4 = :last4")
    suspend fun updateCardBalanceByLast4(last4: String, newBalance: Double, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM bank_cards WHERE id = :id")
    suspend fun deleteCardById(id: Long)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transaction_records ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transaction_records WHERE cardLast4 = :last4 ORDER BY timestamp DESC")
    fun getTransactionsByCard(last4: String): Flow<List<TransactionRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionRecord): Long

    @Query("DELETE FROM transaction_records")
    suspend fun clearAll()
}

@Dao
interface UtilityDao {
    @Query("SELECT * FROM saved_utilities ORDER BY updatedAt DESC")
    fun getAllUtilities(): Flow<List<SavedUtility>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUtility(utility: SavedUtility): Long

    @Query("DELETE FROM saved_utilities WHERE id = :id")
    suspend fun deleteUtility(id: Long)
}
