package com.example.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {
    fun writeTextToPdf(context: Context, filename: String, reportTitle: String, content: String): File {
        val pdfDocument = PdfDocument()
        
        // Paint objects
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        
        val titlePaint = Paint().apply {
            color = Color.rgb(26, 47, 76) // Deep brand blue (#1A2F4C)
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        val lines = content.split("\n")
        var pageNumber = 1
        
        // Page dimensions (A4 size: 595 x 842 points)
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40
        
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas
        
        // Draw Header
        canvas.drawText(reportTitle, margin.toFloat(), 50f, titlePaint)
        canvas.drawText("Généré le ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}", margin.toFloat(), 70f, subtitlePaint)
        
        canvas.drawLine(margin.toFloat(), 80f, (pageWidth - margin).toFloat(), 80f, Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        })

        var y = 100f
        
        for (line in lines) {
            // Skip the main header lines in string content since we already drew a styled header
            if (line.startsWith("RAPPORT D'ÉTAT DES STOCKS") || 
                line.startsWith("RAPPORT DE GESTION DU PARC INFORMATIQUE") || 
                line.startsWith("Généré le:")) {
                continue
            }
            
            // Page break check (A4 is 842 high)
            if (y > pageHeight - margin) {
                pdfDocument.finishPage(currentPage)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                y = 50f // reset y for new page
            }
            
            // Custom styling for separator lines
            if (line.startsWith("===") || line.startsWith("---")) {
                canvas.drawLine(margin.toFloat(), y - 4f, (pageWidth - margin).toFloat(), y - 4f, Paint().apply {
                    color = Color.GRAY
                    strokeWidth = 0.8f
                })
                y += 12f
            } else {
                // Check if line looks like a section header (e.g., capitalized or starts with numbers)
                if (line.matches(Regex("^[1-9]\\..*")) || (line.endsWith(":") && !line.startsWith("-"))) {
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textPaint.textSize = 11f
                    textPaint.color = Color.rgb(26, 47, 76)
                    canvas.drawText(line, margin.toFloat(), y, textPaint)
                    y += 18f
                    // Restore defaults
                    textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                    textPaint.textSize = 9f
                    textPaint.color = Color.BLACK
                } else {
                    canvas.drawText(line, margin.toFloat(), y, textPaint)
                    y += 14f
                }
            }
        }
        
        pdfDocument.finishPage(currentPage)
        
        val file = File(context.cacheDir, filename)
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }
}
