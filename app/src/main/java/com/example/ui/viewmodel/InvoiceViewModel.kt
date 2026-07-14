package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.entity.*
import com.example.data.dao.InvoiceWithRelations
import com.example.data.repository.*
import com.example.utils.BackupRestoreHelper
import com.example.utils.PdfInvoiceGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class InvoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    
    // Repositories
    private val categoryRepository = CategoryRepository(database.categoryDao())
    private val productRepository = ProductRepository(database.productDao())
    private val customerRepository = CustomerRepository(database.customerDao())
    private val invoiceRepository = InvoiceRepository(database.invoiceDao(), database.invoiceItemDao())
    private val paymentRepository = PaymentRepository(database.paymentDao())
    private val companyRepository = CompanyRepository(database.companyInfoDao())
    private val settingsRepository = SettingsRepository(database.settingsDao())
    private val attachmentRepository = AttachmentRepository(database.attachmentDao())

    // Active UI Lists (Observed by view)
    val categories: StateFlow<List<Category>> = categoryRepository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<Product>> = productRepository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = customerRepository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val invoices: StateFlow<List<InvoiceWithRelations>> = invoiceRepository.allInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings & Company Singleton States
    val companyInfo: StateFlow<CompanyInfo?> = companyRepository.companyInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            try {
                // Initialize something if needed
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun seedMockBusinessData() {
        // Mock data generation removed
    }

    // Filtered lists for simple searches
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Dynamic Filtered Products
    val searchedProducts = combine(products, searchQuery) { list, query ->
        if (query.isBlank()) list
        else {
            val normalizedQuery = com.example.utils.PersianDigitConverter.normalizeBarcode(query)
            list.filter {
                it.name.contains(query, ignoreCase = true) ||
                (it.barcode != null && (
                    it.barcode.contains(query) ||
                    com.example.utils.PersianDigitConverter.matchBarcodes(it.barcode, query) ||
                    (normalizedQuery.isNotEmpty() && com.example.utils.PersianDigitConverter.normalizeBarcode(it.barcode).contains(normalizedQuery))
                ))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic Filtered Customers
    val searchedCustomers = combine(customers, searchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter { it.fullName.contains(query, ignoreCase = true) || it.phoneNumber.contains(query) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions: Categories
    fun addCategory(name: String, description: String? = null) {
        viewModelScope.launch {
            categoryRepository.insertCategory(Category(name = name, description = description))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }

    // Actions: Products
    fun addProduct(categoryId: Long?, name: String, unit: String, price: Double, barcode: String? = null, stockQuantity: Double? = null) {
        viewModelScope.launch {
            val product = Product(
                categoryId = categoryId,
                name = name,
                unit = unit,
                defaultUnitPrice = price,
                barcode = barcode,
                stockQuantity = stockQuantity
            )
            productRepository.insertProduct(product)
        }
    }

    suspend fun addProductAndGet(categoryId: Long?, name: String, unit: String, price: Double, barcode: String? = null, description: String? = null, stockQuantity: Double? = null): Long {
        val product = Product(
            categoryId = categoryId,
            name = name,
            unit = unit,
            defaultUnitPrice = price,
            barcode = barcode,
            description = description,
            stockQuantity = stockQuantity
        )
        return productRepository.insertProduct(product)
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            productRepository.insertProduct(product)
        }
    }

    fun deleteProduct(product: Product, onSuccess: (() -> Unit)? = null, onError: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                productRepository.deleteProduct(product)
                onSuccess?.invoke()
            } catch (e: Exception) {
                e.printStackTrace()
                onError?.invoke("خطا در حذف کالا: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    suspend fun getProductByBarcode(barcode: String): Product? {
        return productRepository.getProductByBarcode(barcode)
    }

    // Actions: Customers
    fun addCustomer(fullName: String, phone: String, nationalId: String? = null, code: String? = null, address: String? = null, onSaved: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val customer = Customer(
                fullName = fullName,
                phoneNumber = phone,
                nationalId = nationalId,
                economicCode = code,
                address = address
            )
            val newId = customerRepository.insertCustomer(customer)
            onSaved?.invoke(newId)
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            customerRepository.insertCustomer(customer)
        }
    }

    fun deleteCustomer(customer: Customer, onSuccess: (() -> Unit)? = null, onError: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val invoicesCount = database.invoiceDao().getInvoicesCountForCustomer(customer.id)
                if (invoicesCount > 0) {
                    onError?.invoke("این مشتری دارای فاکتور یا تراکنش ثبت‌شده است و امکان حذف مستقیم وجود ندارد.")
                } else {
                    customerRepository.deleteCustomer(customer)
                    onSuccess?.invoke()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onError?.invoke("خطا در حذف مشتری: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    // Actions: Invoice
    fun generateNextInvoiceNumber(type: String = "INVOICE"): String {
        val currSettings = settings.value ?: AppSettings()
        val prefix = if (type == "PROFORMA") currSettings.proformaPrefix else currSettings.invoicePrefix
        val startNumber = if (type == "PROFORMA") currSettings.proformaStartNumber else currSettings.invoiceStartNumber
        val count = invoices.value.count { it.invoice.invoiceType == type }
        val nextIndex = startNumber + count
        return "$prefix$nextIndex"
    }

    suspend fun createInvoice(
        customerId: Long,
        items: List<InvoiceItem>,
        invoiceType: String,
        discount: Double = 0.0,
        taxPercent: Double = 0.0,
        shipping: Double = 0.0,
        notes: String? = null,
        invoiceNumber: String? = null,
        issueDate: Long = System.currentTimeMillis()
    ): Long = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val finalInvNum = invoiceNumber ?: generateNextInvoiceNumber(invoiceType)
        val invoice = Invoice(
            customerId = customerId,
            invoiceNumber = finalInvNum,
            issueDate = issueDate,
            invoiceType = invoiceType,
            status = "UNPAID",
            discountAmount = discount,
            taxRate = taxPercent,
            shippingFee = shipping,
            notes = notes
        )
        val newId = invoiceRepository.saveInvoiceWithItems(invoice, items)
        
        // Mark as DRAFT if it contains no payments or is isProforma
        if (invoiceType == "PROFORMA") {
            invoiceRepository.updateInvoiceStatus(newId, "DRAFT")
        }
        newId
    }

    suspend fun updateInvoice(
        id: Long,
        customerId: Long,
        items: List<InvoiceItem>,
        invoiceType: String,
        discount: Double,
        taxPercent: Double,
        shipping: Double,
        notes: String?,
        invoiceNumber: String,
        status: String,
        issueDate: Long = System.currentTimeMillis()
    ): Long = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val invoice = Invoice(
            id = id,
            customerId = customerId,
            invoiceNumber = invoiceNumber,
            issueDate = issueDate,
            invoiceType = invoiceType,
            status = status,
            discountAmount = discount,
            taxRate = taxPercent,
            shippingFee = shipping,
            notes = notes
        )
        invoiceRepository.saveInvoiceWithItems(invoice, items)
        id
    }

    fun deleteInvoice(id: Long, onSuccess: (() -> Unit)? = null, onError: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                invoiceRepository.deleteInvoice(id)
                onSuccess?.invoke()
            } catch (e: Exception) {
                e.printStackTrace()
                onError?.invoke("خطا در حذف فاکتور: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun updateInvoiceStatus(id: Long, status: String) {
        viewModelScope.launch {
            invoiceRepository.updateInvoiceStatus(id, status)
        }
    }

    fun getInvoiceById(id: Long): Flow<InvoiceWithRelations?> {
        return invoiceRepository.getInvoiceById(id)
    }

    // Actions: Attachments
    fun getAttachmentsForInvoice(invoiceId: Long): Flow<List<Attachment>> {
        return attachmentRepository.getAttachmentsForInvoice(invoiceId)
    }

    fun addAttachment(invoiceId: Long, fileName: String, filePath: String, mimeType: String) {
        viewModelScope.launch {
            attachmentRepository.insertAttachment(
                Attachment(
                    invoiceId = invoiceId,
                    fileName = fileName,
                    filePath = filePath,
                    mimeType = mimeType
                )
            )
        }
    }

    fun deleteAttachment(attachment: Attachment) {
        viewModelScope.launch {
            attachmentRepository.deleteAttachment(attachment)
            // Also delete the physical file
            try {
                val file = java.io.File(attachment.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteAttachmentById(attachmentId: Long) {
        viewModelScope.launch {
            val attachment = attachmentRepository.getAttachmentById(attachmentId)
            if (attachment != null) {
                deleteAttachment(attachment)
            }
        }
    }

    // Actions: Payments
    fun addPayment(invoiceId: Long, amount: Double, method: String, notes: String?, ref: String? = null) {
        viewModelScope.launch {
            val payment = Payment(
                invoiceId = invoiceId,
                amount = amount,
                paymentDate = System.currentTimeMillis(),
                paymentMethod = method,
                referenceNumber = ref,
                notes = notes
            )
            paymentRepository.insertPayment(payment)
            
            // Re-evaluate Invoice paid status
            val inv = invoiceRepository.getInvoiceByIdSync(invoiceId) ?: return@launch
            val total = inv.calculateTotal()
            val paid = inv.calculatePaid() + amount
            val newStatus = when {
                paid >= total -> "PAID"
                paid > 0.0 -> "PARTIALLY_PAID"
                else -> "UNPAID"
            }
            invoiceRepository.updateInvoiceStatus(invoiceId, newStatus)
        }
    }

    // Actions: Company & Settings Profile Save
    fun saveCompanyInfo(info: CompanyInfo) {
        viewModelScope.launch {
            companyRepository.saveCompanyInfo(info)
        }
    }

    fun saveSettings(appSettings: AppSettings) {
        viewModelScope.launch {
            settingsRepository.saveSettings(appSettings)
        }
    }

    // Backups and Restore Orchestration
    fun createBackup(context: Context): Uri? {
        return BackupRestoreHelper.backupDatabase(context)
    }

    fun restoreBackup(context: Context, uri: Uri): Boolean {
        return BackupRestoreHelper.restoreDatabase(context, uri)
    }

    // PDF export helper trigger
    suspend fun generateInvoicePdf(
        context: Context, 
        invoice: InvoiceWithRelations, 
        includeAttachments: Boolean = false,
        attachments: List<Attachment>? = null
    ): Uri? {
        val activeCo = companyRepository.getCompanyInfoSync() ?: CompanyInfo()
        val activeSet = settingsRepository.getSettingsSync() ?: AppSettings()
        
        // Calculate previous unpaid balance of this customer (excluding this invoice)
        val allUserInvoices = invoices.value.filter { 
            it.invoice.customerId == invoice.invoice.customerId && it.invoice.id != invoice.invoice.id 
        }
        val previousBalance = allUserInvoices.sumOf { it.calculateRemaining() }
        
        return PdfInvoiceGenerator.generateInvoicePdf(context, invoice, activeCo, activeSet, previousBalance, includeAttachments, attachments)
    }

    fun resetPaymentsAndSetTotalPaid(invoiceId: Long, totalPaid: Double) {
        viewModelScope.launch {
            // Because we don't have a direct 'deletePaymentsForInvoice' we can just get them and delete one by one
            val existingPayments = invoiceRepository.getInvoiceByIdSync(invoiceId)?.payments ?: emptyList()
            existingPayments.forEach {
                paymentRepository.deletePayment(it.id)
            }
            if (totalPaid > 0) {
                paymentRepository.insertPayment(Payment(
                    invoiceId = invoiceId,
                    amount = totalPaid,
                    paymentDate = System.currentTimeMillis(),
                    paymentMethod = "CASH" // default
                ))
            }
            
            // Re-evaluate Invoice paid status
            val inv = invoiceRepository.getInvoiceByIdSync(invoiceId) ?: return@launch
            val total = inv.calculateTotal()
            val newStatus = when {
                totalPaid >= total - 0.5 -> "PAID"
                totalPaid > 0.0 -> "PARTIALLY_PAID"
                else -> "UNPAID"
            }
            if (inv.invoice.status != newStatus) {
                invoiceRepository.updateInvoiceStatus(invoiceId, newStatus)
            }
        }
    }
}
