package com.example.utils

import android.content.Context
import android.net.Uri
import android.print.PrintAttributes
import android.util.Base64
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import com.example.data.dao.InvoiceWithRelations
import com.example.data.entity.AppSettings
import com.example.data.entity.CompanyInfo
import com.example.data.entity.Attachment
import com.example.utils.formatCardNumber
import com.example.utils.formatShebaNumber
import com.example.utils.IranianValidationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.StringBuilder
import kotlin.coroutines.resume

object PdfInvoiceGenerator {

    // File to Base64 encoder for seamless sandboxed offline resource delivery inside WebViews
    private fun fileToBase64(path: String?): String? {
        if (path.isNullOrEmpty()) return null
        return try {
            val file = File(path)
            if (!file.exists()) return null
            val bytes = file.readBytes()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/png;base64,$base64"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun generateInvoicePdf(
        context: Context,
        invoiceData: InvoiceWithRelations,
        companyInfo: CompanyInfo,
        settings: AppSettings,
        previousBalance: Double,
        includeAttachments: Boolean = false,
        overrideAttachments: List<Attachment>? = null
    ): Uri? = withContext(Dispatchers.Main) {
        val attachments = overrideAttachments ?: invoiceData.attachments
        
        val isProforma = invoiceData.invoice.invoiceType == "PROFORMA"
        val showLogo = if (isProforma) settings.showProformaLogo else settings.showInvoiceLogo
        val showBusinessStamp = if (isProforma) settings.showProformaBusinessStamp else settings.showInvoiceBusinessStamp
        val showPersonalStamp = if (isProforma) settings.showProformaPersonalStamp else settings.showInvoicePersonalStamp
        val showSignature = if (isProforma) settings.showProformaSignature else settings.showInvoiceSignature

        val currency = settings.currencyUnit.ifEmpty { "ریال" }

        // Secure Base64 Embeds
        val logoBase64 = if (showLogo) fileToBase64(companyInfo.logoPath) else null
        val stampBase64 = if (showBusinessStamp) fileToBase64(companyInfo.stampPath) else null
        val personalStampBase64 = if (showPersonalStamp) fileToBase64(companyInfo.personalStampPath) else null
        val signatureBase64 = if (showSignature) fileToBase64(companyInfo.signaturePath) else null

        // Persian Calendars and Date Formatting
        val issueDateStr = JalaliDateFormatter.format(invoiceData.invoice.issueDate)
        val dueDateStr = if (invoiceData.invoice.dueDate != null) JalaliDateFormatter.format(invoiceData.invoice.dueDate) else "نقدی"

        var invoiceDesc = ""
        var invoiceTerms = ""
        var invoiceDelivery = ""
        var discountType = "AMOUNT"
        var discountPercentage = "0"
        try {
            val json = org.json.JSONObject(invoiceData.invoice.notes ?: "")
            invoiceDesc = json.optString("desc", "")
            invoiceTerms = json.optString("terms", "")
            invoiceDelivery = json.optString("delivery", "")
            discountType = json.optString("discountType", "AMOUNT")
            discountPercentage = json.optString("discountPercentage", "0")
        } catch (e: Exception) {
            invoiceDesc = invoiceData.invoice.notes ?: ""
        }

        // Calculate Totals block
        val subtotal = invoiceData.items.sumOf { (it.unitPrice * it.quantity) - it.discountAmount }
        val taxValue = (invoiceData.invoice.taxRate / 100.0) * (subtotal - invoiceData.invoice.discountAmount)
        val discount = invoiceData.invoice.discountAmount
        val extraCharges = invoiceData.invoice.shippingFee // Correct property name
        val totalWithTaxesDiscounts = subtotal + taxValue + extraCharges - discount
        val payableNet = totalWithTaxesDiscounts + previousBalance
        val paidAmount = invoiceData.calculatePaid()
        val remainingAmount = Math.max(0.0, payableNet - paidAmount)

        // Financial Spelling Words
        val wordsValue = NumberToWordsConverter.convert(payableNet) + " " + currency

        val hasRowDiscount = invoiceData.items.any { it.discountAmount > 0 }

        // Build HTML payload
        val itemRowsHtml = invoiceData.items.mapIndexed { idx, it ->
            val rowNum = PersianDigitConverter.toPersian((idx + 1).toString())
            val name = it.productName
            val qty = PersianDigitConverter.toPersian(if (it.quantity % 1.0 == 0.0) it.quantity.toInt().toString() else it.quantity.toString())
            val unit = it.unit
            val unitPrice = PersianDigitConverter.toPersian(formatPrice(it.unitPrice))
            val rowDiscount = PersianDigitConverter.toPersian(formatPrice(it.discountAmount))
            val rowTotal = PersianDigitConverter.toPersian(formatPrice((it.unitPrice * it.quantity) - it.discountAmount))
            val rowBg = if (idx % 2 == 1) "background-color: #f8fafc;" else "background-color: #ffffff;"
            val discountCol = if (hasRowDiscount) "<td style=\"text-align: left; padding: 7px 10px; border: 1px solid #e2e8f0; font-size: 10px; color: #ef4444; font-weight: bold;\">$rowDiscount</td>" else ""
            """
            <tr style="$rowBg">
                <td style="text-align: center; padding: 7px 6px; border: 1px solid #e2e8f0; font-size: 10px; color: #475569;">$rowNum</td>
                <td style="text-align: right; padding: 7px 10px; border: 1px solid #e2e8f0; font-weight: bold; color: #011627; font-size: 10px;">$name</td>
                <td style="text-align: center; padding: 7px 6px; border: 1px solid #e2e8f0; font-size: 10px; color: #0f172a; font-weight: bold;">$qty</td>
                <td style="text-align: center; padding: 7px 6px; border: 1px solid #e2e8f0; font-size: 10px; color: #475569;">$unit</td>
                <td style="text-align: left; padding: 7px 10px; border: 1px solid #e2e8f0; font-size: 10px; color: #0f172a; font-weight: bold;">$unitPrice</td>
                $discountCol
                <td style="text-align: left; padding: 7px 10px; border: 1px solid #e2e8f0; font-size: 10px; color: #1e3a8a; font-weight: bold;">$rowTotal</td>
            </tr>
            """.trimIndent()
        }.joinToString("\n")

        val subtotalP = PersianDigitConverter.toPersian(formatPrice(subtotal))
        val taxRateP = PersianDigitConverter.toPersian(invoiceData.invoice.taxRate.toString())
        val taxValueP = PersianDigitConverter.toPersian(formatPrice(taxValue))
        val discountP = PersianDigitConverter.toPersian(formatPrice(discount))
        val extraChargesP = PersianDigitConverter.toPersian(formatPrice(extraCharges))
        val previousBalanceP = PersianDigitConverter.toPersian(formatPrice(previousBalance))
        val payableNetP = PersianDigitConverter.toPersian(formatPrice(payableNet))
        val paidAmountP = PersianDigitConverter.toPersian(formatPrice(paidAmount))
        val remainingAmountP = PersianDigitConverter.toPersian(formatPrice(remainingAmount))
        val statusTextP = when {
            paidAmount >= payableNet - 0.5 -> "تسویه شده"
            paidAmount > 0 -> "پرداخت جزئی"
            else -> "پرداخت نشده"
        }

        val title = if (isProforma) "پیش فاکتور فروش رسمی" else "فاکتور فروش رسمی"
        val docNumberP = PersianDigitConverter.toPersian(invoiceData.invoice.invoiceNumber)

        val managerNameStr = companyInfo.managerName?.ifEmpty { "مدیریت رسمی" } ?: "مدیریت رسمی"
        val shopNameStr = companyInfo.companyName.ifEmpty { "فروشگاه دفتری" }
        val shopPhoneStr = companyInfo.phoneNumber.ifEmpty { "----" }
        val shopAddressStr = companyInfo.address?.ifEmpty { "نشانی ثبت نشده" } ?: "نشانی ثبت نشده"

        val bankNameStr = companyInfo.bankName?.ifEmpty { "بانک ملی" } ?: "بانک ملی"
        val cardNumberStr = companyInfo.cardNumber?.ifEmpty { "---" } ?: "---"
        val shabaNumberStr = companyInfo.shabaNumber?.ifEmpty { "---" } ?: "---"

        val htmlString = """
        <!DOCTYPE html>
        <html dir="rtl" lang="fa">
        <head>
        <meta charset="UTF-8">
        <title>$title</title>
        <style>
        @font-face {
            font-family: 'Vazirmatn';
            src: url('file:///android_asset/fonts/vazirmatn_regular.ttf') format('truetype');
            font-weight: normal;
            font-style: normal;
        }
        @font-face {
            font-family: 'Vazirmatn';
            src: url('file:///android_asset/fonts/vazirmatn_bold.ttf') format('truetype');
            font-weight: bold;
            font-style: normal;
        }
        * {
            box-sizing: border-box;
        }
        body {
            font-family: 'Vazirmatn', Tahoma, sans-serif;
            direction: rtl;
            text-align: right;
            margin: 0;
            padding: 10px;
            background-color: #ffffff;
            color: #1e293b;
            font-size: 11px;
            line-height: 1.5;
        }
        .invoice-card {
            width: 100%;
            border: 2px solid #1e3a8a;
            border-radius: 12px;
            padding: 20px;
            background-color: #ffffff;
        }
        </style>
        </head>
        <body>
        <div class="invoice-card">
            <!-- HEADER BLOCK -->
            <table style="width: 100%; border-collapse: collapse; margin-bottom: 20px;">
                <tr>
                    <!-- RIGHT: LOGO AND store details -->
                    <td style="width: 35%; vertical-align: middle; text-align: right;">
                        ${if (logoBase64 != null) """
                            <div style="margin-bottom: 8px;">
                                <img src="$logoBase64" style="max-height: 60px; max-width: 130px; object-fit: contain; border-radius: 4px;" alt="Logo">
                            </div>
                        """.trimIndent() else ""}
                        <div style="font-size: 15px; font-weight: bold; color: #1e3a8a; line-height: 1.2;">$shopNameStr</div>
                        <div style="font-size: 9.5px; color: #64748b; margin-top: 3px;">صاحب امتیاز: $managerNameStr</div>
                    </td>
                    
                    <!-- CENTER: TITLE -->
                    <td style="width: 30%; vertical-align: middle; text-align: center;">
                        <div style="display: inline-block; padding: 10px 20px; border: 2px double #1e3a8a; background-color: #f1f5f9; border-radius: 6px;">
                            <h1 style="margin: 0; font-size: 17px; color: #1e3a8a; font-weight: bold;">$title</h1>
                        </div>
                    </td>
                    
                    <!-- LEFT: INVOICE METADATA -->
                    <td style="width: 35%; vertical-align: middle; text-align: left;">
                        <table style="width: 170px; border-collapse: collapse; margin-right: auto; border: 1px solid #cbd5e1; border-radius: 6px; overflow: hidden; font-size: 9.5px;">
                            <tr style="border-bottom: 1px solid #cbd5e1;">
                                <td style="padding: 5px 8px; background-color: #f8fafc; font-weight: bold; color: #334155; width: 45%;">شماره سند:</td>
                                <td style="padding: 5px 8px; text-align: left; font-weight: bold; color: #1e3a8a; width: 55%;">$docNumberP</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #cbd5e1;">
                                <td style="padding: 5px 8px; background-color: #f8fafc; font-weight: bold; color: #334155;">تاریخ صدور:</td>
                                <td style="padding: 5px 8px; text-align: left; color: #0f172a;">$issueDateStr</td>
                            </tr>
                            <tr>
                                <td style="padding: 5px 8px; background-color: #f8fafc; font-weight: bold; color: #334155;">نوع تسویه:</td>
                                <td style="padding: 5px 8px; text-align: left; color: #0f172a;">$dueDateStr</td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>

            <!-- SECTION: BUYER & SELLER DETAILS -->
            <table style="width: 100%; border-collapse: collapse; margin-bottom: 12px; border: 1px solid #1e3a8a; border-radius: 8px; overflow: hidden;">
                <tr style="background-color: #f1f5f9;">
                    <td style="width: 50%; font-weight: bold; padding: 4px 10px; font-size: 10px; border-bottom: 1px solid #1e3a8a; color: #1e3a8a;">اطلاعات فروشنده</td>
                    <td style="width: 50%; font-weight: bold; padding: 4px 10px; font-size: 10px; border-bottom: 1px solid #1e3a8a; color: #1e3a8a; border-right: 1px solid #1e3a8a;">اطلاعات خریدار</td>
                </tr>
                <tr>
                    <td style="padding: 6px 10px; vertical-align: top; font-size: 9px; line-height: 1.4; color: #334155;">
                        <div style="font-weight: bold; margin-bottom: 2px;">$shopNameStr</div>
                        ${if (!companyInfo.managerName.isNullOrBlank()) "<div>مدیریت: ${companyInfo.managerName}</div>" else ""}
                        <div>تلفن: ${PersianDigitConverter.toPersian(shopPhoneStr)}</div>
                        <div>نشانی: $shopAddressStr</div>
                        ${if (!companyInfo.economicCode.isNullOrBlank()) "<div>کد اقتصادی: ${PersianDigitConverter.toPersian(companyInfo.economicCode)}</div>" else ""}
                        ${if (!companyInfo.nationalId.isNullOrBlank()) "<div>شناسه ملی: ${PersianDigitConverter.toPersian(companyInfo.nationalId)}</div>" else ""}
                        ${if (!companyInfo.registrationNumber.isNullOrBlank()) "<div>شماره ثبت: ${PersianDigitConverter.toPersian(companyInfo.registrationNumber)}</div>" else ""}
                    </td>
                    <td style="padding: 6px 10px; vertical-align: top; font-size: 9px; line-height: 1.4; color: #334155; border-right: 1px solid #cbd5e1;">
                        <div style="font-weight: bold; margin-bottom: 2px;">${invoiceData.customer.fullName.ifEmpty { "مشتری عمومی" }}</div>
                        <div>کد: ${PersianDigitConverter.toPersian(invoiceData.customer.id.toString())}</div>
                        <div>تلفن: ${PersianDigitConverter.toPersian(invoiceData.customer.phoneNumber.ifEmpty { "----" })}</div>
                        <div>نشانی: ${invoiceData.customer.address.orEmpty().ifEmpty { "ثبت نشده" }}</div>
                        ${if (!invoiceData.customer.economicCode.isNullOrBlank()) "<div>کد اقتصادی: ${PersianDigitConverter.toPersian(invoiceData.customer.economicCode)}</div>" else ""}
                        ${if (!invoiceData.customer.nationalId.isNullOrBlank()) "<div>شناسه ملی: ${PersianDigitConverter.toPersian(invoiceData.customer.nationalId)}</div>" else ""}
                    </td>
                </tr>
            </table>

            <!-- SECTION 3: ITEMS DETAILS -->
            <table style="width: 100%; border-collapse: collapse; margin-bottom: 10px; border: 1px solid #cbd5e1; border-radius: 6px; overflow: hidden;">
                <thead>
                    <tr style="background-color: #1e3a8a; color: #ffffff;">
                        <th style="width: 5%; padding: 6px 4px; font-size: 9.5px; font-weight: bold; text-align: center; border: 1px solid #cbd5e1;">ردیف</th>
                        <th style="width: ${if (hasRowDiscount) "37%" else "45%"}; padding: 6px 8px; font-size: 9.5px; font-weight: bold; text-align: right; border: 1px solid #cbd5e1;">شرح کالا یا خدمات ارائه‌شده</th>
                        <th style="width: 10%; padding: 6px 4px; font-size: 9.5px; font-weight: bold; text-align: center; border: 1px solid #cbd5e1;">تعداد</th>
                        <th style="width: 10%; padding: 6px 4px; font-size: 9.5px; font-weight: bold; text-align: center; border: 1px solid #cbd5e1;">واحد</th>
                        <th style="width: 14%; padding: 6px 8px; font-size: 9.5px; font-weight: bold; text-align: left; border: 1px solid #cbd5e1;">قیمت واحد ($currency)</th>
                        ${if (hasRowDiscount) "<th style=\"width: 10%; padding: 6px 8px; font-size: 9.5px; font-weight: bold; text-align: left; border: 1px solid #cbd5e1;\">تخفیف ($currency)</th>" else ""}
                        <th style="width: 14%; padding: 6px 8px; font-size: 9.5px; font-weight: bold; text-align: left; border: 1px solid #cbd5e1;">جمع کل ($currency)</th>
                    </tr>
                </thead>
                <tbody>
                    $itemRowsHtml
                </tbody>
            </table>

            <!-- GENERAL DESCRIPTION AND NOTES -->
            ${if (invoiceDesc.isNotBlank() || invoiceTerms.isNotBlank() || invoiceDelivery.isNotBlank()) """
            <div style="margin-bottom: 15px;">
                ${if (invoiceDesc.isNotBlank()) """
                <div style="font-size: 10px; color: #334155; margin-bottom: 5px;">
                    <strong>شرح عمومی فاکتور:</strong> $invoiceDesc
                </div>
                """.trimIndent() else ""}
                ${if (invoiceTerms.isNotBlank()) """
                <div style="font-size: 10px; color: #334155; margin-bottom: 5px;">
                    <strong>یادداشت و شرایط:</strong> $invoiceTerms
                </div>
                """.trimIndent() else ""}
                ${if (invoiceDelivery.isNotBlank()) """
                <div style="font-size: 10px; color: #334155;">
                    <strong>شرایط ارسال و تحویل:</strong> $invoiceDelivery
                </div>
                """.trimIndent() else ""}
            </div>
            """.trimIndent() else ""}

            <!-- CO WORDS SPELLING BLOCK -->
            <div style="background-color: #f0f9ff; border: 1px dashed #1e3a8a; border-radius: 6px; padding: 7px 12px; font-weight: bold; font-size: 10px; color: #1e3a8a; margin-bottom: 12px;">
                مبلغ کل قابل پرداخت به حروف: $wordsValue
            </div>

            <!-- PAYMENT INFO & STYLED FINANCIAL STATEMENT -->
            <table style="width: 100%; border-collapse: collapse; margin-bottom: 15px;">
                <tr>
                    <!-- LEFT: STORE BANK CARD DETAILS -->
                    <td style="width: 50%; padding-left: 8px; vertical-align: top;">
                        <table style="width: 100%; border-collapse: collapse; border: 1px solid #cbd5e1; border-radius: 8px; overflow: hidden; background-color: #f8fafc;">
                            <tr>
                                <td style="background-color: #f1f5f9; border-bottom: 1px solid #cbd5e1; padding: 6px 12px; font-weight: bold; font-size: 10px; color: #1e3a8a;">
                                    اطلاعات حساب بانکی فروشگاه جهت واریز وجه
                                </td>
                            </tr>
                            <tr>
                                <td style="padding: 10px 12px; line-height: 1.8; font-size: 9.5px; color: #334155;">
                                    <div style="margin-bottom: 4px;"><strong>بانک عامل:</strong> $bankNameStr</div>
                                    <div style="margin-bottom: 4px;"><strong>شماره کارت:</strong> <span style="font-family: monospace; font-size: 10.5px; letter-spacing: 0.5px; font-weight: bold; color: #0f172a;">${formatCardNumber(cardNumberStr)}</span></div>
                                    <div><strong>شبا بانکی:</strong> <span style="font-family: monospace; font-size: 11px; letter-spacing: 0.5px; font-weight: bold; color: #0f172a;">${formatShebaNumber(shabaNumberStr)}</span></div>
                                </td>
                            </tr>
                        </table>
                    </td>
                    
                    <!-- RIGHT: DETAILED ITEMIZED STATEMENT -->
                    <td style="width: 50%; vertical-align: top;">
                        <table style="width: 100%; border-collapse: collapse; border: 1px solid #cbd5e1; border-radius: 8px; overflow: hidden; font-size: 9.5px;">
                            <tr style="border-bottom: 1px solid #e2e8f0;">
                                <td style="padding: 5px 8px; background-color: #f8fafc; font-weight: bold; color: #475569; width: 55%;">جمع کل اقلام:</td>
                                <td style="padding: 5px 8px; text-align: left; font-weight: bold; color: #0f172a; width: 45%;">$subtotalP $currency</td>
                            </tr>
                            ${if (discount > 0) """
                            <tr style="border-bottom: 1px solid #e2e8f0;">
                                <td style="padding: 5px 8px; background-color: #f8fafc; font-weight: bold; color: #475569;">${if (discountType == "PERCENT") "تخفیف درصدی (${PersianDigitConverter.toPersian(discountPercentage)}٪):" else "تخفیف نقدی / مبلغی:"}</td>
                                <td style="padding: 5px 8px; text-align: left; font-weight: bold; color: #ef4444;">$discountP $currency</td>
                            </tr>
                            """.trimIndent() else ""}
                            ${if (taxValue > 0) """
                            <tr style="border-bottom: 1px solid #e2e8f0;">
                                <td style="padding: 5px 8px; background-color: #f8fafc; font-weight: bold; color: #475569;">مالیات بر ارزش افزوده ($taxRateP%):</td>
                                <td style="padding: 5px 8px; text-align: left; font-weight: bold; color: #0f172a;">$taxValueP $currency</td>
                            </tr>
                            """.trimIndent() else ""}
                            ${if (extraCharges > 0) """
                            <tr style="border-bottom: 1px solid #e2e8f0;">
                                <td style="padding: 5px 8px; background-color: #f8fafc; font-weight: bold; color: #475569;">هزینه حمل و ارسال:</td>
                                <td style="padding: 5px 8px; text-align: left; font-weight: bold; color: #0f172a;">$extraChargesP $currency</td>
                            </tr>
                            """.trimIndent() else ""}
                            ${if (previousBalance != 0.0) """
                            <tr style="border-bottom: 1px solid #cbd5e1;">
                                <td style="padding: 5px 8px; background-color: #f8fafc; font-weight: bold; color: #475569;">مانده قبلی بدهی/طلب:</td>
                                <td style="padding: 5px 8px; text-align: left; font-weight: bold; color: #0f172a;">$previousBalanceP $currency</td>
                            </tr>
                            """.trimIndent() else ""}
                            <tr style="background-color: #1e3a8a; color: #ffffff;">
                                <td style="padding: 7px 8px; font-weight: bold; font-size: 10px;">مبلغ کل سند:</td>
                                <td style="padding: 7px 8px; text-align: left; font-weight: bold; font-size: 11px;">$payableNetP $currency</td>
                            </tr>
                            ${if (paidAmount > 0) """
                            <tr style="border-bottom: 1px solid #cbd5e1;">
                                <td style="padding: 5px 8px; background-color: #f8fafc; font-weight: bold; color: #475569;">مبلغ پرداخت شده:</td>
                                <td style="padding: 5px 8px; text-align: left; font-weight: bold; color: #10b981;">$paidAmountP $currency</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #cbd5e1;">
                                <td style="padding: 5px 8px; background-color: #f8fafc; font-weight: bold; color: #475569;">مبلغ باقیمانده:</td>
                                <td style="padding: 5px 8px; text-align: left; font-weight: bold; color: #ef4444;">$remainingAmountP $currency</td>
                            </tr>
                            <tr>
                                <td style="padding: 5px 8px; background-color: #f8fafc; font-weight: bold; color: #475569;">وضعیت تسویه:</td>
                                <td style="padding: 5px 8px; text-align: left; font-weight: bold; color: #0f172a;">$statusTextP</td>
                            </tr>
                            """.trimIndent() else ""}
                        </table>
                    </td>
                </tr>
            </table>

            <!-- SECTION 4: AUTHENTIC STAMPS & SIGNATURES -->
            <table style="width: 100%; border-collapse: collapse; margin-top: 10px; page-break-inside: avoid;">
                <tr>
                    <!-- SELLER BOX -->
                    <td style="width: 50%; padding-right: 8px; vertical-align: top;">
                        <div style="border: 1px solid #cbd5e1; border-radius: 8px; min-height: 110px; padding: 10px; text-align: center; background-color: #f8fafc;">
                            <div style="font-weight: bold; font-size: 10px; color: #475569; margin-bottom: 8px; border-bottom: 1px solid #e2e8f0; padding-bottom: 3px;">
                                مهر و امضای صادرکننده فاکتور (فروشنده)
                            </div>
                            
                            <!-- CONDITONAL STAMP/SIGNATURE IMAGES -->
                            <div style="position: relative; width: 100%; height: 80px; margin-top: 10px; display: flex; justify-content: center; align-items: center;">
                                ${if (stampBase64 != null) """
                                    <img src="$stampBase64" style="position: absolute; max-height: 75px; max-width: 100px; object-fit: contain; z-index: 1; opacity: 1.0;" alt="مهر فروشگاه">
                                """.trimIndent() else ""}
                                ${if (personalStampBase64 != null) """
                                    <img src="$personalStampBase64" style="position: absolute; max-height: 70px; max-width: 95px; object-fit: contain; z-index: 2; opacity: 0.85;" alt="مهر">
                                """.trimIndent() else ""}
                                ${if (signatureBase64 != null) """
                                    <img src="$signatureBase64" style="position: absolute; max-height: 60px; max-width: 110px; object-fit: contain; z-index: 3; opacity: 0.8;" alt="امضاء">
                                """.trimIndent() else ""}
                            </div>
                            
                            ${if (stampBase64 == null && personalStampBase64 == null && signatureBase64 == null) """
                                <div style="font-size: 9.5px; color: #94a3b8; margin-top: 20px; border: 1px dashed #cbd5e1; display: inline-block; padding: 4px 12px; border-radius: 4px;">محل امضاء و مهر صادرکننده</div>
                            """.trimIndent() else ""}
                        </div>
                    </td>
                    
                    <!-- BUYER BOX -->
                    <td style="width: 50%; vertical-align: top;">
                        <div style="border: 1px solid #cbd5e1; border-radius: 8px; min-height: 110px; padding: 10px; text-align: center; background-color: #f8fafc;">
                            <div style="font-weight: bold; font-size: 10px; color: #475569; margin-bottom: 8px; border-bottom: 1px solid #e2e8f0; padding-bottom: 3px;">
                                مهر و امضای تحویل‌گیرنده (خریدار)
                            </div>
                            <div style="font-size: 9.5px; color: #94a3b8; margin-top: 25px; border: 1px dashed #cbd5e1; display: inline-block; padding: 4px 12px; border-radius: 4px;">محل امضاء و اثر انگشت خریدار</div>
                        </div>
                    </td>
                </tr>
            </table>
        </div>
        </body>
        </html>
        """.trimIndent()

        // Write to temporary local PDF asynchronously leveraging Headless WebViews
        val pdfFile = File(context.cacheDir, "invoice_${invoiceData.invoice.invoiceNumber}.pdf")
        
        android.util.Log.d("AttachmentPdfDebug", "شروع ساخت PDF")
        android.util.Log.d("AttachmentPdfDebug", "documentId: ${invoiceData.invoice.id}")
        android.util.Log.d("AttachmentPdfDebug", "documentType: ${invoiceData.invoice.invoiceType}")
        android.util.Log.d("AttachmentPdfDebug", "تعداد پیوست‌های پیدا شده: ${attachments.size}")

        val finalHtml = if (includeAttachments) {
            android.util.Log.d("AttachmentPdfDebug", "شروع merge پیوستها")
            htmlString.replace("</body>", "${attachmentsToHtml(context, attachments)}</body>")
        } else {
            htmlString
        }

        val success = renderHtmlToPdfFile(context, finalHtml, pdfFile)
        
        if (success) {
            android.util.Log.d("AttachmentPdfDebug", "مسیر PDF نهایی: ${pdfFile.absolutePath}")
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
        } else {
            null
        }
    }

    private fun attachmentsToHtml(context: Context, attachments: List<Attachment>): String {
        if (attachments.isEmpty()) return ""
        
        val sb = StringBuilder()
        sb.append("<div style='page-break-before: always;'></div>")
        sb.append("<h2 style='text-align: center; color: #1e3a8a; border-bottom: 2px solid #1e3a8a; padding-bottom: 5px; margin-top: 30px;'>مدیریت اسناد پیوست</h2>")
        
        attachments.forEachIndexed { index, attachment ->
            android.util.Log.d("AttachmentPdfDebug", "مسیر پیوست: ${attachment.filePath}")
            android.util.Log.d("AttachmentPdfDebug", "نوع پیوست: ${attachment.mimeType}")
            val file = File(attachment.filePath)
            val fileExists = file.exists()
            android.util.Log.d("AttachmentPdfDebug", "آیا فایل پیوست وجود دارد: $fileExists")

            if (!fileExists) {
                android.util.Log.d("AttachmentPdfDebug", "شکست اضافه شدن پیوست: ${attachment.fileName} (فایل پیدا نشد)")
                sb.append("<div style='margin-top: 20px; border: 1px solid #ef4444; padding: 10px; border-radius: 8px;'>")
                sb.append("<h3 style='color: #ef4444;'>پیوست ${PersianDigitConverter.toPersian((index + 1).toString())}: ${attachment.fileName}</h3>")
                sb.append("<p style='color: #ef4444;'>فایل پیوست پیدا نشد.</p>")
                sb.append("</div>")
                return@forEachIndexed
            }
            
            sb.append("<div style='page-break-before: always; padding: 10px;'>")
            sb.append("<div style='border-bottom: 1px solid #cbd5e1; margin-bottom: 15px; padding-bottom: 5px;'>")
            sb.append("<span style='font-weight: bold; color: #1e3a8a; font-size: 14px;'>پیوست ${PersianDigitConverter.toPersian((index + 1).toString())}:</span> ")
            sb.append("<span style='color: #475569; font-size: 14px;'>${attachment.fileName}</span>")
            sb.append("<div style='float: left; font-size: 10px; color: #94a3b8;'>تاریخ افزودن: ${PersianDigitConverter.toPersian(JalaliDateFormatter.format(attachment.createdAt))}</div>")
            sb.append("</div>")
            
            val mimeType = attachment.mimeType.lowercase()
            if (mimeType.startsWith("image/")) {
                if (file.exists()) {
                    android.util.Log.d("AttachmentPdfDebug", "موفقیت اضافه شدن پیوست تصویر: ${attachment.fileName}")
                    val imgBase64 = fileToBase64(file.absolutePath)
                    sb.append("<div style='text-align: center; margin-top: 20px;'>")
                    sb.append("<img src='$imgBase64' style='max-width: 100%; max-height: 850px; object-fit: contain; border: 1px solid #e2e8f0; border-radius: 4px;' />")
                    sb.append("</div>")
                } else {
                    android.util.Log.d("AttachmentPdfDebug", "شکست اضافه شدن پیوست تصویر: ${attachment.fileName}")
                    sb.append("<p style='color: #ef4444;'>خطا در بارگذاری تصویر.</p>")
                }
            } else if (mimeType == "application/pdf") {
                try {
                    val pdfPagesPaths = renderPdfPagesToFiles(context, file)
                    if (pdfPagesPaths.isNotEmpty()) {
                        android.util.Log.d("AttachmentPdfDebug", "موفقیت اضافه شدن پیوست PDF: ${attachment.fileName} (${pdfPagesPaths.size} صفحه)")
                        pdfPagesPaths.forEachIndexed { pIdx, path ->
                            if (pIdx > 0) {
                                sb.append("<div style='page-break-before: always; height: 1px;'></div>")
                            }
                            val pageBase64 = fileToBase64(path)
                            sb.append("<div style='text-align: center; margin-bottom: 10px;'>")
                            sb.append("<img src='$pageBase64' style='max-width: 100%; object-fit: contain; border: 1px solid #e2e8f0;' />")
                            sb.append("</div>")
                        }
                    } else {
                        android.util.Log.d("AttachmentPdfDebug", "شکست اضافه شدن پیوست PDF: ${attachment.fileName} (بدون صفحه)")
                        sb.append("<p style='color: #ef4444;'>خطا در بارگذاری صفحات PDF.</p>")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AttachmentPdfDebug", "خطای کامل exception در پیوست PDF: ", e)
                    sb.append("<p style='color: #ef4444;'>خطا در بارگذاری صفحات PDF.</p>")
                }
            } else {
                android.util.Log.d("AttachmentPdfDebug", "موفقیت اضافه شدن پیوست (نوع نامشخص): ${attachment.fileName}")
                sb.append("<div style='text-align: center; margin-top: 80px; padding: 40px; border: 2px dashed #cbd5e1; border-radius: 12px; background-color: #f8fafc;'>")
                sb.append("<div style='font-size: 40px; margin-bottom: 20px;'>📄</div>")
                sb.append("<p style='color: #64748b; font-size: 16px; font-weight: bold;'>این فایل قابل نمایش در خروجی PDF نیست.</p>")
                sb.append("<p style='font-size: 12px; color: #94a3b8;'>نام فایل: ${attachment.fileName}</p>")
                sb.append("<p style='font-size: 12px; color: #94a3b8;'>نوع فایل (MIME): $mimeType</p>")
                sb.append("<p style='margin-top: 20px; font-size: 11px; color: #64748b;'>می‌توانید این فایل را مستقیماً از بخش مدیریت پیوست‌ها در برنامه باز کنید.</p>")
                sb.append("</div>")
            }
            sb.append("</div>")
        }
        
        return sb.toString()
    }

    private fun renderPdfPagesToFiles(context: Context, file: File): List<String> {
        val result = mutableListOf<String>()
        try {
            val parcelFileDescriptor = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = android.graphics.pdf.PdfRenderer(parcelFileDescriptor)
            
            for (i in 0 until pdfRenderer.pageCount) {
                val page = pdfRenderer.openPage(i)
                val scale = 1.5f
                val bitmap = android.graphics.Bitmap.createBitmap(
                    (page.width * scale).toInt(),
                    (page.height * scale).toInt(),
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                val tempFile = File(context.cacheDir, "pdf_page_${file.nameWithoutExtension}_$i.jpg")
                val outputStream = java.io.FileOutputStream(tempFile)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
                outputStream.flush()
                outputStream.close()
                
                result.add(tempFile.absolutePath)
                
                page.close()
                bitmap.recycle()
            }
            pdfRenderer.close()
            parcelFileDescriptor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private suspend fun renderHtmlToPdfFile(context: Context, html: String, outputFile: File): Boolean = kotlinx.coroutines.withTimeoutOrNull(15000L) {
        suspendCancellableCoroutine<Boolean> { continuation ->
            var isFinished = false
            val webView = WebView(context)
            webView.settings.allowFileAccess = true
            webView.settings.allowContentAccess = true
            
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (isFinished) return
                    isFinished = true
                    val printAdapter = webView.createPrintDocumentAdapter("invoice")
                    val attributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(PrintAttributes.Resolution("id", "print", 300, 300))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()

                    android.print.PrintHelper.callPrintAdapter(printAdapter, attributes, outputFile) { success ->
                        try { webView.destroy() } catch (ignored: Exception) {}
                        if (continuation.isActive) continuation.resume(success)
                    }
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (isFinished) return
                    isFinished = true
                    try { webView.destroy() } catch (ignored: Exception) {}
                    if (continuation.isActive) continuation.resume(false)
                }
            }

            webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
        }
    } ?: false
}
