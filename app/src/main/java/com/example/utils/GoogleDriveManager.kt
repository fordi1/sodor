package com.example.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.example.R
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import java.io.File
import java.io.FileOutputStream
import java.util.Collections

object GoogleDriveManager {
    private const val TAG = "DriveBackupDebug"

    fun getDriveService(context: Context, account: GoogleSignInAccount): Drive? {
        Log.d(TAG, "ساخت Drive service شروع شد")
        return try {
            Log.d(TAG, "درخواست scope Drive")
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(DriveScopes.DRIVE_APPDATA)
            )
            Log.d(TAG, "دریافت token یا credential")
            credential.selectedAccount = account.account
            
            val service = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName(context.getString(R.string.app_name))
                .build()
            
            Log.d(TAG, "Drive service با موفقیت ساخته شد")
            service
        } catch (e: Exception) {
            Log.e(TAG, "ساخت Drive service شکست خورد")
            Log.e(TAG, "متن کامل خطای exception: ${Log.getStackTraceString(e)}")
            null
        }
    }

    fun testAppDataFolder(service: Drive): Boolean {
        Log.d(TAG, "تست appDataFolder شروع شد")
        return try {
            val fileMetadata = DriveFile().apply {
                name = "connection_test.txt"
                parents = listOf("appDataFolder")
            }
            val content = ByteArrayContent.fromString("text/plain", "connection test successful")
            val createdFile = service.files().create(fileMetadata, content).execute()
            val fileId = createdFile.id
            Log.d(TAG, "تست appDataFolder موفقیت‌آمیز بود. شناسه فایل تست: $fileId")
            
            try {
                service.files().delete(fileId).execute()
                Log.d(TAG, "فایل تست با موفقیت از درایو پاک شد")
            } catch (e: Exception) {
                Log.e(TAG, "حذف فایل تست با خطا مواجه شد: ${e.message}")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "تست appDataFolder ناموفق بود: ${e.message}", e)
            Log.e(TAG, "متن کامل خطای exception: ${Log.getStackTraceString(e)}")
            false
        }
    }

    fun uploadBackupFile(service: Drive, localFile: File): DriveFile? {
        Log.d(TAG, "شروع ساخت بکاپ و آپلود آن...")
        return try {
            val fileMetadata = DriveFile().apply {
                name = localFile.name
                parents = listOf("appDataFolder")
            }
            val mediaContent = FileContent("application/zip", localFile)
            val uploadedFile = service.files().create(fileMetadata, mediaContent)
                .setFields("id, name, createdTime, size")
                .execute()
                
            Log.d(TAG, "نتیجه upload موفقیت‌آمیز بود. شناسه فایل: ${uploadedFile.id}, اندازه: ${uploadedFile.getSize()}")
            uploadedFile
        } catch (e: Exception) {
            Log.e(TAG, "خطا در آپلود فایل بکاپ: ${e.message}", e)
            Log.e(TAG, "متن کامل خطای exception: ${Log.getStackTraceString(e)}")
            null
        }
    }

    fun listBackupFiles(service: Drive): List<DriveFile> {
        Log.d(TAG, "درخواست لیست فایل‌های بکاپ از درایو...")
        return try {
            val result = service.files().list()
                .setSpaces("appDataFolder")
                .setFields("files(id, name, createdTime, size)")
                .execute()
                
            val filesList = result.files ?: emptyList()
            Log.d(TAG, "دریافت لیست فایل‌های بکاپ. تعداد فایل یافته شده: ${filesList.size}")
            // Sort by creation time descending (latest first)
            filesList.sortedByDescending { it.createdTime?.value ?: 0L }
        } catch (e: Exception) {
            Log.e(TAG, "خطا در دریافت لیست فایل‌ها از درایو: ${e.message}", e)
            Log.e(TAG, "متن کامل خطای exception: ${Log.getStackTraceString(e)}")
            emptyList()
        }
    }

    fun downloadBackupFile(service: Drive, fileId: String, outputFile: File): Boolean {
        Log.d(TAG, "شروع دانلود فایل بکاپ با شناسه $fileId...")
        return try {
            outputFile.parentFile?.mkdirs()
            val outputStream = FileOutputStream(outputFile)
            service.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            outputStream.flush()
            outputStream.close()
            Log.d(TAG, "دانلود فایل بکاپ موفقیت‌آمیز بود. ذخیره شده در: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "خطا در دانلود فایل بکاپ: ${e.message}", e)
            Log.e(TAG, "متن کامل خطای exception: ${Log.getStackTraceString(e)}")
            false
        }
    }
}
