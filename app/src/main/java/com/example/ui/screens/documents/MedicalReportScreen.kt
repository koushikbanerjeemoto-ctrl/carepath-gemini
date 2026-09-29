package com.example.ui.screens.documents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.MedicalReportEntity
import com.example.ui.SmartHealthViewModel
import com.example.ui.theme.HealthPrimaryLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalReportScreen(
    viewModel: SmartHealthViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reports by viewModel.medicalReports.collectAsStateWithLifecycle()
    var showUploadDialog by remember { mutableStateOf(false) }
    var selectedReportForDetails by remember { mutableStateOf<MedicalReportEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medical Reports & AI OCR", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showUploadDialog = true }) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Upload Report")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showUploadDialog = true },
                containerColor = HealthPrimaryLight,
                contentColor = Color.White
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upload Report", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .testTag("medical_report_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Educational Banner
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("AI Report Interpretation Engine", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Upload blood test PDFs or lab photos for plain-language parameter explanations and reference ranges. (Non-diagnostic educational tool).", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            items(reports) { report ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedReportForDetails = report }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = report.reportType,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(report.reportDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(report.fileName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(report.hospitalName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = HealthPrimaryLight, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("AI Analysis Summary", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = HealthPrimaryLight)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = report.aiAnalysis ?: "Standard clinical parameter reference generated.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUploadDialog) {
        var hospName by remember { mutableStateOf("Peerless Hospital / Sonarpur Diag") }
        var type by remember { mutableStateOf("Complete Blood Count (CBC)") }
        var fileName by remember { mutableStateOf("CBC_Report_Aug_2026.pdf") }
        var analysis by remember { mutableStateOf("Hemoglobin: 13.8 g/dL (Normal: 12.0 - 16.0). Platelet Count: 240,000 /uL (Normal). White Blood Cells: 7,200 /uL. General interpretation: All primary hematology markers within standard clinical reference ranges.") }

        AlertDialog(
            onDismissRequest = { showUploadDialog = false },
            title = { Text("Upload Medical Report", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text("File Name / Test Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = hospName,
                        onValueChange = { hospName = it },
                        label = { Text("Diagnostic Centre / Hospital") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        label = { Text("Report Category") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.uploadMedicalReport(hospName, type, fileName, analysis)
                        showUploadDialog = false
                    }
                ) {
                    Text("Analyze & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUploadDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (selectedReportForDetails != null) {
        val rep = selectedReportForDetails!!
        AlertDialog(
            onDismissRequest = { selectedReportForDetails = null },
            title = { Text(rep.fileName, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Hospital: ${rep.hospitalName}", fontWeight = FontWeight.SemiBold)
                    Text("Category: ${rep.reportType}", color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("AI Interpretation & Reference Parameters:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(rep.aiAnalysis ?: "Standard clinical parameter reference generated.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Disclaimer: This AI analysis explains terminology and compares numbers to standard laboratory references. It does not replace a clinical consultation.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedReportForDetails = null }) {
                    Text("Done")
                }
            }
        )
    }
}
