package com.example.ui.screens

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.*

@Composable
fun PersianNumberInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPrice: Boolean = false,
    errorMessage: String? = null
) {
    val displayValue = value.toPersianDigits()
    
    Column(modifier = modifier) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = { newValue ->
                onValueChange(newValue.toEnglishDigits())
            },
            label = { Text(label) },
            placeholder = { Text(if (isPrice) "قیمت را وارد کنید" else "تعداد را وارد کنید") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = errorMessage != null,
            modifier = Modifier.fillMaxWidth()
        )
        if (errorMessage != null) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
