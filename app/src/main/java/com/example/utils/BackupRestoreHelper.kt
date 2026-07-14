package com.example.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupRestoreHelper {

    fun backupDatabaseFile(context: Context): File? {
        try {
            AppDatabase.getDatabase(context).close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        val dbFile = context.getDatabasePath("daftar_invoice_database")
        if (!dbFile.exists()) return null

        val backupFileName = "daftar_invoice_backup_${System.currentTimeMillis()}.zip"
        val backupFile = File(context.cacheDir, backupFileName)

        return try {
            val fos = FileOutputStream(backupFile)
            val zos = ZipOutputStream(fos)

            val dbEntry = ZipEntry("database.db")
            zos.putNextEntry(dbEntry)
            val dbIn = FileInputStream(dbFile)
            dbIn.use { input ->
                input.copyTo(zos)
            }
            zos.closeEntry()

            val filesDir = context.filesDir
            val filesList = filesDir.listFiles()
            if (filesList != null) {
                for (f in filesList) {
                    if (f.isFile) {
                        val fileEntry = ZipEntry("files/${f.name}")
                        zos.putNextEntry(fileEntry)
                        val fileIn = FileInputStream(f)
                        fileIn.use { input ->
                            input.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                }
            }

            zos.flush()
            zos.close()
            fos.close()

            backupFile
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            null
        }
    }

    fun backupDatabase(context: Context): Uri? {
        // Safe database close / checkpoint behavior of active sessions
        try {
            AppDatabase.getDatabase(context).close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val dbFile = context.getDatabasePath("daftar_invoice_database")
        if (!dbFile.exists()) return null

        val backupFileName = "daftar_invoice_backup_${System.currentTimeMillis()}.zip"
        val backupFile = File(context.cacheDir, backupFileName)

        return try {
            val fos = FileOutputStream(backupFile)
            val zos = ZipOutputStream(fos)

            // 1. Pack SQLite Database file under "database.db"
            val dbEntry = ZipEntry("database.db")
            zos.putNextEntry(dbEntry)
            val dbIn = FileInputStream(dbFile)
            dbIn.use { input ->
                input.copyTo(zos)
            }
            zos.closeEntry()

            // 2. Pack all local files inside filesDir (logos, stamps, signatures, etc.)
            val filesDir = context.filesDir
            val filesList = filesDir.listFiles()
            if (filesList != null) {
                for (file in filesList) {
                    if (file.isFile) {
                        val fileEntry = ZipEntry("files/${file.name}")
                        zos.putNextEntry(fileEntry)
                        val fileIn = FileInputStream(file)
                        fileIn.use { input ->
                            input.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                }
            }

            zos.flush()
            zos.close()
            fos.close()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", backupFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun restoreDatabase(context: Context, backupUri: Uri): Boolean {
        try {
            AppDatabase.getDatabase(context).close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val dbFile = context.getDatabasePath("daftar_invoice_database")
        
        // Clean up DB write-ahead logs to prevent schema locks
        val dbWal = context.getDatabasePath("daftar_invoice_database-wal")
        val dbShm = context.getDatabasePath("daftar_invoice_database-shm")
        if (dbWal.exists()) dbWal.delete()
        if (dbShm.exists()) dbShm.delete()

        return try {
            val inputStream = context.contentResolver.openInputStream(backupUri) ?: return false
            val zis = ZipInputStream(inputStream)
            
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                if (name == "database.db") {
                    val dbOut = FileOutputStream(dbFile)
                    dbOut.use { output ->
                        zis.copyTo(output)
                    }
                } else if (name.startsWith("files/")) {
                    val nameInZip = name.substring("files/".length)
                    if (nameInZip.isNotEmpty()) {
                        val restoredFile = File(context.filesDir, nameInZip)
                        val fileOut = FileOutputStream(restoredFile)
                        fileOut.use { output ->
                            zis.copyTo(output)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
            zis.close()
            inputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
