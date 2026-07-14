package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.entity.*
import com.example.data.dao.InvoiceWithRelations
import com.example.ui.viewmodel.InvoiceViewModel
import com.example.utils.JalaliCalendar
import com.example.utils.formatPrice
import com.example.utils.toPersianDigits
import com.example.utils.toEnglishDigits
import com.example.utils.removeCommas
import com.example.utils.ThousandSeparatorVisualTransformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun WizardStep1(
    initType: String,
    chosenCustomerId: Long,
    customers: List<Customer>,
    companyInfo: CompanyInfo?,
    selectDateTimestamp: Long,
    inputSerializerNumber: String,
    onSerializerNumberChanged: (String) -> Unit,
    invoiceDescription: String,
    onDescriptionChanged: (String) -> Unit,
    invoiceNotesField: String,
    onNotesFieldChanged: (String) -> Unit,
    deliveryInfo: String,
    onDeliveryInfoChanged: (String) -> Unit,
    onCustomerSelected: (Long) -> Unit,
    onClearCustomer: () -> Unit,
    onQuickAddCustomer: (String, String) -> Unit,
    showDatePicker: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val isCustomerSelected = chosenCustomerId > 0L
    val selectedCust = customers.find { it.id == chosenCustomerId }
    
    var customerSearchQuery by remember { mutableStateOf("") }
    
    // Quick Add States
    var showQuickCustCreatorField by remember { mutableStateOf(false) }
    var quickNewCustName by remember { mutableStateOf("") }
    var quickNewCustPhone by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    val normalizeText: (String) -> String = { text ->
        text.toEnglishDigits()
            .replace("ي", "ی")
            .replace("ك", "ک")
            .replace("ة", "ه")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    val matchedCustomers = remember(customers, customerSearchQuery) {
        val normalizedSearch = normalizeText(customerSearchQuery)
        if (normalizedSearch.isBlank()) emptyList<Customer>()
        else customers.filter { c ->
            val nName = normalizeText(c.fullName)
            val nPhone = normalizeText(c.phoneNumber)
            val nNationalId = c.nationalId?.let { normalizeText(it) } ?: ""
            nName.contains(normalizedSearch, ignoreCase = true) ||
            nPhone.contains(normalizedSearch, ignoreCase = true) ||
            nNationalId.contains(normalizedSearch, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Buyer Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (isCustomerSelected && selectedCust != null) {
                            OutlinedTextField(
                                value = selectedCust.fullName,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                placeholder = { Text("جهت انتخاب مشتری ضربه بزنید...") },
                                label = { Text("خریدار / مشتری") },
                                leadingIcon = { Icon(Icons.Default.Person, null) },
                                trailingIcon = { 
                                    IconButton(onClick = { 
                                        onClearCustomer()
                                        customerSearchQuery = ""
                                        showQuickCustCreatorField = false
                                    }) {
                                        Icon(Icons.Default.Close, null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.primary,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else {
                            OutlinedTextField(
                                value = customerSearchQuery,
                                onValueChange = { customerSearchQuery = it },
                                label = { Text("جستجو یا انتخاب خریدار") },
                                placeholder = { Text("نام، شماره یا کد خریدار را تایپ کنید...", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.PersonSearch, null) },
                                trailingIcon = {
                                    if (customerSearchQuery.isNotBlank()) {
                                        IconButton(onClick = { customerSearchQuery = "" }) {
                                            Icon(Icons.Default.Close, null)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Autocomplete Box for Customers
                    if (!isCustomerSelected && customerSearchQuery.isNotBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (matchedCustomers.isNotEmpty()) {
                                    matchedCustomers.forEach { c ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    onCustomerSelected(c.id)
                                                    customerSearchQuery = ""
                                                    showQuickCustCreatorField = false
                                                }
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(c.fullName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                                Text(c.phoneNumber.toPersianDigits(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                            }
                                            if (!c.nationalId.isNullOrBlank()) {
                                                Text(c.nationalId.toPersianDigits(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    }
                                } else {
                                    Text("خریدار پیدا نشد؛ می‌توانید ثبت سریع انجام دهید.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(8.dp))
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                        .clickable { 
                                            showQuickCustCreatorField = true
                                            quickNewCustName = customerSearchQuery
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PersonAdd, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("تعریف سریع مشتری جدید", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Quick Add Panel
                    if (showQuickCustCreatorField && !isCustomerSelected) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
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
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary
                                    ),
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
                                            onQuickAddCustomer(quickNewCustName, quickNewCustPhone)
                                            showQuickCustCreatorField = false
                                            customerSearchQuery = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ذخیره و انتخاب فوری")
                                }
                            }
                        }
                    }

                    if (selectedCust != null) {
                        AnimatedVisibility(visible = true) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, start = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("تلفن همراه: ${selectedCust.phoneNumber}".toPersianDigits(), fontSize = 11.sp, color = Color.Gray)
                                if (!selectedCust.nationalId.isNullOrBlank()) {
                                    Text("کد ملی/شناسه اقتصادی: ${selectedCust.nationalId}".toPersianDigits(), fontSize = 11.sp, color = Color.Gray)
                                }
                                if (!selectedCust.address.isNullOrBlank()) {
                                    Text("نشانی خریدار: ${selectedCust.address}", fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Serial Number and Date Details
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "مشخصات و مستندات صادر شده",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .clickable { showDatePicker() }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary)
                            Text("تاریخ صدور سند:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = com.example.utils.JalaliDateFormatter.format(selectDateTimestamp).toPersianDigits(),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 4. Notes & Explanations (terms & public description)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "یادداشت‌ها و تعهدات خریدار",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = invoiceDescription,
                        onValueChange = onDescriptionChanged,
                        label = { Text("شرح عمومی سند / ضمانت نامه") },
                        placeholder = { Text("مانند: گارانتی ۱۸ ماهه طلایی یا مهلت تست و مرجوعی کالا") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )

                    OutlinedTextField(
                        value = invoiceNotesField,
                        onValueChange = onNotesFieldChanged,
                        label = { Text("یادداشت تعهداتی خریدار (داخلی)") },
                        placeholder = { Text("مانند: تسویه از طریق چک صیادی به تاریخ سررسید...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 2
                    )

                    OutlinedTextField(
                        value = deliveryInfo,
                        onValueChange = onDeliveryInfoChanged,
                        label = { Text("شرایط ارسال و تحویل محموله") },
                        placeholder = { Text("مانند: تحویل حضوری یا ارسال با باربری با هزینه مشتری") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 2
                    )
                }
            }
        }

        // Warning and Submit
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isCustomerSelected) {
                    Text(
                        text = "⚠️ مایل به عبور هستید؟ ابتدا خریدار را انتخاب نمایید.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = {
                        if (isCustomerSelected) {
                            onNext()
                        } else {
                            Toast.makeText(context, "لطفاً ابتدا خریدار/مشتری را انتخاب کنید.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCustomerSelected) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                ) {
                    Text("ادامه به افزودن اقلام کالا/خدمات", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun WizardStep2(
    addedItems: List<InvoiceItem>,
    products: List<Product>,
    currency: String,
    itemQuerySearch: String,
    onItemQuerySearchChanged: (String) -> Unit,
    selectedProductId: Long,
    onSelectedProductIdChanged: (Long) -> Unit,
    selectedProductName: String,
    onSelectedProductNameChanged: (String) -> Unit,
    selectedProductUnit: String,
    onSelectedProductUnitChanged: (String) -> Unit,
    itemCustomPrice: String,
    onItemCustomPriceChanged: (String) -> Unit,
    itemQtyInput: String,
    onItemQtyInputChanged: (String) -> Unit,
    onAddProductClick: () -> Unit,
    onShowBarcodeScanner: () -> Unit,
    onShowProductCatalog: () -> Unit,
    onShowQuickCreateProduct: () -> Unit,
    onClearInputFields: () -> Unit,
    onEditItem: (Int) -> Unit,
    onDeleteItem: (InvoiceItem) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val hasItems = addedItems.isNotEmpty()
    
    val searchFocusNode = remember { androidx.compose.ui.focus.FocusRequester() }
    val qtyFocusNode = remember { androidx.compose.ui.focus.FocusRequester() }
    
    LaunchedEffect(addedItems.size) {
        if (addedItems.isNotEmpty()) {
            try { searchFocusNode.requestFocus() } catch(e: Exception) {}
        }
    }

    val normalizeText: (String) -> String = { text ->
        text.toEnglishDigits()
            .replace("ي", "ی")
            .replace("ك", "ک")
            .replace("ة", "ه")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    val matchedProducts = remember(products, itemQuerySearch) {
        val normalizedSearch = normalizeText(itemQuerySearch)
        if (normalizedSearch.isBlank()) emptyList<Product>()
        else products.filter { p ->
            val nName = normalizeText(p.name)
            val nCode = p.description?.let { normalizeText(it) } ?: ""
            val nBarcode = p.barcode?.let { normalizeText(it) } ?: ""
            nName.contains(normalizedSearch, ignoreCase = true) ||
            nCode.contains(normalizedSearch, ignoreCase = true) ||
            nBarcode.contains(normalizedSearch, ignoreCase = true) ||
            nName.startsWith(normalizedSearch, ignoreCase = true)
        }
    }

    val hasExactMatch = remember(products, itemQuerySearch) {
        val normalizedSearch = normalizeText(itemQuerySearch)
        products.any { normalizeText(it.name).equals(normalizedSearch, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp, start = 12.dp, end = 12.dp, bottom = 12.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Form Product creation worksheet card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedProductId > 0L) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val matchedP = products.find { it.id == selectedProductId }
                            if (matchedP != null) {
                                Text(
                                    text = "موجودی انبار: ${matchedP.stockQuantity ?: "نامحدود"} ${matchedP.unit}".toPersianDigits(),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if ((matchedP.stockQuantity ?: 0.0) > 0.0) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .background(
                                            if ((matchedP.stockQuantity ?: 0.0) > 0.0) Color(0xFF10B981).copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Scan Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = itemQuerySearch,
                            onValueChange = {
                                onItemQuerySearchChanged(it)
                                onSelectedProductNameChanged(it)
                                onSelectedProductIdChanged(0L)
                            },
                            placeholder = { Text("عنوان، کد یا بارکد کالا را بنویسید...", fontSize = 12.sp) },
                            label = { Text("جستجو یا تایپ کالا", style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                if (itemQuerySearch.isNotBlank()) {
                                    IconButton(onClick = {
                                        onItemQuerySearchChanged("")
                                        onSelectedProductNameChanged("")
                                        onSelectedProductIdChanged(0L)
                                    }) {
                                        Icon(Icons.Default.Close, null)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).focusRequester(searchFocusNode),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Button(
                            onClick = onShowBarcodeScanner,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(56.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اسکن", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Autocomplete Box
                    if (itemQuerySearch.isNotBlank() && selectedProductId == 0L) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 150.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (matchedProducts.isNotEmpty()) {
                                    matchedProducts.forEach { p ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    onSelectedProductIdChanged(p.id)
                                                    onSelectedProductNameChanged(p.name)
                                                    onSelectedProductUnitChanged(p.unit)
                                                    onItemCustomPriceChanged(p.defaultUnitPrice.toLong().toString())
                                                    onItemQuerySearchChanged(p.name)
                                                    try { qtyFocusNode.requestFocus() } catch(e: Exception) {}
                                                }
                                                .padding(horizontal = 8.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(p.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                                if (p.stockQuantity != null) {
                                                    Text("موجودی: ${p.stockQuantity} ${p.unit}".toPersianDigits(), style = MaterialTheme.typography.bodySmall, color = Color(0xFF10B981))
                                                }
                                            }
                                            Text(
                                                text = "${formatPrice(p.defaultUnitPrice)} $currency".toPersianDigits(),
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    }
                                } else {
                                    Text("کالایی پیدا نشد؛ با افزودن به فاکتور، این کالا ذخیره میشود.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(8.dp))
                                }
                            }
                        }
                    }

                    // Price & Qty Fields (Moved UP)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ItemQuantityInput(
                            value = itemQtyInput,
                            onValueChange = onItemQtyInputChanged,
                            label = "تعداد / مقدار",
                            modifier = Modifier.weight(1.2f).focusRequester(qtyFocusNode)
                        )

                        PriceInput(
                            value = itemCustomPrice,
                            onValueChange = onItemCustomPriceChanged,
                            label = "قیمت واحد فی ($currency)",
                            modifier = Modifier.weight(1.8f)
                        )
                    }

                    // Insert Button (Moved UP)
                    Button(
                        onClick = onAddProductClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.PlaylistAddCheck, null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("افزودن کالا به لیست فاکتور", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Render List layout
        if (hasItems) {
            item {
                Text(
                    text = "ارقام ثبت شده در محاسبات فاکتور:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            itemsIndexed(addedItems) { idx, line ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditItem(idx) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (idx + 1).toString().toPersianDigits(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = line.productName, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${(if (line.quantity % 1.0 == 0.0) line.quantity.toInt().toString() else line.quantity.toString()).toPersianDigits()} ${line.unit} × ${formatPrice(line.unitPrice)} $currency".toPersianDigits(),
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${formatPrice(line.unitPrice * line.quantity)} $currency".toPersianDigits(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { onDeleteItem(line) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("هیج آیتم یا خدمتی هنوز به سند اضافه نشده است.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }

        // Bottom CTAs
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!hasItems) {
                    Text(
                        text = "⚠️ لطفاً حداقل یک قلم کالا یا خدمت جهت ادامه اضافه نمایید.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مرحله قبل")
                    }

                    Button(
                        onClick = {
                            if (hasItems) {
                                onNext()
                            } else {
                                Toast.makeText(context, "لطفاً ابتدا حداقل یک کالا یا خدمت به لیست اضافه بفرمایید.", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (hasItems) Color(0xFF10B981) else Color.Gray)
                    ) {
                        Text("ادامه به خلاصه حساب")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun WizardStep3(
    initType: String,
    currency: String,
    subSum: Double,
    discountType: String,
    onDiscountTypeChanged: (String) -> Unit,
    appliedDiscountAmount: String,
    onAppliedDiscountAmountChanged: (String) -> Unit,
    discountPercentage: String,
    onDiscountPercentageChanged: (String) -> Unit,
    discountVal: Double,
    appliedShippingCost: String,
    onAppliedShippingCostChanged: (String) -> Unit,
    appliedTaxRate: String,
    onAppliedTaxRateChanged: (String) -> Unit,
    finalTaxVal: Double,
    grandTotalSumResult: Double,
    itemCount: Int,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Accounting computations Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Subtotal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("جمع کل مبالغ کالاها:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${formatPrice(subSum)} $currency".toPersianDigits(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Discount
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("تخفیف کلی سند:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isPercent = discountType == "PERCENT"
                            OutlinedIconButton(
                                onClick = { onDiscountTypeChanged("AMOUNT") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (!isPercent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                colors = IconButtonDefaults.outlinedIconButtonColors(
                                    containerColor = if (!isPercent) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) {
                                Text("مبلغی ($currency)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedIconButton(
                                onClick = { onDiscountTypeChanged("PERCENT") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isPercent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                colors = IconButtonDefaults.outlinedIconButtonColors(
                                    containerColor = if (isPercent) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) {
                                Text("درصدی (%)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedTextField(
                            value = if (discountType == "PERCENT") discountPercentage else appliedDiscountAmount,
                            onValueChange = { input ->
                                val clean = input.toEnglishDigits().removeCommas()
                                if (discountType == "PERCENT") {
                                    val pct = clean.toDoubleOrNull() ?: 0.0
                                    if (pct in 0.0..100.0) onDiscountPercentageChanged(clean)
                                    else if (clean.isEmpty()) onDiscountPercentageChanged("")
                                } else {
                                    onAppliedDiscountAmountChanged(clean)
                                }
                            },
                            label = { Text(if (discountType == "PERCENT") "درصد تخفیف" else "مبلغ تخفیف خالص") },
                            placeholder = { Text(if (discountType == "PERCENT") "مانند: ۱۰" else "مبلغ دلخواه") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = if (discountType == "PERCENT") androidx.compose.ui.text.input.VisualTransformation.None else ThousandSeparatorVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (discountType == "PERCENT" && discountVal > 0) {
                            Text(
                                text = "مبلغ کسر شده تخفیف: ${formatPrice(discountVal)} $currency".toPersianDigits(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Shipping Cost
                    OutlinedTextField(
                        value = appliedShippingCost,
                        onValueChange = { onAppliedShippingCostChanged(it.toEnglishDigits().removeCommas()) },
                        label = { Text("سایر هزینه‌ها / هزینه حمل‌ونقل و باربری") },
                        placeholder = { Text("هزینه ارسال") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = ThousandSeparatorVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.LocalShipping, null, tint = MaterialTheme.colorScheme.primary) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Tax
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = appliedTaxRate,
                            onValueChange = {
                                val clean = it.toEnglishDigits()
                                val rate = clean.toDoubleOrNull() ?: 0.0
                                if (rate in 0.0..100.0) onAppliedTaxRateChanged(clean)
                                else if (clean.isEmpty()) onAppliedTaxRateChanged("")
                            },
                            label = { Text("درصد مالیات (%)") },
                            placeholder = { Text("۹ درصد ارزش افزوده") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1.2f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text("مبلغ مالیات بر ارزش افزوده:", fontSize = 11.sp, color = Color.Gray)
                            Text(text = "${formatPrice(finalTaxVal)} $currency".toPersianDigits(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Grand Total
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("قابل پرداخت (${itemCount.toString().toPersianDigits()} ردیف):", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                            Text("واحد ارز: $currency", fontSize = 9.sp, color = Color.Gray)
                        }
                        Text(
                            text = "${formatPrice(grandTotalSumResult)} $currency".toPersianDigits(),
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }

        // Bottom CTA Group
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مرحله قبل")
                }

                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = "ثبت نهایی", 
                            fontWeight = FontWeight.Bold, 
                            maxLines = 1, 
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WizardStep4(
    savedInvoiceId: Long,
    navController: NavController,
    viewModel: InvoiceViewModel,
    onEditClicked: () -> Unit,
    onNewInvoiceRequested: () -> Unit,
    onNewProformaRequested: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val savedRelFlow = remember(savedInvoiceId) {
        if (savedInvoiceId > 0L) viewModel.getInvoiceById(savedInvoiceId) else kotlinx.coroutines.flow.flowOf(null)
    }
    val savedRel by savedRelFlow.collectAsState(initial = null)
    val attachments by (if (savedInvoiceId > 0L) viewModel.getAttachmentsForInvoice(savedInvoiceId) else kotlinx.coroutines.flow.flowOf(emptyList())).collectAsState(initial = emptyList())
    val settings by viewModel.settings.collectAsState()
    val companyInfo by viewModel.companyInfo.collectAsState()
    val currency = settings?.currencyUnit ?: "تومان"

    var isGeneratingPdf by remember { mutableStateOf(false) }

    if (isGeneratingPdf) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text("در حال پردازش سند PDF...", fontSize = 13.sp)
                }
            }
        }
    }

    if (savedRel == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val document = savedRel!!
        val isProforma = document.invoice.invoiceType == "PROFORMA"
        val jalaliDate = com.example.utils.JalaliDateFormatter.format(document.invoice.issueDate)
        val totalCost = document.calculateTotal()

        val subtotal = document.items.sumOf { it.unitPrice * it.quantity }
        val discount = document.invoice.discountAmount
        val shipping = document.invoice.shippingFee
        val taxRate = document.invoice.taxRate
        val taxAmount = subtotal * (taxRate / 100.0)

        val filePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                scope.launch {
                    val copiedFile = com.example.utils.FileHelper.copyUriToInternalStorage(context, it, "attachments")
                    if (copiedFile != null) {
                        val fileName = com.example.utils.FileHelper.getFileName(context, it) ?: "attachment_${System.currentTimeMillis()}"
                        val mimeType = com.example.utils.FileHelper.getMimeType(context, it) ?: "application/octet-stream"
                        viewModel.addAttachment(savedInvoiceId, fileName, copiedFile.absolutePath, mimeType)
                        Toast.makeText(context, "فایل با موفقیت در پیوست‌ها ثبت شد.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Success Card Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }

                        Column {
                            Text(
                                text = if (isProforma) "پیش‌فاکتور با موفقیت ذخیره گردید" else "فاکتور فروش با موفقیت ثبت قطعی شد!",
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF047857),
                                fontSize = 14.sp
                            )
                            Text(
                                text = "کلیه تغییرات در انبارداری و سیستم حسابدرای اعمال شدند.",
                                fontSize = 10.sp,
                                color = Color(0xFF047857).copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Summary accounting rows
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        RulePairRow(label = "کد رهگیری سند:", value = document.invoice.invoiceNumber.toPersianDigits())
                        RulePairRow(label = "مشخصات خریدار:", value = document.customer.fullName)
                        RulePairRow(label = "تاریخ ثبت سند:", value = jalaliDate.toPersianDigits())

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        RulePairRow(label = "جمع ریز اقلام:", value = "${formatPrice(subtotal)} $currency".toPersianDigits())
                        if (discount > 0) {
                            RulePairRow(label = "تخفیف کسر شده:", value = "- ${formatPrice(discount)} $currency".toPersianDigits())
                        }
                        if (shipping > 0) {
                            RulePairRow(label = "حمل‌ونقل / باربری:", value = "+ ${formatPrice(shipping)} $currency".toPersianDigits())
                        }
                        if (taxAmount > 0) {
                            RulePairRow(label = "مالیات بر ارزش افزوده (${taxRate.toInt()}%):", value = "+ ${formatPrice(taxAmount)} $currency".toPersianDigits())
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("مبلغ نهایی پرداخت:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "${formatPrice(totalCost)} $currency".toPersianDigits(),
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // Attachments management
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.AttachFile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "مدیریت اسناد ضمیمه (پیوست)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            TextButton(onClick = { filePickerLauncher.launch("*/*") }) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Text("پیوست جدید", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (attachments.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("هیچ فایلی برای این سند پیوست نشده است.", fontSize = 11.sp, color = Color.Gray)
                            }
                        } else {
                            attachments.forEach { at ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                        .clickable { com.example.utils.FileHelper.openFile(context, File(at.filePath)) }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Text(at.fileName, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteAttachment(at) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Direct interactive service functions
            item {
                Text(
                    text = "ارسال، چاپ و خدمات سند صادر شده:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActionBlockCard(
                            title = "ارسال PDF سند",
                            desc = "به شبکه‌های اجتماعی",
                            icon = Icons.Default.Share,
                            color = MaterialTheme.colorScheme.primary,
                            onClick = {
                                if (!isGeneratingPdf) {
                                    isGeneratingPdf = true
                                    scope.launch {
                                        try {
                                            val pdfUri = viewModel.generateInvoicePdf(context, document, includeAttachments = attachments.isNotEmpty(), attachments = attachments)
                                            if (pdfUri != null) {
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "application/pdf"
                                                    putExtra(Intent.EXTRA_STREAM, pdfUri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                val chooser = Intent.createChooser(intent, "اشتراک‌گذاری PDF سند صادر شده")
                                                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(chooser)
                                            } else {
                                                Toast.makeText(context, "خطا در پیاده‌سازی فرمت پی دی اف", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("PdfActionDebug", "Error sharing PDF: ", e)
                                            Toast.makeText(context, "خطا در سیستم اشتراک‌گذاری", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isGeneratingPdf = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        ActionBlockCard(
                            title = "پیامک فاکتور",
                            desc = "ارسال مستقیم پیامک",
                            icon = Icons.Default.Sms,
                            color = Color(0xFF2563EB),
                            onClick = {
                                val number = document.customer.phoneNumber.toEnglishDigits()
                                if (number.isBlank()) {
                                    Toast.makeText(context, "شماره معتبر برای مشتری وجود ندارد.", Toast.LENGTH_SHORT).show()
                                } else {
                                    val storeName = companyInfo?.companyName?.takeIf { it.isNotBlank() } ?: "صادرکننده"
                                    val smsText = buildString {
                                        append("$storeName\n")
                                        append("${if (isProforma) "پیش‌فاکتور" else "فاکتور"} شماره ${document.invoice.invoiceNumber}\n")
                                        append("مبلغ نهایی: ${formatPrice(totalCost)} $currency\n")
                                        append("با تشکر")
                                    }.toPersianDigits()

                                    try {
                                        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("smsto:${number.trim()}")
                                            putExtra("sms_body", smsText)
                                        }
                                        context.startActivity(smsIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "سیستم پیام کوتاه ناموفق می‌باشد", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActionBlockCard(
                            title = "چاپ روی کاغذ",
                            desc = "خروجی چاپگر همراه فاکتور",
                            icon = Icons.Default.Print,
                            color = MaterialTheme.colorScheme.secondary,
                            onClick = {
                                if (!isGeneratingPdf) {
                                    isGeneratingPdf = true
                                    scope.launch {
                                        try {
                                            val pdfUri = viewModel.generateInvoicePdf(context, document, includeAttachments = attachments.isNotEmpty(), attachments = attachments)
                                            if (pdfUri != null) {
                                                try {
                                                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                                                    val printAdapter = com.example.utils.PdfPrintAdapter(
                                                        context,
                                                        java.io.File(context.cacheDir, "invoice_${document.invoice.invoiceNumber}.pdf")
                                                    )
                                                    printManager.print("invoice_${document.invoice.invoiceNumber}", printAdapter, android.print.PrintAttributes.Builder().build())
                                                } catch (e: Exception) {
                                                    android.util.Log.e("PdfActionDebug", "Error printing PDF: ", e)
                                                    Toast.makeText(context, "خطا در برقراری خروجی پرینتر", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("PdfActionDebug", "Error generating PDF for print: ", e)
                                            Toast.makeText(context, "خطا در ساخت قالب چاپ.", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isGeneratingPdf = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        ActionBlockCard(
                            title = "ویرایش مجدد",
                            desc = "برگشت فرم جهت اصلاح",
                            icon = Icons.Default.Edit,
                            color = Color(0xFFF59E0B),
                            onClick = onEditClicked,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Shortcuts flow
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNewInvoiceRequested,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("صدور فاکتور فروش تازه")
                    }

                    Button(
                        onClick = onNewProformaRequested,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PostAdd, null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("صدور پیش‌فاکتور تازه")
                    }

                    OutlinedButton(
                        onClick = { navController.navigate("dashboard") { popUpTo(0) } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Home, null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("بازگشت به داشبورد حسابداری")
                    }
                }
            }
        }
    }
}

@Composable
fun RulePairRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = Color.Gray)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ActionBlockCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(desc, color = Color.Gray, fontSize = 8.sp)
            }
        }
    }
}
