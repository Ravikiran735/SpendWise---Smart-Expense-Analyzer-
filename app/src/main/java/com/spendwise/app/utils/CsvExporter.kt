package com.spendwise.app.utils

import android.content.Context
import com.spendwise.app.domain.model.Expense
import com.spendwise.app.domain.model.Income
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {
    fun exportToCsv(context: Context, expenses: List<Expense>, incomes: List<Income>): File {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val csvFile = File(context.cacheDir, "SpendWise_Transactions.csv")
        
        csvFile.printWriter().use { out ->
            out.println("Date,Type,Category/Source,Description,Amount")
            
            expenses.forEach {
                out.println("${dateFormat.format(it.date)},Expense,${it.category},${it.description},${it.amount}")
            }
            
            incomes.forEach {
                out.println("${dateFormat.format(it.date)},Income,${it.source},${it.description},${it.amount}")
            }
        }
        
        return csvFile
    }
}
