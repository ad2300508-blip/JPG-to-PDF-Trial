package com.example.util

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import java.io.File

object PdfSecurityUtils {

  private var isInitialized = false

  fun init(context: Context) {
    if (!isInitialized) {
      PDFBoxResourceLoader.init(context.applicationContext)
      isInitialized = true
    }
  }

  /**
   * Applies standard password encryption to an existing PDF file.
   * If userPassword is provided, opening the PDF requires userPassword.
   * If ownerPassword is provided, modifying or removing permissions requires ownerPassword.
   */
  fun encryptPdf(
    context: Context,
    inputFile: File,
    outputFile: File,
    userPassword: String,
    ownerPassword: String,
  ) {
    init(context)
    val effectiveUserPassword = userPassword.trim()
    val effectiveOwnerPassword = if (ownerPassword.isNotBlank()) ownerPassword.trim() else effectiveUserPassword

    val document = PDDocument.load(inputFile)
    try {
      val accessPermission = AccessPermission().apply {
        setCanPrint(true)
        setCanPrintDegraded(true)
        setCanModify(false)
        setCanModifyAnnotations(false)
        setCanExtractContent(true)
      }

      val protectionPolicy = StandardProtectionPolicy(
        effectiveOwnerPassword,
        effectiveUserPassword,
        accessPermission
      ).apply {
        encryptionKeyLength = 128
        permissions = accessPermission
      }

      document.protect(protectionPolicy)
      document.save(outputFile)
    } finally {
      document.close()
    }
  }
}
