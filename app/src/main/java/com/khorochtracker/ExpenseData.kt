package com.khorochtracker

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,
    val amount: Double,
    val category: String,
    val note: String,
    val date: String
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>
    @Insert suspend fun insert(item: TransactionEntity)
    @Insert suspend fun insertAll(items: List<TransactionEntity>)
    @Update suspend fun update(item: TransactionEntity)
    @Delete suspend fun delete(item: TransactionEntity)
    @Query("DELETE FROM transactions") suspend fun clear()
}

@Database(entities = [TransactionEntity::class], version = 2, exportSchema = false)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    companion object {
        @Volatile private var INSTANCE: ExpenseDatabase? = null
        fun get(context: Context): ExpenseDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, ExpenseDatabase::class.java, "khoroch_database"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}

class ExpenseViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = ExpenseDatabase.get(app).transactionDao()
    val transactions = dao.observeAll()
    fun add(type: String, amount: Double, category: String, note: String, date: String) {
        viewModelScope.launch { dao.insert(TransactionEntity(type = type, amount = amount, category = category, note = note, date = date)) }
    }
    fun update(item: TransactionEntity) { viewModelScope.launch { dao.update(item) } }
    fun delete(item: TransactionEntity) { viewModelScope.launch { dao.delete(item) } }
    fun replaceAll(items: List<TransactionEntity>) {
        viewModelScope.launch { dao.clear(); dao.insertAll(items.map { it.copy(id = 0) }) }
    }
}
