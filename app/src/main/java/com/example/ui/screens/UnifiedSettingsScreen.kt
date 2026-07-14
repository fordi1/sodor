package com.example.ui.screens

import android.content.Intent
import android.util.Log
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.entity.AppSettings
import com.example.data.entity.CompanyInfo
import com.example.ui.viewmodel.InvoiceViewModel
import com.example.utils.IranianValidationHelper
import com.example.utils.toEnglishDigits
import com.example.utils.toPersianDigits
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenDark
import android.content.Context
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedSettingsScreen(
    navController: NavController,
    viewModel: InvoiceViewModel,
    initialTab: Int = 0
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val savedCompanyInfo by viewModel.companyInfo.collectAsState()
    val savedSettings by viewModel.settings.collectAsState()

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    
    var tab0SaveTrigger by remember { mutableIntStateOf(0) }
    var tab1SaveTrigger by remember { mutableIntStateOf(0) }
    
    var tab0HasUnsavedChanges by remember { mutableStateOf(false) }
    var tab1HasUnsavedChanges by remember { mutableStateOf(false) }
    
    val hasUnsavedChanges = tab0HasUnsavedChanges || tab1HasUnsavedChanges
    
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var shouldExitAfterSave by remember { mutableStateOf(false) }

    val onSaveResult = { success: Boolean ->
        if (success) {
            if (shouldExitAfterSave) {
                navController.popBackStack()
            }
        } else {
            shouldExitAfterSave = false
        }
    }

    // Intercept system back gestures
    BackHandler(enabled = hasUnsavedChanges) {
        showUnsavedChangesDialog = true
    }

    if (showUnsavedChangesDialog) {
        val isDark = LocalIsDarkTheme.current
        val greenColor = if (isDark) SuccessGreenDark else SuccessGreen
        val contentColor = if (isDark) Color.Black else Color.White

        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "تغییرات ذخیره نشده",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Text(
                    text = "تغییرات ذخیره نشده‌اند. آیا می‌خواهید تغییرات ذخیره شوند؟",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            shouldExitAfterSave = true
                            if (tab0HasUnsavedChanges) tab0SaveTrigger++
                            if (tab1HasUnsavedChanges) tab1SaveTrigger++
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = greenColor,
                            contentColor = contentColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ذخیره و خروج", fontWeight = FontWeight.Bold)
                    }
                    
                    OutlinedButton(
                        onClick = {
                            showUnsavedChangesDialog = false
                            navController.popBackStack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("خروج بدون ذخیره")
                    }
                    
                    TextButton(
                        onClick = { showUnsavedChangesDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("انصراف")
                    }
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تنظیمات", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges) {
                            showUnsavedChangesDialog = true
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    val isDark = LocalIsDarkTheme.current
                    val greenColor = if (isDark) SuccessGreenDark else SuccessGreen
                    val contentColor = if (isDark) Color.Black else Color.White
                    
                    Button(
                        onClick = {
                            shouldExitAfterSave = false
                            if (selectedTab == 0) tab0SaveTrigger++ else tab1SaveTrigger++
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = greenColor,
                            contentColor = contentColor
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "ثبت و ذخیره",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "ثبت و ذخیره",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.PrimaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("تنظیمات برنامه", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Settings, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("هویت فروشگاه", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Storefront, null) }
                )
            }

            Box(modifier = Modifier.weight(1f).clipToBounds()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = if (selectedTab == 0) 0.dp else 4000.dp)
                        .alpha(if (selectedTab == 0) 1f else 0f)
                ) {
                    AppSettingsContent(
                        viewModel = viewModel,
                        saveTrigger = tab0SaveTrigger,
                        onUnsavedChangesStatusChanged = { tab0HasUnsavedChanges = it },
                        onSaveResult = onSaveResult
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = if (selectedTab == 1) 0.dp else 4000.dp)
                        .alpha(if (selectedTab == 1) 1f else 0f)
                ) {
                    StoreIdentityContent(
                        viewModel = viewModel,
                        saveTrigger = tab1SaveTrigger,
                        onUnsavedChangesStatusChanged = { tab1HasUnsavedChanges = it },
                        onSaveResult = onSaveResult
                    )
                }
            }
        }
    }
}

@Composable
fun StoreIdentityContent(
    viewModel: InvoiceViewModel,
    saveTrigger: Int,
    onUnsavedChangesStatusChanged: (Boolean) -> Unit,
    onSaveResult: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedCompanyInfo by viewModel.companyInfo.collectAsState()
    val savedSettings by viewModel.settings.collectAsState()

    // State
    var companyName by remember { mutableStateOf("") }
    var managerName by remember { mutableStateOf("") }
    var companyPhone by remember { mutableStateOf("") }
    var companyAddress by remember { mutableStateOf("") }
    var companyDescription by remember { mutableStateOf("") }
    var companyNationalId by remember { mutableStateOf("") }
    var companyEconomicCode by remember { mutableStateOf("") }
    var companyRegistrationNum by remember { mutableStateOf("") }

    var bankName by remember { mutableStateOf("") }
    var accountHolderName by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var shabaNumber by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }

    var logoPath by remember { mutableStateOf<String?>(null) }
    var stampPath by remember { mutableStateOf<String?>(null) }
    var signaturePath by remember { mutableStateOf<String?>(null) }

    var showInvoiceLogo by remember { mutableStateOf(true) }
    var showInvoiceBusinessStamp by remember { mutableStateOf(true) }
    var showInvoiceSignature by remember { mutableStateOf(true) }
    var showProformaLogo by remember { mutableStateOf(true) }
    var showProformaBusinessStamp by remember { mutableStateOf(true) }
    var showProformaSignature by remember { mutableStateOf(true) }

    var croppingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var croppingTarget by remember { mutableStateOf<String?>(null) }
    var activeBrandingDialog by remember { mutableStateOf<String?>(null) }

    val hasChanges = remember(
        companyName, managerName, companyPhone, companyAddress, companyDescription,
        companyNationalId, companyEconomicCode, companyRegistrationNum,
        bankName, accountHolderName, cardNumber, shabaNumber, accountNumber,
        logoPath, stampPath, signaturePath,
        showInvoiceLogo, showInvoiceBusinessStamp, showInvoiceSignature,
        showProformaLogo, showProformaBusinessStamp, showProformaSignature,
        savedCompanyInfo, savedSettings
    ) {
        val info = savedCompanyInfo ?: CompanyInfo()
        val s = savedSettings ?: AppSettings()
        
        companyName != info.companyName ||
        managerName != (info.managerName ?: "") ||
        companyPhone.toEnglishDigits().filter { it.isDigit() } != info.phoneNumber.toEnglishDigits().filter { it.isDigit() } ||
        companyAddress != (info.address ?: "") ||
        companyDescription != (info.description ?: "") ||
        companyNationalId.toEnglishDigits().filter { it.isDigit() } != (info.nationalId?.toEnglishDigits()?.filter { it.isDigit() } ?: "") ||
        companyEconomicCode.toEnglishDigits().filter { it.isDigit() } != (info.economicCode?.toEnglishDigits()?.filter { it.isDigit() } ?: "") ||
        companyRegistrationNum.toEnglishDigits().filter { it.isDigit() } != (info.registrationNumber?.toEnglishDigits()?.filter { it.isDigit() } ?: "") ||
        bankName != (info.bankName ?: "") ||
        accountHolderName != (info.accountHolderName ?: "") ||
        cardNumber.toEnglishDigits().filter { it.isDigit() } != (info.cardNumber?.toEnglishDigits()?.filter { it.isDigit() } ?: "") ||
        shabaNumber != (info.shabaNumber ?: "") ||
        accountNumber.toEnglishDigits().filter { it.isDigit() } != (info.accountNumber?.toEnglishDigits()?.filter { it.isDigit() } ?: "") ||
        logoPath != info.logoPath ||
        stampPath != info.stampPath ||
        signaturePath != info.signaturePath ||
        showInvoiceLogo != s.showInvoiceLogo ||
        showInvoiceBusinessStamp != s.showInvoiceBusinessStamp ||
        showInvoiceSignature != s.showInvoiceSignature ||
        showProformaLogo != s.showProformaLogo ||
        showProformaBusinessStamp != s.showProformaBusinessStamp ||
        showProformaSignature != s.showProformaSignature
    }

    LaunchedEffect(hasChanges) {
        onUnsavedChangesStatusChanged(hasChanges)
    }

    // Launchers
    val logoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            croppingBitmap = loadMutableBitmap(context, uri)
            croppingTarget = "LOGO"
        }
    }
    val stampPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            croppingBitmap = loadMutableBitmap(context, uri)
            croppingTarget = "STAMP"
        }
    }
    val signaturePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            croppingBitmap = loadMutableBitmap(context, uri)
            croppingTarget = "SIGNATURE"
        }
    }

    // Sync from VM
    LaunchedEffect(savedCompanyInfo) {
        savedCompanyInfo?.let {
            companyName = it.companyName
            managerName = it.managerName ?: ""
            companyPhone = it.phoneNumber
            companyAddress = it.address ?: ""
            companyDescription = it.description ?: ""
            companyNationalId = it.nationalId ?: ""
            companyEconomicCode = it.economicCode ?: ""
            companyRegistrationNum = it.registrationNumber ?: ""
            bankName = it.bankName ?: ""
            accountHolderName = it.accountHolderName ?: ""
            cardNumber = it.cardNumber ?: ""
            shabaNumber = it.shabaNumber ?: ""
            accountNumber = it.accountNumber ?: ""
            logoPath = it.logoPath
            stampPath = it.stampPath
            signaturePath = it.signaturePath
        }
    }

    LaunchedEffect(savedSettings) {
        savedSettings?.let {
            showInvoiceLogo = it.showInvoiceLogo
            showInvoiceBusinessStamp = it.showInvoiceBusinessStamp
            showInvoiceSignature = it.showInvoiceSignature
            showProformaLogo = it.showProformaLogo
            showProformaBusinessStamp = it.showProformaBusinessStamp
            showProformaSignature = it.showProformaSignature
        }
    }

    fun doSaveAll() {
        if (companyName.trim().isEmpty()) {
            Toast.makeText(context, "نام فروشگاه نمی‌تواند خالی باشد.", Toast.LENGTH_SHORT).show()
            onSaveResult(false)
            return
        }
        val cleanPhone = companyPhone.toEnglishDigits().trim()
        if (cleanPhone.isEmpty()) {
            Toast.makeText(context, "شماره تماس فروشگاه نمی‌تواند خالی باشد.", Toast.LENGTH_SHORT).show()
            onSaveResult(false)
            return
        }
        if (!IranianValidationHelper.isValidPhoneNumber(cleanPhone)) {
            Toast.makeText(context, "شماره تماس فروشگاه معتبر نیست (باید با ۰۹ شروع شود و ۱۱ رقم باشد)", Toast.LENGTH_SHORT).show()
            onSaveResult(false)
            return
        }
        if (cardNumber.isNotBlank() && !IranianValidationHelper.isValidCardNumber(cardNumber)) {
            Toast.makeText(context, "شماره کارت بانکی معتبر نیست (باید ۱۶ رقم باشد)", Toast.LENGTH_SHORT).show()
            onSaveResult(false)
            return
        }
        if (shabaNumber.isNotBlank() && !IranianValidationHelper.isValidSheba(shabaNumber)) {
            Toast.makeText(context, "شماره شبا معتبر نیست (باید با IR شروع شود و ۲۶ کاراکتر داشته باشد)", Toast.LENGTH_SHORT).show()
            onSaveResult(false)
            return
        }

        // Save Info
        val currentInfo = savedCompanyInfo ?: CompanyInfo()
        viewModel.saveCompanyInfo(
            currentInfo.copy(
                companyName = companyName,
                managerName = managerName,
                phoneNumber = companyPhone.toEnglishDigits().filter { it.isDigit() },
                address = companyAddress,
                description = companyDescription,
                nationalId = companyNationalId.toEnglishDigits().filter { it.isDigit() },
                economicCode = companyEconomicCode.toEnglishDigits().filter { it.isDigit() },
                registrationNumber = companyRegistrationNum.toEnglishDigits().filter { it.isDigit() },
                bankName = bankName,
                accountHolderName = accountHolderName,
                cardNumber = cardNumber.toEnglishDigits().filter { it.isDigit() },
                shabaNumber = shabaNumber,
                accountNumber = accountNumber.toEnglishDigits().filter { it.isDigit() },
                logoPath = logoPath,
                stampPath = stampPath,
                signaturePath = signaturePath,
                updatedAt = System.currentTimeMillis()
            )
        )
        
        // Save Branding Settings
        val currentSettings = savedSettings ?: AppSettings()
        viewModel.saveSettings(
            currentSettings.copy(
                showInvoiceLogo = showInvoiceLogo,
                showInvoiceBusinessStamp = showInvoiceBusinessStamp,
                showInvoiceSignature = showInvoiceSignature,
                showProformaLogo = showProformaLogo,
                showProformaBusinessStamp = showProformaBusinessStamp,
                showProformaSignature = showProformaSignature,
                updatedAt = System.currentTimeMillis()
            )
        )
        Toast.makeText(context, "اطلاعات با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
        onSaveResult(true)
    }

    LaunchedEffect(saveTrigger) {
        if (saveTrigger > 0) {
            doSaveAll()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            SettingsSectionContainer(title = "مشخصات عمومی فروشگاه", icon = Icons.Default.Business) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("نام فروشگاه / کسب و کار") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = managerName,
                        onValueChange = { managerName = it },
                        label = { Text("نام مدیر / صاحب امتیاز") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    PhoneNumberInput(
                        value = companyPhone,
                        onValueChange = { companyPhone = it }
                    )
                    OutlinedTextField(
                        value = companyAddress,
                        onValueChange = { companyAddress = it },
                        label = { Text("آدرس کامل") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = companyDescription,
                        onValueChange = { companyDescription = it },
                        label = { Text("توضیحات فروشگاه (اختیاری)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModernNumericTextField(
                            value = companyNationalId,
                            onValueChange = { companyNationalId = it },
                            label = "شناسه ملی",
                            modifier = Modifier.weight(1f)
                        )
                        ModernNumericTextField(
                            value = companyEconomicCode,
                            onValueChange = { companyEconomicCode = it },
                            label = "کد اقتصادی",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ModernNumericTextField(
                        value = companyRegistrationNum,
                        onValueChange = { companyRegistrationNum = it },
                        label = "شماره ثبت",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            SettingsSectionContainer(title = "اطلاعات بانکی", icon = Icons.Default.AccountBalance) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("نام بانک") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = accountHolderName,
                        onValueChange = { accountHolderName = it },
                        label = { Text("نام صاحب حساب") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    BankCardInput(
                        value = cardNumber,
                        onValueChange = { cardNumber = it }
                    )
                    ShebaInput(
                        value = shabaNumber,
                        onValueChange = { shabaNumber = it }
                    )
                    ModernNumericTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        label = "شماره حساب",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            SettingsSectionContainer(title = "هویت بصری (لوگو، مهر، امضا)", icon = Icons.Default.Verified) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "هویت بصری کسب‌وکار خود را در این بخش تنظیم کنید. لوگو، مهر و امضا در فایل‌های خروجی شما بر اساس این بخش درج خواهند شد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    BrandingCard(
                        title = "لوگوی فروشگاه",
                        icon = Icons.Default.Storefront,
                        path = logoPath,
                        onUpload = { logoPickerLauncher.launch("image/*") },
                        onDesign = { activeBrandingDialog = "LOGO_PRESETS" },
                        onDelete = { logoPath = null },
                        invoiceChecked = showInvoiceLogo,
                        onInvoiceChange = { showInvoiceLogo = it },
                        proformaChecked = showProformaLogo,
                        onProformaChange = { showProformaLogo = it }
                    )
                    
                    BrandingCard(
                        title = "مهر کسب و کار",
                        icon = Icons.Default.Verified,
                        path = stampPath,
                        onUpload = { stampPickerLauncher.launch("image/*") },
                        onDesign = { activeBrandingDialog = "STAMP_GEN" },
                        onDelete = { stampPath = null },
                        invoiceChecked = showInvoiceBusinessStamp,
                        onInvoiceChange = { showInvoiceBusinessStamp = it },
                        proformaChecked = showProformaBusinessStamp,
                        onProformaChange = { showProformaBusinessStamp = it }
                    )
                    
                    BrandingCard(
                        title = "امضای مدیر",
                        icon = Icons.Default.HistoryEdu,
                        path = signaturePath,
                        onUpload = { signaturePickerLauncher.launch("image/*") },
                        onDesign = { activeBrandingDialog = "SIG_PAD" },
                        onDelete = { signaturePath = null },
                        invoiceChecked = showInvoiceSignature,
                        onInvoiceChange = { showInvoiceSignature = it },
                        proformaChecked = showProformaSignature,
                        onProformaChange = { showProformaSignature = it }
                    )
                }
            }
        }
    }

    if (croppingBitmap != null) {
        ImageCropDialog(
            bitmap = croppingBitmap!!,
            onDismiss = { croppingBitmap = null },
            onCropSuccess = { path ->
                when (croppingTarget) {
                    "LOGO" -> logoPath = path
                    "STAMP" -> stampPath = path
                    "SIGNATURE" -> signaturePath = path
                }
                croppingBitmap = null
            }
        )
    }

    when (activeBrandingDialog) {
        "LOGO_PRESETS" -> LogoPresetDialog(onDismiss = { activeBrandingDialog = null }, onSelect = { path -> logoPath = path; activeBrandingDialog = null })
        "STAMP_GEN" -> SimpleStampGenDialog(onDismiss = { activeBrandingDialog = null }, onSave = { path -> stampPath = path; activeBrandingDialog = null })
        "SIG_PAD" -> SignaturePadDialog(onDismiss = { activeBrandingDialog = null }, onSave = { path -> signaturePath = path; activeBrandingDialog = null })
    }
}

@Composable
fun AppSettingsContent(
    viewModel: InvoiceViewModel,
    saveTrigger: Int,
    onUnsavedChangesStatusChanged: (Boolean) -> Unit,
    onSaveResult: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedSettings by viewModel.settings.collectAsState()

    var themeModeState by remember { mutableStateOf("LIGHT") }
    var currentCurrency by remember { mutableStateOf("تومان") }
    var invoicePref by remember { mutableStateOf("") }
    var proformaPref by remember { mutableStateOf("") }
    var invoiceStartNum by remember { mutableStateOf("1") }
    var proformaStartNum by remember { mutableStateOf("1") }
    var defaultTaxRate by remember { mutableStateOf("0") }
    var sendSmsOnSave by remember { mutableStateOf(false) }

    val hasChanges = remember(
        invoicePref, proformaPref, invoiceStartNum, proformaStartNum, sendSmsOnSave, savedSettings
    ) {
        val s = savedSettings ?: AppSettings()
        invoicePref != s.invoicePrefix ||
        proformaPref != s.proformaPrefix ||
        invoiceStartNum != s.invoiceStartNumber.toString() ||
        proformaStartNum != s.proformaStartNumber.toString() ||
        sendSmsOnSave != s.sendSmsOnSave
    }

    LaunchedEffect(hasChanges) {
        onUnsavedChangesStatusChanged(hasChanges)
    }

    LaunchedEffect(savedSettings) {
        savedSettings?.let {
            themeModeState = it.themeMode
            currentCurrency = it.currencyUnit
            invoicePref = it.invoicePrefix
            proformaPref = it.proformaPrefix
            invoiceStartNum = it.invoiceStartNumber.toString()
            proformaStartNum = it.proformaStartNumber.toString()
            defaultTaxRate = it.defaultTaxRate.toString()
            sendSmsOnSave = it.sendSmsOnSave
        }
    }

    fun doSettingsSave() {
        val invStartNum = invoiceStartNum.toIntOrNull()
        if (invStartNum == null || invStartNum < 1) {
            Toast.makeText(context, "شروع شماره فاکتور باید یک عدد مثبت باشد.", Toast.LENGTH_SHORT).show()
            onSaveResult(false)
            return
        }
        val profStartNum = proformaStartNum.toIntOrNull()
        if (profStartNum == null || profStartNum < 1) {
            Toast.makeText(context, "شروع پیش‌فاکتور باید یک عدد مثبت باشد.", Toast.LENGTH_SHORT).show()
            onSaveResult(false)
            return
        }

        val current = savedSettings ?: AppSettings()
        viewModel.saveSettings(
            current.copy(
                invoicePrefix = invoicePref,
                proformaPrefix = proformaPref,
                invoiceStartNumber = invStartNum,
                proformaStartNumber = profStartNum,
                defaultTaxRate = 0.0,
                sendSmsOnSave = sendSmsOnSave,
                updatedAt = System.currentTimeMillis()
            )
        )
        Toast.makeText(context, "تنظیمات ذخیره شد", Toast.LENGTH_SHORT).show()
        onSaveResult(true)
    }

    LaunchedEffect(saveTrigger) {
        if (saveTrigger > 0) {
            doSettingsSave()
        }
    }

    fun immediateSaveTheme(mode: String) {
        themeModeState = mode
        val current = savedSettings ?: AppSettings()
        viewModel.saveSettings(current.copy(themeMode = mode))
    }

    fun immediateSaveCurrency(curr: String) {
        currentCurrency = curr
        val current = savedSettings ?: AppSettings()
        viewModel.saveSettings(current.copy(currencyUnit = curr))
    }

    // Manual Backup launchers & functions
    var showRestoreConfirmDialog by remember { mutableStateOf<Uri?>(null) }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) showRestoreConfirmDialog = uri
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            SettingsSectionContainer(title = "تنظیمات عمومی و ظاهر", icon = Icons.Default.Palette) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("تم برنامه:", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeChip("روشن", themeModeState == "LIGHT") { immediateSaveTheme("LIGHT") }
                        ThemeChip("تاریک", themeModeState == "DARK") { immediateSaveTheme("DARK") }
                        ThemeChip("سیستم", themeModeState == "SYSTEM") { immediateSaveTheme("SYSTEM") }
                    }
                    
                    HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                    Text("واحد پولی:", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeChip("تومان", currentCurrency == "تومان") { immediateSaveCurrency("تومان") }
                        ThemeChip("ریال", currentCurrency == "ریال") { immediateSaveCurrency("ریال") }
                    }
                }
            }
        }

        item {
            SettingsSectionContainer(title = "قوانین و پیش‌فرض‌های فاکتور", icon = Icons.Default.HistoryEdu) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = invoicePref,
                            onValueChange = { invoicePref = it },
                            label = { Text("پیشوند فاکتور") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        OutlinedTextField(
                            value = proformaPref,
                            onValueChange = { proformaPref = it },
                            label = { Text("پیشوند پیش‌فاکتور") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModernNumericTextField(
                            value = invoiceStartNum,
                            onValueChange = { invoiceStartNum = it },
                            label = "شروع شماره فاکتور از",
                            modifier = Modifier.weight(1f)
                        )
                        ModernNumericTextField(
                            value = proformaStartNum,
                            onValueChange = { proformaStartNum = it },
                            label = "شروع پیش‌فاکتور از",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            SettingsSectionContainer(title = "پشتیبانگیری و بازیابی دستی", icon = Icons.Default.Backup) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { 
                            val uri = viewModel.createBackup(context)
                            if (uri != null) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/zip"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "ارسال فایل پشتیبان"))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تهیه فایل پشتیبان")
                    }
                    Button(
                        onClick = { restoreLauncher.launch("application/zip") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("بازیابی از فایل پشتیبان")
                    }
                }
            }
        }
    }

    if (showRestoreConfirmDialog != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = null },
            title = { Text("تأیید بازیابی اطلاعات") },
            text = { Text("با انجام این کار، تمام اطلاعات فعلی برنامه پاک شده و با اطلاعات فایل پشتیبان جایگزین می‌شود. آیا مطمئن هستید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreBackup(context, showRestoreConfirmDialog!!)
                        showRestoreConfirmDialog = null
                        Toast.makeText(context, "بازیابی با موفقیت انجام شد. برنامه مجدداً اجرا می‌شود.", Toast.LENGTH_LONG).show()
                        java.lang.System.exit(0)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("بله، بازیابی شود")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}
