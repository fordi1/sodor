package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

val onboardingPages = listOf(
    OnboardingPage(
        title = "به برنامه ساخت فاکتور خوش آمدید",
        description = "برای شروع استفاده از برنامه، پیشنهاد می‌کنیم ابتدا اطلاعات فروشگاه یا شرکت خود را تنظیم کنید. شامل نام، لوگو، مهر و امضا برای تنظیم فاکتورها.",
        icon = Icons.Default.Storefront,
        color = Color(0xFF3B82F6)
    ),
    OnboardingPage(
        title = "مدیریت مشتریان و کالاها",
        description = "مشتریان خود را ثبت کنید تا در فاکتورها به سرعت آن‌ها را انتخاب کنید. همچنین کالاها و خدمات خود را با قیمت مشخص ثبت کرده تا بتوانید فاکتوری کامل داشته باشید.",
        icon = Icons.Default.PeopleAlt,
        color = Color(0xFF10B981)
    ),
    OnboardingPage(
        title = "صدور فاکتور و پیش‌فاکتور",
        description = "فاکتورهای رسمی یا پیش‌فاکتور (Proforma) ثبت کنید. می‌توانید با انتخاب فرمت PDF، فاکتورها را به سادگی از طریق پیام‌رسان‌ها برای مشتری ارسال یا مستقیما چاپ کنید.",
        icon = Icons.Default.ReceiptLong,
        color = Color(0xFF8B5CF6)
    ),
    OnboardingPage(
        title = "گزارش‌ها و پشتیبان‌گیری",
        description = "وضعیت مالی، فروش و مطالبات را از بخش گزارشات پیگیری نمایید. برای جلوگیری از پاک شدن اطلاعات، حتما بخش پشتیبان‌گیری ابری با Google Drive را فعال کنید.",
        icon = Icons.Default.CloudSync,
        color = Color(0xFFF59E0B)
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val currentPage = onboardingPages[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(currentPage.color.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = currentPage.icon,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = currentPage.color
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text(
                        text = currentPage.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF1E293B)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = currentPage.description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF64748B),
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2f
                    )
                }
            }

            // Pager Indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(onboardingPages.size) { index ->
                    val color = if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary else Color.LightGray
                    val width = if (pagerState.currentPage == index) 24.dp else 8.dp
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(width = width, height = 8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            // Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(onClick = { 
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }) {
                        Text("قبلی", color = Color.Gray)
                    }
                } else {
                    Spacer(modifier = Modifier.width(64.dp))
                }

                if (pagerState.currentPage < onboardingPages.size - 1) {
                    TextButton(onClick = onFinish) {
                        Text("رد کردن", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("بعدی")
                    }
                } else {
                    Button(
                        onClick = onFinish,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text("شروع استفاده از برنامه", modifier = Modifier.padding(horizontal = 8.dp))
                    }
                }
            }
        }
    }
}
