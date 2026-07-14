package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.utils.JalaliCalendar
import com.example.utils.toPersianDigits
import com.example.utils.PersianDigitConverter

val persianMonths = listOf(
    "فروردین", "اردیبهشت", "خرداد",
    "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر",
    "دی", "بهمن", "اسفند"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersianDatePickerDialog(
    initialTimestamp: Long,
    onDismissRequest: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    // Parse Initial Timestamp into Jalali parts
    val initialDatePart = PersianDigitConverter.toEnglish(JalaliCalendar.fromTimestamp(initialTimestamp)).split("/")
    val initYear = initialDatePart.getOrNull(0)?.toIntOrNull() ?: 1403
    val initMonth = initialDatePart.getOrNull(1)?.toIntOrNull() ?: 1
    val initDay = initialDatePart.getOrNull(2)?.toIntOrNull() ?: 1

    var selectedYear by remember { mutableIntStateOf(initYear) }
    var selectedMonth by remember { mutableIntStateOf(initMonth) }
    var selectedDay by remember { mutableIntStateOf(initDay) }

    fun applyQuickDate(offsetDays: Int) {
        val targetMillis = System.currentTimeMillis() + (offsetDays * 86400000L)
        val parts = PersianDigitConverter.toEnglish(JalaliCalendar.fromTimestamp(targetMillis)).split("/")
        selectedYear = parts.getOrNull(0)?.toIntOrNull() ?: 1403
        selectedMonth = parts.getOrNull(1)?.toIntOrNull() ?: 1
        selectedDay = parts.getOrNull(2)?.toIntOrNull() ?: 1
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header Display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "انتخاب تاریخ",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${selectedYear.toString().toPersianDigits()}/${selectedMonth.toString().padStart(2, '0').toPersianDigits()}/${selectedDay.toString().padStart(2, '0').toPersianDigits()}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider()

                // Quick Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ElevatedAssistChip(
                        onClick = { applyQuickDate(-1) },
                        label = { Text("دیروز") }
                    )
                    ElevatedAssistChip(
                        onClick = { applyQuickDate(0) },
                        label = { Text("امروز") }
                    )
                    ElevatedAssistChip(
                        onClick = { applyQuickDate(1) },
                        label = { Text("فردا") }
                    )
                }

                // Date Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Year
                    NumberPicker(
                        value = selectedYear,
                        range = 1350..1450,
                        onValueChange = { selectedYear = it },
                        label = "سال"
                    )
                    // Month
                    NumberPicker(
                        value = selectedMonth,
                        range = 1..12,
                        onValueChange = { selectedMonth = it },
                        label = "ماه",
                        textMapper = { persianMonths[it - 1] }
                    )
                    // Day
                    NumberPicker(
                        value = selectedDay,
                        range = 1..31,
                        onValueChange = { selectedDay = it },
                        label = "روز"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("انصراف", color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = {
                            val gregorianCal = JalaliCalendar.jalaliToGregorian(selectedYear, selectedMonth, selectedDay)
                            onDateSelected(gregorianCal.timeInMillis)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("تأیید")
                    }
                }
            }
        }
    }
}

@Composable
fun NumberPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    label: String,
    textMapper: ((Int) -> String)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = {
            if (value < range.last) onValueChange(value + 1) else onValueChange(range.first)
        }) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up")
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = textMapper?.invoke(value) ?: value.toString().padStart(if(value<100) 2 else 4, '0').toPersianDigits(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        IconButton(onClick = {
            if (value > range.first) onValueChange(value - 1) else onValueChange(range.last)
        }) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down")
        }
    }
}
