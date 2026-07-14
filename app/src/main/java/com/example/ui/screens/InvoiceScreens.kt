package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.data.dao.InvoiceWithRelations
import com.example.data.entity.Customer
import com.example.data.entity.Product
import com.example.data.entity.InvoiceItem
import com.example.ui.viewmodel.InvoiceViewModel
import kotlinx.coroutines.launch
import com.example.utils.JalaliCalendar
import com.example.utils.formatPrice
import com.example.utils.removeCommas
import com.example.utils.IranianValidationHelper
import com.example.utils.ThousandSeparatorVisualTransformation
import com.example.ui.screens.ItemQuantityInput
import com.example.ui.screens.PriceInput
import com.example.ui.screens.PhoneNumberInput
import com.example.ui.screens.BankCardInput
import com.example.ui.screens.ShebaInput
import com.example.utils.toPersianDigits
import com.example.utils.toEnglishDigits
import com.example.utils.FileHelper
import com.example.data.entity.Attachment
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenDark
import com.example.ui.theme.WarningYellow
import com.example.ui.theme.WarningYellowDark
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceListScreen(navController: NavController, viewModel: InvoiceViewModel) {
    val context = LocalContext.current
    val invoices by viewModel.invoices.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currency = settings?.currencyUnit ?: "تومان"

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Invoices, 1 = Proforma
    var parentQuery by remember { mutableStateOf("") }

    val filteredInvoices = remember(invoices, selectedTab, parentQuery) {
        val typeToMatch = if (selectedTab == 0) "INVOICE" else "PROFORMA"
        invoices.filter {
            val jalaliDateStr = com.example.utils.JalaliDateFormatter.format(it.invoice.issueDate)
            it.invoice.invoiceType == typeToMatch &&
            (it.invoice.invoiceNumber.contains(parentQuery) ||
             it.customer.fullName.contains(parentQuery, ignoreCase = true) ||
             jalaliDateStr.contains(parentQuery))
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Tab Headers
        TabRow(selectedTabIndex = selectedTab, modifier = Modifier.fillMaxWidth()) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("فاکتورهای فروش", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(12.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("پیش‌فاکتورها", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = parentQuery,
            onValueChange = { parentQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("جستجو شماره ردیف یا نام مشتری...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // List View
        if (invoices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "هنوز سندی ثبت نشده است.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (filteredInvoices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "هیچ موردی مطابق با جستجوی شما یافت نشد.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredInvoices) { rel ->
                    InvoiceRowItem(
                        navController = navController,
                        viewModel = viewModel,
                        rel = rel,
                        currency = currency
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Floating Button for creation
        Button(
            onClick = {
                val docType = if (selectedTab == 0) "INVOICE" else "PROFORMA"
                navController.navigate("invoice_edit/new?type=$docType")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (selectedTab == 0) "صدور فاکتور جدید" else "صدور پیش‌فاکتور جدید",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun InvoiceRowItem(
    navController: NavController,
    viewModel: InvoiceViewModel,
    rel: InvoiceWithRelations,
    currency: String
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val jalaliDateStr = remember(rel.invoice.issueDate) {
        com.example.utils.JalaliDateFormatter.format(rel.invoice.issueDate)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("حذف سند") },
            text = { Text("آیا مایل به حذف سند شماره ${rel.invoice.invoiceNumber} هستید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteInvoice(
                            id = rel.invoice.id,
                            onSuccess = {
                                Toast.makeText(context, "سند با موفقیت حذف گردید.", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("بله، حذف شود")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("انصراف") }
            }
        )
    }

    if (showPaymentDialog) {
        val totalAmount = rel.calculateTotal()
        val currentPaid = rel.calculatePaid()
        var newPaidAmountStr by remember { mutableStateOf(currentPaid.takeIf{it > 0}?.toLong()?.toString() ?: "") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("تسویه و وضعیت پرداخت") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("مبلغ کل سند: ${com.example.utils.formatPrice(totalAmount)} $currency".toPersianDigits(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    
                    val parsedPaid = newPaidAmountStr.toEnglishDigits().filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                    val remaining = (totalAmount - parsedPaid).coerceAtLeast(0.0)
                    
                    OutlinedTextField(
                        value = newPaidAmountStr.toPersianDigits(),
                        onValueChange = { input -> 
                            errorMessage = null
                            val cleanInput = input.toEnglishDigits().filter { it.isDigit() }
                            if (cleanInput.isEmpty()) {
                                newPaidAmountStr = ""
                            } else {
                                val value = cleanInput.toDoubleOrNull() ?: 0.0
                                if (value > totalAmount) {
                                    errorMessage = "مبلغ پرداخت شده نمی‌تواند بیشتر از مبلغ کل سند باشد."
                                }
                                newPaidAmountStr = cleanInput
                            }
                        },
                        label = { Text("مبلغ پرداخت شده ($currency)") },
                        placeholder = { Text("مثلاً: ۱۰,۰۰۰".toPersianDigits()) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        isError = errorMessage != null,
                        supportingText = {
                            if (errorMessage != null) {
                                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        colors = defaultAppTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = ThousandSeparatorVisualTransformation()
                    )
                    
                    Text("مبلغ باقیمانده: ${com.example.utils.formatPrice(remaining)} $currency".toPersianDigits(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    
                    val statusText = when {
                        parsedPaid >= totalAmount - 0.5 -> "تسویه شده"
                        parsedPaid > 0 -> "پرداخت جزئی"
                        else -> "پرداخت نشده"
                    }
                    Text("وضعیت نهایی: $statusText", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = newPaidAmountStr.toEnglishDigits().filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                        if (parsed > totalAmount) {
                            errorMessage = "مبلغ پرداخت شده نمی‌تواند بیشتر از مبلغ کل سند باشد."
                            return@Button
                        }
                        scope.launch {
                            try {
                                viewModel.resetPaymentsAndSetTotalPaid(rel.invoice.id, parsed)
                                Toast.makeText(context, "وضعیت تسویه ذخیره شد.", Toast.LENGTH_SHORT).show()
                                showPaymentDialog = false
                            } catch (e: Exception) {
                                Toast.makeText(context, "خطا در ذخیره تسویه.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = errorMessage == null
                ) {
                    Text("ذخیره تسویه")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) { Text("بستن") }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "خریدار: ${rel.customer.fullName}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "سریال: ${rel.invoice.invoiceNumber} | تاریخ: $jalaliDateStr".toPersianDigits(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { navController.navigate("invoice_edit/${rel.invoice.id}?type=${rel.invoice.invoiceType}") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "ویرایش سند", tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (rel.invoice.invoiceType == "PROFORMA") "پیش‌فاکتور" else "فاکتور",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "مبلغ کل سند:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = when (rel.invoice.status) {
                            "PAID" -> Color(0xFF10B981).copy(alpha = 0.2f)
                            "PARTIALLY_PAID" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        },
                        contentColor = when (rel.invoice.status) {
                            "PAID" -> Color(0xFF0F5132)
                            "PARTIALLY_PAID" -> Color(0xFF856404)
                            else -> MaterialTheme.colorScheme.onErrorContainer
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = when (rel.invoice.status) {
                                "PAID" -> "تسویه شده"
                                "PARTIALLY_PAID" -> {
                                    val remaining = rel.calculateRemaining()
                                    "پرداخت جزئی - باقیمانده: ${com.example.utils.formatPrice(remaining)} $currency".toPersianDigits()
                                }
                                else -> "پرداخت نشده"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                
                Text(
                    text = "${formatPrice(rel.calculateTotal())} $currency".toPersianDigits(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showPaymentDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تسویه حساب", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                navController.navigate("invoice_actions/${rel.invoice.id}")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مدیریت و چاپ", fontSize = 11.sp)
                        }

                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف فاکتور", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

data class TempAttachment(
    val fileName: String,
    val filePath: String,
    val mimeType: String
)

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InvoiceEditorScreen(
    navController: NavController,
    viewModel: InvoiceViewModel,
    invoiceId: String?,
    initType: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val customers by viewModel.customers.collectAsState()
    val products by viewModel.products.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val companyInfo by viewModel.companyInfo.collectAsState()
    val currency = settings?.currencyUnit ?: "تومان"

    val standsEditing = invoiceId != null && invoiceId != "new"

    // Multi-Step State management
    var activeStep by remember { mutableIntStateOf(1) }
    var savedInvoiceId by remember { mutableLongStateOf(0L) }

    // Form inputs and UI Dialog States
    var inputSerializerNumber by remember { mutableStateOf("") }
    var chosenCustomerId by remember { mutableLongStateOf(0L) }
    var selectDateTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    // Quick Item Input Fields
    var itemQuerySearch by remember { mutableStateOf("") }
    var selectedProductId by remember { mutableLongStateOf(0L) }
    var selectedProductName by remember { mutableStateOf("") }
    var selectedProductUnit by remember { mutableStateOf("عدد") }
    var itemCustomPrice by remember { mutableStateOf("") }
    var itemQtyInput by remember { mutableStateOf("") }
    var activeEditingItemLineIndex by remember { mutableStateOf(-1) }

    val addedItems = remember { mutableStateListOf<InvoiceItem>() }

    // Discount
    var discountType by remember { mutableStateOf("AMOUNT") } // AMOUNT or PERCENT
    var appliedDiscountAmount by remember { mutableStateOf("0") }
    var discountPercentage by remember { mutableStateOf("0") }

    // Shipping fee & Tax
    var appliedShippingCost by remember { mutableStateOf("0") }
    var appliedTaxRate by remember { mutableStateOf("0") }

    // Extra description notes and delivery info
    var invoiceDescription by remember { mutableStateOf("") }
    var invoiceNotesField by remember { mutableStateOf("") }
    var deliveryInfo by remember { mutableStateOf("") }

    // Attachments
    val existingAttachments = remember { mutableStateListOf<Attachment>() }
    val tempAttachments = remember { mutableStateListOf<TempAttachment>() }

    // UI Dialog controls
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showProductPickerInWorksheet by remember { mutableStateOf(false) }
    var showBarcodeScannerState by remember { mutableStateOf(false) }
    var showProductNotFoundDialog by remember { mutableStateOf(false) }
    var preScannedBarcode by remember { mutableStateOf("") }
    var showQuickProductCreator by remember { mutableStateOf(false) }
    var showExitCheckDialog by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }

    // Focus requesters and input validations
    val itemSearchFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val qtyFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    var qtyError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }

    // Pre-load logic if editing or new invoice
    LaunchedEffect(invoiceId) {
        if (standsEditing && invoiceId != null) {
            val invIdLong = invoiceId.toLongOrNull() ?: 0L
            viewModel.getInvoiceById(invIdLong).collect { rel ->
                rel?.let {
                    inputSerializerNumber = it.invoice.invoiceNumber
                    chosenCustomerId = it.invoice.customerId
                    selectDateTimestamp = it.invoice.issueDate
                    
                    addedItems.clear()
                    addedItems.addAll(it.items)
                    
                    appliedDiscountAmount = it.invoice.discountAmount.toInt().toString()
                    appliedShippingCost = it.invoice.shippingFee.toInt().toString()
                    appliedTaxRate = it.invoice.taxRate.toInt().toString()
                    
                    try {
                        val json = org.json.JSONObject(it.invoice.notes ?: "")
                        invoiceDescription = json.optString("desc", "")
                        invoiceNotesField = json.optString("terms", "")
                        deliveryInfo = json.optString("delivery", "")
                        discountType = json.optString("discountType", "AMOUNT")
                        discountPercentage = json.optString("discountPercentage", "0")
                    } catch (e: Exception) {
                        invoiceDescription = it.invoice.notes ?: ""
                    }
                    
                    existingAttachments.clear()
                    existingAttachments.addAll(it.attachments)
                }
            }
        } else {
            appliedTaxRate = "0"
            inputSerializerNumber = viewModel.generateNextInvoiceNumber(initType)
        }
    }

    val performSave = {
        if (chosenCustomerId <= 0L) {
            Toast.makeText(context, "لطفاً ابتدا خریدار/مشتری را انتخاب کنید.", Toast.LENGTH_LONG).show()
        } else if (addedItems.isEmpty()) {
            Toast.makeText(context, "لیست اقلام فاکتور نمی‌تواند خالی باشد.", Toast.LENGTH_LONG).show()
        } else {
            isSaving = true
            scope.launch {
                try {
                    val discAmt = if (discountType == "PERCENT") {
                        val pct = discountPercentage.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
                        val subSum = addedItems.sumOf { (it.unitPrice * it.quantity) - it.discountAmount }
                        subSum * (pct / 100.0)
                    } else {
                        val subSum = addedItems.sumOf { (it.unitPrice * it.quantity) - it.discountAmount }
                        appliedDiscountAmount.toDoubleOrNull()?.coerceIn(0.0, subSum) ?: 0.0
                    }
                    val shipFee = appliedShippingCost.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                    val taxPct = appliedTaxRate.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
                    
                    val combinedNotes = org.json.JSONObject().apply {
                        put("desc", invoiceDescription)
                        put("terms", invoiceNotesField)
                        put("delivery", deliveryInfo)
                        put("discountType", discountType)
                        put("discountPercentage", discountPercentage)
                    }.toString()
                    
                    val finalId = if (standsEditing) {
                        val invIdLong = invoiceId?.toLongOrNull() ?: 0L
                        viewModel.updateInvoice(
                            id = invIdLong,
                            customerId = chosenCustomerId,
                            items = addedItems,
                            invoiceType = initType,
                            discount = discAmt,
                            taxPercent = taxPct,
                            shipping = shipFee,
                            notes = combinedNotes,
                            invoiceNumber = inputSerializerNumber,
                            status = if (initType == "PROFORMA") "DRAFT" else "UNPAID",
                            issueDate = selectDateTimestamp
                        )
                        invIdLong
                    } else {
                        viewModel.createInvoice(
                            customerId = chosenCustomerId,
                            items = addedItems,
                            invoiceType = initType,
                            discount = discAmt,
                            taxPercent = taxPct,
                            shipping = shipFee,
                            notes = combinedNotes,
                            invoiceNumber = inputSerializerNumber,
                            issueDate = selectDateTimestamp
                        )
                    }
                    
                    tempAttachments.forEach { temp ->
                        viewModel.addAttachment(
                            invoiceId = finalId,
                            fileName = temp.fileName,
                            filePath = temp.filePath,
                            mimeType = temp.mimeType
                        )
                    }
                    
                    Toast.makeText(context, "سند با موفقیت ثبت و ذخیره شد.", Toast.LENGTH_SHORT).show()
                    savedInvoiceId = finalId
                    activeStep = 4 // Direct transition to Step 4 after database commit completes!
                } catch (e: Exception) {
                    Toast.makeText(context, "خطا در ثبت سند: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isSaving = false
                }
            }
        }
    }

    val subSum = addedItems.sumOf { (it.unitPrice * it.quantity) - it.discountAmount }
    
    val discountVal = if (discountType == "PERCENT") {
        val pct = discountPercentage.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
        subSum * (pct / 100.0)
    } else {
        appliedDiscountAmount.toDoubleOrNull()?.coerceIn(0.0, subSum) ?: 0.0
    }
    
    val finalTaxVal = run {
        val taxPct = appliedTaxRate.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
        (subSum - discountVal) * (taxPct / 100.0)
    }
    
    val grandTotalSumResult = run {
        val shipFee = appliedShippingCost.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        subSum - discountVal + finalTaxVal + shipFee
    }

    val isDark = LocalIsDarkTheme.current
    val successColor = if (isDark) SuccessGreenDark else SuccessGreen

    // Handle Hardware/Device back press
    androidx.activity.compose.BackHandler(enabled = true) {
        if (activeStep > 1 && activeStep < 4) {
            activeStep -= 1
        } else if (activeStep == 4) {
            navController.popBackStack()
        } else {
            if (addedItems.isNotEmpty() || chosenCustomerId > 0L) {
                showExitCheckDialog = true
            } else {
                navController.popBackStack()
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            if (activeStep > 1 && activeStep < 4) {
                                activeStep -= 1
                            } else if (activeStep == 4) {
                                navController.popBackStack()
                            } else if (addedItems.isNotEmpty() || chosenCustomerId > 0L) {
                                showExitCheckDialog = true
                            } else {
                                navController.popBackStack()
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "برگشت")
                        }
                        
                        Text(
                            text = if (initType == "INVOICE") "فاکتور" else "پیش‌فاکتور",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val stepNames = listOf("مشخصات", "اقلام", "خلاصه", "تایید")
                        val label = stepNames.getOrElse(activeStep - 1) { "" }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "$activeStep/۴: $label".toPersianDigits(),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (standsEditing) {
                        IconButton(onClick = {
                            viewModel.deleteInvoice(
                                id = invoiceId.toLong(),
                                onSuccess = {
                                    Toast.makeText(context, "فاکتور با موفقیت حذف شد.", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                },
                                onError = { errorMsg ->
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                }
                            )
                        }) {
                            Icon(Icons.Default.Delete, "حذف فاکتور", tint = Color.Red, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (activeStep) {
                    1 -> {
                        WizardStep1(
                            initType = initType,
                            chosenCustomerId = chosenCustomerId,
                            customers = customers,
                            companyInfo = companyInfo,
                            selectDateTimestamp = selectDateTimestamp,
                            inputSerializerNumber = inputSerializerNumber,
                            onSerializerNumberChanged = { inputSerializerNumber = it },
                            invoiceDescription = invoiceDescription,
                            onDescriptionChanged = { invoiceDescription = it },
                            invoiceNotesField = invoiceNotesField,
                            onNotesFieldChanged = { invoiceNotesField = it },
                            deliveryInfo = deliveryInfo,
                            onDeliveryInfoChanged = { deliveryInfo = it },
                            onCustomerSelected = { chosenCustomerId = it },
                            onClearCustomer = { chosenCustomerId = 0L },
                            onQuickAddCustomer = { name, phone ->
                                viewModel.addCustomer(name, phone) { newId ->
                                    chosenCustomerId = newId
                                }
                            },
                            showDatePicker = { showDatePickerDialog = true },
                            onNext = { activeStep = 2 }
                        )
                    }
                    2 -> {
                        WizardStep2(
                            addedItems = addedItems,
                            products = products,
                            currency = currency,
                            itemQuerySearch = itemQuerySearch,
                            onItemQuerySearchChanged = { itemQuerySearch = it },
                            selectedProductId = selectedProductId,
                            onSelectedProductIdChanged = { selectedProductId = it },
                            selectedProductName = selectedProductName,
                            onSelectedProductNameChanged = { selectedProductName = it },
                            selectedProductUnit = selectedProductUnit,
                            onSelectedProductUnitChanged = { selectedProductUnit = it },
                            itemCustomPrice = itemCustomPrice,
                            onItemCustomPriceChanged = { itemCustomPrice = it },
                            itemQtyInput = itemQtyInput,
                            onItemQtyInputChanged = { itemQtyInput = it },
                            onAddProductClick = {
                                val qty = itemQtyInput.toDoubleOrNull()
                                val price = itemCustomPrice.toDoubleOrNull()
                                var hasError = false
                                if (qty == null || qty <= 0.0) { qtyError = "نامعتبر"; hasError = true }
                                if (price == null || price < 0.0) { priceError = "نامعتبر"; hasError = true }
                                if (selectedProductName.isBlank()) { Toast.makeText(context, "نام کالا خالی است", Toast.LENGTH_SHORT).show(); hasError = true }
                                
                                if (!hasError) {
                                    val isNewProduct = selectedProductId <= 0L
                                    val finalProductName = selectedProductName.trim()
                                    val existingProduct = products.find { it.name.trim() == finalProductName }
                                    
                                    if (isNewProduct && existingProduct == null) {
                                        scope.launch {
                                            val newId = viewModel.addProductAndGet(
                                                categoryId = null,
                                                name = finalProductName,
                                                unit = selectedProductUnit.ifBlank { "عدد" },
                                                price = price!!
                                            )
                                            addedItems.add(InvoiceItem(
                                                invoiceId = 0L,
                                                productId = newId,
                                                productName = finalProductName,
                                                unitPrice = price, unit = selectedProductUnit.ifBlank { "عدد" }, quantity = qty!!
                                            ))
                                            itemQuerySearch = ""; selectedProductName = ""; selectedProductId = 0L
                                            itemCustomPrice = ""; itemQtyInput = ""
                                            Toast.makeText(context, "کالا سریعاً ذخیره و لیست افزوده شد", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        val actualId = existingProduct?.id ?: selectedProductId
                                        addedItems.add(InvoiceItem(
                                            invoiceId = 0L,
                                            productId = if (actualId > 0) actualId else null,
                                            productName = finalProductName,
                                            unitPrice = price!!, unit = selectedProductUnit, quantity = qty!!
                                        ))
                                        itemQuerySearch = ""; selectedProductName = ""; selectedProductId = 0L
                                        itemCustomPrice = ""; itemQtyInput = ""
                                        Toast.makeText(context, "کالا با موفقیت افزوده شد", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onShowBarcodeScanner = { showBarcodeScannerState = true },
                            onShowProductCatalog = { showProductPickerInWorksheet = true },
                            onShowQuickCreateProduct = { showQuickProductCreator = true },
                            onClearInputFields = {
                                itemQuerySearch = ""; selectedProductName = ""; selectedProductId = 0L
                                itemCustomPrice = ""; itemQtyInput = ""
                            },
                            onEditItem = { idx -> activeEditingItemLineIndex = idx },
                            onDeleteItem = { it -> addedItems.remove(it) },
                            onBack = { activeStep = 1 },
                            onNext = { activeStep = 3 }
                        )
                    }
                    3 -> {
                        WizardStep3(
                            initType = initType,
                            currency = currency,
                            subSum = subSum,
                            discountType = discountType,
                            onDiscountTypeChanged = { discountType = it },
                            appliedDiscountAmount = appliedDiscountAmount,
                            onAppliedDiscountAmountChanged = { appliedDiscountAmount = it },
                            discountPercentage = discountPercentage,
                            onDiscountPercentageChanged = { discountPercentage = it },
                            discountVal = discountVal,
                            appliedShippingCost = appliedShippingCost,
                            onAppliedShippingCostChanged = { appliedShippingCost = it },
                            appliedTaxRate = appliedTaxRate,
                            onAppliedTaxRateChanged = { appliedTaxRate = it },
                            finalTaxVal = finalTaxVal,
                            grandTotalSumResult = grandTotalSumResult,
                            itemCount = addedItems.size,
                            isSaving = isSaving,
                            onBack = { activeStep = 2 },
                            onSave = { performSave() }
                        )
                    }
                    4 -> {
                        WizardStep4(
                            savedInvoiceId = savedInvoiceId,
                            navController = navController,
                            viewModel = viewModel,
                            onEditClicked = { activeStep = 3 },
                            onNewInvoiceRequested = {
                                activeStep = 1
                                savedInvoiceId = 0L
                                inputSerializerNumber = viewModel.generateNextInvoiceNumber("INVOICE")
                                chosenCustomerId = 0L
                                addedItems.clear()
                                tempAttachments.clear()
                                existingAttachments.clear()
                                appliedDiscountAmount = "0"
                                discountPercentage = "0"
                                appliedShippingCost = "0"
                                appliedTaxRate = "0"
                                invoiceDescription = ""
                                invoiceNotesField = ""
                                deliveryInfo = ""
                            },
                            onNewProformaRequested = {
                                activeStep = 1
                                savedInvoiceId = 0L
                                inputSerializerNumber = viewModel.generateNextInvoiceNumber("PROFORMA")
                                chosenCustomerId = 0L
                                addedItems.clear()
                                tempAttachments.clear()
                                existingAttachments.clear()
                                appliedDiscountAmount = "0"
                                discountPercentage = "0"
                                appliedShippingCost = "0"
                                appliedTaxRate = "0"
                                invoiceDescription = ""
                                invoiceNotesField = ""
                                deliveryInfo = ""
                            }
                        )
                    }
                }
            }
        }
    }

    // dialog management triggers inline

    if (showExitCheckDialog) {
        AlertDialog(
            onDismissRequest = { showExitCheckDialog = false },
            title = { Text("خروج از فرآیند صدور سند رسمی") },
            text = { Text("اطلاعات ثبت شده تا به این مرحله ذخیره نشده است. آیا مایل هستید سند کنونی را حذف کرده و خارج شوید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitCheckDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("بله، لغو و خروج")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitCheckDialog = false }) {
                    Text("خیر، ادامه ثبت")
                }
            }
        )
    }

    if (showDatePickerDialog) {
        PersianDatePickerDialog(
            initialTimestamp = selectDateTimestamp,
            onDismissRequest = { showDatePickerDialog = false },
            onDateSelected = { timestamp ->
                selectDateTimestamp = timestamp
                showDatePickerDialog = false
                Toast.makeText(context, "تاریخ فاکتور ثبت شد", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showCustomerPicker) {
        var queryFilterCustomer by remember { mutableStateOf("") }
        var quickNewCustName by remember { mutableStateOf("") }
        var quickNewCustPhone by remember { mutableStateOf("") }
        var showQuickCustCreatorField by remember { mutableStateOf(false) }
        var nameError by remember { mutableStateOf<String?>(null) }
        var phoneError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showCustomerPicker = false },
            title = { Text("انتخاب خریدار / مشتری رسمی") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = queryFilterCustomer,
                        onValueChange = { queryFilterCustomer = it },
                        label = { Text("جستجوی خریدار") },
                        placeholder = { Text("جستجوی سریع نام یا همراه خریدار...") },
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = defaultAppTextFieldColors(),
                        singleLine = true
                    )

                    val filteredCustomers = remember(customers, queryFilterCustomer) {
                        customers.filter {
                            it.fullName.contains(queryFilterCustomer, ignoreCase = true) ||
                            it.phoneNumber.contains(queryFilterCustomer)
                        }
                    }

                    if (filteredCustomers.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            filteredCustomers.forEach { cust ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            chosenCustomerId = cust.id
                                            showCustomerPicker = false
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(cust.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(cust.phoneNumber.toPersianDigits(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    if (chosenCustomerId == cust.id) {
                                        Icon(Icons.Default.Check, null, tint = successColor)
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(" خریداری با این مشخصات یافت نشد.", fontSize = 11.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(6.dp))
                            if (!showQuickCustCreatorField) {
                                Button(onClick = { showQuickCustCreatorField = true }) {
                                    Text("بله، ثبت سریع خریدار جدید", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    if (showQuickCustCreatorField) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("ثبت آنی مشتری جدید", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = quickNewCustName,
                                    onValueChange = { 
                                        quickNewCustName = it
                                        nameError = null
                                    },
                                    label = { Text("نام کامل خریدار") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = defaultAppTextFieldColors(),
                                    isError = nameError != null,
                                    supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                                )
                                PhoneNumberInput(
                                    value = quickNewCustPhone,
                                    onValueChange = { 
                                        quickNewCustPhone = it 
                                        phoneError = null
                                    },
                                    label = "شماره تماس",
                                    isError = phoneError != null,
                                    errorMessage = phoneError
                                )

                                Button(
                                    onClick = {
                                        var hasErr = false
                                        if (quickNewCustName.isBlank()) {
                                            nameError = "نام خریدار الزامی است"
                                            hasErr = true
                                        }
                                        if (quickNewCustPhone.isBlank()) {
                                            phoneError = "شماره تماس الزامی است"
                                            hasErr = true
                                        }
                                        if (!hasErr) {
                                            viewModel.addCustomer(
                                                fullName = quickNewCustName,
                                                phone = quickNewCustPhone,
                                                onSaved = { newId ->
                                                    chosenCustomerId = newId
                                                    showCustomerPicker = false
                                                    Toast.makeText(context, "مشتری جدید با موفقیت ثبت و انتخاب شد.", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        } else {
                                            Toast.makeText(context, "لطفا نام و شماره را تکمیل فرمائید", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("ذخیره و گزینش خریدار")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCustomerPicker = false }) { Text("انصراف") }
            }
        )
    }

    if (showProductPickerInWorksheet) {
        var catalogSearchQuery by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showProductPickerInWorksheet = false },
            title = { Text("کاتالوگ کالاهای انبار") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = catalogSearchQuery,
                        onValueChange = { catalogSearchQuery = it },
                        placeholder = { Text("جستجوی نام یا بارکد کالا...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) }
                    )

                    val matchedCatalog = products.filter {
                        it.name.contains(catalogSearchQuery, ignoreCase = true) ||
                        (it.barcode?.contains(catalogSearchQuery) == true)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        matchedCatalog.forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedProductId = p.id
                                        selectedProductName = p.name
                                        selectedProductUnit = p.unit
                                        itemCustomPrice = p.defaultUnitPrice.toInt().toString()
                                        itemQuerySearch = p.name
                                        showProductPickerInWorksheet = false
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(p.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("فروش فی: ${formatPrice(p.defaultUnitPrice)}".toPersianDigits(), fontSize = 11.sp, color = Color.Gray)
                                }
                                Text("/ ${p.unit}", fontSize = 10.sp, color = Color.Gray)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showProductPickerInWorksheet = false }) { Text("بستن") }
            }
        )
    }

    if (showBarcodeScannerState) {
        BarcodeScannerLauncher(
            onDismiss = { showBarcodeScannerState = false },
            onBarcodeScanned = { code ->
                showBarcodeScannerState = false
                val matched = products.find { it.barcode == code }
                if (matched != null) {
                    selectedProductId = matched.id
                    selectedProductName = matched.name
                    selectedProductUnit = matched.unit
                    itemCustomPrice = matched.defaultUnitPrice.toInt().toString()
                    itemQuerySearch = matched.name
                    Toast.makeText(context, "${matched.name} یافت شد.", Toast.LENGTH_SHORT).show()
                } else {
                    preScannedBarcode = code
                    showProductNotFoundDialog = true
                }
            }
        )
    }

    if (showProductNotFoundDialog) {
        AlertDialog(
            onDismissRequest = { showProductNotFoundDialog = false },
            title = { Text("کالا یافت نشد") },
            text = { Text("کالایی با بارکد ${preScannedBarcode} در انبار شما تعریف نشده است. مایل به ایجاد مستقیم کالای جدید هستید؟") },
            confirmButton = {
                Button(onClick = {
                    showProductNotFoundDialog = false
                    showQuickProductCreator = true
                }) {
                    Text("بله، ساخت کالا")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProductNotFoundDialog = false }) { Text("انصراف") }
            }
        )
    }

    if (showQuickProductCreator) {
        var quickProdName by remember { mutableStateOf(itemQuerySearch) }
        var quickProdUnit by remember { mutableStateOf("عدد") }
        var quickProdPrice by remember { mutableStateOf("") }
        var quickProdBarcode by remember { mutableStateOf(preScannedBarcode) }

        // Load correct values
        LaunchedEffect(showQuickProductCreator) {
            quickProdName = itemQuerySearch
            quickProdBarcode = preScannedBarcode
        }

        AlertDialog(
            onDismissRequest = { showQuickProductCreator = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
            modifier = Modifier.fillMaxWidth().padding(16.dp).imePadding(),
            title = { Text("ثبت آنی کالای جدید در انبار") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = quickProdName,
                        onValueChange = { quickProdName = it },
                        label = { Text("نام کالا یا خدمت") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    OutlinedTextField(
                        value = quickProdUnit,
                        onValueChange = { quickProdUnit = it },
                        label = { Text("واحد شمارش (مثلا: عدد، کیلو)") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    PriceInput(
                        value = quickProdPrice,
                        onValueChange = { quickProdPrice = it },
                        label = "قیمت پیش‌فرض فروش ($currency)"
                    )
                    ModernNumericTextField(
                        value = quickProdBarcode,
                        onValueChange = { quickProdBarcode = it },
                        label = "کد بارکد",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val basePrice = quickProdPrice.toDoubleOrNull() ?: 0.0
                        if (quickProdName.isNotBlank()) {
                            scope.launch {
                                val newId = viewModel.addProductAndGet(
                                    categoryId = null,
                                    name = quickProdName,
                                    unit = quickProdUnit,
                                    price = basePrice,
                                    barcode = quickProdBarcode.takeIf { it.isNotBlank() }
                                )
                                selectedProductId = newId
                                selectedProductName = quickProdName
                                selectedProductUnit = quickProdUnit
                                itemCustomPrice = basePrice.toInt().toString()
                                itemQuerySearch = quickProdName
                                showQuickProductCreator = false
                                preScannedBarcode = ""
                                Toast.makeText(context, "کالا در سیستم ثبت شد.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "تکمیل نام کالا الزامی است.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("ثبت نهایی")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickProductCreator = false }) { Text("انصراف") }
            }
        )
    }

    if (activeEditingItemLineIndex != -1) {
        val editingInvoiceItem = addedItems[activeEditingItemLineIndex]
        var editQtyStr by remember { mutableStateOf(editingInvoiceItem.quantity.toString()) }
        var editPriceStr by remember { mutableStateOf(editingInvoiceItem.unitPrice.toString()) }

        AlertDialog(
            onDismissRequest = { activeEditingItemLineIndex = -1 },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
            modifier = Modifier.fillMaxWidth().padding(16.dp).imePadding(),
            title = { Text("اصلاح ردیف کالا") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                ) {
                    Text("نام کالا: ${editingInvoiceItem.productName}", fontWeight = FontWeight.Bold)
                    ItemQuantityInput(
                        value = editQtyStr,
                        onValueChange = { editQtyStr = it },
                        label = "تعداد / مقدار"
                    )
                    PriceInput(
                        value = editPriceStr,
                        onValueChange = { editPriceStr = it },
                        label = "قیمت واحد ($currency)"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newQty = editQtyStr.toDoubleOrNull() ?: 0.0
                        val newPrice = editPriceStr.toDoubleOrNull() ?: 0.0
                        qtyError = if (newQty <= 0.0) "تعداد باید بیشتر از ۰ باشد." else null
                        priceError = if (newPrice <= 0.0) "قیمت باید بیشتر از ۰ باشد." else null
                        if (qtyError == null && priceError == null) {
                            addedItems[activeEditingItemLineIndex] = editingInvoiceItem.copy(
                                quantity = newQty,
                                unitPrice = newPrice
                            )
                            Toast.makeText(context, "ردیف کالا اصلاح شد", Toast.LENGTH_SHORT).show()
                            activeEditingItemLineIndex = -1
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = successColor)
                ) {
                    Text("اعمال اصلاحات")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeEditingItemLineIndex = -1 }) { Text("انصراف") }
            }
        )
    }
}
