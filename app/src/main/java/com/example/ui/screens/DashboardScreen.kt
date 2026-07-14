package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import com.example.data.dao.InvoiceWithRelations
import com.example.ui.viewmodel.InvoiceViewModel
import com.example.utils.JalaliCalendar
import com.example.utils.formatPrice
import com.example.utils.toPersianDigits
import coil.compose.AsyncImage
import com.example.data.entity.CompanyInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, viewModel: InvoiceViewModel) {
    val context = LocalContext.current
    val invoices by viewModel.invoices.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val products by viewModel.products.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val companyInfo by viewModel.companyInfo.collectAsState()
    
    val currency = settings?.currencyUnit ?: "تومان"
    val businessName = companyInfo?.companyName ?: "فروشگاه فرضی"

    // Today's jalali date string representation
    val todayStr = com.example.utils.JalaliDateFormatter.format(System.currentTimeMillis())
    val nowMs = System.currentTimeMillis()
    val sevenDaysAgoMs = nowMs - (7 * 24 * 60 * 60 * 1000L)
    val thirtyDaysAgoMs = nowMs - (30 * 24 * 60 * 60 * 1000L)

    // Filter to get only sales invoices (ignoring proforma invoices for pure sales metrics)
    val salesInvoices = remember(invoices) {
        invoices.filter { it.invoice.invoiceType == "INVOICE" }
    }

    // Calculative Metrics
    val dailySales = remember(salesInvoices, todayStr) {
        salesInvoices.filter { 
            com.example.utils.JalaliDateFormatter.format(it.invoice.issueDate) == todayStr 
        }.sumOf { it.calculateTotal() }
    }

    val weeklySales = remember(salesInvoices, sevenDaysAgoMs) {
        salesInvoices.filter { 
            it.invoice.issueDate >= sevenDaysAgoMs 
        }.sumOf { it.calculateTotal() }
    }

    val monthlySales = remember(salesInvoices, thirtyDaysAgoMs) {
        salesInvoices.filter { 
            it.invoice.issueDate >= thirtyDaysAgoMs 
        }.sumOf { it.calculateTotal() }
    }

    val threeMonthsSales = remember(salesInvoices) {
        val ninetyDaysAgoMs = System.currentTimeMillis() - (90 * 24 * 60 * 60 * 1000L)
        salesInvoices.filter { 
            it.invoice.issueDate >= ninetyDaysAgoMs 
        }.sumOf { it.calculateTotal() }
    }

    val yearlySales = remember(salesInvoices) {
        val currentCal = java.util.Calendar.getInstance()
        val currentYear = com.example.utils.JalaliCalendar.gregorianToJalali(
            currentCal.get(java.util.Calendar.YEAR), 
            currentCal.get(java.util.Calendar.MONTH) + 1, 
            currentCal.get(java.util.Calendar.DAY_OF_MONTH)
        ).year
        salesInvoices.filter {
            val invoiceCal = java.util.Calendar.getInstance().apply { timeInMillis = it.invoice.issueDate }
            val yr = com.example.utils.JalaliCalendar.gregorianToJalali(
                invoiceCal.get(java.util.Calendar.YEAR), 
                invoiceCal.get(java.util.Calendar.MONTH) + 1, 
                invoiceCal.get(java.util.Calendar.DAY_OF_MONTH)
            ).year
            yr == currentYear
        }.sumOf { it.calculateTotal() }
    }

    val todayInvoiceCount = remember(salesInvoices, todayStr) {
        salesInvoices.count { 
            com.example.utils.JalaliDateFormatter.format(it.invoice.issueDate) == todayStr 
        }
    }

    val customerCount = customers.size
    val productCount = products.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // App Custom Styled Top Bar Header Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            val logoPath = companyInfo?.logoPath
                            if (!logoPath.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = logoPath,
                                        contentDescription = "لوگوی فروشگاه",
                                        modifier = Modifier.fillMaxSize().padding(4.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = "لوگوی فروشگاه",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = businessName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val ownerName = companyInfo?.managerName ?: "صاحب فروشگاه"
                                Text(
                                    text = "مدیریت: $ownerName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledIconButton(
                                onClick = { navController.navigate("settings?tab=0") },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.size(40.dp)
                            ) {
                        Icon(Icons.Default.Settings, "تنظیمات برنامه", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        // Section: Modern Minimalist Business Summary Card (ERP / Zoho Invoice Inspired)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "فروش امروز",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${formatPrice(dailySales)} $currency".toPersianDigits(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val todayPoints = remember(salesInvoices) {
                                val cal = java.util.Calendar.getInstance()
                                val mutableHours = List(12) { 0.0 }.toMutableList()
                                salesInvoices.filter { com.example.utils.JalaliDateFormatter.format(it.invoice.issueDate) == todayStr }
                                    .forEach { 
                                        cal.timeInMillis = it.invoice.issueDate
                                        val hIdx = (cal.get(java.util.Calendar.HOUR_OF_DAY) / 2).coerceIn(0, 11)
                                        mutableHours[hIdx] += it.calculateTotal()
                                    }
                                mutableHours.map { it.toFloat() }
                             }
                             if (todayPoints.any { it > 0 }) {
                                 Sparkline(points = todayPoints, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth().height(24.dp).padding(top = 4.dp))
                             } else {
                                 Text("بدون تراکنش", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                             }
                        }
                        
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(60.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                                .align(Alignment.CenterVertically)
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "فروش این ماه",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${formatPrice(monthlySales)} $currency".toPersianDigits(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val monthPoints = remember(salesInvoices) {
                                val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
                                val segments = List(10) { 0.0 }.toMutableList()
                                salesInvoices.filter { it.invoice.issueDate >= thirtyDaysAgo }
                                    .forEach { 
                                        val dayIdx = ((System.currentTimeMillis() - it.invoice.issueDate) / (3 * 24 * 60 * 60 * 1000L)).toInt().coerceIn(0, 9)
                                        segments[9 - dayIdx] += it.calculateTotal()
                                    }
                                segments.map { it.toFloat() }
                            }
                            if (monthPoints.any { it > 0 }) {
                                Sparkline(points = monthPoints, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.fillMaxWidth().height(24.dp).padding(top = 4.dp))
                            } else {
                                Text("هنوز فروشی ثبت نشده است.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
        MetricSmallItem(Icons.Rounded.ReceiptLong, "فاکتورها", todayInvoiceCount.toString(), MaterialTheme.colorScheme.primary)
        MetricSmallItem(Icons.Default.Group, "مشتریان", customerCount.toString(), MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
        // Quick Actions area (دسترسی سریع)
        item {
            Column {
                Text(
                    text = "دسترسی سریع",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionItem(
                        title = "فاکتور",
                        icon = Icons.Rounded.Add,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { navController.navigate("invoice_edit/new?type=INVOICE") },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionItem(
                        title = "پیش‌فاکتور",
                        icon = Icons.Default.PostAdd,
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = { navController.navigate("invoice_edit/new?type=PROFORMA") },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionItem(
                        title = "مشتریان",
                        icon = Icons.Default.Group,
                        color = MaterialTheme.colorScheme.tertiary,
                        onClick = { navController.navigate("customers") },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionItem(
                        title = "کالاها",
                        icon = Icons.Rounded.Category,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { navController.navigate("inventory") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        // Headings for recent transactions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخرین فاکتورهای صادر شده",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "مشاهده همه",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { navController.navigate("invoices") }
                )
            }
        }
        // Recent Invoices List
        val recentInvoices = invoices.take(4)
        if (recentInvoices.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PostAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "هنوز فاکتوری صادر نشده است",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "برای صادر کردن فاکتور یا پیش‌فاکتور روی تعریف میانبر کلیک کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(recentInvoices) { inv ->
                val totalCost = inv.calculateTotal()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("invoice_edit/${inv.invoice.id}") },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (inv.invoice.status) {
                                            "PAID" -> Color(0xFF10B981).copy(alpha = 0.1f)
                                            "PARTIALLY_PAID" -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                                            else -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                imageVector = if (inv.invoice.invoiceType == "PROFORMA") Icons.Default.FileOpen else Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = when (inv.invoice.status) {
                                        "PAID" -> Color(0xFF10B981)
                                        "PARTIALLY_PAID" -> Color(0xFFF59E0B)
                                        else -> MaterialTheme.colorScheme.error
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "فاکتور شماره ${inv.invoice.invoiceNumber}".toPersianDigits(),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    // Custom visual badge for Invoice Type
                                    Surface(
                                        color = if (inv.invoice.invoiceType == "PROFORMA") MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        contentColor = if (inv.invoice.invoiceType == "PROFORMA") MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (inv.invoice.invoiceType == "PROFORMA") "پیش‌فاکتور" else "فاکتور فروش",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = inv.customer.fullName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "${formatPrice(totalCost)} $currency".toPersianDigits(),
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium
                             )
                            Surface(
                                color = when (inv.invoice.status) {
                                    "PAID" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                    "PARTIALLY_PAID" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                },
                                contentColor = when (inv.invoice.status) {
                                    "PAID" -> Color(0xFF0F5132)
                                    "PARTIALLY_PAID" -> Color(0xFF856404)
                                    else -> MaterialTheme.colorScheme.onErrorContainer
                                },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = when (inv.invoice.status) {
                                        "PAID" -> "تسویه شده"
                                        "PARTIALLY_PAID" -> {
                                            val remaining = inv.calculateRemaining()
                                            "پرداخت جزئی - باقیمانده: ${com.example.utils.formatPrice(remaining)} ${settings?.currencyUnit ?: "تومان"}".toPersianDigits()
                                        }
                                        else -> "پرداخت نشده"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        // Section: Latest Customers
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخرین مشتریان ثبت شده",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "مشاهده همه",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { navController.navigate("customers") }
                )
            }
        }
        val recentCustomers = customers.take(3)
        if (recentCustomers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "هنوز مشتری ثبت نشده است",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentCustomers) { cust ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("customers") },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                val initial = cust.fullName.trim().take(1).uppercase()
                                Text(
                                    text = initial,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = cust.fullName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = cust.phoneNumber.toPersianDigits(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "جزییات مشتری",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun MetricSmallItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value.toPersianDigits(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DashboardHeader(
    businessName: String,
    companyInfo: CompanyInfo?,
    todayStr: String,
    navController: NavController
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Row 1: Logo, Store Name & Quick Navigation Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Store Logo or Styled Fallback Initials Bubble
                    val logoPath = companyInfo?.logoPath
                    if (!logoPath.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = logoPath,
                                contentDescription = "لوگوی فروشگاه",
                                modifier = Modifier.fillMaxSize().padding(2.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEEF2FF)) // soft Indigo background
                                .border(1.5.dp, Color(0xFFC7D2FE), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = "لوگوی فروشگاه",
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = businessName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            fontSize = 17.sp
                        )
                        val ownerName = companyInfo?.managerName ?: "صاحب فروشگاه ثبت نشده"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "مدیریت: $ownerName",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Navigation Access Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledIconButton(
                        onClick = { navController.navigate("settings?tab=0") },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFFF1F5F9),
                            contentColor = Color(0xFF475569)
                        ),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Settings, "تنظیمات", modifier = Modifier.size(20.dp))
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Row 2: Persian Date Badge Info Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "امروز شمسی:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = todayStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun QuickActionItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BusinessMetricCell(
    title: String,
    amount: Double,
    description: String,
    currency: String,
    accentColor: Color,
    countText: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (countText != null) {
                    Text(
                        text = countText.toPersianDigits(),
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "${formatPrice(amount)} $currency".toPersianDigits(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun Sparkline(points: List<Float>, color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas
        val max = points.maxOrNull()?.takeIf { it > 0f } ?: 1f
        val path = androidx.compose.ui.graphics.Path()
        val stepX = size.width / (points.size - 1).coerceAtLeast(1)
        
        points.forEachIndexed { i, p ->
            val x = i * stepX
            val y = size.height - (p / max * size.height)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}
