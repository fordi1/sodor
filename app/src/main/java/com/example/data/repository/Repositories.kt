package com.example.data.repository

import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CategoryRepository(private val dao: CategoryDao) {
    val allCategories: Flow<List<Category>> = dao.getAllCategories()

    suspend fun insertCategory(category: Category): Long = withContext(Dispatchers.IO) {
        dao.insertCategory(category)
    }

    suspend fun deleteCategory(category: Category) = withContext(Dispatchers.IO) {
        dao.deleteCategory(category)
    }
}

class ProductRepository(private val dao: ProductDao) {
    val allProducts: Flow<List<Product>> = dao.getAllProducts()

    suspend fun getProductById(id: Long): Product? = withContext(Dispatchers.IO) {
        dao.getProductById(id)
    }

    suspend fun getProductByBarcode(barcode: String): Product? = withContext(Dispatchers.IO) {
        dao.getProductByBarcode(barcode)
    }

    suspend fun insertProduct(product: Product): Long = withContext(Dispatchers.IO) {
        dao.insertProduct(product)
    }

    suspend fun deleteProduct(product: Product) = withContext(Dispatchers.IO) {
        dao.deleteProduct(product)
    }
}

class CustomerRepository(private val dao: CustomerDao) {
    val allCustomers: Flow<List<Customer>> = dao.getAllCustomers()

    fun getCustomerById(id: Long): Flow<Customer?> = dao.getCustomerById(id)

    suspend fun insertCustomer(customer: Customer): Long = withContext(Dispatchers.IO) {
        dao.insertCustomer(customer)
    }

    suspend fun deleteCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        dao.deleteCustomer(customer)
    }
}

class InvoiceRepository(
    private val invoiceDao: InvoiceDao,
    private val itemDao: InvoiceItemDao
) {
    val allInvoices: Flow<List<InvoiceWithRelations>> = invoiceDao.getAllInvoices()

    fun getInvoiceById(id: Long): Flow<InvoiceWithRelations?> = invoiceDao.getInvoiceById(id)

    suspend fun getInvoiceByIdSync(id: Long): InvoiceWithRelations? = withContext(Dispatchers.IO) {
        invoiceDao.getInvoiceByIdSync(id)
    }

    suspend fun insertInvoice(invoice: Invoice): Long = withContext(Dispatchers.IO) {
        invoiceDao.insertInvoice(invoice)
    }

    suspend fun saveInvoiceWithItems(invoice: Invoice, items: List<InvoiceItem>): Long = withContext(Dispatchers.IO) {
        val invoiceId = invoiceDao.insertInvoice(invoice)
        // Clean previous items if updating
        itemDao.deleteInvoiceItemsForInvoice(invoiceId)
        val itemsWithId = items.map { it.copy(invoiceId = invoiceId) }
        itemDao.insertInvoiceItems(itemsWithId)
        invoiceId
    }

    suspend fun deleteInvoice(id: Long) = withContext(Dispatchers.IO) {
        invoiceDao.deleteInvoiceByInvoiceId(id)
    }

    suspend fun updateInvoiceStatus(id: Long, status: String) = withContext(Dispatchers.IO) {
        invoiceDao.updateInvoiceStatus(id, status)
    }
}

class PaymentRepository(private val dao: PaymentDao) {
    suspend fun insertPayment(payment: Payment): Long = withContext(Dispatchers.IO) {
        dao.insertPayment(payment)
    }

    fun getPaymentsForInvoice(invoiceId: Long): Flow<List<Payment>> = dao.getPaymentsForInvoice(invoiceId)

    suspend fun deletePayment(paymentId: Long) = withContext(Dispatchers.IO) {
        dao.deletePaymentById(paymentId)
    }
}

class CompanyRepository(private val dao: CompanyInfoDao) {
    val companyInfo: Flow<CompanyInfo?> = dao.getCompanyInfo()

    suspend fun getCompanyInfoSync(): CompanyInfo? = withContext(Dispatchers.IO) {
        dao.getCompanyInfoSync()
    }

    suspend fun saveCompanyInfo(info: CompanyInfo) = withContext(Dispatchers.IO) {
        dao.insertCompanyInfo(info)
    }
}

class SettingsRepository(private val dao: SettingsDao) {
    val settings: Flow<AppSettings?> = dao.getSettings()

    suspend fun getSettingsSync(): AppSettings? = withContext(Dispatchers.IO) {
        dao.getSettingsSync()
    }

    suspend fun saveSettings(settings: AppSettings) = withContext(Dispatchers.IO) {
        dao.insertSettings(settings)
    }
}
