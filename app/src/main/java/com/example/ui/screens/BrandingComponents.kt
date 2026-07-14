package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

@Composable
fun BrandingCard(
    title: String,
    icon: ImageVector,
    path: String?,
    onUpload: () -> Unit,
    onDesign: () -> Unit,
    onDelete: () -> Unit,
    invoiceChecked: Boolean,
    onInvoiceChange: (Boolean) -> Unit,
    proformaChecked: Boolean,
    onProformaChange: (Boolean) -> Unit
) {
    val isSelected = path != null

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            }
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Header Layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Status Badge
                Surface(
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isSelected) "انتخاب شده" else "انتخاب نشده",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. Preview Frame Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    AsyncImage(
                        model = path,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(12.dp)
                    )

                    // Overlay Delete Button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(30.dp)
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                                shape = CircleShape
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "جهت ثبت در اسناد، تصویر را انتخاب یا طراحی کنید",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 3. Side-by-side action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Design button
                FilledTonalButton(
                    onClick = onDesign,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "طراحی اختصاصی",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Upload button
                OutlinedButton(
                    onClick = onUpload,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "انتخاب از گالری",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )

            // 4. Compact integrated display Toggles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "نمایش خودکار در سند:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Invoice Checked
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onInvoiceChange(!invoiceChecked) }
                            .padding(4.dp)
                    ) {
                        Checkbox(
                            checked = invoiceChecked,
                            onCheckedChange = onInvoiceChange,
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "فاکتور",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Proforma Checked
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onProformaChange(!proformaChecked) }
                            .padding(4.dp)
                    ) {
                        Checkbox(
                            checked = proformaChecked,
                            onCheckedChange = onProformaChange,
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "پیش‌فاکتور",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LogoPresetDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedColor by remember { mutableStateOf(Color(0xFF0F172A)) }
    var brandName by remember { mutableStateOf("") }
    
    val colors = listOf(
        Color(0xFF0F172A), // Luxury Slate
        Color(0xFF2563EB), // Royal Blue
        Color(0xFF10B981), // Emerald Green
        Color(0xFFDC2626), // Persian Rose
        Color(0xFFD97706), // Warm Amber
        Color(0xFF7C3AED)  // Royal Purple
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "طراحی فوری لوگوی متنی",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = brandName,
                    onValueChange = { brandName = it },
                    label = { Text("نام فروشگاه (برای درج روی لوگو)") },
                    placeholder = { Text("مثال: فروشگاه گندم") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Text(
                    text = "رنگ سازمانی لوگو:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = color }
                                .border(
                                    width = if (selectedColor == color) 3.dp else 1.dp,
                                    color = if (selectedColor == color) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                        )
                    }
                }
                
                Spacer(Modifier.height(4.dp))
                
                // Textual Logo design box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(selectedColor.copy(alpha = 0.08f))
                        .border(1.5.dp, selectedColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (brandName.isBlank()) "لوگوی شما" else brandName,
                        color = selectedColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        val file = File(context.filesDir, "logo_${System.currentTimeMillis()}.png")
                        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(
                                (selectedColor.alpha * 255).toInt(),
                                (selectedColor.red * 255).toInt(),
                                (selectedColor.green * 255).toInt(),
                                (selectedColor.blue * 255).toInt()
                            )
                            textSize = 72f
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                        val yPos = (canvas.height / 2f) - ((paint.descent() + paint.ascent()) / 2f)
                        canvas.drawText(if (brandName.isBlank()) "SHOP" else brandName, 256f, yPos, paint)
                        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        onSelect(file.absolutePath)
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("ثبت و تایید", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("لغو")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun SimpleStampGenDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val context = LocalContext.current
    var stampText by remember { mutableStateOf("") }
    var stampSubText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "طراحی هوشمند مهر دایره‌ای",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = stampText,
                    onValueChange = { stampText = it },
                    label = { Text("عنوان مهر (مرکز)") },
                    placeholder = { Text("مثال: تایید شد") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = stampSubText,
                    onValueChange = { stampSubText = it },
                    label = { Text("متن پیرامون (اختیاری)") },
                    placeholder = { Text("مثال: فروشگاه گندم - شماره ثبت ۱۱۰") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Spacer(Modifier.height(4.dp))
                
                // Seal circular shape representation
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .align(Alignment.CenterHorizontally)
                        .background(Color(0xFF1E3A8A).copy(alpha = 0.05f), CircleShape)
                        .border(3.dp, Color(0xFF1E3A8A), CircleShape)
                        .padding(8.dp)
                        .border(1.dp, Color(0xFF1E3A8A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = if (stampText.isBlank()) "مهر تایید" else stampText,
                            color = Color(0xFF1E3A8A),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        if (stampSubText.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stampSubText,
                                color = Color(0xFF1E3A8A).copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Normal,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val file = File(context.filesDir, "stamp_${System.currentTimeMillis()}.png")
                    val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#1E3A8A")
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 14f
                        isAntiAlias = true
                    }
                    
                    canvas.drawCircle(256f, 256f, 240f, paint)
                    paint.strokeWidth = 4f
                    canvas.drawCircle(256f, 256f, 220f, paint)
                    
                    // Center Main Text
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#1E3A8A")
                        textSize = 52f
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    
                    val textY = if (stampSubText.isBlank()) {
                        256f - ((textPaint.descent() + textPaint.ascent()) / 2f)
                    } else {
                        230f - ((textPaint.descent() + textPaint.ascent()) / 2f)
                    }
                    canvas.drawText(if (stampText.isBlank()) "مهر تایید" else stampText, 256f, textY, textPaint)
                    
                    // Subtitle Ring Text
                    if (stampSubText.isNotBlank()) {
                        val subPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#1E3A8A")
                            textSize = 34f
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                            typeface = android.graphics.Typeface.DEFAULT
                        }
                        val subY = 325f - ((subPaint.descent() + subPaint.ascent()) / 2f)
                        canvas.drawText(stampSubText, 256f, subY, subPaint)
                    }
                    
                    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    onSave(file.absolutePath)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("ذخیره مهر", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("لغو")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun SignaturePadDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val context = LocalContext.current
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke = remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ترسیم امضای دیجیتال",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "امضای خود را در کادر زیر بکشید. امضا کاملاً در محدوده کادر حفظ شده و با ذخیره‌سازی، پس‌زمینه آن شفاف ثبت خواهد شد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .clipToBounds()
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val w = canvasSize.width.toFloat()
                                    val h = canvasSize.height.toFloat()
                                    val clampedX = offset.x.coerceIn(0f, w)
                                    val clampedY = offset.y.coerceIn(0f, h)
                                    currentStroke.value = listOf(Offset(clampedX, clampedY))
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val w = canvasSize.width.toFloat()
                                    val h = canvasSize.height.toFloat()
                                    val clampedX = change.position.x.coerceIn(0f, w)
                                    val clampedY = change.position.y.coerceIn(0f, h)
                                    currentStroke.value = currentStroke.value + Offset(clampedX, clampedY)
                                },
                                onDragEnd = {
                                    if (currentStroke.value.isNotEmpty()) {
                                        strokes.add(currentStroke.value)
                                        currentStroke.value = emptyList()
                                    }
                                }
                            )
                        }
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        strokes.forEach { stroke ->
                            for (i in 0 until stroke.size - 1) {
                                drawLine(
                                    color = Color(0xFF0F172A), // Premium Ink Color Deep Indigo/Slate
                                    start = stroke[i],
                                    end = stroke[i + 1],
                                    strokeWidth = 4.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                        val cur = currentStroke.value
                        for (i in 0 until cur.size - 1) {
                            drawLine(
                                color = Color(0xFF0F172A),
                                start = cur[i],
                                end = cur[i + 1],
                                strokeWidth = 4.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    
                    if (strokes.isEmpty() && currentStroke.value.isEmpty()) {
                        Text(
                            text = "محل ترسیم امضا",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray.copy(alpha = 0.45f),
                            modifier = Modifier.align(Alignment.Center),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    // Clear Button
                    if (strokes.isNotEmpty()) {
                        IconButton(
                            onClick = { 
                                strokes.clear()
                                currentStroke.value = emptyList()
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(34.dp)
                                .background(Color.Red.copy(alpha = 0.08f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "پاک کردن",
                                tint = Color.Red,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (strokes.isEmpty()) return@Button
                    val w = if (canvasSize.width > 0) canvasSize.width else 600
                    val h = if (canvasSize.height > 0) canvasSize.height else 300
                    
                    val file = File(context.filesDir, "sig_${System.currentTimeMillis()}.png")
                    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
                    
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#0F172A")
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 10f
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        strokeJoin = android.graphics.Paint.Join.ROUND
                        isAntiAlias = true
                    }
                    
                    strokes.forEach { stroke ->
                        val path = android.graphics.Path()
                        if (stroke.isNotEmpty()) {
                            path.moveTo(stroke[0].x, stroke[0].y)
                            for (i in 1 until stroke.size) {
                                path.lineTo(stroke[i].x, stroke[i].y)
                            }
                            canvas.drawPath(path, paint)
                        }
                    }
                    
                    try {
                        FileOutputStream(file).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        onSave(file.absolutePath)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("ثبت و ذخیره امضا", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("لغو")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun ImageCropDialog(bitmap: Bitmap, onDismiss: () -> Unit, onCropSuccess: (String) -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "برش مربعی تصویر",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = { 
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black), 
                contentAlignment = Alignment.Center
            ) {
                Image(bitmap.asImageBitmap(), null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                Box(Modifier.fillMaxSize().border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp)))
            }
        },
        confirmButton = { 
            Button(
                onClick = {
                    val size = minOf(bitmap.width, bitmap.height)
                    val x = (bitmap.width - size) / 2
                    val y = (bitmap.height - size) / 2
                    val cropped = Bitmap.createBitmap(bitmap, x, y, size, size)
                    
                    val file = File(context.filesDir, "branding_${System.currentTimeMillis()}.png")
                    FileOutputStream(file).use { out: FileOutputStream ->
                        cropped.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    onCropSuccess(file.absolutePath)
                },
                shape = RoundedCornerShape(10.dp)
            ) { 
                Text("برش و ذخیره تصویر", fontWeight = FontWeight.Bold) 
            } 
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text("لغو") 
            } 
        },
        shape = RoundedCornerShape(24.dp)
    )
}

fun loadMutableBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        null
    }
}

// Deprecated visual elements kept for backwards compatibility/fallback reference
@Composable
fun BrandingItemRow(
    title: String,
    path: String?,
    onUpload: () -> Unit,
    onDesign: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            if (path != null) {
                AsyncImage(
                    model = path,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDesign, contentPadding = PaddingValues(0.dp)) {
                    Text("طراحی", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onUpload, contentPadding = PaddingValues(0.dp)) {
                    Text("انتخاب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                if (path != null) {
                    TextButton(onClick = onDelete, contentPadding = PaddingValues(0.dp)) {
                        Text("حذف", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BrandingVisibilityToggle(
    title: String,
    invoiceChecked: Boolean,
    onInvoiceChange: (Boolean) -> Unit,
    proformaChecked: Boolean,
    onProformaChange: (Boolean) -> Unit
) {
    Column {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(invoiceChecked, onInvoiceChange)
            Text("فاکتور", fontSize = 12.sp)
            Spacer(Modifier.width(16.dp))
            Checkbox(proformaChecked, onProformaChange)
            Text("پیش‌فاکتور", fontSize = 12.sp)
        }
    }
}
