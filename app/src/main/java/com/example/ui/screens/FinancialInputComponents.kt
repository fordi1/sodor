package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.toEnglishDigits
import com.example.utils.toPersianDigits

import androidx.compose.ui.text.input.VisualTransformation
import com.example.utils.ThousandSeparatorVisualTransformation
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.style.TextDirection

@Composable
fun defaultAppTextFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), // stronger border
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), // slight grey background to separate from white background
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
)

/**
 * A generic modern numeric text field styled like Samsung Health.
 * Handles Persian display, English storage, and optional thousand separators.
 */
@Composable
fun ModernNumericTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPrice: Boolean = false,
    errorMessage: String? = null,
    isError: Boolean = false,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    focusRequester: FocusRequester? = null
) {
    OutlinedTextField(
        value = value.toPersianDigits(),
        onValueChange = { input ->
            val english = input.toEnglishDigits()
            // Filter: only digits
            val filtered = english.filter { it.isDigit() || it == '.' } 
            onValueChange(filtered)
        },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        modifier = if (focusRequester != null) modifier.focusRequester(focusRequester) else modifier,
        isError = isError || errorMessage != null,
        supportingText = {
            if (errorMessage != null) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = if (isPrice) ThousandSeparatorVisualTransformation() else VisualTransformation.None,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(12.dp),
        colors = defaultAppTextFieldColors()
    )
}

@Composable
fun PriceInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "مبلغ",
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    isError: Boolean = false
) {
    ModernNumericTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier.fillMaxWidth(),
        isPrice = true,
        errorMessage = errorMessage,
        isError = isError
    )
}

@Composable
fun ItemQuantityInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "تعداد",
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    focusRequester: FocusRequester? = null
) {
    ModernNumericTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier.fillMaxWidth(),
        isPrice = false,
        errorMessage = errorMessage,
        focusRequester = focusRequester
    )
}

@Composable
fun BankCardInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "شماره کارت",
    isError: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    val f1 = remember { FocusRequester() }
    val f2 = remember { FocusRequester() }
    val f3 = remember { FocusRequester() }
    val f4 = remember { FocusRequester() }

    // Normalize input (expected raw 16 digits)
    val cleanTotal = value.toEnglishDigits().filter { it.isDigit() }.take(16)
    
    // Split into 4 parts
    val p1 = cleanTotal.substring(0, cleanTotal.length.coerceAtMost(4))
    val p2 = if (cleanTotal.length > 4) cleanTotal.substring(4, cleanTotal.length.coerceAtMost(8)) else ""
    val p3 = if (cleanTotal.length > 8) cleanTotal.substring(8, cleanTotal.length.coerceAtMost(12)) else ""
    val p4 = if (cleanTotal.length > 12) cleanTotal.substring(12, cleanTotal.length.coerceAtMost(16)) else ""

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BankCardPartField(p4, { input ->
                val newTotal = p1 + p2 + p3 + input
                onValueChange(newTotal)
                if (input.length == 4) focusManager.clearFocus() 
            }, f4, { if (p4.isEmpty()) f3.requestFocus() }, isError)
            
            Text("-", color = MaterialTheme.colorScheme.outlineVariant)
            
            BankCardPartField(p3, { input ->
                val newTotal = p1 + p2 + input + p4
                onValueChange(newTotal)
                if (input.length == 4) f4.requestFocus()
            }, f3, { if (p3.isEmpty()) f2.requestFocus() }, isError)
            
            Text("-", color = MaterialTheme.colorScheme.outlineVariant)
            
            BankCardPartField(p2, { input ->
                val newTotal = p1 + input + p3 + p4
                onValueChange(newTotal)
                if (input.length == 4) f3.requestFocus()
            }, f2, { if (p2.isEmpty()) f1.requestFocus() }, isError)
            
            Text("-", color = MaterialTheme.colorScheme.outlineVariant)
            
            BankCardPartField(p1, { input ->
                // Handle Paste logic: if input is > 4, assume it's full 16 digits
                if (input.length > 4) {
                    onValueChange(input.take(16))
                } else {
                    val newTotal = input + p2 + p3 + p4
                    onValueChange(newTotal)
                    if (input.length == 4) f2.requestFocus()
                }
            }, f1, {}, isError)
        }
    }
}

@Composable
fun RowScope.BankCardPartField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onBackspaceAtStart: () -> Unit,
    isError: Boolean
) {
    OutlinedTextField(
        value = value.toPersianDigits(),
        onValueChange = { input ->
            val clean = input.toEnglishDigits().filter { it.isDigit() }
            // Allow more than 4 for the first field to support paste
            onValueChange(clean)
        },
        modifier = Modifier
            .weight(1f)
            .focusRequester(focusRequester)
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.Backspace && value.isEmpty()) {
                    onBackspaceAtStart()
                    true
                } else {
                    false
                }
            },
        textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.Bold),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        isError = isError,
        colors = defaultAppTextFieldColors()
    )
}

@Composable
fun ShebaInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "شماره شبا (IBAN)",
    isError: Boolean = false,
    errorMessage: String? = null
) {
    val digitsOnly = value.removePrefix("IR").toEnglishDigits().filter { it.isDigit() }.take(24)
    
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        OutlinedTextField(
            value = digitsOnly.toPersianDigits(),
            onValueChange = { input ->
                val english = input.toEnglishDigits().filter { it.isDigit() }.take(24)
                onValueChange("IR$english")
            },
            label = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = label,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                }
            },
            placeholder = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = "۲۴ رقم بعد از IR",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            isError = isError || errorMessage != null,
            supportingText = {
                if (errorMessage != null) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = LocalTextStyle.current.copy(
                textDirection = TextDirection.Ltr,
                textAlign = TextAlign.Left
            ),
            leadingIcon = {
                Text(
                    text = "IR",
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = defaultAppTextFieldColors()
        )
    }
}

@Composable
fun PhoneNumberInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "شماره تماس",
    placeholder: String = "مثال: ۰۹۱۲۳۴۵۶۷۸۹",
    isError: Boolean = false,
    errorMessage: String? = null
) {
    ModernNumericTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        modifier = Modifier.fillMaxWidth(),
        isPrice = false,
        isError = isError,
        errorMessage = errorMessage
    )
}
