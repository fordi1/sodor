package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE products ADD COLUMN stockQuantity REAL NOT NULL DEFAULT 0.0")
    }
}

@Database(
    entities = [
        Category::class,
        Product::class,
        Customer::class,
        Invoice::class,
        InvoiceItem::class,
        Payment::class,
        CompanyInfo::class,
        AppSettings::class,
        Attachment::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun invoiceItemDao(): InvoiceItemDao
    abstract fun paymentDao(): PaymentDao
    abstract fun companyInfoDao(): CompanyInfoDao
    abstract fun settingsDao(): SettingsDao
    abstract fun attachmentDao(): AttachmentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "daftar_invoice_database"
                )
                .addCallback(DatabaseCallback(context))
                .addMigrations(MIGRATION_5_6)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val dbInstance = getDatabase(context)
                    // 1. Seed Settings (Only essential defaults)
                    dbInstance.settingsDao().insertSettings(
                        AppSettings(
                            id = 1,
                            currencyUnit = "تومان",
                            defaultTaxRate = 0.0,
                            pdfTemplateType = "OFFICIAL",
                            invoicePrefix = "INV-"
                        )
                    )
                    
                    // Seed empty company info
                    dbInstance.companyInfoDao().insertCompanyInfo(
                        CompanyInfo(id = 1)
                    )
                }
            }
        }
    }
}
