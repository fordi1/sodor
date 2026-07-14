package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.navigation.NavController
import com.example.ui.viewmodel.InvoiceViewModel
import com.example.utils.formatPrice
import com.example.utils.toPersianDigits
import com.example.utils.JalaliDateFormatter
import com.example.utils.IranianValidationHelper
import com.example.utils.PersianDigitConverter
import com.example.ui.theme.LocalIsDarkTheme
import com.example.data.entity.Customer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(navController: NavController, viewModel: InvoiceViewModel, customerId: Long) {
    val isDark = LocalIsDarkTheme.current
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsState()
    val allInvoices by viewModel.invoices.collectAsState()
    val settings by viewModel.settings.collectAsState()
    
    val currency = settings?.currencyUnit ?: "تومان"
    val customer = customers.firstOrNull { it.id == customerId }
    
    val customerInvoices = allInvoices.filter { it.invoice.customerId == customerId && it.invoice.invoiceType == "INVOICE" }
    val customerProformas = allInvoices.filter { it.invoice.customerId == customerId && it.invoice.invoiceType == "PROFORMA" }
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var showEditDialog by remember { mutableStateOf(false) }
    
    if (customer == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("مشتری یافت نشد.")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.popBackStack() }) {
                Text("بازگشت")
            }
        }
        return
    }

    val totalInvoiceAmount = customerInvoices.sumOf { rel ->
        val subtotal = rel.items.sumOf { it.unitPrice * it.quantity }
        val tax = subtotal * (rel.invoice.taxRate / 100.0)
        subtotal + tax + rel.invoice.shippingFee - rel.invoice.discountAmount
    }
    
    val totalPaid = customerInvoices.sumOf { rel ->
        rel.payments.sumOf { it.amount }
    }
    
    val balance = totalInvoiceAmount - totalPaid
    
    // Calculate paid vs unpaid for chart
    val paidPercentage = if (totalInvoiceAmount > 0) (totalPaid / totalInvoiceAmount).toFloat().coerceIn(0f, 1f) else 0f
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("جزئیات مشتری", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "ویرایش مشتری")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }
            
            // Customer Details Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(customer.fullName, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("شماره تماس: ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            Text(customer.phoneNumber.toPersianDigits(), fontSize = 14.sp)
                        }
                        
                        if (!customer.address.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("آدرس: ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                Text(customer.address.toPersianDigits(), fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
            
            // Financial Summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        title = "جمع کل فاکتورها",
                        amount = formatPrice(totalInvoiceAmount),
                        currency = currency,
                        color = if (isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "تعداد",
                        amount = "${customerInvoices.size}".toPersianDigits(),
                        currency = "فاکتور",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        title = "پرداخت شده",
                        amount = formatPrice(totalPaid),
                        currency = currency,
                        color = if (isDark) Color(0xFF34D399) else Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "مانده حساب",
                        amount = formatPrice(balance),
                        currency = currency,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Minimal Chart
            item {
                if (customerInvoices.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                            Text("هنوز فاکتوری برای این مشتری ثبت نشده است.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("وضعیت تسویه فاکتورها", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Simple horizontal bar chart
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .background(if (isDark) Color(0xFFF87171) else Color(0xFFEF4444), shape = RoundedCornerShape(12.dp)) // Red for unpaid base
                            ) {
                                if (paidPercentage > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(paidPercentage)
                                            .background(if (isDark) Color(0xFF34D399) else Color(0xFF10B981), shape = RoundedCornerShape(12.dp)) // Green for paid
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(12.dp).background(if (isDark) Color(0xFF34D399) else Color(0xFF10B981), CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تسویه شده", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(12.dp).background(if (isDark) Color(0xFFF87171) else Color(0xFFEF4444), CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تسویه نشده", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            
            // Tabs
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("فاکتورها (${customerInvoices.size})".toPersianDigits(), fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("پیش‌فاکتورها (${customerProformas.size})".toPersianDigits(), fontWeight = FontWeight.Bold) }
                    )
                }
            }
            
            // List Items
            val currentList = if (selectedTab == 0) customerInvoices else customerProformas
            
            if (currentList.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(if (selectedTab == 0) "فاکتوری یافت نشد" else "پیش‌فاکتوری یافت نشد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(currentList) { rel ->
                    val isProforma = rel.invoice.invoiceType == "PROFORMA"
                    
                    val subtotal = rel.items.sumOf { it.unitPrice * it.quantity }
                    val tax = subtotal * (rel.invoice.taxRate / 100.0)
                    val totalAmt = subtotal + tax + rel.invoice.shippingFee - rel.invoice.discountAmount
                    val paidAmt = rel.payments.sumOf { it.amount }
                    val bal = totalAmt - paidAmt
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate("invoice_actions/${rel.invoice.id}")
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("شماره: ${rel.invoice.invoiceNumber}".toPersianDigits(), fontWeight = FontWeight.Bold)
                                Text(JalaliDateFormatter.format(rel.invoice.issueDate).toPersianDigits(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                            
                            HorizontalDivider()
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("مبلغ کل:", fontSize = 14.sp)
                                Text("${formatPrice(totalAmt).toPersianDigits()} $currency", fontWeight = FontWeight.Bold)
                            }
                            
                            if (!isProforma) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("پرداخت شده:", fontSize = 14.sp)
                                    Text("${formatPrice(paidAmt).toPersianDigits()} $currency", color = if (isDark) Color(0xFF34D399) else Color(0xFF10B981))
                                }
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("مانده:", fontSize = 14.sp)
                                    Text("${formatPrice(bal).toPersianDigits()} $currency", color = MaterialTheme.colorScheme.error)
                                }
                                
                                val statusText = when {
                                    paidAmt >= totalAmt && totalAmt > 0 -> "تسویه شده"
                                    paidAmt > 0 && paidAmt < totalAmt -> "پرداخت جزئی"
                                    else -> "تسویه نشده"
                                }
                                val statusColor = when {
                                    paidAmt >= totalAmt && totalAmt > 0 -> if (isDark) Color(0xFF34D399) else Color(0xFF10B981)
                                    paidAmt > 0 && paidAmt < totalAmt -> if (isDark) Color(0xFFFBBF24) else Color(0xFFF59E0B)
                                    else -> MaterialTheme.colorScheme.error
                                }
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("وضعیت:", fontSize = 14.sp)
                                    Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            } else {
                                val statusText = when (rel.invoice.status) {
                                    "ACCEPTED" -> "تایید شده"
                                    "REJECTED" -> "رد شده"
                                    else -> "در انتظار بررسی"
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("وضعیت:", fontSize = 14.sp)
                                    Text(statusText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }

        if (showEditDialog && customer != null) {
            EditCustomerDialog(
                customer = customer,
                onDismiss = { showEditDialog = false },
                onConfirm = { updatedCustomer ->
                    viewModel.updateCustomer(updatedCustomer)
                    showEditDialog = false
                    Toast.makeText(context, "اطلاعات مشتری با موفقیت ویرایش شد.", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun EditCustomerDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onConfirm: (Customer) -> Unit
) {
    var fullName by remember { mutableStateOf(customer.fullName) }
    var phoneVal by remember { mutableStateOf(customer.phoneNumber) }
    var nationalIdVal by remember { mutableStateOf(customer.nationalId ?: "") }
    var economicCodeVal by remember { mutableStateOf(customer.economicCode ?: "") }
    var customerAddress by remember { mutableStateOf(customer.address ?: "") }
    
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    
    var showUnsavedChangesConfirm by remember { mutableStateOf(false) }

    val hasChanges = fullName != customer.fullName ||
            phoneVal != customer.phoneNumber ||
            nationalIdVal != (customer.nationalId ?: "") ||
            economicCodeVal != (customer.economicCode ?: "") ||
            customerAddress != (customer.address ?: "")

    val handleDismiss = {
        if (hasChanges) {
            showUnsavedChangesConfirm = true
        } else {
            onDismiss()
        }
    }

    // Intercept back button when editing
    BackHandler(onBack = handleDismiss)

    if (showUnsavedChangesConfirm) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesConfirm = false },
            title = { Text("تغییرات ذخیره نشده", fontWeight = FontWeight.Bold) },
            text = { Text("تغییرات ذخیره نشده‌اند. خارج می‌شوید؟") },
            confirmButton = {
                TextButton(onClick = { onDismiss() }) {
                    Text("بله")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedChangesConfirm = false }) {
                    Text("خیر")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = handleDismiss,
        title = {
            Text(
                text = "ویرایش اطلاعات مشتری",
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { 
                        fullName = it
                        nameError = null
                    },
                    label = { Text("نام شخص یا شرکت (خریدار)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = {
                        if (nameError != null) Text(nameError!!, color = MaterialTheme.colorScheme.error)
                    },
                    shape = RoundedCornerShape(8.dp)
                )

                PhoneNumberInput(
                    value = phoneVal,
                    onValueChange = { 
                        phoneVal = it
                        phoneError = null
                    },
                    label = "شماره تماس / همراه",
                    errorMessage = phoneError
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = nationalIdVal.toPersianDigits(),
                        onValueChange = { nationalIdVal = PersianDigitConverter.toEnglish(it).filter { c -> c.isDigit() } },
                        label = { Text("شناسه/کد ملی") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = economicCodeVal.toPersianDigits(),
                        onValueChange = { economicCodeVal = PersianDigitConverter.toEnglish(it).filter { c -> c.isDigit() } },
                        label = { Text("کد اقتصادی") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                OutlinedTextField(
                    value = customerAddress,
                    onValueChange = { customerAddress = it },
                    label = { Text("نشانی و آدرس خریدار") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var showError = false
                    if (fullName.isBlank()) {
                        nameError = "نام مشتری را وارد کنید"
                        showError = true
                    }
                    if (phoneVal.isNotBlank() && !IranianValidationHelper.isValidPhoneNumber(phoneVal)) {
                        phoneError = "شماره موبایل نامعتبر است. فرمت صحیح: ۱۱ رقم با شروع ۰۹"
                        showError = true
                    }

                    if (!showError) {
                        onConfirm(
                            customer.copy(
                                fullName = fullName.trim(),
                                phoneNumber = phoneVal.trim(),
                                nationalId = nationalIdVal.trim().ifBlank { null },
                                economicCode = economicCodeVal.trim().ifBlank { null },
                                address = customerAddress.trim().ifBlank { null }
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("ذخیره تغییرات", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = handleDismiss) {
                Text("انصراف", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun SummaryCard(title: String, amount: String, currency: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
            Text("${amount.toPersianDigits()} $currency", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
