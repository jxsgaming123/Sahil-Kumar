package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "medication_reminders")
data class MedicationReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicineName: String,
    val dosage: String,
    val time: String, // format e.g. "08:30 AM"
    val isActive: Boolean = true,
    val dayOfWeek: String = "Daily",
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ReminderDao {
    @Query("SELECT * FROM medication_reminders ORDER BY createdAt DESC")
    fun getAllReminders(): Flow<List<MedicationReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: MedicationReminder): Long

    @Update
    suspend fun updateReminder(reminder: MedicationReminder)

    @Delete
    suspend fun deleteReminder(reminder: MedicationReminder)

    @Query("DELETE FROM medication_reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Int)
}

@Database(entities = [MedicationReminder::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "generation_connect_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ReminderRepository(private val reminderDao: ReminderDao) {
    val allReminders: Flow<List<MedicationReminder>> = reminderDao.getAllReminders()

    suspend fun insert(reminder: MedicationReminder): Long {
        return reminderDao.insertReminder(reminder)
    }

    suspend fun update(reminder: MedicationReminder) {
        reminderDao.updateReminder(reminder)
    }

    suspend fun delete(reminder: MedicationReminder) {
        reminderDao.deleteReminder(reminder)
    }

    suspend fun deleteById(id: Int) {
        reminderDao.deleteReminderById(id)
    }
}
