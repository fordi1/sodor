package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.ui.screens.defaultAppTextFieldColors
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.entity.Category
import com.example.data.entity.Product
import com.example.ui.viewmodel.InvoiceViewModel
import com.example.ui.screens.PriceInput
import com.example.utils.toEnglishDigits
import com.example.utils.toPersianDigits
import com.example.utils.removeCommas
import com.example.utils.formatPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: InvoiceViewModel) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val foundProducts by viewModel.searchedProducts.collectAsState()
    val allProducts by viewModel.products.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currency = settings?.currencyUnit ?: "تومان"

    var showAddProductDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var chosenCategoryFilter by remember { mutableStateOf<Category?>(null) }

    var scannedBarcodeForAction by remember { mutableStateOf("") }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var showProductDetailsDialog by remember { mutableStateOf(false) }
    var selectedDetailedProduct by remember { mutableStateOf<Product?>(null) }
    var showProductNotFoundDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // App Header Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "لیست کالاها و خدمات",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "تعریف اقلام فاکتور، دسته‌بندی و قیمت‌گذاری",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showAddProductDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("افزودن کالا", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Search Bar + Mock Barcode scanner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("نام کالا، کد یا بارکد...") },
                modifier = Modifier.weight(1f),
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
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            var isBarcodeScanningForSearch by remember { mutableStateOf(false) }

            if (isBarcodeScanningForSearch) {
                BarcodeScannerLauncher(
                    onBarcodeScanned = { barcode ->
                        isBarcodeScanningForSearch = false
                        val matchedProduct = allProducts.firstOrNull { p ->
                            com.example.utils.PersianDigitConverter.matchBarcodes(p.barcode, barcode)
                        }
                        if (matchedProduct != null) {
                            selectedDetailedProduct = matchedProduct
                            showProductDetailsDialog = true
                            viewModel.updateSearchQuery(barcode)
                            Toast.makeText(context, "کالای ${matchedProduct.name} شناسایی شد", Toast.LENGTH_SHORT).show()
                        } else {
                            scannedBarcodeForAction = barcode
                            showProductNotFoundDialog = true
                        }
                    },
                    onDismiss = {
                        isBarcodeScanningForSearch = false
                    }
                )
            }

            IconButton(
                onClick = {
                    isBarcodeScanningForSearch = true
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "بارکد کالا",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Category Row Tag Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { showAddCategoryDialog = true },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.CreateNewFolder,
                    contentDescription = "دسته جدید",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = chosenCategoryFilter == null,
                        onClick = { chosenCategoryFilter = null },
                        label = { Text("همه اقلام", fontWeight = FontWeight.SemiBold) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }

                items(categories) { cat ->
                    FilterChip(
                        selected = chosenCategoryFilter?.id == cat.id,
                        onClick = { chosenCategoryFilter = cat },
                        label = { Text(cat.name, fontWeight = FontWeight.SemiBold) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Process final filter List
        val filteredList = if (chosenCategoryFilter != null) {
            foundProducts.filter { it.categoryId == chosenCategoryFilter?.id }
        } else {
            foundProducts
        }

        if (allProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "هنوز کالا یا خدمتی ثبت نشده است",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "کالایی یافت نشد",
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
                items(filteredList) { prod ->
                    val catName = categories.firstOrNull { it.id == prod.categoryId }?.name ?: "متفرقه"
                    ProductCard(
                        product = prod,
                        categoryName = catName,
                        currency = currency,
                        onDelete = {
                            viewModel.deleteProduct(
                                product = prod,
                                onSuccess = {
                                    Toast.makeText(context, "کالا با موفقیت حذف شد.", Toast.LENGTH_SHORT).show()
                                },
                                onError = { errorMsg ->
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        onEdit = {
                            productToEdit = prod
                            showAddProductDialog = true
                        }
                    )
                }
            }
        }
    }

    // Modal Popup sheet: New Product
    if (showAddProductDialog) {
        var productNameInput by remember { mutableStateOf("") }
        var productUnitInput by remember { mutableStateOf("عدد") }
        var productPriceInput by remember { mutableStateOf("") }
        var productStockInput by remember { mutableStateOf("") }
        var productBarcode by remember { mutableStateOf(scannedBarcodeForAction) }
        var productCatId by remember { mutableStateOf<Long?>(null) }
        var expandCatDrop by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { 
                showAddProductDialog = false 
                scannedBarcodeForAction = ""
                productToEdit = null
            },
            title = {
                Text(
                    text = if (productToEdit != null) "ویرایش کالا" else "ثبت کالا / خدمات جدید",
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                LaunchedEffect(productToEdit) {
                    productToEdit?.let {
                        productNameInput = it.name
                        productUnitInput = it.unit
                        productPriceInput = it.defaultUnitPrice.toLong().toString()
                        productStockInput = it.stockQuantity?.toLong()?.toString() ?: ""
                        productBarcode = it.barcode ?: ""
                        productCatId = it.categoryId
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = productNameInput,
                        onValueChange = { productNameInput = it },
                        label = { Text("نام کالا یا خدمت") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = defaultAppTextFieldColors()
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = productUnitInput,
                            onValueChange = { productUnitInput = it },
                            label = { Text("واحد شمارش") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = defaultAppTextFieldColors()
                        )
                        OutlinedTextField(
                            value = productStockInput.toPersianDigits(),
                            onValueChange = { productStockInput = it.toEnglishDigits().filter { c -> c.isDigit() } },
                            label = { Text("موجودی اولیه") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = defaultAppTextFieldColors()
                        )
                    }

                    PriceInput(
                        value = productPriceInput,
                        onValueChange = { productPriceInput = it },
                        label = "قیمت واحد فروش"
                    )

                    var isBarcodeScanningForAdd by remember { mutableStateOf(false) }

                    if (isBarcodeScanningForAdd) {
                        BarcodeScannerLauncher(
                            onBarcodeScanned = { barcode ->
                                productBarcode = barcode
                                isBarcodeScanningForAdd = false
                                Toast.makeText(context, "بارکد ثبت شد", Toast.LENGTH_SHORT).show()
                            },
                            onDismiss = {
                                isBarcodeScanningForAdd = false
                            }
                        )
                    }

                    OutlinedTextField(
                        value = productBarcode,
                        onValueChange = { productBarcode = it },
                        label = { Text("بارکد (اختیاری)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { isBarcodeScanningForAdd = true }) {
                                Icon(Icons.Default.QrCodeScanner, null)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = defaultAppTextFieldColors()
                    )

                    val dropLabel = categories.firstOrNull { it.id == productCatId }?.name ?: "بدون دسته‌بندی"
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = dropLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("گروه") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandCatDrop = true },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        DropdownMenu(
                            expanded = expandCatDrop,
                            onDismissRequest = { expandCatDrop = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("بدون دسته‌بندی (متفرقه)") },
                                onClick = {
                                    productCatId = null
                                    expandCatDrop = false
                                }
                            )
                            categories.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.name) },
                                    onClick = {
                                        productCatId = c.id
                                        expandCatDrop = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountVal = productPriceInput.toDoubleOrNull()
                        val stockVal = if (productStockInput.isBlank()) null else productStockInput.toDoubleOrNull()
                        
                        // Validation logic
                        var isValid = true
                        if (productNameInput.isBlank()) {
                            Toast.makeText(context, "نام کالا الزامی است", Toast.LENGTH_SHORT).show()
                            isValid = false
                        } else if (amountVal == null || amountVal <= 0.0) {
                            Toast.makeText(context, "قیمت نامعتبر", Toast.LENGTH_SHORT).show()
                            isValid = false
                        }

                        if (isValid) {
                            if (productToEdit != null) {
                                viewModel.updateProduct(productToEdit!!.copy(
                                    categoryId = productCatId,
                                    name = productNameInput.trim(),
                                    unit = productUnitInput.trim(),
                                    defaultUnitPrice = amountVal!!,
                                    barcode = productBarcode.ifBlank { null },
                                    stockQuantity = stockVal
                                ))
                                Toast.makeText(context, "کالا با موفقیت ویرایش شد.", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.addProduct(
                                    categoryId = productCatId,
                                    name = productNameInput.trim(),
                                    unit = productUnitInput.trim(),
                                    price = amountVal!!,
                                    barcode = productBarcode.ifBlank { null },
                                    stockQuantity = stockVal
                                )
                                Toast.makeText(context, "کالا با موفقیت اضافه شد.", Toast.LENGTH_SHORT).show()
                            }
                            showAddProductDialog = false
                            scannedBarcodeForAction = ""
                            productToEdit = null
                        }
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(if (productToEdit != null) "ویرایش" else "ذخیره", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddProductDialog = false 
                    scannedBarcodeForAction = ""
                    productToEdit = null
                }) {
                    Text("انصراف", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Modal Popup sheet: New Category
    if (showAddCategoryDialog) {
        var catNameInput by remember { mutableStateOf("") }
        var catDescInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = {
                Text(
                    text = "ایجاد دسته‌بندی (گروه) جدید",
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = catNameInput,
                        onValueChange = { catNameInput = it },
                        label = { Text("نام گروه جدید") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = defaultAppTextFieldColors()
                    )
                    OutlinedTextField(
                        value = catDescInput,
                        onValueChange = { catDescInput = it },
                        label = { Text("توضیحات کوتاه (اختیاری)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = defaultAppTextFieldColors()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (catNameInput.isNotBlank()) {
                            viewModel.addCategory(
                                name = catNameInput.trim(),
                                description = catDescInput.trim().ifBlank { null }
                            )
                            showAddCategoryDialog = false
                        } else {
                            Toast.makeText(context, "وارد کردن نام گروه الزامی است", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("ثبت گروه", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("انصراف", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Product Details Dialog (Product Management)
    if (showProductDetailsDialog && selectedDetailedProduct != null) {
        val catName = categories.firstOrNull { it.id == selectedDetailedProduct?.categoryId }?.name ?: "متفرقه"
        AlertDialog(
            onDismissRequest = { 
                showProductDetailsDialog = false
                selectedDetailedProduct = null
            },
            title = {
                Text(
                    text = "جزئیات کالا / خدمت",
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selectedDetailedProduct?.name ?: "",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("دسته‌بندی (گروه):", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Text(catName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("قیمت واحد:", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${formatPrice(selectedDetailedProduct?.defaultUnitPrice ?: 0.0)} $currency".toPersianDigits(),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("واحد شمارش:", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Text(selectedDetailedProduct?.unit ?: "عدد", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (!selectedDetailedProduct?.barcode.isNullOrEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("بارکد دیجیتال:", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            Text((selectedDetailedProduct?.barcode ?: "").toPersianDigits(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showProductDetailsDialog = false
                        selectedDetailedProduct = null
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("بستن", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Product Not Found Dialog (With invitation to create)
    if (showProductNotFoundDialog) {
        AlertDialog(
            onDismissRequest = { 
                showProductNotFoundDialog = false
                scannedBarcodeForAction = ""
            },
            title = {
                Text(
                    text = "کالا یافت نشد",
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "کالایی با این بارکد یافت نشد. آیا میخواهید کالای جدید ثبت کنید؟",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showProductNotFoundDialog = false
                        // pre-fills using scannedBarcodeForAction and triggers add dialog
                        showAddProductDialog = true
                    },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("ثبت کالای جدید", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showProductNotFoundDialog = false
                        scannedBarcodeForAction = ""
                    }
                ) {
                    Text("انصراف", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun ProductCard(
    product: Product,
    categoryName: String,
    currency: String,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Category Pill Tag
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "قیمت: ${formatPrice(product.defaultUnitPrice)} $currency".toPersianDigits(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "واحد: ${product.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!product.barcode.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "بارکد: ${product.barcode}".toPersianDigits(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row {
                IconButton(
                    onClick = { onEdit() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "ویرایش کالا",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف کالا",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("تایید حذف", fontWeight = FontWeight.Bold) },
            text = { Text("آیا از حذف این کالا مطمئن هستید؟") },
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
