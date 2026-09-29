package com.example.ui.screens.health

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.HealthProfileEntity
import com.example.data.model.UserRole
import com.example.ui.SmartHealthViewModel
import com.example.ui.navigation.Screen
import com.example.ui.theme.HealthPrimaryLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthProfileScreen(
    viewModel: SmartHealthViewModel,
    onNavigateTo: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val healthProfile by viewModel.healthProfile.collectAsStateWithLifecycle()
    val activeRole by viewModel.activeRole.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Profile & Identity", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditProfileDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .testTag("health_profile_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Header Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = currentUser?.name ?: "Guest Patient",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = currentUser?.phone ?: "No phone registered (Guest Mode)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (currentUser == null) {
                            Button(
                                onClick = { viewModel.setLoginDialogOpen(true) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Create Account / Sign In")
                            }
                        }
                    }
                }
            }

            // Health Parameters (Vitals, Blood Group, Allergies)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Core Clinical Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            VitalsChip("Blood Group", healthProfile?.bloodGroup ?: "O+", Modifier.weight(1f))
                            VitalsChip("Height", "${healthProfile?.heightCm ?: 172.0} cm", Modifier.weight(1f))
                            VitalsChip("Weight", "${healthProfile?.weightKg ?: 68.5} kg", Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Known Allergies & Drug Sensitivities:", fontWeight = FontWeight.SemiBold)
                        Text(healthProfile?.allergies ?: "None reported", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Chronic Medical Conditions:", fontWeight = FontWeight.SemiBold)
                        Text(healthProfile?.chronicDiseases ?: "Mild Seasonal Bronchospasm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Quick Links
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        ProfileMenuRow("Emergency Contacts", Icons.Default.Emergency, Color(0xFFC62828)) {
                            onNavigateTo(Screen.EmergencyContacts.route)
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                        ProfileMenuRow("Frontline Health Worker Portal (ASHA)", Icons.Default.VolunteerActivism, HealthPrimaryLight) {
                            onNavigateTo(Screen.HealthWorkerPortal.route)
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                        ProfileMenuRow("Hospital Staff Portal (Bed Management)", Icons.Default.LocalHospital, Color(0xFF006874)) {
                            onNavigateTo(Screen.HospitalStaffPortal.route)
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                        ProfileMenuRow("Government Health Quality Portal", Icons.Default.AdminPanelSettings, Color(0xFF5C6BC0)) {
                            onNavigateTo(Screen.GovernmentAdmin.route)
                        }
                    }
                }
            }
        }
    }

    if (showEditProfileDialog) {
        var bloodGroup by remember { mutableStateOf(healthProfile?.bloodGroup ?: "O+") }
        var allergies by remember { mutableStateOf(healthProfile?.allergies ?: "Penicillin (Mild)") }
        var chronic by remember { mutableStateOf(healthProfile?.chronicDiseases ?: "Mild Seasonal Bronchospasm") }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Update Health Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(value = bloodGroup, onValueChange = { bloodGroup = it }, label = { Text("Blood Group") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = allergies, onValueChange = { allergies = it }, label = { Text("Allergies") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = chronic, onValueChange = { chronic = it }, label = { Text("Chronic Conditions") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateHealthProfile(
                            HealthProfileEntity(
                                profileId = healthProfile?.profileId ?: "prof_default",
                                userId = "user_default",
                                bloodGroup = bloodGroup,
                                allergies = allergies,
                                chronicDiseases = chronic
                            )
                        )
                        showEditProfileDialog = false
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun VitalsChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ProfileMenuRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
