package com.spendwise.app.presentation.expense

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.spendwise.app.di.AppModule
import com.spendwise.app.domain.model.Expense
import com.spendwise.app.domain.model.ExpenseCategory
import com.spendwise.app.domain.model.PaymentMethod
import com.spendwise.app.utils.ReceiptScanner
import com.spendwise.app.utils.ScannedReceipt
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    navController: NavController,
    onReceiptScanned: (Double, String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var scanning by remember { mutableStateOf(false) }
    var scannedResult by remember { mutableStateOf<ScannedReceipt?>(null) }

    // State for extracted details in review sheet
    var editAmount by remember { mutableStateOf("") }
    var editMerchant by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf(ExpenseCategory.SHOPPING.displayName) }
    var editPaymentMethod by remember { mutableStateOf(PaymentMethod.UPI.displayName) }
    var isSaving by remember { mutableStateOf(false) }

    // Gallery Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scanning = true
            coroutineScope.launch {
                val scanned = ReceiptScanner.scanReceipt(context, uri)
                scanning = false
                if (scanned != null) {
                    scannedResult = scanned
                    editAmount = if (scanned.amount > 0) scanned.amount.toString() else ""
                    editMerchant = scanned.merchant
                    editCategory = scanned.suggestedCategory
                    editPaymentMethod = scanned.suggestedPaymentMethod
                } else {
                    Toast.makeText(context, "Could not extract text. Please try another image.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Receipt / Bill", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gallery")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Camera permission is required to scan receipts live.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Camera Permission")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Choose from Gallery / Photos")
                    }
                }
            }

            // Bottom capture control bar
            if (scannedResult == null && !scanning && hasCameraPermission) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedIconButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.size(54.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White)
                    }

                    Button(
                        onClick = {
                            val file = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                            scanning = true
                            imageCapture.takePicture(
                                outputOptions,
                                cameraExecutor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        coroutineScope.launch {
                                            val scanned = ReceiptScanner.scanReceipt(context, Uri.fromFile(file))
                                            scanning = false
                                            if (scanned != null) {
                                                scannedResult = scanned
                                                editAmount = if (scanned.amount > 0) scanned.amount.toString() else ""
                                                editMerchant = scanned.merchant
                                                editCategory = scanned.suggestedCategory
                                                editPaymentMethod = scanned.suggestedPaymentMethod
                                            } else {
                                                Toast.makeText(context, "Could not detect clear receipt text.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    override fun onError(exception: ImageCaptureException) {
                                        scanning = false
                                        Toast.makeText(context, "Capture error: ${exception.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        },
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Camera, contentDescription = "Capture", modifier = Modifier.size(32.dp))
                    }

                    Spacer(modifier = Modifier.size(54.dp))
                }
            }

            // Scanning Progress State
            if (scanning) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                        Text("Reading Receipt & Extracting Total...", fontWeight = FontWeight.Bold)
                        Text("AI OCR pattern recognition in progress", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Extracted Result Modal Sheet
            if (scannedResult != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Receipt Extracted", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            IconButton(onClick = { scannedResult = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Retake")
                            }
                        }

                        // Confidence reason badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = "✨ ${scannedResult?.reason ?: "Receipt analyzed"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        // Amount Field
                        OutlinedTextField(
                            value = editAmount,
                            onValueChange = { editAmount = it },
                            label = { Text("Total Amount (₹) *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                        )

                        // Merchant / Description
                        OutlinedTextField(
                            value = editMerchant,
                            onValueChange = { editMerchant = it },
                            label = { Text("Merchant / Description") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Category Selector
                        Text("Category", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Food", "Shopping", "Transport", "Healthcare", "Utilities", "Other").forEach { cat ->
                                FilterChip(
                                    selected = editCategory == cat,
                                    onClick = { editCategory = cat },
                                    label = { Text(cat, fontSize = 12.sp) }
                                )
                            }
                        }

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val amt = editAmount.toDoubleOrNull() ?: 0.0
                                    onReceiptScanned(amt, editMerchant)
                                    navController.popBackStack()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Edit in Form")
                            }

                            Button(
                                onClick = {
                                    val parsedAmt = editAmount.toDoubleOrNull()
                                    if (parsedAmt == null || parsedAmt <= 0) {
                                        Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isSaving = true
                                    coroutineScope.launch {
                                        val newExpense = Expense(
                                            amount = parsedAmt,
                                            category = editCategory,
                                            description = editMerchant.ifBlank { "Receipt Scan" },
                                            paymentMethod = editPaymentMethod,
                                            date = Date(),
                                            source = "RECEIPT"
                                        )
                                        AppModule.expenseRepository.addExpense(newExpense)
                                        isSaving = false
                                        Toast.makeText(context, "Receipt saved as ₹$parsedAmt expense!", Toast.LENGTH_SHORT).show()
                                        onReceiptScanned(parsedAmt, editMerchant)
                                        navController.popBackStack()
                                    }
                                },
                                modifier = Modifier.weight(1.2f),
                                enabled = !isSaving
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                } else {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save Expense")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
