package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("barcode"), Index("name")]
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long? = null,
    val name: String,
    val description: String? = null,
    val unit: String = "عدد",
    val defaultUnitPrice: Double = 0.0,
    val barcode: String? = null,
    val taxRate: Double = 0.0,
    val stockQuantity: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customers",
    indices = [Index("fullName"), Index("phoneNumber")]
)
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val phoneNumber: String,
    val nationalId: String? = null,
    val economicCode: String? = null,
    val address: String? = null,
    val postalCode: String? = null,
    val email: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("invoiceNumber", unique = true), Index("customerId")]
)
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val invoiceNumber: String,
    val issueDate: Long,
    val dueDate: Long? = null,
    val invoiceType: String, // "INVOICE" or "PROFORMA"
    val status: String = "DRAFT", // "DRAFT", "UNPAID", "PARTIALLY_PAID", "PAID", "CANCELLED"
    val discountAmount: Double = 0.0,
    val taxRate: Double = 0.0,
    val shippingFee: Double = 0.0,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = Invoice::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("invoiceId"), Index("productId")]
)
data class InvoiceItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val productId: Long? = null,
    val productName: String,
    val unitPrice: Double,
    val unit: String,
    val quantity: Double,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0
)

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = Invoice::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("invoiceId")]
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val amount: Double,
    val paymentDate: Long,
    val paymentMethod: String, // "CASH", "CARD", "POS", "CHEQUE", "SHABA"
    val referenceNumber: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "company_info")
data class CompanyInfo(
    @PrimaryKey val id: Int = 1, // Singleton row
    val companyName: String = "",
    val managerName: String? = null,
    val nationalId: String? = null,
    val economicCode: String? = null,
    val registrationNumber: String? = null,
    val phoneNumber: String = "",
    val address: String? = null,
    val postalCode: String? = null,
    val email: String? = null,
    val description: String? = null, // Added description for Store Profile
    val logoPath: String? = null,
    val signaturePath: String? = null,
    val stampPath: String? = null,
    val personalStampPath: String? = null,
    val bankName: String? = null,
    val accountHolderName: String? = null, // Added account holder name
    val cardNumber: String? = null,
    val shabaNumber: String? = null,
    val accountNumber: String? = null, // Added account number
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1, // Singleton row
    val currencyUnit: String = "تومان", // "تومان", "ریال"
    val usePersianNumbers: Boolean = true, // Added Persian numbers toggle
    val defaultTaxRate: Double = 0.0, // Default 0%
    val pdfTemplateType: String = "OFFICIAL", // "OFFICIAL", "SIMPLE"
    val themeMode: String = "LIGHT", // "LIGHT", "DARK", "SYSTEM"
    val requireBiometricAuth: Boolean = false,
    val invoicePrefix: String = "INV-",
    val proformaPrefix: String = "PRO-", // Added proforma prefix
    val invoiceStartNumber: Int = 1, // Added invoice start number
    val proformaStartNumber: Int = 1, // Added proforma start number
    val showInvoiceLogo: Boolean = true,
    val showInvoiceBusinessStamp: Boolean = true,
    val showInvoicePersonalStamp: Boolean = true,
    val showInvoiceSignature: Boolean = true,
    val showProformaLogo: Boolean = true,
    val showProformaBusinessStamp: Boolean = true,
    val showProformaPersonalStamp: Boolean = true,
    val showProformaSignature: Boolean = true,
    val showItemDetailsOnInvoice: Boolean = true, // Added detail visibility
    val sendSmsOnSave: Boolean = false, // Added SMS toggle
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = Invoice::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("invoiceId")]
)
data class Attachment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val fileName: String,
    val filePath: String,
    val mimeType: String,
    val createdAt: Long = System.currentTimeMillis()
)
