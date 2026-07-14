package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.example.ui.screens.defaultAppTextFieldColors
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalIsDarkTheme
import com.example.data.entity.Customer
import com.example.ui.viewmodel.InvoiceViewModel
import com.example.utils.IranianValidationHelper
import com.example.utils.toPersianDigits
import com.example.utils.toEnglishDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(navController: androidx.navigation.NavController, viewModel: InvoiceViewModel) {
    val context = LocalContext.current
    val foundCustomers by viewModel.searchedCustomers.collectAsState()
    val allCustomers by viewModel.customers.collectAsState()
    val query by viewModel.searchQuery.collectAsState()

    var showAddCustomerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // App Header Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "مشتریان و خریداران",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "مدیریت اطلاعات و تاریخچه مشتریان",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showAddCustomerDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("افزودن خریدار", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Search Field Style Design
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("جستجوی نام مشتری، شماره همراه...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else null,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Customers list
        if (allCustomers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "هنوز مشتری ثبت نشده است",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (foundCustomers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "هیچ مشتری فعالی پیدا نشد",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(foundCustomers) { cust ->
                    CustomerCard(
                        customer = cust,
                        onClick = {
                            navController.navigate("customer_detail/${cust.id}")
                        },
                        onDelete = {
                            viewModel.deleteCustomer(
                                customer = cust,
                                onSuccess = {
                                    Toast.makeText(context, "مشتری با موفقیت حذف شد.", Toast.LENGTH_SHORT).show()
                                },
                                onError = { errorMsg ->
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    // Modal popup creation sheet
    if (showAddCustomerDialog) {
        var fullName by remember { mutableStateOf("") }
        var phoneVal by remember { mutableStateOf("") }
        var nationalIdVal by remember { mutableStateOf("") }
        var economicCodeVal by remember { mutableStateOf("") }
        var customerAddress by remember { mutableStateOf("") }
        
        var nameError by remember { mutableStateOf<String?>(null) }
        var phoneError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddCustomerDialog = false },
            title = {
                Text(
                    text = "تعریف حساب مشتری جدید",
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
                        placeholder = { Text("نام مشتری را وارد یا انتخاب کنید") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = nameError != null,
                        supportingText = {
                            if (nameError != null) Text(nameError!!, color = MaterialTheme.colorScheme.error)
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = defaultAppTextFieldColors()
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
                        ModernNumericTextField(
                            value = nationalIdVal,
                            onValueChange = { nationalIdVal = it },
                            label = "شناسه/کد ملی",
                            modifier = Modifier.weight(1f)
                        )
                        ModernNumericTextField(
                            value = economicCodeVal,
                            onValueChange = { economicCodeVal = it },
                            label = "کد اقتصادی",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = customerAddress,
                        onValueChange = { customerAddress = it },
                        label = { Text("نشانی و آدرس خریدار") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        shape = RoundedCornerShape(8.dp),
                        colors = defaultAppTextFieldColors()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        var hasError = false
                        val englishPhoneVal = com.example.utils.PersianDigitConverter.toEnglish(phoneVal.trim())
                        val phonePattern = Regex("^09\\d{9}$")

                        if (fullName.isBlank()) {
                            nameError = "نام مشتری نمی‌تواند خالی باشد"
                            hasError = true
                        }
                        if (phoneVal.isBlank()) {
                            phoneError = "شماره موبایل نمی‌تواند خالی باشد"
                            hasError = true
                        } else if (!IranianValidationHelper.isValidPhoneNumber(phoneVal)) {
                            phoneError = "شماره موبایل نامعتبر است. فرمت صحیح: ۱۱ رقم با شروع ۰۹"
                            hasError = true
                        }

                        if (!hasError) {
                            viewModel.addCustomer(
                                fullName = fullName.trim(),
                                phone = phoneVal.trim(),
                                nationalId = nationalIdVal.trim().ifBlank { null },
                                code = economicCodeVal.trim().ifBlank { null },
                                address = customerAddress.trim().ifBlank { null }
                            )
                            showAddCustomerDialog = false
                        }
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("ذخیره مشخصات", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerDialog = false }) {
                    Text("انصراف", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun CustomerCard(customer: Customer, onClick: () -> Unit = {}, onDelete: () -> Unit) {
    val isDark = LocalIsDarkTheme.current
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading avatar bubble + Details
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = customer.fullName.trim().take(1).uppercase()
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column {
                        Text(
                            text = customer.fullName,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "همراه: ${customer.phoneNumber}".toPersianDigits(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action Tray: Phone Dial, WhatsApp send, and Trash Delete
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Call Option
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phoneNumber}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Call, "تماس تلفنی", tint = if (isDark) Color(0xFF34D399) else Color(0xFF10B981), modifier = Modifier.size(18.dp))
                    }

                    // Delete customer trigger
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Delete, "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Legal identifiers (Economic Code & National ID codes)
            if (!customer.nationalId.isNullOrEmpty() || !customer.economicCode.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!customer.nationalId.isNullOrEmpty()) {
                        Text(
                            text = "کد ملی: ${customer.nationalId}".toPersianDigits(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!customer.economicCode.isNullOrEmpty()) {
                        Text(
                            text = "کد اقتصادی: ${customer.economicCode}".toPersianDigits(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Geographic billing address layout
            if (!customer.address.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "نشانی: ${customer.address}".toPersianDigits(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("تایید حذف", fontWeight = FontWeight.Bold) },
            text = { Text("آیا از حذف این مشتری مطمئن هستید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}
