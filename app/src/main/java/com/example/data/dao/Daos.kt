package com.example.data.dao

import androidx.room.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Delete
    suspend fun deleteCategory(category: Category)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Delete
    suspend fun deleteProduct(product: Product)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY fullName ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    fun getCustomerById(id: Long): Flow<Customer?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Delete
    suspend fun deleteCustomer(customer: Customer)
}

data class InvoiceWithRelations(
    @Embedded val invoice: Invoice,
    @Relation(
        parentColumn = "customerId",
        entityColumn = "id"
    )
    val customer: Customer,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val items: List<InvoiceItem>,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val payments: List<Payment>,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val attachments: List<Attachment> = emptyList()
) {
    fun calculateTotal(): Double {
        var itemsTotal = 0.0
        for (item in items) {
            val itemCost = (item.unitPrice * item.quantity) - item.discountAmount + item.taxAmount
            itemsTotal += itemCost
        }
        val withTax = itemsTotal + (itemsTotal * (invoice.taxRate / 100.0))
        return withTax - invoice.discountAmount + invoice.shippingFee
    }

    fun calculatePaid(): Double {
        return payments.sumOf { it.amount }
    }

    fun calculateRemaining(): Double {
        return calculateTotal() - calculatePaid()
    }
}

@Dao
interface InvoiceDao {
    @Transaction
    @Query("SELECT * FROM invoices ORDER BY issueDate DESC, id DESC")
    fun getAllInvoices(): Flow<List<InvoiceWithRelations>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    fun getInvoiceById(id: Long): Flow<InvoiceWithRelations?>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getInvoiceByIdSync(id: Long): InvoiceWithRelations?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Query("SELECT COUNT(*) FROM invoices WHERE customerId = :customerId")
    suspend fun getInvoicesCountForCustomer(customerId: Long): Int

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoiceByInvoiceId(id: Long)

    @Query("UPDATE invoices SET status = :status WHERE id = :id")
    suspend fun updateInvoiceStatus(id: Long, status: String)
}

@Dao
interface InvoiceItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItem(item: InvoiceItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItem>)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteInvoiceItemsForInvoice(invoiceId: Long)
}

@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Query("SELECT * FROM payments WHERE invoiceId = :invoiceId ORDER BY paymentDate DESC")
    fun getPaymentsForInvoice(invoiceId: Long): Flow<List<Payment>>

    @Query("DELETE FROM payments WHERE id = :paymentId")
    suspend fun deletePaymentById(paymentId: Long)
}

@Dao
interface CompanyInfoDao {
    @Query("SELECT * FROM company_info WHERE id = 1 LIMIT 1")
    fun getCompanyInfo(): Flow<CompanyInfo?>

    @Query("SELECT * FROM company_info WHERE id = 1 LIMIT 1")
    suspend fun getCompanyInfoSync(): CompanyInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompanyInfo(info: CompanyInfo)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<AppSettings?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: AppSettings)
}

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE invoiceId = :invoiceId ORDER BY createdAt DESC")
    fun getAttachmentsForInvoice(invoiceId: Long): Flow<List<Attachment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: Attachment): Long

    @Delete
    suspend fun deleteAttachment(attachment: Attachment)

    @Query("DELETE FROM attachments WHERE id = :attachmentId")
    suspend fun deleteAttachmentById(attachmentId: Long)
    
    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun getAttachmentById(id: Long): Attachment?
}
