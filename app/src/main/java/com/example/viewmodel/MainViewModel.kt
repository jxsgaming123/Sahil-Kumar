package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.MedicationReminder
import com.example.data.ReminderRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

// --- Data Models for Stories, Bhajans, and Quotes ---

data class MoralStory(
    val id: Int,
    val title: String,
    val moral: String,
    val content: String,
    val illustrationEmoji: String,
    val audioDuration: String
)

data class Bhajan(
    val id: Int,
    val title: String,
    val duration: String,
    val lyrics: String,
    val description: String
)

data class Quote(
    val id: Int,
    val text: String,
    val author: String
)

data class PuzzleTile(
    val id: Int,
    val originalIndex: Int,
    val currentEmoji: String,
    val colorHex: Long
)

class MainViewModel(private val repository: ReminderRepository) : ViewModel() {

    // --- Mode States ---
    private val _isKidsMode = MutableStateFlow(false)
    val isKidsMode: StateFlow<Boolean> = _isKidsMode.asStateFlow()

    // --- TTS (Text-To-Speech) and STT (Speech-To-Text) Integration ---
    private val _speechEvent = MutableSharedFlow<String>()
    val speechEvent: SharedFlow<String> = _speechEvent.asSharedFlow()

    private val _voiceStatusMessage = MutableStateFlow<String?>(null)
    val voiceStatusMessage: StateFlow<String?> = _voiceStatusMessage.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    // --- Kids stories ---
    val stories = listOf(
        MoralStory(
            id = 1,
            title = "The Thirsty Crow (प्यासा कौआ)",
            moral = "Where there is a will, there is a way. (जहाँ चाह, वहाँ राह।)",
            content = "Once, a thirsty crow flew all over the fields looking for water. For a long time, he could not find any. Suddenly, he saw a water jug with very little water at the bottom. He tried to reach the water but his beak was too short. He thought of a clever plan. He started picking up small pebbles one by one and dropping them into the jug. With each pebble, the water level rose higher. Soon, the water reached the top, and the clever crow drank his fill and flew away happily!",
            illustrationEmoji = "🐦‍⬛",
            audioDuration = "1.5 min"
        ),
        MoralStory(
            id = 2,
            title = "The Golden Goose (सोने का अंडा देने वाली मुर्गी)",
            moral = "Greed brings grief and loss. (अति लालच का अंत बुरा होता है।)",
            content = "Once upon a time, a poor farmer had a magical goose. Every single day, the goose laid a beautiful egg made of pure gold. The farmer soon became very rich, but he also became extremely greedy. He thought, 'If I cut open the goose, I can get all the golden eggs at once and become the richest man in the world!' He gathered his courage, cut the goose open, but found absolutely nothing inside! The goose was dead, and the greedy farmer lost his source of daily gold forever.",
            illustrationEmoji = "🪿",
            audioDuration = "1.8 min"
        ),
        MoralStory(
            id = 3,
            title = "The Honest Woodcutter (ईमानदार लकड़हारा)",
            moral = "Honesty is always rewarded. (ईमानदारी का फल हमेशा मीठा होता है।)",
            content = "A poor woodcutter was chopping wood near a river when his iron axe slipped from his hand and fell into deep water. He sat down and began to cry. Suddenly, the River Goddess appeared. Hearing his story, she dived into the water and brought out a golden axe. 'Is this yours?' she asked. The woodcutter shook his head, 'No.' She dived again and brought out a silver axe. Again he said, 'No.' Finally, she brought up his old iron axe. He smiled and said, 'Yes, this is mine!' Touched by his absolute honesty, the Goddess gifted him all three axes.",
            illustrationEmoji = "🪓",
            audioDuration = "2.0 min"
        )
    )

    private val _selectedStory = MutableStateFlow<MoralStory?>(null)
    val selectedStory: StateFlow<MoralStory?> = _selectedStory.asStateFlow()

    private val _isStoryPlaying = MutableStateFlow(false)
    val isStoryPlaying: StateFlow<Boolean> = _isStoryPlaying.asStateFlow()

    // --- Kids Puzzle State ---
    private val puzzleEmojis = listOf("🐱", "🐶", "🦁", "🐼", "🐸", "🐷", "🦄", "🐵", "🦊")
    private val puzzleColors = listOf(
        0xFFFF8A80, 0xFFFFD180, 0xFFFFE082, 
        0xFFA5D6A7, 0xFF80DEEA, 0xFF9FA8DA, 
        0xFFCE93D8, 0xFFF48FB1, 0xFFB0BEC5
    )

    private val _puzzleTiles = MutableStateFlow<List<PuzzleTile>>(emptyList())
    val puzzleTiles: StateFlow<List<PuzzleTile>> = _puzzleTiles.asStateFlow()

    private val _puzzleSolved = MutableStateFlow(false)
    val puzzleSolved: StateFlow<Boolean> = _puzzleSolved.asStateFlow()

    // --- Kids Drawing Settings ---
    private val _brushColor = MutableStateFlow(0xFFFF5252) // Default red
    val brushColor: StateFlow<Long> = _brushColor.asStateFlow()

    private val _brushSize = MutableStateFlow(12f)
    val brushSize: StateFlow<Float> = _brushSize.asStateFlow()

    // --- Seniors Medication Reminders ---
    private val _reminders = MutableStateFlow<List<MedicationReminder>>(emptyList())
    val reminders: StateFlow<List<MedicationReminder>> = _reminders.asStateFlow()

    private val _activeAlarm = MutableStateFlow<MedicationReminder?>(null)
    val activeAlarm: StateFlow<MedicationReminder?> = _activeAlarm.asStateFlow()

    // --- Seniors Daily Bhajans & Quotes ---
    val quotes = listOf(
        Quote(1, "The secret of health for both mind and body is not to mourn for the past, worry about the future, but to live in the present moment wisely.", "Buddha"),
        Quote(2, "चिंता ऐसी डाकिनी, काट कलेजो खाए। वैद बिचारा क्या करे, कहां तक दवा लगाए॥ शांत मन ही सबसे बड़ी औषधि है।", "कबीर"),
        Quote(3, "Strength does not come from physical capacity. It comes from an indomitable will to spread love and kindness.", "Mahatma Gandhi"),
        Quote(4, "कर्मण्येवाधिकारस्ते मा फलेषु कदाचन। अपना कर्तव्य करते रहें, फल की चिंता न करें। जीवन सुंदर है।", "श्रीमद्भगवद्गीता")
    )

    private val _currentQuote = MutableStateFlow(quotes[0])
    val currentQuote: StateFlow<Quote> = _currentQuote.asStateFlow()

    val bhajans = listOf(
        BhajansList.GAYATRI,
        BhajansList.ACHYUTAM,
        BhajansList.AARTI,
        BhajansList.RAM_DHYAN
    )

    private val _playingBhajan = MutableStateFlow<Bhajan?>(null)
    val playingBhajan: StateFlow<Bhajan?> = _playingBhajan.asStateFlow()

    private val _bhajanProgress = MutableStateFlow(0f)
    val bhajanProgress: StateFlow<Float> = _bhajanProgress.asStateFlow()

    // --- Seniors Emergency SOS Countdown ---
    private val _sosCountdown = MutableStateFlow<Int?>(null)
    val sosCountdown: StateFlow<Int?> = _sosCountdown.asStateFlow()

    private val _sosActive = MutableStateFlow(false)
    val sosActive: StateFlow<Boolean> = _sosActive.asStateFlow()

    // Timer jobs
    private var alarmCheckJob: Job? = null
    private var bhajanJob: Job? = null
    private var sosJob: Job? = null

    init {
        // Fetch medication reminders
        viewModelScope.launch {
            repository.allReminders.collectLatest { list ->
                _reminders.value = list
            }
        }

        // Start background clock/alarm ticker
        startAlarmTicker()
        
        // Initialize kids puzzle
        resetPuzzle()
    }

    // --- Toggle Modes ---
    fun toggleMode() {
        val nextMode = !_isKidsMode.value
        _isKidsMode.value = nextMode
        
        // Stop any active content when switching modes
        stopStory()
        stopBhajan()
        cancelSOS()

        viewModelScope.launch {
            if (nextMode) {
                _speechEvent.emit("Kids Zone activated! Let's read, draw, and play!")
            } else {
                _speechEvent.emit("Senior Care mode enabled. High contrast layout is active.")
            }
        }
    }

    fun setKidsMode(active: Boolean) {
        if (_isKidsMode.value != active) {
            _isKidsMode.value = active
            stopStory()
            stopBhajan()
            cancelSOS()
            viewModelScope.launch {
                if (active) {
                    _speechEvent.emit("Kids Zone activated!")
                } else {
                    _speechEvent.emit("Senior Care mode enabled.")
                }
            }
        }
    }

    // --- Voice Assistant Routing Logic ---
    fun processVoiceCommand(spokenText: String) {
        val text = spokenText.lowercase().trim()
        _voiceStatusMessage.value = "Heard: \"$spokenText\""
        
        viewModelScope.launch {
            delay(1500)
            _voiceStatusMessage.value = null
        }

        when {
            // Check commands for switching Modes
            containsAny(text, listOf("kids", "bacche", "bacha", "child", "game", "toy", "drawing", "paint")) -> {
                setKidsMode(true)
                if (containsAny(text, listOf("drawing", "paint", "canvas"))) {
                    // Navigate to canvas or speech triggers story
                    speakText("Opening Drawing Canvas for you!")
                } else if (containsAny(text, listOf("story", "kahani", "moral", "sunao"))) {
                    // Start reading first story
                    selectStory(stories[0])
                    playStory()
                } else {
                    speakText("Kids mode activated!")
                }
            }
            containsAny(text, listOf("senior", "dada", "dadi", "nana", "nani", "elder", "old", "care")) -> {
                setKidsMode(false)
                speakText("Senior Care Mode activated.")
            }
            // Check commands for Medication Reminder
            containsAny(text, listOf("dawa", "medicine", "reminder", "alarm", "pills", "tablet", "dava")) -> {
                setKidsMode(false)
                speakText("Showing your medication reminders.")
            }
            // Check commands for Bhajans or quotes
            containsAny(text, listOf("bhajan", "song", "prayer", "mantra", "devotional", "music")) -> {
                setKidsMode(false)
                speakText("Playing deep devotional prayer for you.")
                playBhajan(bhajans[0])
            }
            containsAny(text, listOf("quote", "suvichar", "motivation", " विचार", "vichar")) -> {
                setKidsMode(false)
                nextQuote()
                speakText("Here is a peaceful thought: " + _currentQuote.value.text)
            }
            // Check commands for Emergency SOS
            containsAny(text, listOf("sos", "emergency", "save", "help", "bachao", "danger", "doctor")) -> {
                setKidsMode(false)
                triggerSOS()
            }
            // Simple fallbacks
            else -> {
                speakText("I heard you say: $spokenText. Try saying: Kahani sunao, Dawa dikhao, or Bhajan bajao.")
            }
        }
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }

    fun speakText(text: String) {
        viewModelScope.launch {
            _speechEvent.emit(text)
        }
    }

    fun setIsListening(listening: Boolean) {
        _isListening.value = listening
    }

    // --- Kids Stories Logic ---
    fun selectStory(story: MoralStory) {
        _selectedStory.value = story
        stopStory()
    }

    fun playStory() {
        val story = _selectedStory.value ?: stories[0]
        if (_selectedStory.value == null) {
            _selectedStory.value = story
        }
        _isStoryPlaying.value = true
        speakText("Now reading: ${story.title}. Story text: ${story.content}. Moral of the story is: ${story.moral}")
    }

    fun stopStory() {
        _isStoryPlaying.value = false
    }

    // --- Kids Jigsaw Emoji Puzzle Logic ---
    fun resetPuzzle() {
        val list = puzzleEmojis.mapIndexed { index, emoji ->
            PuzzleTile(
                id = index,
                originalIndex = index,
                currentEmoji = emoji,
                colorHex = puzzleColors[index]
            )
        }.shuffled() // Shuffle pieces
        
        _puzzleTiles.value = list
        _puzzleSolved.value = false
    }

    fun swapTiles(index1: Int, index2: Int) {
        if (_puzzleSolved.value) return
        val currentList = _puzzleTiles.value.toMutableList()
        val temp = currentList[index1]
        currentList[index1] = currentList[index2]
        currentList[index2] = temp
        _puzzleTiles.value = currentList

        // Check if solved
        val isCorrect = currentList.indices.all { i ->
            currentList[i].originalIndex == i
        }
        if (isCorrect) {
            _puzzleSolved.value = true
            speakText("Amazing! You solved the puzzle! Hurrah!")
        }
    }

    // --- Kids Drawing Brush Logic ---
    fun selectBrushColor(color: Long) {
        _brushColor.value = color
    }

    fun setBrushSize(size: Float) {
        _brushSize.value = size
    }

    // --- Seniors Medication Logic ---
    fun addReminder(name: String, dosage: String, time: String) {
        viewModelScope.launch {
            repository.insert(
                MedicationReminder(
                    medicineName = name,
                    dosage = dosage,
                    time = time
                )
            )
            speakText("Medication reminder for $name added successfully!")
        }
    }

    fun toggleReminderActive(reminder: MedicationReminder) {
        viewModelScope.launch {
            repository.update(reminder.copy(isActive = !reminder.isActive))
        }
    }

    fun deleteReminder(reminder: MedicationReminder) {
        viewModelScope.launch {
            repository.delete(reminder)
            speakText("Reminder for ${reminder.medicineName} deleted.")
        }
    }

    private fun startAlarmTicker() {
        alarmCheckJob?.cancel()
        alarmCheckJob = viewModelScope.launch {
            while (true) {
                delay(10000) // Check every 10 seconds
                val activeList = _reminders.value.filter { it.isActive }
                if (activeList.isNotEmpty() && _activeAlarm.value == null) {
                    val currentTimeStr = getCurrentTimeFormatted()
                    val matchingReminder = activeList.firstOrNull { 
                        it.time.equals(currentTimeStr, ignoreCase = true) 
                    }
                    if (matchingReminder != null) {
                        triggerAlarm(matchingReminder)
                    }
                }
            }
        }
    }

    private fun triggerAlarm(reminder: MedicationReminder) {
        _activeAlarm.value = reminder
        speakText("Attention please! Dawa ka samay ho gaya hai! It is time for your medicine: ${reminder.medicineName}. Dosage is ${reminder.dosage}. Please take it now.")
    }

    fun dismissAlarm() {
        _activeAlarm.value = null
    }

    fun markMedicineTaken() {
        val reminder = _activeAlarm.value
        _activeAlarm.value = null
        if (reminder != null) {
            speakText("Thank you for taking your medicine. Stay healthy!")
        }
    }

    private fun getCurrentTimeFormatted(): String {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR)
        val minute = cal.get(Calendar.MINUTE)
        val amPm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
        val displayHour = if (hour == 0) 12 else hour
        return String.format("%02d:%02d %s", displayHour, minute, amPm)
    }

    // --- Seniors Quote & Bhajan Logic ---
    fun nextQuote() {
        val currentIndex = quotes.indexOf(_currentQuote.value)
        val nextIndex = (currentIndex + 1) % quotes.size
        _currentQuote.value = quotes[nextIndex]
    }

    fun playBhajan(bhajan: Bhajan) {
        stopBhajan()
        _playingBhajan.value = bhajan
        _bhajanProgress.value = 0f
        
        bhajanJob = viewModelScope.launch {
            speakText("Playing devotional prayer: ${bhajan.title}. Here are the peaceful words: ${bhajan.lyrics.take(60)}")
            // Simulate music progress
            for (i in 1..100) {
                delay(300)
                _bhajanProgress.value = i / 100f
            }
            _playingBhajan.value = null
            _bhajanProgress.value = 0f
            speakText("Devotional prayer completed. Feel the inner peace.")
        }
    }

    fun stopBhajan() {
        bhajanJob?.cancel()
        _playingBhajan.value = null
        _bhajanProgress.value = 0f
    }

    // --- Seniors SOS Logic ---
    fun triggerSOS() {
        cancelSOS()
        _sosActive.value = true
        _sosCountdown.value = 5
        speakText("Emergency SOS initiated. Sending helper alerts in five seconds. Click cancel to stop.")
        
        sosJob = viewModelScope.launch {
            var count = 5
            while (count > 0) {
                delay(1000)
                count--
                _sosCountdown.value = count
                if (count > 0) {
                    speakText("$count")
                }
            }
            // Trigger final emergency alert
            speakText("Emergency Alert! Emergency Alert! Medical attention needed immediately!")
            _sosCountdown.value = 0
        }
    }

    fun cancelSOS() {
        sosJob?.cancel()
        _sosCountdown.value = null
        _sosActive.value = false
    }

    override fun onCleared() {
        super.onCleared()
        alarmCheckJob?.cancel()
        bhajanJob?.cancel()
        sosJob?.cancel()
    }
}

// Factory class for MainViewModel
class MainViewModelFactory(private val repository: ReminderRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// --- List of Peaceful Bhajans ---
object BhajansList {
    val GAYATRI = Bhajan(
        id = 1,
        title = "Gayatri Mantra",
        duration = "3.5 min",
        lyrics = "ॐ भूर्भुवः स्वः। तत्सवितुर्वरेण्यं। भर्गो देवस्य धीमहि। धियो यो नः प्रचोदयात्॥",
        description = "Chanting this mantra brings supreme intellectual clarity, wisdom, and cosmic peace to mind."
    )
    val ACHYUTAM = Bhajan(
        id = 2,
        title = "Achyutam Keshavam",
        duration = "4.2 min",
        lyrics = "अच्युतम् केशवम् कृष्ण दामोदरम्। राम नारायणम् जानकी वल्लभम्॥",
        description = "A peaceful song praising Lord Krishna, ideal for inducing sleep, relieving anxiety, and meditation."
    )
    val AARTI = Bhajan(
        id = 3,
        title = "Om Jai Jagdish Hare",
        duration = "5.0 min",
        lyrics = "ॐ जय जगदीश हरे, स्वामी जय जगदीश हरे। भक्त जनों के संकट, क्षण में दूर करे॥",
        description = "Traditional evening prayer sung with family to express deep gratitude and cleanse the surrounding atmosphere."
    )
    val RAM_DHYAN = Bhajan(
        id = 4,
        title = "Raghupati Raghav Raja Ram",
        duration = "3.8 min",
        lyrics = "रघुपति राघव राजाराम, पतित पावन सीताराम। ईश्वर अल्लाह तेरो नाम, सब को सन्मति दे भगवान॥",
        description = "Mahatma Gandhi's favourite meditative chant for universal love, social harmony, and peaceful breathing."
    )
}
