package com.example.ui.screens

import android.text.format.DateFormat
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import com.example.data.MedicationReminder
import com.example.ui.theme.*
import com.example.viewmodel.Bhajan
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.MoralStory
import com.example.viewmodel.PuzzleTile
import com.example.viewmodel.Quote
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Drawing canvas path element
data class CustomPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

private fun makePhoneCall(context: Context, phoneNumber: String) {
    try {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            context.startActivity(dialIntent)
        } catch (ex: Exception) {
            Toast.makeText(context, "Could not place call", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onStartVoiceListening: () -> Unit
) {
    val isKidsMode by viewModel.isKidsMode.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val voiceStatusMessage by viewModel.voiceStatusMessage.collectAsStateWithLifecycle()
    val activeAlarm by viewModel.activeAlarm.collectAsStateWithLifecycle()
    val sosCountdown by viewModel.sosCountdown.collectAsStateWithLifecycle()
    val sosActive by viewModel.sosActive.collectAsStateWithLifecycle()
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
    val emergencyContact by viewModel.seniorEmergencyContact.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val weatherTemp by viewModel.weatherTemp.collectAsStateWithLifecycle()
    val weatherEmoji by viewModel.weatherEmoji.collectAsStateWithLifecycle()
    val seniorName by viewModel.seniorName.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf("home") }

    LaunchedEffect(sosCountdown, sosActive) {
        if (sosActive && (sosCountdown == 0 || sosCountdown == null)) {
            makePhoneCall(context, emergencyContact)
        }
    }

    // Alarm full-screen interrupt modal
    if (activeAlarm != null) {
        AlarmInterruptDialog(
            reminder = activeAlarm!!,
            onTaken = { viewModel.markMedicineTaken() },
            onDismiss = { viewModel.dismissAlarm() }
        )
    }

    if (!isOnboardingCompleted) {
        OnboardingScreen(viewModel = viewModel)
    } else {
        Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isKidsMode) "👶 Kids Zone" else "👵 SeniorCare Mode",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("app_title")
                        )
                        // Live dynamic formatted date
                        val currentDateStr = remember {
                            val sdf = java.text.SimpleDateFormat("EEEE, MMMM dd", java.util.Locale.getDefault())
                            sdf.format(java.util.Date())
                        }
                        Text(
                            text = currentDateStr,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    if (currentScreen != "home") {
                        IconButton(
                            onClick = { currentScreen = "home" },
                            modifier = Modifier.size(54.dp).testTag("back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to home",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                actions = {
                    if (isKidsMode) {
                        Button(
                            onClick = { viewModel.toggleMode() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("mode_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChildCare,
                                contentDescription = "Toggle mode",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Go Senior",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    } else {
                        // PREMIUM HIGH CONTRAST RAJPURA LIVE WEATHER + PROFILE CIRCLE
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            // Rajpura weather status pill
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .background(Color(0xFFE2F1FF), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = weatherEmoji, fontSize = 16.sp)
                                Text(
                                    text = "Rajpura $weatherTemp",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0061A4)
                                )
                            }
                            // Circular Profile initials avatar button
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFCE93D8))
                                    .clickable { currentScreen = "profile_caregiver" }
                                    .testTag("top_profile_circle"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (seniorName.isNotBlank()) seniorName.take(1).uppercase() else "👤",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            // Siri aura pulsing animation
            val infiniteTransition = rememberInfiniteTransition(label = "siri_pulse")
            val pulseScale1 by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.45f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "scale1"
            )
            val pulseAlpha1 by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 0.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "alpha1"
            )

            val pulseScale2 by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.7f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, delayMillis = 400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "scale2"
            )
            val pulseAlpha2 by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 0.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, delayMillis = 400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "alpha2"
            )

            Box(contentAlignment = Alignment.Center) {
                if (isListening) {
                    // Outer aura 2
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .graphicsLayer {
                                scaleX = pulseScale2
                                scaleY = pulseScale2
                                alpha = pulseAlpha2
                            }
                            .background(Color(0xFFFF3B30), CircleShape)
                    )
                    // Outer aura 1
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .graphicsLayer {
                                scaleX = pulseScale1
                                scaleY = pulseScale1
                                alpha = pulseAlpha1
                            }
                            .background(Color(0xFFFF3B30), CircleShape)
                    )
                }

                // High visibility speak mic FAB for speech navigation
                FloatingActionButton(
                    onClick = { onStartVoiceListening() },
                    containerColor = if (isKidsMode) Color(0xFFFF5252) else Color(0xFFFF3B30),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(80.dp)
                        .testTag("speech_mic_fab")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicNone else Icons.Default.Mic,
                        contentDescription = "Speak Command",
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Main content screens based on state
            Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                when (screen) {
                    "home" -> HomeScreen(
                        viewModel = viewModel,
                        isKidsMode = isKidsMode,
                        onNavigate = { screenId -> currentScreen = screenId }
                    )
                    "kids_stories" -> KidsStoriesScreen(viewModel)
                    "kids_drawing" -> KidsDrawingScreen(viewModel)
                    "kids_puzzle" -> KidsPuzzleScreen(viewModel)
                    "senior_meds" -> SeniorMedsScreen(viewModel)
                    "senior_bhajans" -> SeniorBhajansScreen(viewModel)
                    "senior_prescription" -> SeniorPrescriptionScreen(viewModel)
                    "senior_companion" -> {
                        LaunchedEffect(Unit) {
                            viewModel.onEnterCompanionScreen()
                        }
                        SeniorCompanionScreen(viewModel)
                    }
                    "shor_sharaba" -> ShorSharabaScreen(viewModel)
                    "profile_caregiver" -> ProfileCaregiverScreen(viewModel, onBack = { currentScreen = "home" })
                    "daily_games" -> DailyGamesScreen(viewModel, onBack = { currentScreen = "home" })
                    "community_corner" -> CommunityCornerScreen(viewModel, onBack = { currentScreen = "home" })
                    "daily_care_serenity" -> DailyCareSerenityScreen(viewModel, onBack = { currentScreen = "home" })
                    "keeps_smiling" -> KeepsSmilingScreen(viewModel, onBack = { currentScreen = "home" })
                }
            }

            // High contrast Voice Overlay
            if (isListening || voiceStatusMessage != null) {
                VoiceOverlayPanel(
                    isListening = isListening,
                    voiceMessage = voiceStatusMessage,
                    isKidsMode = isKidsMode,
                    onCancel = { viewModel.setIsListening(false) },
                    onSubmitCommand = { text -> viewModel.processVoiceCommand(text) }
                )
            }

            // SOS Full-screen Overlay count down
            if (sosActive) {
                SOSOverlayPanel(
                    countdown = sosCountdown,
                    onCancel = { viewModel.cancelSOS() }
                )
            }
        }
    }
  }
}

// --- HOME DASHBOARD ---

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    isKidsMode: Boolean,
    onNavigate: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val isHCSupported = remember { viewModel.isHealthConnectAvailable() }
    val permissionsLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        if (grantedPermissions.containsAll(viewModel.getRequiredPermissions())) {
            viewModel.fetchHealthConnectSteps()
        }
    }

    val triggerPermissionRequest = {
        permissionsLauncher.launch(viewModel.getRequiredPermissions())
    }

    LaunchedEffect(Unit) {
        viewModel.fetchHealthConnectSteps()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Theme Header (Mode switcher toggle pill & Avatar or Premium Profile Header)
        item {
            if (isKidsMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Kids Mode Header Title
                    Text(
                        text = "🎈 Kids Play Zone",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFE91E63)
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEEF0))
                            .clickable { viewModel.toggleMode() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👵", fontSize = 20.sp)
                    }
                }
            } else {
                // PREMIUM APPLE STYLE PROFILE HEADER CARD (COMPLETELY REPLACES ZONE SWITCHERS)
                val name by viewModel.seniorName.collectAsStateWithLifecycle()
                val age by viewModel.seniorAge.collectAsStateWithLifecycle()
                val bloodGroup by viewModel.seniorBloodGroup.collectAsStateWithLifecycle()
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onNavigate("profile_caregiver") }
                        .shadow(elevation = 3.dp, shape = RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Apple style elegant avatar with high-contrast text
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2F1FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name.take(1).uppercase(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0061A4)
                            )
                        }
                        
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = name,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF191C1E)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Age: $age Yrs",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF555E71)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(Color(0xFF555E71), CircleShape)
                                )
                                Text(
                                    text = "Blood: $bloodGroup",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFBA1A1A)
                                )
                            }
                        }
                        
                        // Edit profile / view indicator icon
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "View Profile",
                            tint = Color(0xFF555E71),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // 2. Big bold heading section matching mockup h1 text-[34px]
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = if (isKidsMode) "Hello, Kiddo!" else "Hello, Dad",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color(0xFF191C1E)
                )
                Spacer(modifier = Modifier.height(2.dp))
                val sdf = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
                Text(
                    text = "It's ${sdf.format(Date())}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF555E71),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 3. Daily Care & Serenity Banner (Premium Soft Gradient Card)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(20.dp))
                    .clickable {
                        if (!isKidsMode) onNavigate("daily_care_serenity")
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isKidsMode) {
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFFFE5EC), Color(0xFFE8F0FE))
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFFFF0F2), Color(0xFFFFF7EE))
                                )
                            }
                        )
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isKidsMode) "🎈 Play & Learn" else "🌸 Daily Care & Serenity",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1C1E)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isKidsMode) "Fun stories, puzzles & drawing" else "Daily wellness suggestions, steps, tasks & breathing exercises",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF8E8E93)
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White.copy(alpha = 0.8f), CircleShape)
                                .shadow(1.dp, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isKidsMode) "🎯" else "🧘",
                                fontSize = 32.sp
                            )
                        }
                    }
                }
            }
        }

        // 4. Voice Action panel matching Apple card style
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0x0F000000), RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color(0xFFE2F1FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎙️",
                            fontSize = 26.sp
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "VOICE ACTION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp,
                            color = Color(0xFF007AFF) // System Blue
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isKidsMode) "Say \"Kahani Sunao\" or \"Game\"" else "Say \"Dawa\" or \"Bhajan\"",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1E) // Charcoal Black
                        )
                    }
                }
            }
        }

        if (!isKidsMode) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp, shape = RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0x0F000000), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "📞 QUICK DIAL (त्वरित कॉल)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = Color(0xFF1C1C1E) // Charcoal
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val context = LocalContext.current
                            val emergencyContact by viewModel.seniorEmergencyContact.collectAsStateWithLifecycle()

                            // Call Son button
                            OutlinedButton(
                                onClick = { 
                                    viewModel.speakText("[hi]बेटा को फ़ोन मिलाया जा रहा है।")
                                    makePhoneCall(context, emergencyContact)
                                },
                                border = BorderStroke(1.5.dp, Color(0xFF007AFF).copy(alpha = 0.3f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF007AFF)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("👨‍💼", fontSize = 20.sp)
                                    Text(
                                        text = "Call Son",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF007AFF)
                                    )
                                }
                            }

                            // Emergency Button
                            OutlinedButton(
                                onClick = { 
                                    viewModel.speakText("[hi]आपातकालीन नंबर पर कॉल किया जा रहा है।")
                                    makePhoneCall(context, "112")
                                },
                                border = BorderStroke(1.5.dp, Color(0xFFFF3B30).copy(alpha = 0.3f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFFF3B30)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🚨", fontSize = 20.sp)
                                    Text(
                                        text = "Emergency",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF3B30)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Steps & Hydration Panel
            item {
                val dailySteps by viewModel.dailySteps.collectAsStateWithLifecycle()
                val stepGoal by viewModel.stepGoal.collectAsStateWithLifecycle()
                val waterGlasses by viewModel.waterGlasses.collectAsStateWithLifecycle()
                val waterGoal by viewModel.waterGoal.collectAsStateWithLifecycle()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp, shape = RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0x0F000000), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "🏃‍♂️ WELLBEING & HEALTH (स्वास्थ्य)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = Color(0xFF1C1C1E) // Charcoal
                        )

                        // Steps Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🚶‍♂️ Walks Tracker",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C1C1E)
                                )
                                Text(
                                    text = "$dailySteps / $stepGoal steps logged",
                                    fontSize = 14.sp,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                            
                            // Health Connect sensor sync trigger badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isHCSupported) Color(0xFFE2F1FF) else Color(0xFFE8F5E9))
                                    .clickable {
                                        if (isHCSupported) {
                                            triggerPermissionRequest()
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                if (isHCSupported) Color(0xFF007AFF) else Color(0xFF4CAF50),
                                                CircleShape
                                            )
                                    )
                                    Text(
                                        text = if (isHCSupported) "CONNECT SENSORS" else "LIVE ACTIVE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isHCSupported) Color(0xFF007AFF) else Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFEEEEEE))

                        // Water Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "💧 Hydration Tracker",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C1C1E)
                                )
                                Text(
                                    text = "$waterGlasses / $waterGoal glasses",
                                    fontSize = 14.sp,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.decrementWater() },
                                    border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF1C1C1E)
                                    ),
                                    modifier = Modifier.size(44.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = CircleShape
                                ) {
                                    Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { viewModel.incrementWater() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                                    modifier = Modifier.size(44.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = CircleShape
                                ) {
                                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Feature Grid Cards
        item {
            if (isKidsMode) {
                // Kids Grid Layout
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FeatureCard(
                        title = "📖 Moral Stories (कहानियां)",
                        description = "Interactive audio moral tales that read themselves!",
                        color = Color(0xFFFF8A80),
                        emoji = "🎨",
                        onClick = { onNavigate("kids_stories") },
                        isKids = true
                    )
                    FeatureCard(
                        title = "🎨 Magic Canvas (चित्रकला)",
                        description = "Draw beautiful shapes and practice typing/drawing!",
                        color = Color(0xFF80DEEA),
                        emoji = "✏️",
                        onClick = { onNavigate("kids_drawing") },
                        isKids = true
                    )
                    FeatureCard(
                        title = "🧩 Jigsaw Puzzle (पहेली)",
                        description = "Slide and arrange tiles to solve funny emoji faces!",
                        color = Color(0xFFFFD180),
                        emoji = "🦁",
                        onClick = { onNavigate("kids_puzzle") },
                        isKids = true
                    )
                    FeatureCard(
                        title = "🔊 Shor Sharaba Meter (शोर मीटर)",
                        description = "Measure background noise and find if it's too loud!",
                        color = Color(0xFFA5D6A7),
                        emoji = "👂",
                        onClick = { onNavigate("shor_sharaba") },
                        isKids = true
                    )
                }
            } else {
                // Seniors High Contrast layout
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // EMERGENCY SOS RED BUTTON - MATCHING CALL FOR HELP MOCKUP CARD
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 2.dp, shape = RoundedCornerShape(32.dp))
                            .border(width = 2.dp, color = Color.White, shape = RoundedCornerShape(32.dp))
                            .clickable { viewModel.triggerSOS() }
                            .testTag("sos_button"),
                        colors = CardDefaults.cardColors(containerColor = BoldCardHelpBg),
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🚨 EMERGENCY SOS (मदद)",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFBA1A1A),
                                    lineHeight = 30.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap to call helpers & sound loud voice alarm",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BoldCardHelpText.copy(alpha = 0.9f)
                                )
                            }
                            Text(
                                text = "🆘",
                                fontSize = 48.sp,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }

                    // Medication Reminders Row
                    FeatureCard(
                        title = "💊 MEDICATION ALARMS",
                        description = "Add daily medicine timings. Alarms will read aloud when it's time!",
                        color = MaterialTheme.colorScheme.primary,
                        emoji = "⏰",
                        onClick = { onNavigate("senior_meds") },
                        isKids = false
                    )

                    // Peaceful Bhajans Card
                    FeatureCard(
                        title = "🌸 SOOTHING BHAJANS",
                        description = "Listen to Gayatri Mantra, Ram Dhyan & peaceful chants.",
                        color = MaterialTheme.colorScheme.secondary,
                        emoji = "🎼",
                        onClick = { onNavigate("senior_bhajans") },
                        isKids = false
                    )

                    // Bolne Wala Prescription - "Dr. Ki Awaaz" Card
                    FeatureCard(
                        title = "📄 DR. KI AWAAZ (प्रिस्क्रिप्शन)",
                        description = "Scan doctor's handwritten prescriptions. Reads out medicines in English, Hindi, or Punjabi!",
                        color = Color(0xFFA5D6A7),
                        emoji = "🗣️",
                        onClick = { onNavigate("senior_prescription") },
                        isKids = false
                    )

                    // Buzurgon Ka Sahil Card
                    FeatureCard(
                        title = "🤝 BUZURGON KA SAHIL (साथी)",
                        description = "Feeling lonely? Chat with your friendly companion Sahil. Speaks comforting words and stories!",
                        color = Color(0xFFCE93D8),
                        emoji = "❤️",
                        onClick = { onNavigate("senior_companion") },
                        isKids = false
                    )

                    // Shor Sharaba Meter Card
                    FeatureCard(
                        title = "🔊 SHOR SHARABA METER (शोर मीटर)",
                        description = "Measure room decibels. Generate formal noise complaints for local bodies using Gemini!",
                        color = Color(0xFF80DEEA),
                        emoji = "📣",
                        onClick = { onNavigate("shor_sharaba") },
                        isKids = false
                    )

                    // Daily Brain Games Card
                    FeatureCard(
                        title = "🧠 DAILY BRAIN GAMES (खेल)",
                        description = "Play high-contrast memory games, puzzles & trivia to keep mentally active!",
                        color = Color(0xFFFFB74D),
                        emoji = "🦁",
                        onClick = { onNavigate("daily_games") },
                        isKids = false
                    )

                    // Community Corner Card
                    FeatureCard(
                        title = "💬 COMMUNITY CORNER (चौपाल)",
                        description = "Voice-enabled social feed & peaceful audio rooms to chat with peers!",
                        color = Color(0xFFBA68C8),
                        emoji = "🗣️",
                        onClick = { onNavigate("community_corner") },
                        isKids = false
                    )
                }
            }
        }

        // Live Date & Time / Dynamic Card for Weather & Wellbeing (Cohesive secondary card)
        if (!isKidsMode) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(32.dp))
                        .border(width = 2.dp, color = Color.White, shape = RoundedCornerShape(32.dp))
                        .clickable { onNavigate("keeps_smiling") },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEEF0)), // Soft warm laughing color shade
                    shape = RoundedCornerShape(32.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Keep smiling. (हँसते रहिए)",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1C1E)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap to read & hear fun Hindi jokes!",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1A1C1E).copy(alpha = 0.7f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.SentimentVerySatisfied,
                            contentDescription = "Jokes",
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }
        }

        // Spacer for bottom voice FAB overlap protection
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// --- REUSABLE BIG CARD COMPONENT ---

@Composable
fun FeatureCard(
    title: String,
    description: String,
    color: Color, // Fallback base color
    emoji: String,
    onClick: () -> Unit,
    isKids: Boolean
) {
    // Resolve background and text color based on design guidelines
    val (containerColor, contentColor) = remember(title, isKids) {
        if (isKids) {
            when {
                title.contains("Stories", ignoreCase = true) -> BoldCardPhotosBg to BoldCardPhotosText
                title.contains("Canvas", ignoreCase = true) -> BoldCardMedicineBg to BoldCardMedicineText
                title.contains("Puzzle", ignoreCase = true) -> BoldCardBhajanBg to BoldCardBhajanText
                else -> Color(0xFFF2DFFB) to Color(0xFF251431)
            }
        } else {
            when {
                title.contains("MEDIC", ignoreCase = true) -> BoldCardMedicineBg to BoldCardMedicineText
                title.contains("BHAJAN", ignoreCase = true) -> BoldCardBhajanBg to BoldCardBhajanText
                else -> BoldCardMedicineBg to BoldCardMedicineText
            }
        }
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("feature_${title.take(6).lowercase().trim()}")
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(2.dp, Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor,
                    lineHeight = 28.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor.copy(alpha = 0.8f),
                    lineHeight = 20.sp
                )
            }
            Text(
                text = emoji,
                fontSize = 48.sp,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

// --- KIDS SECTION: MORAL STORIES SCREEN ---

@Composable
fun KidsStoriesScreen(viewModel: MainViewModel) {
    val selectedStory by viewModel.selectedStory.collectAsStateWithLifecycle()
    val isStoryPlaying by viewModel.isStoryPlaying.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick instruction banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🧚‍♀️", fontSize = 36.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Tap on any moral story book to open and speak it aloud!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // List of stories
        Text(
            text = "Select a Storybook:",
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            viewModel.stories.forEach { story ->
                val isSelected = selectedStory?.id == story.id
                Card(
                    onClick = { viewModel.selectStory(story) },
                    modifier = Modifier
                        .width(200.dp)
                        .height(130.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 4.dp else 2.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = story.illustrationEmoji, fontSize = 28.sp)
                            Text(
                                text = story.audioDuration,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Text(
                            text = story.title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 4.dp))

        // Active Story content view
        val activeStory = selectedStory ?: viewModel.stories[0]
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeStory.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(text = activeStory.illustrationEmoji, fontSize = 36.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Play Audio Reading bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isStoryPlaying) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isStoryPlaying) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                contentDescription = "Speaking status",
                                tint = if (isStoryPlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isStoryPlaying) "Reading story aloud... 🔊" else "Listen with Speech 🎧",
                                fontWeight = FontWeight.Bold,
                                color = if (isStoryPlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }

                        if (isStoryPlaying) {
                            Button(
                                onClick = { viewModel.stopStory() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Stop")
                            }
                        } else {
                            Button(
                                onClick = { viewModel.playStory() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Play Audio")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Story Content body
                Text(
                    text = activeStory.content,
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Moral highlight box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                    border = BorderStroke(2.dp, Color(0xFFFDA4AF)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "💡 STORY MORAL (सीख):",
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFE11D48),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeStory.moral,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF9F1239),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

// --- KIDS SECTION: DRAWING CANVA COMPONENT ---

enum class CanvasTemplate(val title: String, val emoji: String, val description: String) {
    BLANK("Blank Canvas", "⬜", "Draw anything!"),
    SUN("Happy Sun", "☀️", "Color a sunny day!"),
    STAR("Smiling Star", "⭐", "Trace the star!"),
    HEART("Love Heart", "❤️", "Color the sweet heart!"),
    TREE("Magic Tree", "🌳", "Trace a tall green tree!"),
    FISH("Golden Fish", "🐟", "Color a little fish!")
}

@Composable
fun KidsDrawingScreen(viewModel: MainViewModel) {
    val brushColorVal by viewModel.brushColor.collectAsStateWithLifecycle()
    val brushSizeVal by viewModel.brushSize.collectAsStateWithLifecycle()

    val currentPoints = remember { mutableStateListOf<Offset>() }
    val paths = remember { mutableStateListOf<CustomPath>() }
    val undonePaths = remember { mutableStateListOf<CustomPath>() }
    var selectedTemplate by remember { mutableStateOf(CanvasTemplate.BLANK) }

    val brushColor = Color(brushColorVal)

    val colorOptions = listOf(
        0xFFFF5252, // Coral Red
        0xFFFF9100, // Juicy Orange
        0xFFFFD700, // Sunshine Yellow
        0xFF00E676, // Froggy Green
        0xFF29B6F6, // Ocean Blue
        0xFFAB47BC, // Grape Purple
        0xFFFF4081, // Cupcake Pink
        0xFF000000  // Midnight Black
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDF9)) // Pastel warm background for kids
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Kids Header Title Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE0B2)), // Soft orange
            border = BorderStroke(2.dp, Color(0xFFFFB74D))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "🎨", fontSize = 36.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kids Zone Magic Canvas",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFE65100)
                    )
                    Text(
                        text = selectedTemplate.description,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF57C00)
                    )
                }
            }
        }

        // Horizontal Tracing Template Selector
        Text(
            text = "Select Trace Template:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFE65100)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CanvasTemplate.values().forEach { template ->
                val isSelected = selectedTemplate == template
                Card(
                    onClick = { selectedTemplate = template },
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFFFFB74D) else Color.White
                    ),
                    border = BorderStroke(
                        width = 2.dp,
                        color = if (isSelected) Color(0xFFE65100) else Color(0xFFFFE0B2)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = template.emoji, fontSize = 18.sp)
                        Text(
                            text = template.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSelected) Color.White else Color(0xFFE65100)
                        )
                    }
                }
            }
        }

        // Undo, Redo, Clear and Celebration Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo button
            Button(
                onClick = {
                    if (paths.isNotEmpty()) {
                        val last = paths.removeAt(paths.size - 1)
                        undonePaths.add(last)
                    }
                },
                enabled = paths.isNotEmpty(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF3E0)),
                border = BorderStroke(1.5.dp, Color(0xFFFFE0B2)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("↩️ Undo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
            }

            // Redo button
            Button(
                onClick = {
                    if (undonePaths.isNotEmpty()) {
                        val last = undonePaths.removeAt(undonePaths.size - 1)
                        paths.add(last)
                    }
                },
                enabled = undonePaths.isNotEmpty(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF3E0)),
                border = BorderStroke(1.5.dp, Color(0xFFFFE0B2)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("↪️ Redo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
            }

            // Clear button
            Button(
                onClick = {
                    currentPoints.clear()
                    paths.clear()
                    undonePaths.clear()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE)),
                border = BorderStroke(1.5.dp, Color(0xFFFFCDD2)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("🗑️ Clear", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
            }

            // Celebration Sound/Voice button
            Button(
                onClick = {
                    val celebrationText = listOf(
                        "[hi]अरे वाह! कितनी सुंदर चित्रकारी की है! आप तो बहुत बड़े कलाकार हैं! शाबाश!",
                        "Wow! That is an absolutely gorgeous drawing! You are a superstar artist!",
                        "[hi]ये चित्रकारी बहुत ही सुंदर है! मुझे ये रंग बहुत पसंद आए!"
                    ).random()
                    viewModel.speakText(celebrationText)
                },
                modifier = Modifier.weight(1.2f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F5E9)),
                border = BorderStroke(1.5.dp, Color(0xFFC8E6C9)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("🎉 Show Off!", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
            }
        }

        // Whiteboard drawing board
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .border(3.dp, Color(0xFFFFB74D), RoundedCornerShape(24.dp))
                .background(Color.White)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPoints.clear()
                            currentPoints.add(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentPoints.add(change.position)
                        },
                        onDragEnd = {
                            if (currentPoints.isNotEmpty()) {
                                paths.add(CustomPath(currentPoints.toList(), brushColor, brushSizeVal))
                                undonePaths.clear() // Clear redo stack on new stroke
                                currentPoints.clear()
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // 1. Draw Tracing Template Background Outline
                val templateColor = Color.LightGray.copy(alpha = 0.5f)
                val templateStroke = Stroke(
                    width = 4.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f),
                    cap = StrokeCap.Round
                )

                when (selectedTemplate) {
                    CanvasTemplate.SUN -> {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val radius = size.width.coerceAtMost(size.height) * 0.22f
                        drawCircle(
                            color = templateColor,
                            radius = radius,
                            center = Offset(centerX, centerY),
                            style = templateStroke
                        )
                        val rayLength = radius * 0.45f
                        for (i in 0 until 8) {
                            val angle = i * (Math.PI / 4)
                            val startX = (centerX + radius * Math.cos(angle)).toFloat()
                            val startY = (centerY + radius * Math.sin(angle)).toFloat()
                            val endX = (centerX + (radius + rayLength) * Math.cos(angle)).toFloat()
                            val endY = (centerY + (radius + rayLength) * Math.sin(angle)).toFloat()
                            drawLine(
                                color = templateColor,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = 4.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                        // Draw smile
                        val smilePath = Path().apply {
                            arcTo(
                                rect = androidx.compose.ui.geometry.Rect(
                                    left = centerX - radius * 0.4f,
                                    top = centerY - radius * 0.4f,
                                    right = centerX + radius * 0.4f,
                                    bottom = centerY + radius * 0.4f
                                ),
                                startAngleDegrees = 30f,
                                sweepAngleDegrees = 120f,
                                forceMoveTo = false
                            )
                        }
                        drawPath(path = smilePath, color = templateColor, style = templateStroke)
                        drawCircle(color = templateColor, radius = 5.dp.toPx(), center = Offset(centerX - radius * 0.35f, centerY - radius * 0.2f))
                        drawCircle(color = templateColor, radius = 5.dp.toPx(), center = Offset(centerX + radius * 0.35f, centerY - radius * 0.2f))
                    }
                    CanvasTemplate.STAR -> {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val outerRadius = size.width.coerceAtMost(size.height) * 0.3f
                        val innerRadius = outerRadius * 0.45f
                        val starPath = Path().apply {
                            for (i in 0 until 10) {
                                val angle = i * Math.PI / 5 - Math.PI / 2
                                val r = if (i % 2 == 0) outerRadius else innerRadius
                                val x = (centerX + r * Math.cos(angle)).toFloat()
                                val y = (centerY + r * Math.sin(angle)).toFloat()
                                if (i == 0) moveTo(x, y) else lineTo(x, y)
                            }
                            close()
                        }
                        drawPath(path = starPath, color = templateColor, style = templateStroke)
                    }
                    CanvasTemplate.HEART -> {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val sizeVal = size.width.coerceAtMost(size.height) * 0.45f
                        val heartPath = Path().apply {
                            moveTo(centerX, centerY - sizeVal * 0.25f)
                            cubicTo(
                                centerX - sizeVal * 0.5f, centerY - sizeVal * 0.7f,
                                centerX - sizeVal, centerY - sizeVal * 0.2f,
                                centerX, centerY + sizeVal * 0.5f
                            )
                            cubicTo(
                                centerX + sizeVal, centerY - sizeVal * 0.2f,
                                centerX + sizeVal * 0.5f, centerY - sizeVal * 0.7f,
                                centerX, centerY - sizeVal * 0.25f
                            )
                        }
                        drawPath(path = heartPath, color = templateColor, style = templateStroke)
                    }
                    CanvasTemplate.TREE -> {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val sizeVal = size.width.coerceAtMost(size.height) * 0.45f
                        val trunkPath = Path().apply {
                            moveTo(centerX - sizeVal * 0.12f, centerY + sizeVal * 0.45f)
                            lineTo(centerX - sizeVal * 0.12f, centerY)
                            lineTo(centerX + sizeVal * 0.12f, centerY)
                            lineTo(centerX + sizeVal * 0.12f, centerY + sizeVal * 0.45f)
                            close()
                        }
                        drawPath(path = trunkPath, color = templateColor, style = templateStroke)
                        drawCircle(color = templateColor, radius = sizeVal * 0.28f, center = Offset(centerX, centerY - sizeVal * 0.15f), style = templateStroke)
                        drawCircle(color = templateColor, radius = sizeVal * 0.25f, center = Offset(centerX - sizeVal * 0.22f, centerY - sizeVal * 0.05f), style = templateStroke)
                        drawCircle(color = templateColor, radius = sizeVal * 0.25f, center = Offset(centerX + sizeVal * 0.22f, centerY - sizeVal * 0.05f), style = templateStroke)
                    }
                    CanvasTemplate.FISH -> {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val sizeVal = size.width.coerceAtMost(size.height) * 0.45f
                        val fishPath = Path().apply {
                            moveTo(centerX - sizeVal * 0.45f, centerY)
                            cubicTo(
                                centerX - sizeVal * 0.2f, centerY - sizeVal * 0.3f,
                                centerX + sizeVal * 0.2f, centerY - sizeVal * 0.3f,
                                centerX + sizeVal * 0.3f, centerY
                            )
                            lineTo(centerX + sizeVal * 0.5f, centerY - sizeVal * 0.25f)
                            lineTo(centerX + sizeVal * 0.42f, centerY)
                            lineTo(centerX + sizeVal * 0.5f, centerY + sizeVal * 0.25f)
                            lineTo(centerX + sizeVal * 0.3f, centerY)
                            cubicTo(
                                centerX + sizeVal * 0.2f, centerY + sizeVal * 0.3f,
                                centerX - sizeVal * 0.2f, centerY + sizeVal * 0.3f,
                                centerX - sizeVal * 0.45f, centerY
                            )
                            close()
                        }
                        drawPath(path = fishPath, color = templateColor, style = templateStroke)
                        drawCircle(color = templateColor, radius = 5.dp.toPx(), center = Offset(centerX - sizeVal * 0.28f, centerY - sizeVal * 0.05f))
                    }
                    else -> {}
                }

                // 2. Draw historical saved strokes
                paths.forEach { drawPath ->
                    val pathObj = Path().apply {
                        if (drawPath.points.isNotEmpty()) {
                            moveTo(drawPath.points.first().x, drawPath.points.first().y)
                            for (i in 1 until drawPath.points.size) {
                                lineTo(drawPath.points[i].x, drawPath.points[i].y)
                            }
                        }
                    }
                    drawPath(
                        path = pathObj,
                        color = drawPath.color,
                        style = Stroke(
                            width = drawPath.strokeWidth,
                            cap = StrokeCap.Round
                        )
                    )
                }

                // 3. Draw current active stroke
                if (currentPoints.isNotEmpty()) {
                    val activePath = Path().apply {
                        moveTo(currentPoints.first().x, currentPoints.first().y)
                        for (i in 1 until currentPoints.size) {
                            lineTo(currentPoints[i].x, currentPoints[i].y)
                        }
                    }
                    drawPath(
                        path = activePath,
                        color = brushColor,
                        style = Stroke(
                            width = brushSizeVal,
                            cap = StrokeCap.Round
                        )
                    )
                }
            }

            // Simple text guide overlay
            if (paths.isEmpty() && currentPoints.isEmpty()) {
                Text(
                    text = "🎨 Trace & Draw Here!",
                    color = Color.Gray.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // Palette selector & Brush size controller
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(2.dp, Color(0xFFFFE0B2))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Vibrant color list
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pick A Color:",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = Color(0xFFE65100)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorOptions.forEach { colHex ->
                        val colObj = Color(colHex)
                        val isSelected = brushColorVal == colHex
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colObj)
                                .border(
                                    width = if (isSelected) 4.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFE65100) else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.selectBrushColor(colHex) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Brush thickness slider & preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Brush Size: ",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = Color(0xFFE65100),
                        modifier = Modifier.width(80.dp)
                    )
                    Slider(
                        value = brushSizeVal,
                        onValueChange = { viewModel.setBrushSize(it) },
                        valueRange = 4f..40f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFB74D),
                            activeTrackColor = Color(0xFFFFB74D)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Dynamic brush size preview circle!
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(brushSizeVal.coerceIn(4f, 32f).dp)
                                .clip(CircleShape)
                                .background(brushColor)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}

// --- KIDS SECTION: PICTURE JIGSAW PUZZLE ---

@Composable
fun KidsPuzzleScreen(viewModel: MainViewModel) {
    val puzzleTiles by viewModel.puzzleTiles.collectAsStateWithLifecycle()
    val puzzleSolved by viewModel.puzzleSolved.collectAsStateWithLifecycle()
    
    var selectedTileIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title block
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🦁 Shape Slide Puzzle 🦁",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap a block, then tap another to swap them. Put the animal list in the correct order to win!",
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Win Congratulatory pop banner
        if (puzzleSolved) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)),
                border = BorderStroke(3.dp, Color(0xFF10B981)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎉 YOU WON THE PUZZLE! 🎉",
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = Color(0xFF065F46)
                    )
                    Text(
                        text = "Great job! Your brain is super smart!",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.resetPuzzle() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Play Again", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Puzzle grid area
        Card(
            modifier = Modifier
                .size(320.dp)
                .border(4.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(puzzleTiles) { index, tile ->
                    val isSelected = selectedTileIndex == index
                    Card(
                        onClick = {
                            if (!puzzleSolved) {
                                if (selectedTileIndex == null) {
                                    selectedTileIndex = index
                                } else {
                                    viewModel.swapTiles(selectedTileIndex!!, index)
                                    selectedTileIndex = null
                                }
                            }
                        },
                        modifier = Modifier
                            .aspectRatio(1f)
                            .testTag("puzzle_tile_${index}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(tile.colorHex)),
                        border = BorderStroke(
                            width = if (isSelected) 4.dp else 1.dp,
                            color = if (isSelected) Color.White else Color.Black.copy(alpha = 0.15f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = tile.currentEmoji, fontSize = 36.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                // Show diagnostic order hint
                                Text(
                                    text = "Pos: ${tile.originalIndex + 1}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Direct reset trigger
        OutlinedButton(
            onClick = { viewModel.resetPuzzle() },
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Shuffle")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Shuffle Board", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}

// --- SENIORS SECTION: MEDICATION SCHEDULER ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeniorMedsScreen(viewModel: MainViewModel) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }

    // Medication inputs
    var medName by remember { mutableStateOf("") }
    var medDosage by remember { mutableStateOf("1 Tablet") }
    var medTimeHour by remember { mutableStateOf(8) }
    var medTimeMinute by remember { mutableStateOf(30) }
    var medTimeAmPm by remember { mutableStateOf("AM") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and add action trigger
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Medication Reminders",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(
                onClick = { showAddSheet = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(54.dp).testTag("add_reminder_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add medicine", modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Divider()

        // List of timers
        if (reminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = "No medicines",
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(80.dp)
                    )
                    Text(
                        text = "No Medication Scheduled",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Tap the 'Add' button above to configure your medication timings.",
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reminders, key = { it.id }) { reminder ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 2.dp,
                                color = if (reminder.isActive) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (reminder.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Outlined.Timer,
                                    contentDescription = "Reminder Time",
                                    tint = if (reminder.isActive) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = reminder.medicineName,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (reminder.isActive) MaterialTheme.colorScheme.onSurface else Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${reminder.dosage}  •  ",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = reminder.time,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (reminder.isActive) MaterialTheme.colorScheme.primary else Color.Gray
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Enable / Disable alarm switch (large touch target)
                                Switch(
                                    checked = reminder.isActive,
                                    onCheckedChange = { viewModel.toggleReminderActive(reminder) },
                                    modifier = Modifier.scale(1.2f).testTag("reminder_switch_${reminder.id}")
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                IconButton(
                                    onClick = { viewModel.deleteReminder(reminder) },
                                    modifier = Modifier.size(54.dp).testTag("delete_reminder_${reminder.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete Reminder",
                                        tint = SOSRed,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }

    // Modal adding sheet dialog
    if (showAddSheet) {
        Dialog(onDismissRequest = { showAddSheet = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(4.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "New Reminder timing",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = medName,
                        onValueChange = { medName = it },
                        label = { Text("Medicine Name (दवा का नाम)", fontSize = 16.sp) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                        modifier = Modifier.fillMaxWidth().testTag("med_name_input")
                    )

                    // Dosage options buttons
                    Text(text = "Dosage Amount:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("1 Pill", "2 Pills", "1 Spoon", "Drops").forEach { amt ->
                            val isSelected = medDosage == amt
                            Button(
                                onClick = { medDosage = amt },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = amt, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    // Time selector
                    Text(text = "Select timing alarm:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hour spinner
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Hr:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 4.dp))
                            Card(onClick = { medTimeHour = if (medTimeHour >= 12) 1 else medTimeHour + 1 }) {
                                Text(
                                    text = String.format("%02d", medTimeHour),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        // Minute spinner
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Min:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 4.dp))
                            Card(onClick = { medTimeMinute = (medTimeMinute + 5) % 60 }) {
                                Text(
                                    text = String.format("%02d", medTimeMinute),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        // AM PM
                        Card(onClick = { medTimeAmPm = if (medTimeAmPm == "AM") "PM" else "AM" }) {
                            Text(
                                text = medTimeAmPm,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dialog Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddSheet = false },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f).height(54.dp)
                        ) {
                            Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                if (medName.isNotBlank()) {
                                    val formattedTime = String.format("%02d:%02d %s", medTimeHour, medTimeMinute, medTimeAmPm)
                                    viewModel.addReminder(medName, medDosage, formattedTime)
                                    medName = ""
                                    showAddSheet = false
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f).height(54.dp).testTag("save_reminder_button")
                        ) {
                            Text("Save Timing", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- SENIORS SECTION: BHAJANS & DEVOTIONAL CHANTS ---

@Composable
fun SeniorBhajansScreen(viewModel: MainViewModel) {
    val currentQuote by viewModel.currentQuote.collectAsStateWithLifecycle()
    val playingBhajan by viewModel.playingBhajan.collectAsStateWithLifecycle()
    val bhajanProgress by viewModel.bhajanProgress.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Daily Peaceful Quote card with TTS audio trigger
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🌞 Peace Message of the Day (सुविचार)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = { viewModel.speakText("Peace message by ${currentQuote.author}: ${currentQuote.text}") },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak quote aloud",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "\"${currentQuote.text}\"",
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "— ${currentQuote.author}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    TextButton(onClick = { viewModel.nextQuote() }) {
                        Text("Next thought ➔", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Divider()

        // Active Player Panel (Animated Retro Vinyl visualizer)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 3.dp,
                    color = if (playingBhajan != null) MaterialTheme.colorScheme.secondary else Color.Gray.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (playingBhajan != null) {
                    Text(
                        text = "🎼 NOW PLAYING 🎼",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = playingBhajan!!.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Retro glowing bar visualizer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "audio_bars")
                        for (i in 0..7) {
                            val duration = 400 + (i * 120)
                            val barHeight by infiniteTransition.animateFloat(
                                initialValue = 8f,
                                targetValue = 40f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(duration, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "bar_$i"
                            )
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height(barHeight.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.secondary,
                                                MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { bhajanProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.stopBhajan() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Pause, contentDescription = "Stop", modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pause Bhajan", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    // Live Karaoke Lyrics Card
                    val lines = playingBhajan!!.lyrics.split("\n").filter { it.isNotBlank() }
                    val currentLineIndex = if (lines.isNotEmpty()) {
                        (bhajanProgress * lines.size).toInt().coerceIn(0, lines.size - 1)
                    } else 0

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "📖 Live Karaoke Lyrics (भजन के बोल):",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFE65100),
                        modifier = Modifier.align(Alignment.Start)
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp, max = 320.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF5)),
                        border = BorderStroke(2.dp, Color(0xFFFFB74D))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            lines.forEachIndexed { index, line ->
                                val isActive = index == currentLineIndex
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isActive) Color(0xFFFFF3E0) else Color.Transparent
                                    ),
                                    border = if (isActive) BorderStroke(1.5.dp, Color(0xFFFF9100)) else null
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isActive) {
                                            Text("🌸 ", fontSize = 22.sp)
                                        } else {
                                            Text("🕊️ ", fontSize = 16.sp, color = Color.Gray.copy(alpha = 0.5f))
                                        }
                                        Text(
                                            text = line,
                                            fontSize = if (isActive) 20.sp else 16.sp,
                                            fontWeight = if (isActive) FontWeight.Black else FontWeight.Bold,
                                            color = if (isActive) Color(0xFFE65100) else Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "No music active",
                        tint = Color.LightGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Select a peace chant below to play",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            }
        }

        // List of Bhajans
        Text(
            text = "Select Devotional Chants:",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            viewModel.bhajans.forEach { bhajan ->
                val isPlaying = playingBhajan?.id == bhajan.id
                Card(
                    onClick = { viewModel.playBhajan(bhajan) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPlaying) MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (isPlaying) 3.dp else 1.dp,
                        color = if (isPlaying) MaterialTheme.colorScheme.secondary else Color.LightGray.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(text = "🌸", fontSize = 32.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = bhajan.title,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = bhajan.description,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.playBhajan(bhajan) },
                            modifier = Modifier.size(54.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                                contentDescription = "Play/pause bhajan",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

// --- OVERLAYS AND POPUPS ---

@Composable
fun VoiceOverlayPanel(
    isListening: Boolean,
    voiceMessage: String?,
    isKidsMode: Boolean,
    onCancel: () -> Unit,
    onSubmitCommand: (String) -> Unit
) {
    var manualCommandText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { /* Block clicks through */ },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .testTag("voice_assistant_overlay"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(4.dp, MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎙️ Voice Assistant",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onCancel) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(28.dp))
                    }
                }

                Divider()

                if (isListening) {
                    // Pulsing microphone ring
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse_mic")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Listening...",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Text(
                        text = "Listening to your voice...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Try saying: \"Kahani\", \"Dawa\", or \"Bhajan\"",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                if (voiceMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    ) {
                        Text(
                            text = voiceMessage,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Divider()

                // Keyboard manual fallback input (Crucial accessibility fallback!)
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Or type command manually:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualCommandText,
                            onValueChange = { manualCommandText = it },
                            placeholder = { Text("e.g. story, medicine, bhajans") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_voice_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Button(
                            onClick = {
                                if (manualCommandText.isNotBlank()) {
                                    onSubmitCommand(manualCommandText)
                                    manualCommandText = ""
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(54.dp)
                        ) {
                            Text("Go")
                        }
                    }
                }
            }
        }
    }
}

// --- SOS FULL-SCREEN ALARM WINDOW ---

@Composable
fun SOSOverlayPanel(
    countdown: Int?,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SOSRed),
        contentAlignment = Alignment.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = FastOutLinearInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "sos_scale"
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Alert",
                    tint = SOSRed,
                    modifier = Modifier.size(80.dp)
                )
            }

            Text(
                text = "EMERGENCY ALERT",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            if (countdown != null && countdown > 0) {
                Text(
                    text = "Alerting helpers in:",
                    fontSize = 20.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "$countdown",
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            } else {
                Text(
                    text = "🚨 ALARMS ACTIVE 🚨\nSPOKEN ASSISTANCE BROADCASTING",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Yellow,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
                )

                // Call relative presets
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Quick Contact Helpers:", fontWeight = FontWeight.Black, fontSize = 16.sp, color = SOSRed)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = SOSRed)) {
                                Icon(imageVector = Icons.Default.Phone, contentDescription = "Call")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call Doctor")
                            }
                            Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))) {
                                Icon(imageVector = Icons.Default.FamilyRestroom, contentDescription = "Call Son")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call Son")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = SOSRed),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("cancel_sos_button")
            ) {
                Text("CANCEL ALARM", fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

// --- MEDICATION FULL-SCREEN IN-APP POPUP ---

@Composable
fun AlarmInterruptDialog(
    reminder: MedicationReminder,
    onTaken: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(6.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(28.dp))
                .testTag("alarm_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "⏰ MEDICATION ALARM ⏰",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Divider()

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AlarmOn,
                        contentDescription = "Pill active",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(60.dp)
                    )
                }

                Text(
                    text = "Time to take medicine!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = reminder.medicineName,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Dosage: ${reminder.dosage}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Divider()

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onTaken,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .testTag("taken_button")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Taken", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("I HAVE TAKEN IT", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text("Snooze Alarm", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// --- SCREEN 1: DR. KI AWAAZ (Prescription Scanner) ---
// ==========================================
@Composable
fun SeniorPrescriptionScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val prescriptionLoading by viewModel.prescriptionLoading.collectAsStateWithLifecycle()
    val prescriptionResult by viewModel.prescriptionResult.collectAsStateWithLifecycle()
    val prescriptionMedicines by viewModel.prescriptionMedicines.collectAsStateWithLifecycle()
    val engSpeech by viewModel.prescriptionSpeechEnglish.collectAsStateWithLifecycle()
    val hinSpeech by viewModel.prescriptionSpeechHindi.collectAsStateWithLifecycle()
    val punSpeech by viewModel.prescriptionSpeechPunjabi.collectAsStateWithLifecycle()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Helper to convert Bitmap to Base64
    fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            capturedBitmap = null
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bitmap?.let { b ->
                    viewModel.scanPrescription(b.toBase64())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            capturedBitmap = it
            selectedImageUri = null
            viewModel.scanPrescription(it.toBase64())
        }
    }

    // High Contrast Scrollable Layout
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "📄 डॉक्टर की आवाज़",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF191C1E),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Scan prescription handwriting & listen to it clearly!",
                fontSize = 16.sp,
                color = Color(0xFF555E71),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }

        // Image Container with Scanning Sweep Animation
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .border(2.dp, Color(0xFFD1E4FF), RoundedCornerShape(24.dp))
                    .shadow(4.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        capturedBitmap != null -> {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Scanned prescription",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        selectedImageUri != null -> {
                            val painter = coil.compose.rememberAsyncImagePainter(selectedImageUri)
                            Image(
                                painter = painter,
                                contentDescription = "Scanned prescription",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        else -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(text = "💊", fontSize = 64.sp)
                                Text(
                                    text = "No Prescription Photo",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF555E71)
                                )
                            }
                        }
                    }

                    // Scan Laser Line Sweep animation
                    if (prescriptionLoading) {
                        val infiniteTransition = rememberInfiniteTransition(label = "Laser")
                        val progress by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "LaserLine"
                        )

                        // Draw a red scanning line sweeping vertically
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.02f)
                                .align(Alignment.TopCenter)
                                .offset(y = (260.dp * progress))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Red.copy(alpha = 0.2f), Color.Red, Color.Red.copy(alpha = 0.2f))
                                    )
                                )
                        )
                    }
                }
            }
        }

        // Actions Row
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { cameraLauncher.launch(null) },
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .testTag("prescription_camera_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Camera", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Take Photo", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .testTag("prescription_gallery_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2F1FF)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color(0xFF001E30))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF001E30))
                    }
                }

                // Demo Prescription Button for sterile environments
                Button(
                    onClick = {
                        capturedBitmap = null
                        selectedImageUri = null
                        // Tiny 1x1 transparent PNG representation in Base64
                        val dummyBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
                        viewModel.scanPrescription(dummyBase64)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("prescription_demo_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E1EC)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "🧪 Try Demo Prescription",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191C1E)
                    )
                }
            }
        }

        // Status or loading spinner
        item {
            if (prescriptionLoading) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(color = Color(0xFF0061A4))
                    Text(
                        text = "Reading doctor's handwriting via Gemini...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0061A4)
                    )
                }
            } else if (prescriptionResult != null) {
                Text(
                    text = prescriptionResult!!,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        // Output Panel (Detected Medicines)
        if (prescriptionMedicines.isNotEmpty()) {
            item {
                Text(
                    text = "📋 detected medicines (दवाइयाँ)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF191C1E),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            items(prescriptionMedicines) { medicine ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, Color(0xFFE1E2EC), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = "💊", fontSize = 32.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = medicine.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF001E30)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    text = "Dosage: ${medicine.dosage}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF44474E)
                                )
                                Text(
                                    text = "Timing: ${medicine.timing}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0061A4)
                                )
                            }
                        }
                    }
                }
            }

            // Loud Audio Readout Section (Multilingual Buttons)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .border(2.dp, Color(0xFFFFD9E2), RoundedCornerShape(28.dp))
                        .shadow(2.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBFF))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🔊 Badi Awaaz Me Sunein (बड़ी आवाज़ में सुनें)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFBA1A1A)
                        )
                        Divider()

                        // Hindi Speak Button
                        Button(
                            onClick = { viewModel.speakPrescriptionHindi() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD9E2)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(text = "📢 ", fontSize = 20.sp)
                            Text(
                                text = "Listen in HINDI (हिंदी में सुनें)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF410012)
                            )
                        }

                        // Punjabi Speak Button
                        Button(
                            onClick = { viewModel.speakPrescriptionPunjabi() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD1E4FF)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(text = "📢 ", fontSize = 20.sp)
                            Text(
                                text = "Listen in PUNJABI (ਪੰਜਾਬੀ ਵਿੱਚ ਸੁਣੋ)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF001D36)
                            )
                        }

                        // English Speak Button
                        Button(
                            onClick = { viewModel.speakPrescriptionEnglish() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E1EC)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(text = "📢 ", fontSize = 20.sp)
                            Text(
                                text = "Listen in ENGLISH",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF191C1E)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// ==========================================
// --- SCREEN 2: BUZURGON KA SAHIL (Elder Companion) ---
// ==========================================
@Composable
fun SeniorCompanionScreen(viewModel: MainViewModel) {
    val companionLoading by viewModel.companionLoading.collectAsStateWithLifecycle()
    val chatHistory by viewModel.companionChatHistory.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }

    // Pulsing Loneliness Button animation
    val infiniteTransition = rememberInfiniteTransition(label = "Pulsing")
    val scaleFactor by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutBack),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LonelyPulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F5F7))
    ) {
        // Comforting Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFCE93D8))
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🤝", fontSize = 36.sp)
                    Column {
                        Text(
                            text = "बुजुर्गों का साहिल",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF251431)
                        )
                        Text(
                            text = "Sahil - Your AI Friend & Guardian Companion",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF251431).copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Chat Conversation Frame
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Heartwarming "Feeling Lonely" button
            Card(
                onClick = { viewModel.triggerFeelingLonely() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .scale(scaleFactor)
                    .shadow(elevation = 3.dp, shape = RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD9E2))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "❤️", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "अकेलापन लग रहा है? (Tap Sahil)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF410012)
                    )
                }
            }

            // Message Chat Bubble Window
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chatHistory) { msg ->
                    val isCompanion = msg.sender == "companion"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isCompanion) Arrangement.Start else Arrangement.End
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .shadow(1.dp, RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp,
                                bottomStart = if (isCompanion) 4.dp else 20.dp,
                                bottomEnd = if (isCompanion) 20.dp else 4.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCompanion) Color.White else Color(0xFFE2F1FF)
                            ),
                            border = BorderStroke(1.5.dp, if (isCompanion) Color(0xFFE1E2EC) else Color(0xFFD1E4FF))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (isCompanion) "Sahil (साहिल) 🤝" else "You (आप) 👤",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isCompanion) Color(0xFF7A0099) else Color(0xFF00497E)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = msg.text,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF191C1E)
                                )
                            }
                        }
                    }
                }

                if (companionLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Card(
                                modifier = Modifier.padding(8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text("Sahil is thinking...", fontSize = 14.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            // Quick helpers cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "✨ सुविचार सुनाओ" to "मुझे कोई प्यारा सुविचार सुनाओ",
                    "📖 कहानी सुनाओ" to "मुझे एक छोटी नैतिक कहानी सुनाओ",
                    "🌿 मन अशांत है" to "मेरा मन थोड़ा अशांत है, कुछ शांति की बातें बोलो",
                    "💬 बातें करो" to "साहिल, तुम कैसे हो? मुझसे थोड़ी बात करो"
                ).forEach { (label, prompt) ->
                    SuggestionChip(
                        onClick = {
                            textInput = prompt
                            viewModel.sendCompanionMessage(prompt)
                            textInput = ""
                        },
                        label = { Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Message Input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Sahil se baat karein...", fontSize = 16.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Button(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendCompanionMessage(textInput)
                            textInput = ""
                        }
                    },
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCE93D8))
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color(0xFF251431),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// --- SCREEN 3: SHOR SHARABA METER (Noise Meter) ---
// ==========================================
@Composable
fun ShorSharabaScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentDb by viewModel.currentDecibel.collectAsStateWithLifecycle()
    val isNoiseTracking by viewModel.isNoiseTracking.collectAsStateWithLifecycle()
    val complaintDraft by viewModel.noiseComplaintDraft.collectAsStateWithLifecycle()
    val isGeminiLoading by viewModel.companionLoading.collectAsStateWithLifecycle()

    // Request dynamic permission
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startNoiseTracking()
        } else {
            Toast.makeText(context, "Using simulated Sandbox Mode since mic permission was denied.", Toast.LENGTH_LONG).show()
            viewModel.startNoiseTracking() // Falls back to sandbox/simulation automatically
        }
    }

    // Launch Noise Meter tracking automatically when screen mounts
    DisposableEffect(Unit) {
        recordPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        onDispose {
            viewModel.stopNoiseTracking()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F3F5))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "🔊 शोर शराबा मीटर",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF191C1E)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Decibel sound meter & Gemini complaint assistant",
                fontSize = 15.sp,
                color = Color(0xFF555E71),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        }

        // Beautiful Rotating Needle Canvas Gauge
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .border(2.dp, Color.White, RoundedCornerShape(28.dp))
                    .shadow(3.dp, RoundedCornerShape(28.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(28.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        val center = Offset(size.width / 2, size.height * 0.85f)
                        val radius = size.width * 0.4f

                        // Draw background colored arcs (Green, Yellow, Red)
                        drawArc(
                            color = Color(0xFFA5D6A7), // Green Safe
                            startAngle = 180f,
                            sweepAngle = 60f,
                            useCenter = false,
                            style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                            topLeft = Offset(center.x - radius, center.y - radius)
                        )

                        drawArc(
                            color = Color(0xFFFFD180), // Yellow Moderate
                            startAngle = 240f,
                            sweepAngle = 60f,
                            useCenter = false,
                            style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                            topLeft = Offset(center.x - radius, center.y - radius)
                        )

                        drawArc(
                            color = Color(0xFFFF8A80), // Red Danger
                            startAngle = 300f,
                            sweepAngle = 60f,
                            useCenter = false,
                            style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                            topLeft = Offset(center.x - radius, center.y - radius)
                        )

                        // Calculate needle angle based on dB (30 to 100)
                        val angleDb = currentDb.coerceIn(30f, 100f)
                        val anglePercent = (angleDb - 30f) / 70f
                        val needleAngleDeg = 180f + (anglePercent * 180f)
                        val needleRad = Math.toRadians(needleAngleDeg.toDouble())

                        val needleLength = radius * 0.85f
                        val needleEnd = Offset(
                            (center.x + needleLength * Math.cos(needleRad)).toFloat(),
                            (center.y + needleLength * Math.sin(needleRad)).toFloat()
                        )

                        // Draw Needle Line
                        drawLine(
                            color = Color(0xFF191C1E),
                            start = center,
                            end = needleEnd,
                            strokeWidth = 6.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Draw Center Pin
                        drawCircle(
                            color = Color(0xFF0061A4),
                            radius = 12.dp.toPx(),
                            center = center
                        )
                    }

                    // Display decibel text
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${String.format("%.1f", currentDb)} dB",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF001E30)
                        )
                        Text(
                            text = when {
                                currentDb < 50f -> "💚 SAFE & SILENT"
                                currentDb < 75f -> "💛 MODERATE CHATTER"
                                else -> "❤️ TO LOUD & DANGEROUS!"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                currentDb < 50f -> Color(0xFF2E7D32)
                                currentDb < 75f -> Color(0xFFE65100)
                                else -> Color(0xFFC62828)
                            }
                        )
                    }
                }
            }
        }

        // Sandbox Simulator (Ambient Sandbox Exploration)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFFE1E2EC), RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🎛️ Noise Level Sandbox Simulator",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF44474E)
                    )
                    Slider(
                        value = currentDb,
                        onValueChange = { viewModel.setManualDecibelValueForTesting(it) },
                        valueRange = 30f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF0061A4),
                            activeTrackColor = Color(0xFF0061A4)
                        ),
                        modifier = Modifier.testTag("noise_slider")
                    )
                    Text(
                        text = "Drag slider to test how different decibel volumes affect kids sleep or grandparents' stress.",
                        fontSize = 12.sp,
                        color = Color(0xFF74777F)
                    )
                }
            }
        }

        // Real Environment Explanation Box
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFFFFD180).copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF6)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "💡", fontSize = 32.sp)
                    Column {
                        Text(
                            text = "What does this sound mean?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF191C1E)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                currentDb < 45f -> "This is like a quiet library. Extremely peaceful and absolutely safe for babies and elders to rest smoothly."
                                currentDb < 60f -> "This is like a quiet home or moderate chatter. Completely comfortable, perfect environment for daily operations."
                                currentDb < 75f -> "This is like a vacuum cleaner or normal restaurant. Hard for kids to sleep comfortably, and might trigger mild anxiety for seniors."
                                else -> "This is like heavy traffic, truck honking, or street construction. Extremely disturbing! Can trigger blood pressure spikes in seniors and wake sleeping children instantly."
                            },
                            fontSize = 14.sp,
                            color = Color(0xFF44474E)
                        )
                    }
                }
            }
        }

        // Gemini Complaint Action Box
        item {
            Button(
                onClick = { viewModel.generateNoiseComplaintDraft() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("complaint_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF191C1E)),
                shape = RoundedCornerShape(20.dp),
                enabled = !isGeminiLoading
            ) {
                if (isGeminiLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Drafting via Gemini...", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.BorderColor, contentDescription = "Draft Complaint", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generate Noise Complaint Draft",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }

        // Complaint Output Draft Box
        if (complaintDraft != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, Color(0xFFE1E2EC), RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📝 Generated Complaint Letter",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF001E30)
                            )
                            IconButton(
                                onClick = {
                                    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clipData = android.content.ClipData.newPlainText("Noise Complaint", complaintDraft)
                                    clipboardManager.setPrimaryClip(clipData)
                                    Toast.makeText(context, "Complaint copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                        Text(
                            text = complaintDraft!!,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF191C1E),
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// --- NEW SCREENS FOR PROFILE/CAREGIVER, DAILY BRAIN GAMES, AND COMMUNITY CORNER ---

@Composable
fun ProfileCaregiverScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val seniorName by viewModel.seniorName.collectAsStateWithLifecycle()
    val seniorAge by viewModel.seniorAge.collectAsStateWithLifecycle()
    val seniorBloodGroup by viewModel.seniorBloodGroup.collectAsStateWithLifecycle()
    val seniorEmergencyContact by viewModel.seniorEmergencyContact.collectAsStateWithLifecycle()

    val caregiverLinked by viewModel.caregiverLinked.collectAsStateWithLifecycle()
    val caregiverName by viewModel.caregiverName.collectAsStateWithLifecycle()
    val caregiverSyncMeds by viewModel.caregiverSyncMeds.collectAsStateWithLifecycle()
    val caregiverSyncSOS by viewModel.caregiverSyncSOS.collectAsStateWithLifecycle()
    val caregiverSyncLocation by viewModel.caregiverSyncLocation.collectAsStateWithLifecycle()

    var tempName by remember { mutableStateOf(seniorName) }
    var tempAge by remember { mutableStateOf(seniorAge.toString()) }
    var tempBlood by remember { mutableStateOf(seniorBloodGroup) }
    var tempEmergency by remember { mutableStateOf(seniorEmergencyContact) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Back Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back", modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "👤 PROFILE & CAREGIVER",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF191C1E)
                )
            }
        }

        // Senior Profile Details Form
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFFD1E4FF), RoundedCornerShape(28.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "👴 Senior User Details (वरिष्ठ नागरिक)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0061A4)
                    )

                    // Name
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Name (नाम)", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    )

                    // Age & Blood Group Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = tempAge,
                            onValueChange = { tempAge = it },
                            label = { Text("Age (उम्र)", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        )
                        OutlinedTextField(
                            value = tempBlood,
                            onValueChange = { tempBlood = it },
                            label = { Text("Blood Group (रक्त समूह)", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        )
                    }

                    // Emergency Contact
                    OutlinedTextField(
                        value = tempEmergency,
                        onValueChange = { tempEmergency = it },
                        label = { Text("Son's Phone / User Number (बेटा का फ़ोन नंबर)", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    )

                    Button(
                        onClick = {
                            viewModel.updateProfile(
                                tempName,
                                tempAge.toIntOrNull() ?: 72,
                                tempBlood,
                                tempEmergency
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("💾 Save Profile (सुरक्षित करें)", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }
        }

        // Caregiver Sync Area
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFFFFD9E2), RoundedCornerShape(28.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "📲 Caregiver Remote Sync (सहयोगी सिंक)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFBA1A1A)
                    )

                    // QR / Pairing Code Simulation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFF1F2), RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Canvas(modifier = Modifier.size(80.dp)) {
                            drawRect(color = Color.Black, size = androidx.compose.ui.geometry.Size(30f, 30f), topLeft = Offset(0f, 0f))
                            drawRect(color = Color.Black, size = androidx.compose.ui.geometry.Size(30f, 30f), topLeft = Offset(130f, 0f))
                            drawRect(color = Color.Black, size = androidx.compose.ui.geometry.Size(30f, 30f), topLeft = Offset(0f, 130f))
                            drawRect(color = Color.Black, size = androidx.compose.ui.geometry.Size(20f, 20f), topLeft = Offset(50f, 50f))
                            drawRect(color = Color.Black, size = androidx.compose.ui.geometry.Size(40f, 15f), topLeft = Offset(90f, 90f))
                            drawRect(color = Color.Black, size = androidx.compose.ui.geometry.Size(15f, 40f), topLeft = Offset(30f, 90f))
                        }
                        Column {
                            Text(
                                text = "Pairing Code: SRC-7291",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF191C1E)
                            )
                            Text(
                                text = "Scan or type code on children's phone to link.",
                                fontSize = 13.sp,
                                color = Color(0xFF555E71)
                            )
                        }
                    }

                    if (caregiverLinked) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE2F1FF), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "✅", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Linked Caregiver: $caregiverName",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF001E30)
                                )
                                Text(
                                    text = "Connected & monitoring live status",
                                    fontSize = 13.sp,
                                    color = Color(0xFF001E30).copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Remote Sync Toggles
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Medication Adherence Sync", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text("Let Rahul monitor prescription details", fontSize = 12.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = caregiverSyncMeds,
                                    onCheckedChange = { viewModel.toggleCaregiverSyncMeds() }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("SOS Alert Remote Sync", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text("Rahul gets loud remote phone alarms", fontSize = 12.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = caregiverSyncSOS,
                                    onCheckedChange = { viewModel.toggleCaregiverSyncSOS() }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Live GPS Location Sync", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text("Rahul can see safe walking zones", fontSize = 12.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = caregiverSyncLocation,
                                    onCheckedChange = { viewModel.toggleCaregiverSyncLocation() }
                                )
                            }
                        }

                        if (caregiverSyncLocation) {
                            // Map Simulation Card with Pulse
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(20.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF4FB))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFB2B2)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("📍", fontSize = 24.sp)
                                    }
                                    Column {
                                        Text(
                                            text = "GPS: Lat 28.6139, Lon 77.2090",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF001E30)
                                        )
                                        Text(
                                            text = "Safe zone status: Home (Rajouri Garden)",
                                            fontSize = 13.sp,
                                            color = Color(0xFF001E30).copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun DailyGamesScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val gameScore by viewModel.gameScore.collectAsStateWithLifecycle()
    val memoryCards by viewModel.memoryCards.collectAsStateWithLifecycle()
    val memoryMatchedPairs by viewModel.memoryMatchedPairs.collectAsStateWithLifecycle()

    val triviaIndex by viewModel.triviaIndex.collectAsStateWithLifecycle()
    val triviaSelectedAnswer by viewModel.triviaSelectedAnswer.collectAsStateWithLifecycle()
    val triviaQuizComplete by viewModel.triviaQuizComplete.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("memory") } // "memory" or "trivia"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back and Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back", modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🧠 DAILY BRAIN",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF191C1E)
                    )
                }

                // Score indicator
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                    border = BorderStroke(2.dp, Color(0xFFFBC02D)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "🏆 $gameScore pts",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFF57F17),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Tab Selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE1E2EC), RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                Button(
                    onClick = { activeTab = "memory" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTab == "memory") Color.White else Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = null
                ) {
                    Text(
                        text = "🦁 Memory Game",
                        fontWeight = FontWeight.Bold,
                        color = if (activeTab == "memory") Color(0xFF191C1E) else Color(0xFF44474E)
                    )
                }
                Button(
                    onClick = { activeTab = "trivia" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTab == "trivia") Color.White else Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = null
                ) {
                    Text(
                        text = "🧠 Trivia Quiz",
                        fontWeight = FontWeight.Bold,
                        color = if (activeTab == "trivia") Color(0xFF191C1E) else Color(0xFF44474E)
                    )
                }
            }
        }

        if (activeTab == "memory") {
            // Memory Match Panel
            item {
                Text(
                    text = "Pair up the animal emojis below! Keeps spatial memory active.",
                    fontSize = 15.sp,
                    color = Color(0xFF555E71),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            // Grid of Memory Cards
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(memoryCards.size) { index ->
                            val card = memoryCards[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .clickable { viewModel.selectMemoryCard(index) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (card.isFlipped || card.isMatched) Color(0xFFE2F1FF) else Color(0xFF191C1E)
                                ),
                                border = BorderStroke(2.dp, Color.White)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (card.isFlipped || card.isMatched) {
                                        Text(text = card.emoji, fontSize = 32.sp)
                                    } else {
                                        Text(text = "❓", fontSize = 28.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { viewModel.initMemoryGame() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("🔄 Reset Board & Reshuffle", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

        } else {
            // Trivia Panel
            if (triviaQuizComplete) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, Color(0xFFA5D6A7), RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("🎉", fontSize = 64.sp)
                            Text(
                                text = "Quiz Completed! (खेल समाप्त!)",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                text = "You did a marvelous job today! Keep playing daily to maintain sharp focus.",
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF555E71)
                            )
                            Button(
                                onClick = { viewModel.resetTriviaQuiz() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                Text("Play Again (दोबारा खेलें)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                val question = viewModel.triviaQuestions[triviaIndex]

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, Color(0xFFD1E4FF), RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Question ${triviaIndex + 1} of ${viewModel.triviaQuestions.size}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0061A4)
                            )
                            Text(
                                text = question.question,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF191C1E)
                            )
                        }
                    }
                }

                items(question.options.size) { optionIdx ->
                    val option = question.options[optionIdx]
                    val isSelected = triviaSelectedAnswer == optionIdx
                    val isCorrect = optionIdx == question.correctAnswerIndex
                    val cardBg = when {
                        triviaSelectedAnswer == null -> Color.White
                        isSelected && isCorrect -> Color(0xFFE8F5E9)
                        isSelected && !isCorrect -> Color(0xFFFFEBEE)
                        isCorrect -> Color(0xFFE8F5E9)
                        else -> Color.White
                    }
                    val borderOutline = when {
                        triviaSelectedAnswer == null -> Color(0xFFE1E2EC)
                        isCorrect -> Color(0xFF2E7D32)
                        isSelected && !isCorrect -> Color(0xFFC62828)
                        else -> Color(0xFFE1E2EC)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .border(2.dp, borderOutline, RoundedCornerShape(16.dp))
                            .clickable { viewModel.answerTriviaQuestion(optionIdx) },
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = option,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (triviaSelectedAnswer != null && isCorrect) Color(0xFF2E7D32) else Color(0xFF191C1E)
                            )
                        }
                    }
                }

                if (triviaSelectedAnswer != null) {
                    item {
                        Button(
                            onClick = { viewModel.nextTriviaQuestion() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Next Question (अगला सवाल) ➡️", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDEB)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("💡", fontSize = 18.sp)
                                Text(text = "Hint: " + question.hint, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF7F6F00))
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun CommunityCornerScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val posts by viewModel.communityPosts.collectAsStateWithLifecycle()
    var communityTab by remember { mutableStateOf("posts") } // "posts" or "audio_rooms"
    var isInsideRoom by remember { mutableStateOf<String?>(null) }
    var textInputState by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back and Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back", modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "👥 COMMUNITY CORNER",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF191C1E)
                )
            }
        }

        // Tab selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE1E2EC), RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                Button(
                    onClick = { communityTab = "posts" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (communityTab == "posts") Color.White else Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = null
                ) {
                    Text(
                        text = "💬 Voice Feed",
                        fontWeight = FontWeight.Bold,
                        color = if (communityTab == "posts") Color(0xFF191C1E) else Color(0xFF44474E)
                    )
                }
                Button(
                    onClick = { communityTab = "audio_rooms" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (communityTab == "audio_rooms") Color.White else Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = null
                ) {
                    Text(
                        text = "📻 Radio Rooms",
                        fontWeight = FontWeight.Bold,
                        color = if (communityTab == "audio_rooms") Color(0xFF191C1E) else Color(0xFF44474E)
                    )
                }
            }
        }

        if (communityTab == "posts") {
            // Write/Post Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, Color(0xFFD1E4FF), RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🎙️ Share your thought (विचार साझा करें)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0061A4)
                        )
                        OutlinedTextField(
                            value = textInputState,
                            onValueChange = { textInputState = it },
                            placeholder = { Text("What is on your mind today?") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (textInputState.isNotBlank()) {
                                        viewModel.addNewCommunityPost(textInputState)
                                        textInputState = ""
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Post Now", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    textInputState = "आज की ठंडी सुबह की राम-राम! बगीचे में सुंदर कोयल कूक रही थी और ताजी हवा से मन प्रसन्न हो गया।"
                                    viewModel.speakText("Voice Dictation Success!")
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2F1FF)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("🎙️ Hold to Speak", fontWeight = FontWeight.Bold, color = Color(0xFF001E30))
                            }
                        }
                    }
                }
            }

            // List of posts
            items(posts) { post ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFCE93D8)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("👴")
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = post.author, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF191C1E))
                                    Text(text = post.relativeTime, fontSize = 12.sp, color = Color.Gray)
                                }
                            }

                            if (post.hasAudio) {
                                IconButton(
                                    onClick = { viewModel.playPostAudio(post) },
                                    modifier = Modifier
                                        .background(Color(0xFFE8E5F4), CircleShape)
                                        .size(36.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play voice note", tint = Color(0xFF5C5394))
                                }
                            }
                        }

                        Text(
                            text = post.text,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF191C1E),
                            lineHeight = 24.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.likePost(post.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF1F2)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("❤️ Like (${post.likesCount})", color = Color(0xFFE91E63), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // Audio Rooms Panel
            if (isInsideRoom == null) {
                item {
                    Text(
                        text = "Join these live audio channels. Chat or listen to groups reciting bhajans & news!",
                        fontSize = 15.sp,
                        color = Color(0xFF555E71),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("🌸 Morning Bhajan Satsang", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text("👥 3 speakers • 12 listening", fontSize = 13.sp, color = Color.Gray)
                                }
                                Button(
                                    onClick = {
                                        isInsideRoom = "Bhajan Satsang"
                                        viewModel.speakText("Joined Bhajan Satsang room!")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Join Room")
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("☕ Chai Pe Charcha (News & Tea)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text("👥 5 speakers • 24 listening", fontSize = 13.sp, color = Color.Gray)
                                }
                                Button(
                                    onClick = {
                                        isInsideRoom = "Chai Pe Charcha"
                                        viewModel.speakText("Joined Chai Pe Charcha room!")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Join Room")
                                }
                            }
                        }
                    }
                }
            } else {
                // Inside Room Simulator
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, Color(0xFFCE93D8), RoundedCornerShape(28.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Text(
                                text = "📻 Connected to: $isInsideRoom",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF5C5394)
                            )

                            // Voice waves animation using Canvas drawing waving bars
                            val infiniteTransition = rememberInfiniteTransition(label = "VoiceWaves")
                            val animVal by infiniteTransition.animateFloat(
                                initialValue = 10f,
                                targetValue = 60f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "WaveValue"
                            )

                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                            ) {
                                val barWidth = 12.dp.toPx()
                                val spacing = 8.dp.toPx()
                                val numBars = 10
                                val startX = (size.width - (numBars * barWidth + (numBars - 1) * spacing)) / 2

                                for (i in 0 until numBars) {
                                    val factor = if (i % 2 == 0) animVal else (60f - animVal + 15f)
                                    val barHeight = factor.dp.toPx()
                                    val x = startX + i * (barWidth + spacing)
                                    val y = (size.height - barHeight) / 2
                                    drawRoundRect(
                                        color = Color(0xFFCE93D8),
                                        topLeft = Offset(x, y),
                                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.speakText("Namaste! Sharing my voice with the room.")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2F1FF)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("🎙️ Speak", color = Color(0xFF001E30), fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        isInsideRoom = null
                                        viewModel.speakText("Left the room.")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD9E2)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("🚪 Leave Room", color = Color(0xFFBA1A1A), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun OnboardingScreen(viewModel: MainViewModel) {
    var selectedModeIsKids by remember { mutableStateOf(false) } // false = Senior, true = Kids
    
    // Form fields for Senior setup starting as completely blank as requested
    var seniorName by remember { mutableStateOf("") }
    var seniorAgeStr by remember { mutableStateOf("") }
    var seniorBloodGroup by remember { mutableStateOf("") }
    var seniorEmergencyContact by remember { mutableStateOf("") }
    
    val isFormValid = selectedModeIsKids || (seniorName.isNotBlank() && seniorEmergencyContact.isNotBlank())
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFBFBFD)) // Apple premium off-white
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(30.dp))
            
            // Header Illustration / Hero
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0061A4).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HealthAndSafety,
                    contentDescription = "App Icon",
                    tint = Color(0xFF0061A4),
                    modifier = Modifier.size(60.dp)
                )
            }
            
            Text(
                text = "Welcome to SeniorCare",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF191C1E),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Please customize your experience below to configure the layout.",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF555E71),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Apple style Segmented Selector for Mode
            Text(
                text = "WHO IS THIS APP FOR? (यह ऐप किसके लिए है?)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0061A4),
                modifier = Modifier.align(Alignment.Start)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF1F3F6))
                    .padding(4.dp)
            ) {
                // Senior Mode Option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (!selectedModeIsKids) Color.White else Color.Transparent)
                        .clickable { selectedModeIsKids = false }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Elderly,
                            contentDescription = "Senior Mode",
                            tint = if (!selectedModeIsKids) Color(0xFF0061A4) else Color(0xFF555E71)
                        )
                        Text(
                            text = "Senior (वरिष्ठ)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (!selectedModeIsKids) Color(0xFF191C1E) else Color(0xFF555E71)
                        )
                    }
                }
                
                // Kids Zone Option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedModeIsKids) Color.White else Color.Transparent)
                        .clickable { selectedModeIsKids = true }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChildCare,
                            contentDescription = "Kids Mode",
                            tint = if (selectedModeIsKids) Color(0xFFD32F2F) else Color(0xFF555E71)
                        )
                        Text(
                            text = "Kids Zone",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (selectedModeIsKids) Color(0xFF191C1E) else Color(0xFF555E71)
                        )
                    }
                }
            }
            
            // Dynamic Form based on selection
            AnimatedVisibility(visible = !selectedModeIsKids) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "SENIOR MEMBER DETAILS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0061A4),
                        modifier = Modifier.padding(top = 10.dp)
                    )
                    
                    // Name Field
                    OutlinedTextField(
                        value = seniorName,
                        onValueChange = { seniorName = it },
                        label = { Text("Senior's Full Name (पूरा नाम)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                    
                    // Age & Blood Group Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = seniorAgeStr,
                            onValueChange = { seniorAgeStr = it },
                            label = { Text("Age (उम्र)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = seniorBloodGroup,
                            onValueChange = { seniorBloodGroup = it },
                            label = { Text("Blood Group (रक्त)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )
                    }
                    
                    // Emergency Caregiver Contact
                    OutlinedTextField(
                        value = seniorEmergencyContact,
                        onValueChange = { seniorEmergencyContact = it },
                        label = { Text("Caregiver's Phone (आपातकालीन नंबर)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                }
            }
            
            AnimatedVisibility(visible = selectedModeIsKids) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .background(Color(0xFFFFEEF0), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "🎈 Kids Zone will be configured with engaging moral stories, fun speech recognition quizzes, and dynamic voice companions!",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFC2185B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))

            if (!isFormValid) {
                Text(
                    text = "कृपया नाम और आपातकालीन नंबर दर्ज करें (Please enter Name & Caregiver's Phone to proceed)",
                    color = Color(0xFFBA1A1A),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            
            // Big Apple-style CTA Button
            Button(
                onClick = {
                    if (isFormValid) {
                        val age = seniorAgeStr.toIntOrNull() ?: 65
                        viewModel.completeOnboarding(
                            isKids = selectedModeIsKids,
                            name = seniorName,
                            age = age,
                            blood = if (seniorBloodGroup.isBlank()) "O+" else seniorBloodGroup,
                            emergency = seniorEmergencyContact
                        )
                    }
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedModeIsKids) Color(0xFFE91E63) else Color(0xFF0061A4)
                )
            ) {
                Text(
                    text = "Get Started (शुरू करें)  ➔",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyCareSerenityScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val currentDay = remember {
        val sdf = java.text.SimpleDateFormat("EEEE", java.util.Locale.US)
        sdf.format(java.util.Date())
    }
    
    val dailyTip = remember(currentDay) {
        when (currentDay) {
            "Monday" -> "आज का सुझाव: सुबह उठकर दो गिलास गुनगुना पानी पिएं। यह पाचन क्रिया को स्वस्थ और सक्रिय रखता है।"
            "Tuesday" -> "आज का सुझाव: दोपहर के भोजन के बाद कम से कम १०० कदम अवश्य चलें। इससे खाया हुआ खाना अच्छे से पचता है।"
            "Wednesday" -> "आज का सुझाव: शाम को ५ मिनट गहरी लंबी सांसें लें (प्राणायाम करें)। इससे तनाव दूर होता है और रात में अच्छी नींद आती है।"
            "Thursday" -> "आज का सुझाव: आंखों को स्वस्थ रखने के लिए नियमित अंतराल पर ठंडे पानी के छीटें मारें और स्क्रीन से दूरी बनाएं।"
            "Friday" -> "आज का सुझाव: अपने भोजन में मौसमी फलों और हरी पत्तेदार सब्जियों को जरूर शामिल करें। यह आपके शरीर को प्राकृतिक ऊर्जा प्रदान करेगा।"
            "Saturday" -> "आज का सुझाव: जोड़ों के दर्द से बचने के लिए नियमित रूप से हल्के स्ट्रेच और गर्दन के व्यायाम करें।"
            "Sunday" -> "आज का सुझाव: खुश रहें और मुस्कुराएं! सकारात्मक सोचें और अपने मित्रों या परिवार से खुलकर बातें करें।"
            else -> "आज का सुझाव: खुश रहें, पर्याप्त पानी पिएं और रोज सुबह १५ मिनट धूप का आनंद लें।"
        }
    }

    val exercises = listOf(
        Triple("🧘 प्राणायाम (Deep Breathing)", "Deep breathing promotes oxygen flow, calms the mind, and lowers blood pressure. Sit straight, close your eyes, and take slow, deep breaths.", "प्राणायाम कीजिए। सीधे बैठें, आंखें बंद करें और धीमी गहरी सांस लें।"),
        Triple("🪑 कुर्सी खिंचाव (Chair Stretch)", "Chair stretch relieves lower back stiffness and improves spine flexibility. Sit straight, extend hands upward, and stretch gently.", "कुर्सी खिंचाव कीजिए। आराम से बैठकर दोनों हाथों को ऊपर ले जाएं और रीढ़ की हड्डी को सीधा खींचें।"),
        Triple("💆 गर्दन घुमाना (Neck Rotation)", "Neck rotation relieves cervical stiffness and relaxes shoulders. Slowly rotate your neck clockwise, then counter-clockwise.", "गर्दन घुमाएं। धीरे-धीरे पहले गर्दन को दाईं ओर, फिर बाईं ओर और फिर गोल घुमाएं।"),
        Triple("👐 हाथ खोलना-बंद करना (Hand Stretches)", "Hand stretches relieve finger arthritis stiffness. Stretch your fingers fully wide, then squeeze them tight into fists.", "हाथों का व्यायाम करें। उंगलियों को पूरा खोलें और फिर मुट्ठी बंद करें।")
    )

    var activeTimerExercise by remember { mutableStateOf<String?>(null) }
    var timerSecondsRemaining by remember { mutableStateOf(60) }

    LaunchedEffect(activeTimerExercise) {
        if (activeTimerExercise != null) {
            timerSecondsRemaining = 60
            while (timerSecondsRemaining > 0) {
                delay(1000)
                timerSecondsRemaining--
            }
            viewModel.speakText("[hi]समय पूरा हुआ! बहुत बढ़िया काम किया!")
            activeTimerExercise = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Care & Serenity", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFBFBFD))
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Live Daily Suggestion Tip Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE2F1FF)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("💡", fontSize = 28.sp)
                        Text(
                            text = "TODAY'S WELLNESS SUGGESTION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            color = Color(0xFF0061A4)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = dailyTip,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191C1E)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.speakText("[hi]$dailyTip") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = Color(0xFF0061A4), modifier = Modifier.size(18.dp))
                            Text("सुझाव सुनें (Listen)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0061A4))
                        }
                    }
                }
            }

            Text(
                text = "SENIOR PHYSICAL EXERCISES (व्यायाम)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                color = Color(0xFF555E71)
            )

            // Dynamic Exercise Cards
            exercises.forEach { (title, desc, ttsSpeak) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp, shape = RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFE1E2EC))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = title,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191C1E)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = desc,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF555E71)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.speakText("[hi]$ttsSpeak") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F3F6)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = Color(0xFF0061A4), modifier = Modifier.size(18.dp))
                                    Text("सुने (Voice)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0061A4))
                                }
                            }

                            Button(
                                onClick = { activeTimerExercise = title },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCE93D8)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start", tint = Color(0xFF251431), modifier = Modifier.size(18.dp))
                                    Text("शुरू करें (Timer)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF251431))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Active Timer Overlay Dialog
    if (activeTimerExercise != null) {
        Dialog(onDismissRequest = { activeTimerExercise = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = activeTimerExercise ?: "",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF191C1E),
                        textAlign = TextAlign.Center
                    )
                    
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = timerSecondsRemaining / 60f,
                            modifier = Modifier.size(110.dp),
                            strokeWidth = 8.dp,
                            color = Color(0xFF0061A4)
                        )
                        Text(
                            text = "${timerSecondsRemaining}s",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0061A4)
                        )
                    }

                    Text(
                        text = "धीमी और गहरी सांसें लें, तनाव मुक्त रहें।",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = { activeTimerExercise = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Stop (बंद करें)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeepsSmilingScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val currentJokeIndex by viewModel.currentJokeIndex.collectAsStateWithLifecycle()
    val isJokePlaying by viewModel.isJokePlaying.collectAsStateWithLifecycle()
    val jokes = viewModel.jokes
    val currentJokeText = jokes[currentJokeIndex]

    LaunchedEffect(Unit) {
        viewModel.onEnterJokesScreen()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("हँसते रहिए (Keep Smiling)", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = {
                        // Stop any active joke speech when leaving
                        if (isJokePlaying) {
                            viewModel.toggleJokePlaying()
                        }
                        onBack()
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFBFBFD))
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                
                Text("😆", fontSize = 64.sp)
                
                Text(
                    text = "आज का चुटकुला (Joke of the Day)",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0061A4)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp))
                        .border(2.dp, Color(0xFFFFD9E2), RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = currentJokeText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191C1E),
                            lineHeight = 28.sp
                        )
                    }
                }
            }

            // High Visibility Controls Card at the bottom
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .shadow(elevation = 3.dp, shape = RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Button
                    IconButton(
                        onClick = { viewModel.prevJoke() },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFFF1F3F6), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Joke",
                            tint = Color(0xFF0061A4),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Play/Pause Button
                    IconButton(
                        onClick = { viewModel.toggleJokePlaying() },
                        modifier = Modifier
                            .size(72.dp)
                            .background(if (isJokePlaying) Color(0xFFFCE8E6) else Color(0xFFE2F1FF), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isJokePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause Speech",
                            tint = if (isJokePlaying) Color(0xFFBA1A1A) else Color(0xFF0061A4),
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    // Next Button
                    IconButton(
                        onClick = { viewModel.nextJoke() },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFFF1F3F6), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Joke",
                            tint = Color(0xFF0061A4),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

