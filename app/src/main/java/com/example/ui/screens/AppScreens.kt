package com.example.ui.screens

import android.text.format.DateFormat
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

    var currentScreen by remember { mutableStateOf("home") }

    // Alarm full-screen interrupt modal
    if (activeAlarm != null) {
        AlarmInterruptDialog(
            reminder = activeAlarm!!,
            onTaken = { viewModel.markMedicineTaken() },
            onDismiss = { viewModel.dismissAlarm() }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isKidsMode) "👶 GenerationConnect" else "👵 SeniorCare Mode",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (isKidsMode) 22.sp else 26.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("app_title")
                        )
                        if (isKidsMode) {
                            Text(
                                text = "✨ KIDS ZONE",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
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
                    // Quick Mode Toggle
                    Button(
                        onClick = { viewModel.toggleMode() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isKidsMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (isKidsMode) Icons.Default.ChildCare else Icons.Default.Elderly,
                            contentDescription = "Toggle mode",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isKidsMode) "Go Senior" else "Go Kids",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            // High visibility speak mic FAB for speech navigation
            FloatingActionButton(
                onClick = { onStartVoiceListening() },
                containerColor = if (isKidsMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
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

// --- HOME DASHBOARD ---

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    isKidsMode: Boolean,
    onNavigate: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Theme Header (Mode switcher toggle pill & Avatar)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode switcher toggle pill matching: bg-[#E1E2EC] p-1.5 rounded-full flex w-full max-w-[240px]
                Row(
                    modifier = Modifier
                        .background(Color(0xFFE1E2EC), CircleShape)
                        .padding(5.dp)
                        .width(220.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Senior Mode tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(CircleShape)
                            .background(if (!isKidsMode) Color.White else Color.Transparent)
                            .clickable { if (isKidsMode) viewModel.toggleMode() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Senior Mode",
                            color = if (!isKidsMode) Color(0xFF0061A4) else Color(0xFF44474E),
                            fontWeight = if (!isKidsMode) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                    // Kids Zone tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(CircleShape)
                            .background(if (isKidsMode) Color.White else Color.Transparent)
                            .clickable { if (!isKidsMode) viewModel.toggleMode() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Kids Zone",
                            color = if (isKidsMode) Color(0xFF0061A4) else Color(0xFF44474E),
                            fontWeight = if (isKidsMode) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }

                // Profile Avatar: w-12 h-12 bg-[#D1E4FF] rounded-full flex items-center justify-center border-2 border-white shadow-sm
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD1E4FF))
                        .border(2.dp, Color.White, CircleShape)
                        .shadow(elevation = 2.dp, shape = CircleShape)
                        .clickable { /* Profile info */ },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isKidsMode) "👶" else "👤",
                        fontSize = 20.sp
                    )
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

        // 3. Optional Hero banner (from original code, beautifully rounded to 32.dp)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isTablet) 200.dp else 140.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .border(
                        width = 2.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(32.dp)
                    )
                    .shadow(1.dp, shape = RoundedCornerShape(32.dp))
            ) {
                Image(
                    painter = painterResource(
                        id = if (isKidsMode) {
                            com.example.R.drawable.img_kids_banner_1783050948901
                        } else {
                            com.example.R.drawable.img_senior_banner_1783050962827
                        }
                    ),
                    contentDescription = "Welcome banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Ambient banner shade overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                )
                Text(
                    text = if (isKidsMode) "Welcome to Play Zone!" else "Daily Care & Serenity",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = if (isKidsMode) 20.sp else 24.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                )
            }
        }

        // 4. Voice Action panel matching bg-white border-2 border-[#D1E4FF] rounded-[28px] p-6
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(28.dp))
                    .border(2.dp, Color(0xFFD1E4FF), RoundedCornerShape(28.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "🎙️",
                        fontSize = 32.sp
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "VOICE ACTION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp,
                            color = Color(0xFF0061A4)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isKidsMode) "Say \"Kahani Sunao\" or \"Game\"" else "Say \"Dawa\" or \"Bhajan\"",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191C1E)
                        )
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
                        .border(width = 2.dp, color = Color.White, shape = RoundedCornerShape(32.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE2E1EC)), // Soft secondary shade
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
                                text = "Keep smiling.",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1C1E)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Everything is safe & sound.",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1A1C1E).copy(alpha = 0.7f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = "Weather",
                            tint = Color(0xFFF59E0B),
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

@Composable
fun KidsDrawingScreen(viewModel: MainViewModel) {
    val brushColorVal by viewModel.brushColor.collectAsStateWithLifecycle()
    val brushSizeVal by viewModel.brushSize.collectAsStateWithLifecycle()

    val currentPoints = remember { mutableStateListOf<Offset>() }
    val paths = remember { mutableStateListOf<CustomPath>() }

    val brushColor = Color(brushColorVal)

    val colorOptions = listOf(
        0xFFFF5252, // Coral/Red
        0xFF40C4FF, // Sky Blue
        0xFF69F0AE, // Neon Green
        0xFFFFD740, // Sunny Yellow
        0xFFE040FB, // Magenta Pink
        0xFF000000  // Dark Black
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Drawing tools panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Color Select Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Choose Brush Color:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Clear board button
                    OutlinedButton(
                        onClick = {
                            currentPoints.clear()
                            paths.clear()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear Canvas")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colObj)
                                .border(
                                    width = if (isSelected) 4.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { viewModel.selectBrushColor(colHex) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Brush Size slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Brush Size: ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.width(90.dp)
                    )
                    Slider(
                        value = brushSizeVal,
                        onValueChange = { viewModel.setBrushSize(it) },
                        valueRange = 4f..40f,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(brushColor)
                    )
                }
            }
        }

        // Main whiteboard canvas with shadow border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .background(Color.White)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPoints.clear()
                            currentPoints.add(offset)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentPoints.add(change.position)
                        },
                        onDragEnd = {
                            if (currentPoints.isNotEmpty()) {
                                paths.add(CustomPath(currentPoints.toList(), brushColor, brushSizeVal))
                                currentPoints.clear()
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw historic path elements
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

                // Draw active gesture lines
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

            // Decorative star watermark for kids
            Text(
                text = "✨ DRAW IN THIS BOARD ✨",
                color = Color.LightGray.copy(alpha = 0.4f),
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.Center)
            )
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
