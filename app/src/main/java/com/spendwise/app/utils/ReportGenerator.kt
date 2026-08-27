package com.spendwise.app.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.spendwise.app.domain.model.Expense
import java.io.File
import java.io.FileOutputStream

object ReportGenerator {
    fun generatePdfReport(context: Context, expenses: List<Expense>, totalIncome: Double): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()
        
        paint.textSize = 12f
        canvas.drawText("SpendWise Financial Report", 80f, 50f, paint)
        
        paint.textSize = 10f
        canvas.drawText("Total Income: ₹$totalIncome", 20f, 80f, paint)
        canvas.drawText("Total Expenses: ₹${expenses.sumOf { it.amount }}", 20f, 100f, paint)
        
        var y = 130f
        canvas.drawText("Recent Expenses:", 20f, y, paint)
        y += 20f
        
        expenses.take(10).forEach { expense ->
            canvas.drawText("${expense.category}: ₹${expense.amount}", 20f, y, paint)
            y += 15f
        }
        
        pdfDocument.finishPage(page)
        
        val file = File(context.cacheDir, "SpendWise_Report.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()
        return file
    }
}
