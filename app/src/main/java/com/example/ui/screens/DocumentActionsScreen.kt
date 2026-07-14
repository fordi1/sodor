package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.dao.InvoiceWithRelations
import com.example.ui.viewmodel.InvoiceViewModel
import kotlinx.coroutines.launch
import com.example.utils.JalaliCalendar
import com.example.utils.formatPrice
import com.example.utils.toPersianDigits
import com.example.utils.toEnglishDigits
import com.example.utils.removeCommas
import com.example.utils.ThousandSeparatorVisualTransformation
import com.example.ui.screens.defaultAppTextFieldColors
import java.io.File
import java.net.URLEncoder

import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenDark
import com.example.ui.theme.WarningYellow
import com.example.ui.theme.WarningYellowDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentActionsScreen(
    navController: NavController,
    viewModel: InvoiceViewModel,
    invoiceId: Long
) {
    val context = LocalContext.current
    val invoiceFlow = remember(invoiceId) { viewModel.getInvoiceById(invoiceId) }
    val rel by invoiceFlow.collectAsState(initial = null)
    val attachments by viewModel.getAttachmentsForInvoice(invoiceId).collectAsState(initial = emptyList())
    val settings by viewModel.settings.collectAsState()
    val companyInfo by viewModel.companyInfo.collectAsState()
    val currency = settings?.currencyUnit ?: "تومان"

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var isGeneratingPdf by remember { mutableStateOf(false) }

    if (isGeneratingPdf) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text("در حال ساخت سند PDF...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    val docTypeName = if (rel?.invoice?.invoiceType == "PROFORMA") "پیش‌فاکتور" else "فاکتور"
                    Text("عملیات $docTypeName شماره #${rel?.invoice?.invoiceNumber ?: ""}".toPersianDigits(), fontWeight = FontWeight.Bold) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("dashboard") { popUpTo(0) } }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("invoice_edit/${invoiceId}") }) {
                        Icon(Icons.Default.Edit, contentDescription = "ویرایش سند")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (rel == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val document = rel!!
            val totalCost = document.calculateTotal()
            val jalaliDate = com.example.utils.JalaliDateFormatter.format(document.invoice.issueDate)
            val isProforma = document.invoice.invoiceType == "PROFORMA"

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val isDark = LocalIsDarkTheme.current
                val successColor = if (isDark) SuccessGreenDark else SuccessGreen
                val warningColor = if (isDark) WarningYellowDark else WarningYellow

                // 1. Success Message Banner Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else successColor.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else successColor.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(successColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isProforma) "پیش‌فاکتور با موفقیت ذخیره شد" else "فاکتور فروش با موفقیت ثبت و نهایی گردید!",
                                fontWeight = FontWeight.Black,
                                color = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else successColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "کلیه تغییرات و محاسبات مالی در سیستم حسابداری ذخیره و اعمال شد.",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else successColor.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // 2. Document Summary Card
                val subtotal = document.items.sumOf { it.unitPrice * it.quantity }
                val discount = document.invoice.discountAmount
                val shipping = document.invoice.shippingFee
                val taxRate = document.invoice.taxRate
                val taxAmount = subtotal * (taxRate / 100.0)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isProforma) "خلاصه حساب پیش‌فاکتور" else "خلاصه حساب فاکتور",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        SummaryRow(label = "نوع سند صادر شده:", value = if (isProforma) "پیش‌فاکتور رسمی" else "فاکتور فروش")
                        SummaryRow(label = "شماره ردیف رهگیری:", value = document.invoice.invoiceNumber.toPersianDigits())
                        SummaryRow(label = "نام و مشخصات خریدار:", value = document.customer.fullName)
                        SummaryRow(label = "تاریخ صدور سند:", value = jalaliDate.toPersianDigits())
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        SummaryRow(label = "جمع کل ریز اقلام:", value = "${formatPrice(subtotal)} $currency".toPersianDigits())
                        if (discount > 0) {
                            SummaryRow(label = "تخفیف کلی اعطایی:", value = "- ${formatPrice(discount)} $currency".toPersianDigits())
                        }
                        if (shipping > 0) {
                            SummaryRow(label = "هزینه حمل‌ونقل:", value = "+ ${formatPrice(shipping)} $currency".toPersianDigits())
                        }
                        if (taxRate > 0) {
                            SummaryRow(label = "مالیات بر ارزش افزوده (${taxRate.toInt()}%):", value = "+ ${formatPrice(taxAmount)} $currency".toPersianDigits())
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مبلغ نهایی قابل پرداخت:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${formatPrice(totalCost)} $currency".toPersianDigits(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "وضعیت پرداخت و تسویه:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                color = when (document.invoice.status) {
                                    "PAID" -> if (isDark) SuccessGreenDark.copy(alpha = 0.2f) else SuccessGreen.copy(alpha = 0.1f)
                                    "PARTIALLY_PAID" -> if (isDark) WarningYellowDark.copy(alpha = 0.2f) else WarningYellow.copy(alpha = 0.1f)
                                    else -> MaterialTheme.colorScheme.errorContainer
                                },
                                contentColor = when (document.invoice.status) {
                                    "PAID" -> if (isDark) SuccessGreenDark else SuccessGreen
                                    "PARTIALLY_PAID" -> if (isDark) WarningYellowDark else WarningYellow
                                    else -> MaterialTheme.colorScheme.error
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = when (document.invoice.status) {
                                        "PAID" -> "تسویه شده"
                                        "PARTIALLY_PAID" -> {
                                            val remaining = document.calculateRemaining()
                                            "پرداخت جزئی - باقیمانده: ${formatPrice(remaining)} $currency".toPersianDigits()
                                        }
                                        else -> "پرداخت نشده"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Attachments Management Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "مدیریت اسناد پیوست",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (attachments.isNotEmpty()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = attachments.size.toString().toPersianDigits(),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                                androidx.activity.result.contract.ActivityResultContracts.GetContent()
                            ) { uri: Uri? ->
                                uri?.let {
                                    val copiedFile = com.example.utils.FileHelper.copyUriToInternalStorage(context, it, "attachments")
                                    if (copiedFile != null) {
                                        val fileName = com.example.utils.FileHelper.getFileName(context, it) ?: "unnamed_file"
                                        val mimeType = com.example.utils.FileHelper.getMimeType(context, it) ?: "application/octet-stream"
                                        viewModel.addAttachment(invoiceId, fileName, copiedFile.absolutePath, mimeType)
                                        Toast.makeText(context, "پیوست با موفقیت اضافه شد.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "خطا در ذخیره فایل پیوست.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            TextButton(
                                onClick = { filePickerLauncher.launch("*/*") },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("افزودن پیوست جدید", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        if (attachments.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "هنوز هیچ فایلی برای این سند ضمیمه نشده است.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            attachments.forEach { attachment ->
                                AttachmentItem(
                                    attachment = attachment,
                                    onOpen = { com.example.utils.FileHelper.openFile(context, File(attachment.filePath)) },
                                    onDelete = { viewModel.deleteAttachment(attachment) }
                                )
                            }
                        }
                    }
                }

                // 4. Actions Title
                Text(
                    text = "ارسال، چاپ و خدمات سند",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )

                // 4. Primary Interactive PDF, Print, Share Action Grid (Visuals in Columns)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActionCard(
                            title = if (isProforma) "ارسال PDF پیش‌فاکتور" else "ارسال PDF فاکتور",
                            desc = "به پلتفرم‌های پیام‌رسان",
                            icon = Icons.Default.Share,
                            color = MaterialTheme.colorScheme.secondary,
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
                                                val chooser = Intent.createChooser(intent, "اشتراک‌گذاری قالب ارسال")
                                                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(chooser)
                                            } else {
                                                Toast.makeText(context, "خطا در ساخت قالب ارسال", Toast.LENGTH_SHORT).show()
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
                        
                        ActionCard(
                            title = if (isProforma) "ارسال پیش‌فاکتور با پیامک" else "ارسال فاکتور با پیامک",
                            desc = "ارسال خلاصه سند برای مشتری",
                            icon = Icons.Default.Sms,
                            color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                            onClick = {
                                val number = com.example.utils.PersianDigitConverter.toEnglish(document.customer.phoneNumber.trim())
                                val phonePattern = Regex("^09\\d{9}$")
                                if (number.isBlank() || !phonePattern.matches(number)) {
                                    Toast.makeText(context, "شماره موبایل معتبر برای مشتری ثبت نشده است.", Toast.LENGTH_SHORT).show()
                                } else {
                                    val storeName = companyInfo?.companyName?.takeIf { it.isNotBlank() } ?: "فروشگاه"
                                    val docName = if (isProforma) "پیش‌فاکتور" else "فاکتور"
                                    val invoiceNum = document.invoice.invoiceNumber
                                    val totalPaid = document.calculatePaid()
                                    val remaining = document.calculateRemaining()
                                    
                                    val smsText = buildString {
                                        append("$storeName\n")
                                        append("$docName شماره $invoiceNum\n")
                                        append("تاریخ: $jalaliDate\n")
                                        append("----------------\n")
                                        append("مبلغ کل: ${formatPrice(totalCost)} $currency\n")
                                        if (totalPaid > 0) {
                                            append("پرداخت شده: ${formatPrice(totalPaid)} $currency\n")
                                            append("مانده حساب: ${formatPrice(remaining)} $currency\n")
                                        }
                                        append("----------------\n")
                                        append("با تشکر از خرید شما")
                                    }.toPersianDigits()
                                    
                                    try {
                                        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("smsto:${number.trim()}")
                                            putExtra("sms_body", smsText)
                                        }
                                        context.startActivity(smsIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "امکان باز کردن برنامه پیامک وجود ندارد.", Toast.LENGTH_SHORT).show()
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
                        ActionCard(
                            title = "چاپ مستقیم",
                            desc = "ارسال به چاپگر متصل",
                            icon = Icons.Default.Print,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                                    printManager.print(
                                                        "invoice_${document.invoice.invoiceNumber}",
                                                        printAdapter,
                                                        android.print.PrintAttributes.Builder().build()
                                                    )
                                                } catch (e: Exception) {
                                                    android.util.Log.e("PdfActionDebug", "Error printing PDF: ", e)
                                                    Toast.makeText(context, "خطا در آماده‌سازی چاپ.", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "خطا در آماده‌سازی چاپ.", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch(e: Exception) {
                                            android.util.Log.e("PdfActionDebug", "Error generating PDF: ", e)
                                            Toast.makeText(context, "خطا در ساخت قالب چاپ.", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isGeneratingPdf = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        ActionCard(
                            title = "تسویه حساب",
                            desc = "ثبت وضعیت پرداخت",
                            icon = Icons.Default.Payments,
                            color = successColor,
                            onClick = {
                                showPaymentDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Delete Action Section (Discreet / destructive check)
                Button(
                    onClick = { showDeleteConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer, 
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حذف دائم این سند مالی از انبار", fontWeight = FontWeight.Black)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 5. Workflows Shortcuts Title
                Text(
                    text = "عملیات جانبی و گام‌های بعدی",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Navigation and Add Shortcuts
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { navController.navigate("invoice_edit/new?type=INVOICE") },
                        colors = ButtonDefaults.buttonColors(containerColor = successColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ثبت و صدور فاکتور جدید فروش", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { navController.navigate("invoice_edit/new?type=PROFORMA") },
                        colors = ButtonDefaults.buttonColors(containerColor = warningColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PostAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ثبت و ثبت پیش‌فاکتور جدید", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { navController.navigate("dashboard") { popUpTo(0) } },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("بازگشت به پیشخوان اصلی مدیریت", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        val isProforma = rel?.invoice?.invoiceType == "PROFORMA"
        val titleText = if (isProforma) "حذف پیش‌فاکتور" else "حذف فاکتور"
        val messageText = if (isProforma) "آیا از حذف این پیش فاکتور مطمئن هستید؟" else "آیا از حذف این فاکتور مطمئن هستید؟"
        val successMessage = if (isProforma) "پیش فاکتور با موفقیت حذف شد." else "فاکتور با موفقیت حذف شد."

        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(titleText) },
            text = { Text(messageText) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteInvoice(
                            id = invoiceId,
                            onSuccess = {
                                Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
                                navController.navigate("dashboard") { popUpTo(0) }
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        )
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (showPaymentDialog && rel != null) {
        val document = rel!!
        val totalAmount = document.calculateTotal()
        val currentPaid = document.calculatePaid()
        var newPaidAmountStr by remember { mutableStateOf(currentPaid.takeIf{it > 0}?.toLong()?.toString() ?: "") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("تسویه و وضعیت پرداخت") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("مبلغ کل سند: ${formatPrice(totalAmount)} $currency".toPersianDigits(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    
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
                    
                    Text("مبلغ باقیمانده: ${formatPrice(remaining)} $currency".toPersianDigits(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    
                    val statusText = when {
                        parsedPaid >= totalAmount - 0.5 -> "تسویه شده" // Small allowance for rounding
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
                        
                        // We will clear existing payments and add one simple payment representing total paid amount.
                        // Or we can just calculate the difference and add a payment for that!
                        // The user said: "اگر کاربر دوباره باز کرد مقدار پرداخت شده حفظ شده باشد".
                        // So a simple approach is: delete all existing payments for invoice and add one payment equaling this new amount.
                        scope.launch {
                            try {
                                viewModel.resetPaymentsAndSetTotalPaid(invoiceId, parsed)
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
                TextButton(onClick = { showPaymentDialog = false }) {
                    Text("بستن")
                }
            }
        )
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ActionCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
fun AttachmentItem(
    attachment: com.example.data.entity.Attachment,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .clickable { onOpen() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val icon = when {
            attachment.mimeType.startsWith("image/") -> Icons.Default.Image
            attachment.mimeType == "application/pdf" -> Icons.Default.PictureAsPdf
            else -> Icons.Default.Description
        }
        
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attachment.fileName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = attachment.mimeType.substringAfter("/"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = com.example.utils.JalaliDateFormatter.format(attachment.createdAt).toPersianDigits(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
        }
    }
}
