package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalIsDarkTheme
import com.example.data.dao.InvoiceWithRelations
import com.example.ui.viewmodel.InvoiceViewModel
import com.example.utils.formatPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: InvoiceViewModel) {
    val isDark = LocalIsDarkTheme.current
    val invoices by viewModel.invoices.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currency = settings?.currencyUnit ?: "تومان"

    // Analytical metrics calculations
    val totalInvoiceSum = invoices.sumOf { it.calculateTotal() }
    val totalReceiptSum = invoices.sumOf { it.calculatePaid() }
    val outstandingDebt = totalInvoiceSum - totalReceiptSum

    val invoiceCount = invoices.size
    val paidCount = invoices.count { it.invoice.status == "PAID" }
    val unpaidCount = invoices.count { it.invoice.status == "UNPAID" }
    val partialCount = invoices.count { it.invoice.status == "PARTIALLY_PAID" }

    // Grouping by status
    val statusBreakdown = mapOf(
        "تسویه کامل" to (if (totalInvoiceSum > 0) totalReceiptSum / totalInvoiceSum else 0.0),
        "مانده فاکتورها" to (if (totalInvoiceSum > 0) outstandingDebt / totalInvoiceSum else 0.0)
    )

    // Calculate top selling items
    val itemSalesMap = remember(invoices) {
        val map = mutableMapOf<String, Double>()
        invoices.forEach { rel ->
            rel.items.forEach { item ->
                val revenue = item.unitPrice * item.quantity
                map[item.productName] = (map[item.productName] ?: 0.0) + revenue
            }
        }
        map.toList().sortedByDescending { it.second }.take(4)
    }

    // Calculate top customers
    val customerSalesMap = remember(invoices) {
        val map = mutableMapOf<String, Double>()
        invoices.forEach { rel ->
            val total = rel.calculateTotal()
            map[rel.customer.fullName] = (map[rel.customer.fullName] ?: 0.0) + total
        }
        map.toList().sortedByDescending { it.second }.take(4)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper Title Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "گزارش‌ها و تحلیل مالی",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "بررسی عملکرد فروش و جریان نقدی",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "آمار",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        if (invoices.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "برای نمایش گزارش، ابتدا فاکتور ثبت کنید.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        } else {
            // Ledger distribution - Main stats Card Style
            item {
                Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "جریان نقدی غرفه (حساب تجمعی)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Total Sale block
                        Column(modifier = Modifier.weight(1f)) {
                            Text("کل فروش فاکتور شده", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${formatPrice(totalInvoiceSum)} $currency".toPersianDigits(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Received block
                        Column(modifier = Modifier.weight(1f)) {
                            Text("وصول شده (دریافتی)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${formatPrice(totalReceiptSum)} $currency".toPersianDigits(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF34D399) else Color(0xFF10B981) // Emerald Green Accent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Outstanding debt block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("کل مانده مطالبات (طلب فاکتور)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${formatPrice(outstandingDebt)} $currency".toPersianDigits(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error // Light red
                            )
                        }

                        // Circular indicator chart drawing using canvas
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(65.dp)) {
                            val paidRatio = if (totalInvoiceSum > 0) (totalReceiptSum / totalInvoiceSum).toFloat() else 0f
                            val arcColorEmpty = if (isDark) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                            val arcColorFull = if (isDark) Color(0xFF34D399) else Color(0xFF10B981)
                            
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(
                                    color = arcColorEmpty,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = arcColorFull,
                                    startAngle = -90f,
                                    sweepAngle = paidRatio * 360f,
                                    useCenter = false,
                                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Text(
                                text = "${(paidRatio * 100).toInt()}%".toPersianDigits(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Summary breakdowns (counters grid)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Invoices Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تعداد فاکتورها", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(Icons.Default.Receipt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$invoiceCount فقره".toPersianDigits(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Status breakdown counter
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تسویه شده کامل", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(Icons.Default.AssignmentTurnedIn, null, tint = if (isDark) Color(0xFF34D399) else Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$paidCount فاکتور".toPersianDigits(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF34D399) else Color(0xFF10B981)
                        )
                    }
                }
            }
        }

        // Chart Visual block (Horizontal Stack Chart)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "مانده فاکتور در مقایسه با تسویه نهایی",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val paidPercent = if (totalInvoiceSum > 0) (totalReceiptSum / totalInvoiceSum).toFloat() else 0f
                    val debtPercent = if (totalInvoiceSum > 0) (outstandingDebt / totalInvoiceSum).toFloat() else 0f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (paidPercent > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(if (paidPercent <= 0) 1e-5f else paidPercent)
                                    .background(if (isDark) Color(0xFF34D399) else Color(0xFF10B981))
                            )
                        }
                        if (debtPercent > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(if (debtPercent <= 0) 1e-5f else debtPercent)
                                    .background(if (isDark) Color(0xFFF87171) else Color(0xFFEF4444))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (isDark) Color(0xFF34D399) else Color(0xFF10B981)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("دریافت شده: ${(paidPercent * 100).toInt()}%".toPersianDigits(), style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (isDark) Color(0xFFF87171) else Color(0xFFEF4444)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مانده مطالبات فروشگاه: ${(debtPercent * 100).toInt()}%".toPersianDigits(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // Itemized top selling lists (Data Table styled)
        item {
            Text(
                text = "پرفروش‌ترین خدمات و کالاها",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (itemSalesMap.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "داده‌ای برای اقلام پرفروش وجود ندارد",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        // Data Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("عنوان سرویس / کالا", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text("حجم ناخالص فروش", modifier = Modifier.weight(1f), textAlign = Alignment.End.let { TextAlign.End }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }

                        itemSalesMap.forEach { pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(pair.first, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${formatPrice(pair.second)} $currency".toPersianDigits(),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Purchasing Customers (Customers Rank)
        item {
            Text(
                text = "بزرگترین خریداران (حجم مالی)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        if (customerSalesMap.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "مشتری با واریزی ثبت شده یافت نشد",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        // Data Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("نام و نام خانوادگی خریدار", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text("کل پرداخت فاکتور", modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }

                        customerSalesMap.forEach { pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(pair.first, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${formatPrice(pair.second)} $currency".toPersianDigits(),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF34D399) else Color(0xFF10B981),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
