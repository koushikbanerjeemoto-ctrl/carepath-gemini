package com.example.ui.screens.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.data.local.ChatMessageEntity
import com.example.data.model.ChatSender
import com.example.data.model.HospitalType
import com.example.ui.SmartHealthViewModel
import com.example.ui.navigation.Screen
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.HealthPrimaryLight
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatBottomSheet(
    viewModel: SmartHealthViewModel,
    onNavigateTo: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isAIGenerating by viewModel.isAIGenerating.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val quickChips = listOf(
        "Request Emergency Ambulance",
        "Find ICU beds near me",
        "Show Government hospitals",
        "Check my symptoms (Triage)",
        "Explain blood test report",
        "Book a doctor appointment",
        "Search medicines in pharmacy"
    )

    LaunchedEffect(chatMessages.size, isAIGenerating) {
        val count = chatMessages.size + if (isAIGenerating) 1 else 0
        if (count > 0) {
            listState.animateScrollToItem(count - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .testTag("ai_chat_bottom_sheet")
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(HealthPrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Smart Health AI Assistant",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Connected • Triage & Action Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider()

            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (chatMessages.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Welcome to Smart Health AI",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Ask me to find ICU beds, dispatch an ambulance, analyze symptoms, locate government hospitals, or explain clinical reports in English, Hindi, or Bengali.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                items(chatMessages) { msg ->
                    ChatBubbleItem(
                        message = msg,
                        onActionClick = { actionType, payload ->
                            when (actionType) {
                                "NAV_AMBULANCE" -> {
                                    onDismiss()
                                    onNavigateTo(Screen.Ambulance.route)
                                }
                                "NAV_EMERGENCY" -> {
                                    onDismiss()
                                    onNavigateTo(Screen.EmergencyCenter.route)
                                }
                                "NAV_HOSPITALS_ICU" -> {
                                    viewModel.updateFilter(filterState.copy(requireIcu = true))
                                    onDismiss()
                                    onNavigateTo(Screen.Hospitals.route)
                                }
                                "NAV_HOSPITALS_GOVT" -> {
                                    viewModel.updateFilter(filterState.copy(type = HospitalType.GOVERNMENT))
                                    onDismiss()
                                    onNavigateTo(Screen.Hospitals.route)
                                }
                                "NAV_HOSPITALS_NEAREST" -> {
                                    onDismiss()
                                    onNavigateTo(Screen.Hospitals.route)
                                }
                                "NAV_SYMPTOMS" -> {
                                    onDismiss()
                                    onNavigateTo(Screen.SymptomsTriage.route)
                                }
                                "NAV_DOCTORS" -> {
                                    onDismiss()
                                    onNavigateTo(Screen.Doctors.route)
                                }
                                "NAV_REPORTS" -> {
                                    onDismiss()
                                    onNavigateTo(Screen.Reports.route)
                                }
                                "NAV_PRESCRIPTIONS" -> {
                                    onDismiss()
                                    onNavigateTo(Screen.Prescriptions.route)
                                }
                                "NAV_MEDICINES" -> {
                                    onDismiss()
                                    onNavigateTo(Screen.Medicines.route)
                                }
                                "NAV_REFERRALS" -> {
                                    onDismiss()
                                    onNavigateTo(Screen.Referrals.route)
                                }
                            }
                        }
                    )
                }

                if (isAIGenerating) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = HealthPrimaryLight
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "CarePath AI is thinking...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Quick Prompt Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickChips) { chip ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable(enabled = !isAIGenerating) {
                            viewModel.sendChatMessage(chip)
                        }
                    ) {
                        Text(
                            text = chip,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Bottom Input Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask CarePath AI...") },
                    shape = RoundedCornerShape(24.dp),
                    enabled = !isAIGenerating,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    maxLines = 3
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isAIGenerating) {
                            val txt = inputText
                            inputText = ""
                            viewModel.sendChatMessage(txt)
                        }
                    },
                    enabled = inputText.isNotBlank() && !isAIGenerating,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = HealthPrimaryLight,
                        contentColor = Color.White,
                        disabledContainerColor = HealthPrimaryLight.copy(alpha = 0.5f),
                        disabledContentColor = Color.White.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    if (isAIGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(
    message: ChatMessageEntity,
    onActionClick: (String, String) -> Unit
) {
    val isUser = message.sender == ChatSender.USER

    Column(
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) HealthPrimaryLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            tonalElevation = 2.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                )

                if (!isUser && message.actionType != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onActionClick(message.actionType, message.actionPayload ?: "") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (message.actionType.contains("EMERGENCY") || message.actionType.contains("AMBULANCE")) EmergencyRed else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when (message.actionType) {
                                "NAV_AMBULANCE" -> "Request Ambulance Now"
                                "NAV_EMERGENCY" -> "Open Emergency SOS"
                                "NAV_HOSPITALS_ICU" -> "Show Hospitals with ICU Beds"
                                "NAV_HOSPITALS_GOVT" -> "View Government Hospitals"
                                "NAV_HOSPITALS_NEAREST" -> "Find Nearest Facilities"
                                "NAV_SYMPTOMS" -> "Check Symptoms"
                                "NAV_DOCTORS" -> "Book Doctor"
                                "NAV_REPORTS" -> "View Reports"
                                "NAV_PRESCRIPTIONS" -> "Open Prescriptions"
                                "NAV_MEDICINES" -> "Check Pharmacy Stock"
                                "NAV_REFERRALS" -> "Track Referral"
                                else -> "Take Action"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
