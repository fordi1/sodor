package com.example.data.repository

import com.example.data.dao.AttachmentDao
import com.example.data.entity.Attachment
import kotlinx.coroutines.flow.Flow

class AttachmentRepository(private val attachmentDao: AttachmentDao) {

    fun getAttachmentsForInvoice(invoiceId: Long): Flow<List<Attachment>> {
        return attachmentDao.getAttachmentsForInvoice(invoiceId)
    }

    suspend fun insertAttachment(attachment: Attachment): Long {
        return attachmentDao.insertAttachment(attachment)
    }

    suspend fun deleteAttachment(attachment: Attachment) {
        attachmentDao.deleteAttachment(attachment)
    }

    suspend fun deleteAttachmentById(attachmentId: Long) {
        attachmentDao.deleteAttachmentById(attachmentId)
    }
    
    suspend fun getAttachmentById(id: Long): Attachment? {
        return attachmentDao.getAttachmentById(id)
    }
}
