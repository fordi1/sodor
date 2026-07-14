package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object FileHelper {

    fun copyUriToInternalStorage(context: Context, uri: Uri, subDir: String): File? {
        val contentResolver = context.contentResolver
        val fileName = getFileName(context, uri) ?: "file_${UUID.randomUUID()}"
        val folder = File(context.filesDir, subDir)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val destinationFile = File(folder, "${System.currentTimeMillis()}_$fileName")
        
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(destinationFile)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            destinationFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    fun getMimeType(context: Context, uri: Uri): String? {
        return if (uri.scheme == "content") {
            context.contentResolver.getType(uri)
        } else {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase())
        }
    }

    fun getMimeTypeFromFile(file: File): String? {
        val extension = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    fun openFile(context: Context, file: File) {
        if (!file.exists()) {
            android.widget.Toast.makeText(context, "فایل پیدا نشد.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mimeType = getMimeTypeFromFile(file) ?: "*/*"
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "برنامه‌ای برای باز کردن این فایل پیدا نشد.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
