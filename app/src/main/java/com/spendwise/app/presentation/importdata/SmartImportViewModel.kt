package com.spendwise.app.presentation.importdata

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.di.AppModule
import com.spendwise.app.domain.model.*
import com.spendwise.app.utils.ImportAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

data class ImportStats(
    val totalImports: Int = 0,
    val totalTransactionsImported: Int = 0,
    val totalDuplicatesPrevented: Int = 0,
    val totalNeedsReview: Int = 0,
    val csvImportsCount: Int = 0,
    val excelImportsCount: Int = 0,
    val receiptImportsCount: Int = 0,
    val voiceImportsCount: Int = 0,
    val manualImportsCount: Int = 0
)

class SmartImportViewModel : ViewModel() {

    private val importRepository = AppModule.importRepository
    private val expenseRepository = AppModule.expenseRepository
    private val incomeRepository = AppModule.incomeRepository
    private val authRepository = AppModule.authRepository

    val importHistory: StateFlow<List<ImportHistory>> = importRepository.getImportHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val importPreferences: StateFlow<ImportPreferences> = importRepository.getImportPreferences()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ImportPreferences())

    private val expenses = expenseRepository.getExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val incomes = incomeRepository.getIncomes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state for in-progress import
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _candidates = MutableStateFlow<List<ImportCandidate>>(emptyList())
    val candidates: StateFlow<List<ImportCandidate>> = _candidates.asStateFlow()

    private val _rawTable = MutableStateFlow<List<List<String>>>(emptyList())
    val rawTable: StateFlow<List<List<String>>> = _rawTable.asStateFlow()

    private val _currentFileName = MutableStateFlow("")
    val currentFileName: StateFlow<String> = _currentFileName.asStateFlow()

    private val _currentSourceType = MutableStateFlow("CSV")
    val currentSourceType: StateFlow<String> = _currentSourceType.asStateFlow()

    private val _showReviewDialog = MutableStateFlow(false)
    val showReviewDialog: StateFlow<Boolean> = _showReviewDialog.asStateFlow()

    private val _showColumnMappingDialog = MutableStateFlow(false)
    val showColumnMappingDialog: StateFlow<Boolean> = _showColumnMappingDialog.asStateFlow()

    private val _detectedMapping = MutableStateFlow(ImportAnalyzer.ColumnMapping())
    val detectedMapping: StateFlow<ImportAnalyzer.ColumnMapping> = _detectedMapping.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    val importStats: StateFlow<ImportStats> = importHistory.map { historyList ->
        var totalTx = 0
        var totalDup = 0
        var totalRev = 0
        var csv = 0
        var excel = 0
        var receipt = 0
        var voice = 0
        var manual = 0

        for (item in historyList) {
            totalTx += item.newRecords
            totalDup += item.duplicateRecords
            totalRev += item.reviewRecords
            when (item.sourceType.uppercase(Locale.ROOT)) {
                "CSV" -> csv++
                "EXCEL" -> excel++
                "RECEIPT" -> receipt++
                "VOICE" -> voice++
                "MANUAL" -> manual++
            }
        }

        ImportStats(
            totalImports = historyList.size,
            totalTransactionsImported = totalTx,
            totalDuplicatesPrevented = totalDup,
            totalNeedsReview = totalRev,
            csvImportsCount = csv,
            excelImportsCount = excel,
            receiptImportsCount = receipt,
            voiceImportsCount = voice,
            manualImportsCount = manual
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ImportStats())

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    /**
     * Process an uploaded file (CSV / XLSX)
     */
    fun processFile(uri: Uri, context: Context, fileName: String) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _currentFileName.value = fileName
            _errorMessage.value = null

            val isExcel = fileName.endsWith(".xlsx", ignoreCase = true) || fileName.endsWith(".xls", ignoreCase = true)
            val sourceType = if (isExcel) "EXCEL" else "CSV"
            _currentSourceType.value = sourceType

            try {
                val rawData = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        if (isExcel) {
                            ImportAnalyzer.parseExcelStream(stream)
                        } else {
                            ImportAnalyzer.parseCsvStream(stream)
                        }
                    } ?: emptyList()
                }

                if (rawData.isEmpty()) {
                    _errorMessage.value = "Selected file is empty or could not be read."
                    _isAnalyzing.value = false
                    return@launch
                }

                _rawTable.value = rawData
                val headers = rawData[0]
                val mapping = ImportAnalyzer.detectColumnMapping(headers)
                _detectedMapping.value = mapping

                if (mapping.isValid) {
                    runAnalysis(rawData, mapping, sourceType)
                    _showReviewDialog.value = true
                } else {
                    _showColumnMappingDialog.value = true
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error parsing file: ${e.localizedMessage ?: e.message}"
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun applyCustomMapping(mapping: ImportAnalyzer.ColumnMapping) {
        _showColumnMappingDialog.value = false
        _detectedMapping.value = mapping
        runAnalysis(_rawTable.value, mapping, _currentSourceType.value)
        _showReviewDialog.value = true
    }

    private fun runAnalysis(
        rawRows: List<List<String>>,
        mapping: ImportAnalyzer.ColumnMapping,
        sourceType: String
    ) {
        val prefs = importPreferences.value
        val exps = expenses.value
        val incs = incomes.value

        val processedCandidates = ImportAnalyzer.analyzeRows(
            rawRows = rawRows,
            mapping = mapping,
            sourceType = sourceType,
            existingExpenses = exps,
            existingIncomes = incs,
            enableDuplicateDetection = prefs.duplicateDetection,
            enableAutoCategorization = prefs.autoCategorization
        )

        _candidates.value = processedCandidates
    }

    fun toggleCandidateSelection(candidateId: String) {
        _candidates.update { list ->
            list.map { if (it.id == candidateId) it.copy(isSelected = !it.isSelected) else it }
        }
    }

    fun selectAll(select: Boolean) {
        _candidates.update { list ->
            list.map { candidate ->
                if (candidate.status == CandidateStatus.DUPLICATE || candidate.status == CandidateStatus.INVALID) {
                    candidate.copy(isSelected = false)
                } else {
                    candidate.copy(isSelected = select)
                }
            }
        }
    }

    fun updateCandidate(updated: ImportCandidate) {
        _candidates.update { list ->
            list.map { if (it.id == updated.id) updated else it }
        }
    }

    fun closeReview() {
        _showReviewDialog.value = false
        _showColumnMappingDialog.value = false
        _candidates.value = emptyList()
        _rawTable.value = emptyList()
    }

    /**
     * Commit reviewed transactions to Firestore
     */
    fun commitImport() {
        viewModelScope.launch {
            val list = _candidates.value
            val selected = list.filter { it.isSelected && it.status != CandidateStatus.INVALID && it.status != CandidateStatus.DUPLICATE }
            if (selected.isEmpty()) {
                _errorMessage.value = "No valid records selected to import."
                return@launch
            }

            _isImporting.value = true
            try {
                val totalCount = list.size
                val newCount = selected.size
                val dupCount = list.count { it.status == CandidateStatus.DUPLICATE }
                val revCount = list.count { it.status == CandidateStatus.NEEDS_REVIEW }

                val history = ImportHistory(
                    importId = UUID.randomUUID().toString(),
                    userId = authRepository.currentUser.firstOrNull()?.uid ?: "",
                    fileName = _currentFileName.value.ifBlank { "Smart Import" },
                    sourceType = _currentSourceType.value,
                    totalRecords = totalCount,
                    newRecords = newCount,
                    duplicateRecords = dupCount,
                    reviewRecords = revCount,
                    status = "COMPLETED",
                    createdAt = Date()
                )

                // 1. Record in importHistory
                val recordResult = importRepository.recordImport(history)
                val importId = recordResult.getOrNull() ?: history.importId

                // 2. Save transactions to Firestore
                val saveResult = importRepository.saveImportedTransactions(importId, list)
                if (saveResult.isSuccess) {
                    val (exp, inc) = saveResult.getOrNull() ?: Pair(0, 0)
                    _successMessage.value = "Successfully imported $newCount transactions ($exp expenses, $inc incomes). $dupCount duplicates prevented."
                    closeReview()
                } else {
                    _errorMessage.value = "Failed to save transactions: ${saveResult.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Import failed: ${e.localizedMessage ?: e.message}"
            } finally {
                _isImporting.value = false
            }
        }
    }
}
