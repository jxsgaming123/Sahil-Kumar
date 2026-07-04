package com.example.viewmodel

import android.graphics.Bitmap
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.GeminiService
import com.example.data.MedicationReminder
import com.example.data.ReminderRepository
import kotlinx.coroutines.Dispatchers
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Calendar

// --- New Models for Prescription and Companion Chat ---
data class PrescriptionMedicine(
    val name: String,
    val dosage: String,
    val timing: String
)

data class CompanionMessage(
    val sender: String, // "user" or "companion"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class MemoryCard(
    val id: Int,
    val emoji: String,
    val isFlipped: Boolean = false,
    val isMatched: Boolean = false
)

data class TriviaQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val hint: String
)

data class CommunityFeedPost(
    val id: Int,
    val author: String,
    val relativeTime: String,
    val text: String,
    val likesCount: Int,
    val hasAudio: Boolean = false,
    val audioDuration: String = ""
)

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

class MainViewModel(
    private val repository: ReminderRepository,
    private val healthConnectManager: com.example.data.HealthConnectManager,
    private val context: android.content.Context
) : ViewModel() {

    fun getRequiredPermissions(): Set<String> {
        return healthConnectManager.getRequiredPermissions()
    }

    fun isHealthConnectAvailable(): Boolean {
        return healthConnectManager.isAvailable()
    }

    fun fetchHealthConnectSteps() {
        viewModelScope.launch {
            if (healthConnectManager.isAvailable() && healthConnectManager.hasReadStepsPermission()) {
                val steps = healthConnectManager.fetchDailySteps()
                if (steps > 0) {
                    _dailySteps.value = steps
                }
            }
        }
    }

    // --- Mode States ---
    private val _isKidsMode = MutableStateFlow(false)
    val isKidsMode: StateFlow<Boolean> = _isKidsMode.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(false)
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _meditationHour = MutableStateFlow(7)
    val meditationHour: StateFlow<Int> = _meditationHour.asStateFlow()

    private val _meditationMinute = MutableStateFlow(0)
    val meditationMinute: StateFlow<Int> = _meditationMinute.asStateFlow()

    private val _meditationAlarmSet = MutableStateFlow(false)
    val meditationAlarmSet: StateFlow<Boolean> = _meditationAlarmSet.asStateFlow()

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
            title = "प्यासा कौआ (The Thirsty Crow)",
            moral = "जहाँ चाह, वहाँ राह। (Where there is a will, there is a way.)",
            content = "एक बार की बात है, एक प्यासा कौआ पानी की तलाश में चारों ओर उड़ रहा था। बहुत देर तक खोजने के बाद भी उसे कहीं पानी नहीं मिला। जब वह बहुत थक गया, तो उसे एक बगीचे में एक घड़ा दिखाई दिया। कौआ तुरंत उड़कर घड़े के पास गया। उसने देखा कि घड़े में बहुत थोड़ा पानी था और उसकी चोंच पानी तक नहीं पहुँच पा रही थी। कौवे ने हिम्मत नहीं हारी और एक तरकीब सोची। उसने पास पड़े छोटे-छोटे कंकड़ अपनी चोंच से उठाए और एक-एक करके घड़े में डालने लगा। जैसे-जैसे कंकड़ घड़े में गिरते गए, पानी का स्तर ऊपर उठता गया। जल्द ही पानी घड़े के मुंह तक आ गया। चतुर कौवे ने जी भरकर पानी पिया और खुशी-खुशी वहाँ से उड़ गया!",
            illustrationEmoji = "🐦‍⬛",
            audioDuration = "1.5 min"
        ),
        MoralStory(
            id = 2,
            title = "सोने का अंडा (The Golden Goose)",
            moral = "लालच का अंत हमेशा बुरा होता है। (Greed brings grief.)",
            content = "बहुत समय पहले, एक गरीब किसान के पास एक जादुई बत्तख थी। वह बत्तख हर रोज सोने का एक खूबसूरत अंडा देती थी। किसान उस अंडे को बाजार में बेचकर अमीर होने लगा। लेकिन जल्द ही वह बहुत लालची हो गया। उसने सोचा, 'इस बत्तख के पेट में तो सोने के अंडों का खजाना होगा। क्यों न मैं इसका पेट काटकर सारे अंडे एक साथ निकाल लूं और दुनिया का सबसे अमीर आदमी बन जाऊं!' लालच में आकर उसने बत्तख को मार डाला और उसका पेट चीर दिया। लेकिन उसे बत्तख के अंदर कुछ नहीं मिला! लालची किसान ने अपनी जादुई बत्तख खो दी और उसका रोजाना मिलने वाला सोना भी हमेशा के लिए खत्म हो गया। वह रोने लगा लेकिन अब बहुत देर हो चुकी थी।",
            illustrationEmoji = "🪿",
            audioDuration = "1.8 min"
        ),
        MoralStory(
            id = 3,
            title = "ईमानदार लकड़हारा (The Honest Woodcutter)",
            moral = "ईमानदारी ही सच्ची नीति है और इसका फल मीठा होता है।",
            content = "एक गरीब लकड़हारा नदी के किनारे सूखी लकड़ी काट रहा था कि अचानक उसकी लोहे की कुल्हाड़ी हाथ से फिसलकर गहरे पानी में गिर गई। वह बहुत दुखी हुआ और नदी के किनारे बैठकर रोने लगा। तभी नदी की जलदेवी पानी से बाहर प्रकट हुईं। लकड़हारे की बात सुनकर देवी ने नदी में डुबकी लगाई और एक सोने की कुल्हाड़ी लेकर बाहर आईं। उन्होंने पूछा, 'क्या यह तुम्हारी कुल्हाड़ी है?' लकड़हारे ने कहा, 'नहीं देवी, यह मेरी नहीं है।' देवी ने फिर डुबकी लगाई और इस बार चांदी की कुल्हाड़ी लेकर आईं। लकड़हारे ने फिर मना कर दिया। अंत में, देवी ने उसकी पुरानी लोहे की कुल्हाड़ी निकाली। उसे देखकर लकड़हारा खुशी से चिल्लाया, 'हाँ, यही मेरी कुल्हाड़ी है!' देवी उसकी ईमानदारी से बहुत प्रसन्न हुईं और उन्होंने उसे तीनों कुल्हाड़ियाँ उपहार में दे दीं।",
            illustrationEmoji = "🪓",
            audioDuration = "2.0 min"
        ),
        MoralStory(
            id = 4,
            title = "कछुआ और खरगोश (The Tortoise and the Hare)",
            moral = "धीमी और निरंतर मेहनत करने वाले की ही हमेशा जीत होती है।",
            content = "एक जंगल में एक घमंडी खरगोश और एक सीधा-सादा कछुआ रहते थे। खरगोश को अपनी तेज दौड़ पर बहुत घमंड था और वह हमेशा कछुए का मज़ाक उड़ाता था। एक दिन दोनों ने दौड़ लगाने का फैसला किया। दौड़ शुरू होते ही खरगोश तेजी से भागा और कछुए से बहुत आगे निकल गया। उसने पीछे मुड़कर देखा कि कछुआ बहुत दूर है, तो उसने सोचा, 'क्यों न मैं इस पेड़ की छाँव में थोड़ी देर सो लूँ, कछुआ तो शाम तक भी यहाँ नहीं पहुँच पाएगा।' खरगोश गहरी नींद में सो गया। दूसरी ओर, कछुआ बिना रुके धीरे-धीरे अपनी मंजिल की तरफ चलता रहा। वह सोते हुए खरगोश के पास से गुजर गया और फिनिश लाइन पर पहुँच गया। जब खरगोश की आँख खुली, तब तक कछुआ दौड़ जीत चुका था! घमंडी खरगोश का सिर शर्म से झुक गया।",
            illustrationEmoji = "🐢",
            audioDuration = "2.2 min"
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
        BhajansList.RAM_DHYAN,
        BhajansList.HANUMAN_CHALISA,
        BhajansList.KRISHNA_GOVIND,
        BhajansList.SHIV_TANDAV,
        BhajansList.HARE_KRISHNA
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

    // --- State variables for "Dr. Ki Awaaz" (Prescription Scanner) ---
    private val _prescriptionLoading = MutableStateFlow(false)
    val prescriptionLoading: StateFlow<Boolean> = _prescriptionLoading.asStateFlow()

    private val _prescriptionResult = MutableStateFlow<String?>(null)
    val prescriptionResult: StateFlow<String?> = _prescriptionResult.asStateFlow()

    private val _prescriptionMedicines = MutableStateFlow<List<PrescriptionMedicine>>(emptyList())
    val prescriptionMedicines: StateFlow<List<PrescriptionMedicine>> = _prescriptionMedicines.asStateFlow()

    private val _prescriptionSpeechEnglish = MutableStateFlow("")
    val prescriptionSpeechEnglish: StateFlow<String> = _prescriptionSpeechEnglish.asStateFlow()

    private val _prescriptionSpeechHindi = MutableStateFlow("")
    val prescriptionSpeechHindi: StateFlow<String> = _prescriptionSpeechHindi.asStateFlow()

    private val _prescriptionSpeechPunjabi = MutableStateFlow("")
    val prescriptionSpeechPunjabi: StateFlow<String> = _prescriptionSpeechPunjabi.asStateFlow()

    // --- State variables for "Buzurgon Ka Sahil" (Elder Companion) ---
    private val _companionLoading = MutableStateFlow(false)
    val companionLoading: StateFlow<Boolean> = _companionLoading.asStateFlow()

    private val _companionChatHistory = MutableStateFlow<List<CompanionMessage>>(
        listOf(
            CompanionMessage("companion", "नमस्ते दादाजी/दादीजी! मैं आपका साथी साहिल हूँ। आज आप कैसे महसूस कर रहे हैं? अगर आपको अकेलापन लग रहा है तो ऊपर 'Akeli Lag Rahi Hai' बटन दबाएं, या मुझसे बातें करें।")
        )
    )
    val companionChatHistory: StateFlow<List<CompanionMessage>> = _companionChatHistory.asStateFlow()

    // --- State variables for "Shor Sharaba Meter" (Noise Meter) ---
    private val _currentDecibel = MutableStateFlow(32f)
    val currentDecibel: StateFlow<Float> = _currentDecibel.asStateFlow()

    private val _isNoiseTracking = MutableStateFlow(false)
    val isNoiseTracking: StateFlow<Boolean> = _isNoiseTracking.asStateFlow()

    private val _noiseComplaintDraft = MutableStateFlow<String?>(null)
    val noiseComplaintDraft: StateFlow<String?> = _noiseComplaintDraft.asStateFlow()

    // --- Profile & Caregiver Sync States ---
    private val _seniorName = MutableStateFlow("")
    val seniorName: StateFlow<String> = _seniorName.asStateFlow()

    private val _seniorAge = MutableStateFlow(0)
    val seniorAge: StateFlow<Int> = _seniorAge.asStateFlow()

    private val _seniorBloodGroup = MutableStateFlow("")
    val seniorBloodGroup: StateFlow<String> = _seniorBloodGroup.asStateFlow()

    private val _seniorEmergencyContact = MutableStateFlow("")
    val seniorEmergencyContact: StateFlow<String> = _seniorEmergencyContact.asStateFlow()

    private val _caregiverLinked = MutableStateFlow(true)
    val caregiverLinked: StateFlow<Boolean> = _caregiverLinked.asStateFlow()

    private val _caregiverName = MutableStateFlow("Rahul Sharma")
    val caregiverName: StateFlow<String> = _caregiverName.asStateFlow()

    private val _caregiverSyncMeds = MutableStateFlow(true)
    val caregiverSyncMeds: StateFlow<Boolean> = _caregiverSyncMeds.asStateFlow()

    private val _caregiverSyncSOS = MutableStateFlow(true)
    val caregiverSyncSOS: StateFlow<Boolean> = _caregiverSyncSOS.asStateFlow()

    private val _caregiverSyncLocation = MutableStateFlow(true)
    val caregiverSyncLocation: StateFlow<Boolean> = _caregiverSyncLocation.asStateFlow()

    // --- Steps & Hydration States ---
    private val _dailySteps = MutableStateFlow(1250)
    val dailySteps: StateFlow<Int> = _dailySteps.asStateFlow()

    private val _stepGoal = MutableStateFlow(3000)
    val stepGoal: StateFlow<Int> = _stepGoal.asStateFlow()

    private val _waterGlasses = MutableStateFlow(3)
    val waterGlasses: StateFlow<Int> = _waterGlasses.asStateFlow()

    private val _waterGoal = MutableStateFlow(8)
    val waterGoal: StateFlow<Int> = _waterGoal.asStateFlow()

    // --- Weather States ---
    private val _weatherTemp = MutableStateFlow("28°C")
    val weatherTemp: StateFlow<String> = _weatherTemp.asStateFlow()

    private val _weatherEmoji = MutableStateFlow("☀️")
    val weatherEmoji: StateFlow<String> = _weatherEmoji.asStateFlow()

    private val _weatherStatusText = MutableStateFlow("Clear Sky")
    val weatherStatusText: StateFlow<String> = _weatherStatusText.asStateFlow()

    // --- Keeps Smiling (Hindi Jokes) States ---
    val jokes = listOf(
        "चिंटू: पापा, हमारे नए पड़ोसी बहुत गरीब हैं!\nपापा: वो कैसे बेटा?\nचिंटू: कल उनके बच्चे ने एक सिक्का निगल लिया था, तो उनकी मम्मी कह रही थीं कि 'हे भगवान! बस दो रुपये का सिक्का था, अब क्या करेंगे!' 😂",
        "अध्यापक: रमेश, कल स्कूल क्यों नहीं आए?\nरमेश: सर, कल मुझे एक सपना आया था कि मैं अमेरिका जा रहा हूँ।\nअध्यापक: तो फिर स्कूल क्यों नहीं आए?\nरमेश: सर, मैं तो एयरपोर्ट पर ही घूमता रह गया, सुबह आँख खुली तो पता चला फ्लाइट छूट गई! ✈️😴",
        "पप्पू: डॉक्टर साहब, जब मैं सोता हूँ तो मुझे सपने में बंदर फुटबॉल खेलते हुए दिखते हैं।\nडॉक्टर: ठीक है, आज रात से यह दवा खाकर सोना, सब ठीक हो जाएगा।\nपप्पू: डॉक्टर साहब, क्या मैं यह दवा कल रात से खा सकता हूँ?\nडॉक्टर: क्यों, आज रात खाने में क्या दिक्कत है?\nपप्पू: सर, आज रात तो फाइनल मैच है! 🐒⚽😂",
        "पत्नी: सुनो जी, शादी से पहले तुम मुझे कितने अच्छे-अच्छे गिफ्ट देते थे, अब क्यों नहीं देते?\nपति: प्रिय, क्या तुमने कभी किसी मछुआरे को मछली पकड़ने के बाद भी चारा खिलाते देखा है? 🎣🤐",
        "मरीज: डॉक्टर साहब, मुझे बहुत अजीब बीमारी है, जब भी मैं चाय पीता हूँ तो मेरी दाहिनी आँख में दर्द होता है।\nडॉक्टर: अच्छा! अगली बार चाय पीने से पहले कप में से चम्मच बाहर निकाल लिया करो! ☕🥄😅"
    )

    private val _currentJokeIndex = MutableStateFlow(0)
    val currentJokeIndex: StateFlow<Int> = _currentJokeIndex.asStateFlow()

    private val _isJokePlaying = MutableStateFlow(false)
    val isJokePlaying: StateFlow<Boolean> = _isJokePlaying.asStateFlow()

    // --- Brain Games States ---
    private val _gameScore = MutableStateFlow(120)
    val gameScore: StateFlow<Int> = _gameScore.asStateFlow()

    private val _memoryCards = MutableStateFlow<List<MemoryCard>>(emptyList())
    val memoryCards: StateFlow<List<MemoryCard>> = _memoryCards.asStateFlow()

    private val _memoryMatchedPairs = MutableStateFlow(0)
    val memoryMatchedPairs: StateFlow<Int> = _memoryMatchedPairs.asStateFlow()

    private val _triviaIndex = MutableStateFlow(0)
    val triviaIndex: StateFlow<Int> = _triviaIndex.asStateFlow()

    private val _triviaSelectedAnswer = MutableStateFlow<Int?>(null)
    val triviaSelectedAnswer: StateFlow<Int?> = _triviaSelectedAnswer.asStateFlow()

    private val _triviaQuizComplete = MutableStateFlow(false)
    val triviaQuizComplete: StateFlow<Boolean> = _triviaQuizComplete.asStateFlow()

    // --- Community Corner States ---
    private val _communityPosts = MutableStateFlow<List<CommunityFeedPost>>(emptyList())
    val communityPosts: StateFlow<List<CommunityFeedPost>> = _communityPosts.asStateFlow()

    private val _isRecordingPost = MutableStateFlow(false)
    val isRecordingPost: StateFlow<Boolean> = _isRecordingPost.asStateFlow()

    // --- Games & Tracker Methods ---
    val emojiPool = listOf("🦁", "🦁", "🐵", "🐵", "🐼", "🐼", "🐸", "🐸", "🦊", "🦊", "🐘", "🐘")

    fun initMemoryGame() {
        val shuffled = emojiPool.shuffled().mapIndexed { index, emoji ->
            MemoryCard(id = index, emoji = emoji)
        }
        _memoryCards.value = shuffled
        _memoryMatchedPairs.value = 0
    }

    private var firstSelectedCardIndex: Int? = null

    fun selectMemoryCard(index: Int) {
        val cards = _memoryCards.value.toMutableList()
        val card = cards[index]
        if (card.isFlipped || card.isMatched) return

        cards[index] = card.copy(isFlipped = true)
        _memoryCards.value = cards

        val firstIndex = firstSelectedCardIndex
        if (firstIndex == null) {
            firstSelectedCardIndex = index
        } else {
            val firstCard = cards[firstIndex]
            if (firstCard.emoji == card.emoji) {
                cards[firstIndex] = firstCard.copy(isMatched = true)
                cards[index] = card.copy(isMatched = true)
                _memoryCards.value = cards
                _memoryMatchedPairs.value = _memoryMatchedPairs.value + 1
                firstSelectedCardIndex = null
                _gameScore.value = _gameScore.value + 20
                speakText("Shabash! It's a match!")

                if (_memoryMatchedPairs.value == emojiPool.size / 2) {
                    speakText("Bahut badiya! You matched all cards! You scored twenty more points.")
                }
            } else {
                viewModelScope.launch {
                    delay(1000)
                    val currentCards = _memoryCards.value.toMutableList()
                    if (currentCards[firstIndex].isMatched.not()) {
                        currentCards[firstIndex] = currentCards[firstIndex].copy(isFlipped = false)
                    }
                    if (currentCards[index].isMatched.not()) {
                        currentCards[index] = currentCards[index].copy(isFlipped = false)
                    }
                    _memoryCards.value = currentCards
                    firstSelectedCardIndex = null
                }
            }
        }
    }

    val triviaQuestions = listOf(
        TriviaQuestion(
            question = "Which is the capital city of India? (भारत की राजधानी क्या है?)",
            options = listOf("Mumbai (मुंबई)", "New Delhi (नई दिल्ली)", "Kolkata (कोलकाता)", "Chennai (चेन्नई)"),
            correctAnswerIndex = 1,
            hint = "It houses the Rashtrapati Bhavan."
        ),
        TriviaQuestion(
            question = "How many days are there in a week? (एक सप्ताह में कितने दिन होते हैं?)",
            options = listOf("5", "6", "7", "8"),
            correctAnswerIndex = 2,
            hint = "Starts with Monday, ends with Sunday."
        ),
        TriviaQuestion(
            question = "Which fruit is known as the King of Fruits? (फलों का राजा किसे कहा जाता है?)",
            options = listOf("Apple (सेब)", "Mango (आम)", "Banana (केला)", "Orange (संतरा)"),
            correctAnswerIndex = 1,
            hint = "It's yellow, sweet, and grows in summer."
        )
    )

    fun answerTriviaQuestion(optionIndex: Int) {
        if (_triviaSelectedAnswer.value != null) return
        _triviaSelectedAnswer.value = optionIndex
        val isCorrect = optionIndex == triviaQuestions[_triviaIndex.value].correctAnswerIndex
        if (isCorrect) {
            _gameScore.value = _gameScore.value + 15
            speakText("Vaah! Correct answer! Fifteen points added.")
        } else {
            speakText("Oops, that was incorrect. The correct answer was " + triviaQuestions[_triviaIndex.value].options[triviaQuestions[_triviaIndex.value].correctAnswerIndex])
        }
    }

    fun nextTriviaQuestion() {
        _triviaSelectedAnswer.value = null
        val nextIdx = _triviaIndex.value + 1
        if (nextIdx < triviaQuestions.size) {
            _triviaIndex.value = nextIdx
            speakText("Next Question: " + triviaQuestions[nextIdx].question)
        } else {
            _triviaQuizComplete.value = true
            speakText("Quiz completed! Excellent job keeping your brain active.")
        }
    }

    fun resetTriviaQuiz() {
        _triviaIndex.value = 0
        _triviaSelectedAnswer.value = null
        _triviaQuizComplete.value = false
        speakText("Starting the daily trivia game. Here is your first question.")
    }

    fun incrementSteps() {
        _dailySteps.value = _dailySteps.value + 100
        _gameScore.value = _gameScore.value + 5
        speakText("One hundred steps logged! Walk more to stay healthy and active.")
    }

    fun decrementSteps() {
        if (_dailySteps.value >= 100) {
            _dailySteps.value = _dailySteps.value - 100
        }
    }

    fun incrementWater() {
        _waterGlasses.value = _waterGlasses.value + 1
        speakText("Splendid! You drank one more glass of water. Keep hydrating!")
    }

    fun decrementWater() {
        if (_waterGlasses.value > 0) {
            _waterGlasses.value = _waterGlasses.value - 1
        }
    }

    fun updateProfile(name: String, age: Int, blood: String, emergency: String) {
        _seniorName.value = name
        _seniorAge.value = age
        _seniorBloodGroup.value = blood
        _seniorEmergencyContact.value = emergency
        speakText("Profile updated successfully!")
    }

    private var firstStepValue: Int? = null
    private var startingStepsAtAppLaunch = 1250

    fun updateRealSteps(totalStepsSinceBoot: Int) {
        if (firstStepValue == null) {
            firstStepValue = totalStepsSinceBoot
            startingStepsAtAppLaunch = _dailySteps.value
        } else {
            val delta = totalStepsSinceBoot - firstStepValue!!
            if (delta >= 0) {
                _dailySteps.value = startingStepsAtAppLaunch + delta
            }
        }
    }

    fun completeOnboarding(isKids: Boolean, name: String, age: Int, blood: String, emergency: String) {
        _isOnboardingCompleted.value = true
        _isKidsMode.value = isKids
        _seniorName.value = name
        _seniorAge.value = age
        _seniorBloodGroup.value = blood
        _seniorEmergencyContact.value = emergency
        if (isKids) {
            speakText("[hi]किड्स मोड ऑन हो गया है! खेलने के लिए तैयार हो जाओ!")
        } else {
            speakText("[hi]वरिष्ठ नागरिक सेवा मोड ऑन हो गया है। प्रणाम $name जी!")
        }
    }

    fun setMeditationAlarm(hour: Int, minute: Int) {
        _meditationHour.value = hour
        _meditationMinute.value = minute
        _meditationAlarmSet.value = true
        speakText("[hi]प्रतिदिन $hour बजकर $minute मिनट पर ध्यान लगाने का अलार्म सेट हो गया है।")
    }

    fun disableMeditationAlarm() {
        _meditationAlarmSet.value = false
        speakText("[hi]ध्यान लगाने का अलार्म बंद कर दिया गया है।")
    }

    fun toggleCaregiverSyncMeds() {
        _caregiverSyncMeds.value = !_caregiverSyncMeds.value
        speakText("Caregiver medication monitoring status updated.")
    }

    fun toggleCaregiverSyncSOS() {
        _caregiverSyncSOS.value = !_caregiverSyncSOS.value
        speakText("Caregiver SOS alerts status updated.")
    }

    fun toggleCaregiverSyncLocation() {
        _caregiverSyncLocation.value = !_caregiverSyncLocation.value
        speakText("Live location sharing status updated.")
    }

    fun initCommunityPosts() {
        _communityPosts.value = listOf(
            CommunityFeedPost(
                id = 1,
                author = "Sharma ji (Ramesh)",
                relativeTime = "10 mins ago",
                text = "आज सुबह का मौसम बहुत सुहावना था। बगीचे में सैर करके आनंद आ गया। आशा है आप सब भी स्वस्थ हैं!",
                likesCount = 5,
                hasAudio = true,
                audioDuration = "0.2 min"
            ),
            CommunityFeedPost(
                id = 2,
                author = "Sudha ji",
                relativeTime = "1 hour ago",
                text = "हरे कृष्ण! आज दोपहर अच्युतम केशवम भजन का समूह गान होगा। सभी अवश्य जुड़ें!",
                likesCount = 12,
                hasAudio = false
            ),
            CommunityFeedPost(
                id = 3,
                author = "Gupta ji",
                relativeTime = "3 hours ago",
                text = "मैंने आज अपनी सुबह की सैर पूरी की और ५ ग्लास पानी भी पी लिया है। बच्चों ने कहा सेहत ही सबसे बड़ी संपत्ति है।",
                likesCount = 8,
                hasAudio = true,
                audioDuration = "0.4 min"
            )
        )
    }

    fun likePost(postId: Int) {
        val current = _communityPosts.value.map {
            if (it.id == postId) it.copy(likesCount = it.likesCount + 1) else it
        }
        _communityPosts.value = current
        speakText("Liked.")
    }

    fun playPostAudio(post: CommunityFeedPost) {
        speakText("[hi]वॉयस पोस्ट ${post.author} से: ${post.text}")
    }

    fun addNewCommunityPost(text: String) {
        val newPost = CommunityFeedPost(
            id = _communityPosts.value.size + 1,
            author = "Satish (You)",
            relativeTime = "Just now",
            text = text,
            likesCount = 0,
            hasAudio = true,
            audioDuration = "0.1 min"
        )
        _communityPosts.value = listOf(newPost) + _communityPosts.value
        speakText("Story shared with Community Corner!")
    }

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

        // Initialize brain games and community feed
        initMemoryGame()
        initCommunityPosts()

        // Fetch Real-time live weather for Rajpura, Punjab periodically every 3 minutes (180,000 ms)
        viewModelScope.launch {
            while (true) {
                fetchLiveWeatherForRajpura()
                delay(180000)
            }
        }

        // Start real-time walking tracker (uses Health Connect when authorized, else falls back to simulation)
        viewModelScope.launch {
            while (true) {
                if (healthConnectManager.isAvailable() && healthConnectManager.hasReadStepsPermission()) {
                    val steps = healthConnectManager.fetchDailySteps()
                    if (steps > 0) {
                        _dailySteps.value = steps
                    }
                } else {
                    val addedSteps = (2..6).random()
                    _dailySteps.value = _dailySteps.value + addedSteps
                }
                delay(8000)
            }
        }
    }

    // --- Weather Fetch Engine ---
    fun fetchLiveWeatherForRajpura() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder()
                    .url("https://api.open-meteo.com/v1/forecast?latitude=30.4839&longitude=76.5947&current_weather=true")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string()
                        if (bodyString != null) {
                            val json = JSONObject(bodyString)
                            val current = json.getJSONObject("current_weather")
                            val temp = current.getDouble("temperature")
                            val weathercode = current.getInt("weathercode")
                            
                            val mappedEmoji = when (weathercode) {
                                0 -> "☀️"
                                1, 2, 3 -> "⛅"
                                45, 48 -> "🌫️"
                                51, 53, 55 -> "🌧️"
                                61, 63, 65, 66, 67, 80, 81, 82 -> "🌧️☔"
                                71, 73, 75, 77, 85, 86 -> "❄️"
                                95, 96, 99 -> "⛈️"
                                else -> "⛅"
                            }
                            val mappedText = when (weathercode) {
                                0 -> "Clear Sky"
                                1, 2, 3 -> "Partly Cloudy"
                                45, 48 -> "Foggy"
                                51, 53, 55 -> "Light Drizzle"
                                61, 63, 65 -> "Raining in Rajpura"
                                80, 81, 82 -> "Rain Showers"
                                95, 96, 99 -> "Thunderstorm"
                                else -> "Fair Weather"
                            }
                            _weatherTemp.value = "${temp.toInt()}°C"
                            _weatherEmoji.value = mappedEmoji
                            _weatherStatusText.value = mappedText
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to fetch open-meteo weather", e)
            }
        }
    }

    // --- Sahil Auto Companion Entry Speech ---
    fun onEnterCompanionScreen() {
        viewModelScope.launch {
            _speechEvent.emit("STOP_TTS")
            delay(250)
            speakText("[hi]हेलो हेलो! नमस्तेजी! मैं आपका साथी साहिल हूँ। आपसे बात करने के लिए बहुत उत्सुक था! आज आपका दिन कैसा रहा? घर पर सब कैसे हैं? मुझसे बातें कीजिए, मुझे आपकी आवाज़ सुनना बहुत अच्छा लगता है!")
        }
    }

    // --- Keeps Smiling (Jokes) Actions ---
    fun onEnterJokesScreen() {
        _isJokePlaying.value = true
        speakCurrentJoke()
    }

    fun toggleJokePlaying() {
        val nextPlaying = !_isJokePlaying.value
        _isJokePlaying.value = nextPlaying
        if (nextPlaying) {
            speakCurrentJoke()
        } else {
            viewModelScope.launch {
                _speechEvent.emit("STOP_TTS")
            }
        }
    }

    fun nextJoke() {
        _currentJokeIndex.value = (_currentJokeIndex.value + 1) % jokes.size
        if (_isJokePlaying.value) {
            speakCurrentJoke()
        }
    }

    fun prevJoke() {
        val current = _currentJokeIndex.value
        _currentJokeIndex.value = if (current == 0) jokes.size - 1 else current - 1
        if (_isJokePlaying.value) {
            speakCurrentJoke()
        }
    }

    private fun speakCurrentJoke() {
        val jokeText = jokes[_currentJokeIndex.value]
        speakText("[hi]सुनिए एक प्यारा चुटकुला। $jokeText")
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
            // Check commands for Calling Beta/Rahul
            containsAny(text, listOf("beta", "rahul", "call", "phone", "dial", "फ़ोन", "फ़ोन")) -> {
                setKidsMode(false)
                speakText("[hi]राहुल बेटा को फ़ोन मिलाया जा रहा है। कृपया प्रतीक्षा करें...")
            }
            // Check commands for News / Samachar
            containsAny(text, listOf("khabar", "samachar", "news", "bulletin", "समाचार", "खबर")) -> {
                setKidsMode(false)
                speakText("[hi]आज के मुख्य समाचार: भारत ने चिकित्सा के क्षेत्र में बड़ी सफलता हासिल की है। वैज्ञानिकों ने बुजुर्गों की देखभाल के लिए नया सहायक रोबोट विकसित किया है। और आज दिन भर धूप खिली रहेगी।")
            }
            // Check commands for Weather
            containsAny(text, listOf("mausam", "weather", "temperature", "तापमान", "मौसम")) -> {
                setKidsMode(false)
                speakText("[hi]आज का मौसम बहुत ही सुहावना और साफ़ है। बाहर का तापमान छब्बीस डिग्री सेल्सियस है। हल्की और ठंडी हवा चल रही है, सैर करने के लिए यह बहुत उत्तम समय है!")
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
        speakText("[hi]कहानी का नाम: ${story.title}। कहानी शुरू होती है: ${story.content}। इस कहानी की सीख है: ${story.moral}")
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
            val generatedId = repository.insert(
                MedicationReminder(
                    medicineName = name,
                    dosage = dosage,
                    time = time
                )
            )
            com.example.receiver.AlarmReceiver.scheduleMedicationAlarm(
                context,
                generatedId.toInt(),
                name,
                dosage,
                time
            )
            speakText("Medication reminder for $name added successfully!")
        }
    }

    fun toggleReminderActive(reminder: MedicationReminder) {
        viewModelScope.launch {
            val newActive = !reminder.isActive
            repository.update(reminder.copy(isActive = newActive))
            if (newActive) {
                com.example.receiver.AlarmReceiver.scheduleMedicationAlarm(
                    context,
                    reminder.id,
                    reminder.medicineName,
                    reminder.dosage,
                    reminder.time
                )
                speakText("Alarm enabled for ${reminder.medicineName}")
            } else {
                com.example.receiver.AlarmReceiver.cancelMedicationAlarm(context, reminder.id)
                speakText("Alarm disabled for ${reminder.medicineName}")
            }
        }
    }

    fun deleteReminder(reminder: MedicationReminder) {
        viewModelScope.launch {
            repository.delete(reminder)
            com.example.receiver.AlarmReceiver.cancelMedicationAlarm(context, reminder.id)
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

    private var mediaPlayer: android.media.MediaPlayer? = null

    fun playBhajan(bhajan: Bhajan) {
        stopBhajan()
        _playingBhajan.value = bhajan
        _bhajanProgress.value = 0f

        val url = when(bhajan.id) {
            1 -> "https://archive.org/download/GayatriMantraChant108Times/Gayatri_Mantra_Chant.mp3"
            2 -> "https://archive.org/download/achyutam_keshavam_peaceful/Achyutam_Keshavam.mp3"
            3 -> "https://archive.org/download/om_jai_jagdish_hare_devotional/Om_Jai_Jagdish_Hare.mp3"
            4 -> "https://archive.org/download/raghupati_raghav_raja_ram_meditation/Raghupati_Raghav.mp3"
            5 -> "https://archive.org/download/hanuman_chalisa_peace/Hanuman_Chalisa.mp3"
            6 -> "https://archive.org/download/krishna_govind_hare_murari_peace/Krishna_Govind.mp3"
            7 -> "https://archive.org/download/shiv_tandav_stotram_energy/Shiv_Tandav.mp3"
            8 -> "https://archive.org/download/hare_krishna_mahamantra_peaceful/Hare_Krishna_Mantra.mp3"
            else -> "https://archive.org/download/GayatriMantraChant108Times/Gayatri_Mantra_Chant.mp3"
        }

        try {
            val player = android.media.MediaPlayer().apply {
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { mp ->
                    mp.start()
                    bhajanJob = viewModelScope.launch(Dispatchers.Main) {
                        while (mp.isPlaying) {
                            val duration = mp.duration
                            if (duration > 0) {
                                _bhajanProgress.value = mp.currentPosition.toFloat() / duration.toFloat()
                            }
                            delay(1000)
                        }
                    }
                }
                setOnCompletionListener {
                    stopBhajan()
                    speakText("[hi]भजन पूर्ण हुआ। मन की शांति और आनंद का अनुभव करें।")
                }
                setOnErrorListener { _, _, _ ->
                    viewModelScope.launch {
                        speakText("[hi]इंटरनेट धीमा होने के कारण भजन को टेक्स्ट में सुनाया जा रहा है।")
                        playBhajanFallback(bhajan)
                    }
                    true
                }
            }
            mediaPlayer = player
            player.prepareAsync()
        } catch (e: Exception) {
            Log.e("MainViewModel", "MediaPlayer failed", e)
            playBhajanFallback(bhajan)
        }
    }

    private fun playBhajanFallback(bhajan: Bhajan) {
        bhajanJob = viewModelScope.launch {
            val lines = bhajan.lyrics.split("\n").filter { it.isNotBlank() }
            if (lines.isNotEmpty()) {
                val totalLines = lines.size
                for (index in 0 until totalLines) {
                    val currentLine = lines[index]
                    _bhajanProgress.value = index.toFloat() / totalLines.toFloat()
                    speakText("[bhajan]$currentLine")
                    delay(5800)
                }
            } else {
                speakText("[bhajan]${bhajan.lyrics}")
                for (i in 1..100) {
                    delay(300)
                    _bhajanProgress.value = i / 100f
                }
            }
            _bhajanProgress.value = 1f
            delay(1500)
            _playingBhajan.value = null
            _bhajanProgress.value = 0f
            speakText("[hi]भजन पूर्ण हुआ। मन की शांति और आनंद का अनुभव करें।")
        }
    }

    fun stopBhajan() {
        bhajanJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Safe ignore
        }
        mediaPlayer = null
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

    // ==========================================
    // --- FEATURE 1: DR. KI AWAAZ (Prescription Scanner) ---
    // ==========================================
    fun scanPrescription(base64Image: String) {
        _prescriptionLoading.value = true
        _prescriptionResult.value = null
        _prescriptionMedicines.value = emptyList()
        _prescriptionSpeechEnglish.value = ""
        _prescriptionSpeechHindi.value = ""
        _prescriptionSpeechPunjabi.value = ""

        viewModelScope.launch(Dispatchers.IO) {
            val prompt = """
                Analyze this doctor's prescription. Detect and extract all medicines. 
                For each medicine, extract:
                1. name: Name of the medicine
                2. dosage: Exact dosage (e.g., 1 tablet, 5ml, 2 drops)
                3. timing: Timing or frequency (e.g., Once daily in morning, After breakfast, Twice a day)
                
                Also, write a short, warm, supportive, and loud audio script to read to an elderly patient in:
                - englishSpeech: A clear reading script in English.
                - hindiSpeech: A clear reading script in Hindi (written in standard Hindi, Devanagari script).
                - punjabiSpeech: A clear reading script in Punjabi (written in standard Punjabi, Gurmukhi script).
                
                Make sure the scripts include all the medicines, their dosages, and timings clearly in simple words so the grandparents can understand perfectly.
                
                Return the response strictly as a single JSON object with the following keys:
                {
                  "medicines": [
                    { "name": "...", "dosage": "...", "timing": "..." }
                  ],
                  "englishSpeech": "...",
                  "hindiSpeech": "...",
                  "punjabiSpeech": "..."
                }
            """.trimIndent()

            val responseText = GeminiService.analyzeImage(prompt, base64Image)
            
            viewModelScope.launch(Dispatchers.Main) {
                _prescriptionLoading.value = false
                try {
                    var cleanJson = responseText.trim()
                    if (cleanJson.startsWith("```json")) {
                        cleanJson = cleanJson.removePrefix("```json")
                    }
                    if (cleanJson.endsWith("```")) {
                        cleanJson = cleanJson.removeSuffix("```")
                    }
                    cleanJson = cleanJson.trim()

                    val jsonObject = JSONObject(cleanJson)
                    
                    val medicinesList = mutableListOf<PrescriptionMedicine>()
                    val medicinesArray = jsonObject.getJSONArray("medicines")
                    for (i in 0 until medicinesArray.length()) {
                        val medObj = medicinesArray.getJSONObject(i)
                        medicinesList.add(
                            PrescriptionMedicine(
                                name = medObj.getString("name"),
                                dosage = medObj.getString("dosage"),
                                timing = medObj.getString("timing")
                            )
                        )
                    }
                    _prescriptionMedicines.value = medicinesList
                    _prescriptionSpeechEnglish.value = jsonObject.optString("englishSpeech", "No English speech script available.")
                    _prescriptionSpeechHindi.value = jsonObject.optString("hindiSpeech", "कोई हिंदी अनुवाद उपलब्ध नहीं है।")
                    _prescriptionSpeechPunjabi.value = jsonObject.optString("punjabiSpeech", "ਕੋਈ ਪੰਜਾਬੀ ਅਨੁਵਾਦ ਉਪਲਬਧ ਨਹੀਂ ਹੈ।")
                    _prescriptionResult.value = "Prescription parsed successfully! Alarms are being set."
                    
                    // Auto-set the medication alarms
                    autoSetAllAlarmsFromScannedPrescription()
                    
                    speakPrescriptionHindi()
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error parsing prescription json: ${e.message}", e)
                    _prescriptionResult.value = "Could not parse JSON. Gemini Response:\n$responseText"
                }
            }
        }
    }

    fun autoSetAllAlarmsFromScannedPrescription() {
        val meds = _prescriptionMedicines.value
        if (meds.isEmpty()) return
        
        viewModelScope.launch {
            meds.forEach { med ->
                val timingLower = med.timing.lowercase()
                val alarmTime = when {
                    timingLower.contains("morning") || timingLower.contains("breakfast") -> "08:00 AM"
                    timingLower.contains("afternoon") || timingLower.contains("lunch") || timingLower.contains("noon") -> "01:30 PM"
                    timingLower.contains("evening") || timingLower.contains("tea") -> "05:00 PM"
                    timingLower.contains("night") || timingLower.contains("dinner") || timingLower.contains("bed") -> "09:00 PM"
                    else -> "10:00 AM"
                }
                repository.insert(
                    MedicationReminder(
                        medicineName = med.name,
                        dosage = med.dosage,
                        time = alarmTime
                    )
                )
            }
            speakText("Perfect! Alarms for all ${meds.size} medicines have been set automatically!")
        }
    }

    fun speakPrescriptionEnglish() {
        val text = _prescriptionSpeechEnglish.value
        if (text.isNotEmpty()) {
            speakText("[en]$text")
        }
    }

    fun speakPrescriptionHindi() {
        val text = _prescriptionSpeechHindi.value
        if (text.isNotEmpty()) {
            speakText("[hi]$text")
        }
    }

    fun speakPrescriptionPunjabi() {
        val text = _prescriptionSpeechPunjabi.value
        if (text.isNotEmpty()) {
            speakText("[pa]$text")
        }
    }

    // ==========================================
    // --- FEATURE 2: BUZURGON KA SAHIL (Elder Companion) ---
    // ==========================================
    fun sendCompanionMessage(text: String) {
        if (text.isBlank()) return
        
        val userMsg = CompanionMessage("user", text)
        val currentHistory = _companionChatHistory.value.toMutableList()
        currentHistory.add(userMsg)
        _companionChatHistory.value = currentHistory
        
        _companionLoading.value = true
        
        viewModelScope.launch(Dispatchers.IO) {
            val historyPrompt = currentHistory.joinToString("\n") { 
                "${if (it.sender == "user") "Grandparent" else "Companion Sahil"}: ${it.text}"
            }
            
            val systemPrompt = """
                You are Sahil (साहिल), a respectful, caring, and sweet AI Companion for lonely elderly grandparents (Dada-Dadi, Nana-Nani) in India. 
                Talk to them in a loving, gentle, and respectful Hindi/Hinglish language. Show deep respect and kindness. 
                Keep your responses concise, comforting, and under 3-4 sentences.
                Always respond as Sahil. 
                
                Here is the chat history:
                $historyPrompt
                
                Respond to the last message of the Grandparent with affection and warmth.
            """.trimIndent()
            
            val reply = GeminiService.generateText(systemPrompt)
            
            viewModelScope.launch(Dispatchers.Main) {
                _companionLoading.value = false
                val compMsg = CompanionMessage("companion", reply)
                val updatedHistory = _companionChatHistory.value.toMutableList()
                updatedHistory.add(compMsg)
                _companionChatHistory.value = updatedHistory
                
                speakText("[hi]$reply")
            }
        }
    }

    fun triggerFeelingLonely() {
        _companionLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val prompt = """
                The grandparent just pressed the 'I feel lonely' (अकेलापन लग रहा है) button because they are feeling isolated. 
                Speak directly to them as Sahil, their loving grandson/companion. 
                Give them an extremely warm, sweet, reassuring, and comforting greeting in Hindi. 
                Tell them they are deeply loved, they are not alone, and tell them a lovely positive suvichar or small moral thought to uplift their spirit. 
                Keep the text very warm, smooth, clear, and under 4 lines so it reads beautifully.
            """.trimIndent()
            
            val reply = GeminiService.generateText(prompt)
            
            viewModelScope.launch(Dispatchers.Main) {
                _companionLoading.value = false
                val compMsg = CompanionMessage("companion", reply)
                val updatedHistory = _companionChatHistory.value.toMutableList()
                updatedHistory.add(compMsg)
                _companionChatHistory.value = updatedHistory
                
                speakText("[hi]$reply")
            }
        }
    }

    fun clearCompanionChat() {
        _companionChatHistory.value = listOf(
            CompanionMessage("companion", "नमस्ते दादाजी/दादीजी! मैं आपका साथी साहिल हूँ। आज आप कैसे महसूस कर रहे हैं? अगर आपको अकेलापन लग रहा है तो ऊपर 'Akeli Lag Rahi Hai' बटन दबाएं, या मुझसे बातें करें।")
        )
    }

    // ==========================================
    // --- FEATURE 3: SHOR SHARABA METER (Noise Meter) ---
    // ==========================================
    private var audioRecord: AudioRecord? = null
    private var noiseTrackingJob: Job? = null

    fun startNoiseTracking() {
        if (_isNoiseTracking.value) return
        _isNoiseTracking.value = true
        _noiseComplaintDraft.value = null

        noiseTrackingJob = viewModelScope.launch(Dispatchers.IO) {
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

            try {
                val record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
                audioRecord = record

                if (record.state == AudioRecord.STATE_INITIALIZED) {
                    record.startRecording()
                    val buffer = ShortArray(bufferSize)
                    
                    while (_isNoiseTracking.value) {
                        val readSize = record.read(buffer, 0, buffer.size)
                        if (readSize > 0) {
                            var sum = 0.0
                            for (i in 0 until readSize) {
                                val sample = buffer[i].toFloat()
                                sum += sample * sample
                            }
                            val rms = Math.sqrt(sum / readSize)
                            var db = 20 * Math.log10(rms + 1.0)
                            db = if (db > 10.0) db * 1.5 else 32.0
                            if (db > 100.0) db = 100.0
                            if (db < 32.0) db = 32.0 + (Math.random() * 3.0)

                            _currentDecibel.value = db.toFloat()
                        }
                        delay(150)
                    }
                } else {
                    simulateNoiseTracking()
                }
            } catch (e: SecurityException) {
                Log.e("MainViewModel", "SecurityException starting noise tracker: ${e.message}")
                simulateNoiseTracking()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Exception starting noise tracker: ${e.message}")
                simulateNoiseTracking()
            }
        }
    }

    private suspend fun simulateNoiseTracking() {
        Log.d("MainViewModel", "Falling back to simulated noise tracker")
        while (_isNoiseTracking.value) {
            val randomNoise = 35f + (Math.random() * 20f).toFloat()
            _currentDecibel.value = randomNoise
            delay(200)
        }
    }

    fun stopNoiseTracking() {
        _isNoiseTracking.value = false
        noiseTrackingJob?.cancel()
        try {
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error releasing AudioRecord: ${e.message}")
        }
        audioRecord = null
    }

    fun setManualDecibelValueForTesting(db: Float) {
        stopNoiseTracking()
        _currentDecibel.value = db
    }

    fun generateNoiseComplaintDraft() {
        _companionLoading.value = true
        _noiseComplaintDraft.value = null
        val db = _currentDecibel.value
        
        viewModelScope.launch(Dispatchers.IO) {
            val prompt = """
                Write a formal and highly professional noise complaint draft in both English and Hindi.
                The user measured a noise level of ${String.format("%.1f", db)} dB in their area (school/hospital/home vicinity).
                State the measured noise level and explain how such noise level (over ${String.format("%.1f", db)} dB) 
                causes stress, sleep deprivation for toddlers, and blood pressure risks for elder grandparents in the house.
                Write a polite but firm request to the community manager or local police, demanding silence and control.
                Keep it extremely clean, with clear sections: "ENGLISH DRAFT" and "HINDI DRAFT".
            """.trimIndent()

            val response = GeminiService.generateText(prompt)
            
            viewModelScope.launch(Dispatchers.Main) {
                _companionLoading.value = false
                _noiseComplaintDraft.value = response
                speakText("Here is your noise complaint draft. It contains both Hindi and English copies.")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        alarmCheckJob?.cancel()
        bhajanJob?.cancel()
        sosJob?.cancel()
        stopNoiseTracking()
    }
}

// Factory class for MainViewModel
class MainViewModelFactory(
    private val repository: ReminderRepository,
    private val healthConnectManager: com.example.data.HealthConnectManager,
    private val context: android.content.Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, healthConnectManager, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// --- List of Peaceful Bhajans ---
object BhajansList {
    val GAYATRI = Bhajan(
        id = 1,
        title = "Gayatri Mantra (गायत्री मंत्र)",
        duration = "3.5 min",
        lyrics = "ॐ भूर्भुवः स्वः।\nतत्सवितुर्वरेण्यं।\nभर्गो देवस्य धीमहि।\nधियो यो नः प्रचोदयात्॥\nॐ भूर्भुवः स्वः।\nतत्सवितुर्वरेण्यं।\nभर्गो देवस्य धीमहि।\nधियो यो नः प्रचोदयात्॥\nॐ शांतिः शांतिः शांतिः॥",
        description = "Chanting this ancient mantra brings supreme intellectual clarity, wisdom, and eternal cosmic peace to the soul."
    )
    val ACHYUTAM = Bhajan(
        id = 2,
        title = "Achyutam Keshavam (अच्युतम् केशवम्)",
        duration = "4.2 min",
        lyrics = "अच्युतम् केशवम् कृष्ण दामोदरम्।\nराम नारायणम् जानकी वल्लभम्॥\nकौन कहता है भगवान आते नहीं।\nतुम मीरा के जैसे बुलाते नहीं॥\nअच्युतम् केशवम् कृष्ण दामोदरम्।\nराम नारायणम् जानकी वल्लभम्॥\nकौन कहता है भगवान खाते नहीं।\nबेर शबरी के जैसे खिलाते नहीं॥\nअच्युतम् केशवम् कृष्ण दामोदरम्।\nराम नारायणम् जानकी वल्लभम्॥\nकौन कहता है भगवान सोते नहीं।\nमाँ यशोदा के जैसे सुलाते नहीं॥\nहरे कृष्ण हरे राम, राम राम हरे हरे॥",
        description = "A peaceful song praising Lord Krishna, ideal for inducing deep sleep, relieving anxiety, and pure meditation."
    )
    val AARTI = Bhajan(
        id = 3,
        title = "Om Jai Jagdish Hare (ॐ जय जगदीश हरे)",
        duration = "5.0 min",
        lyrics = "ॐ जय जगदीश हरे, स्वामी जय जगदीश हरे।\nभक्त जनों के संकट, क्षण में दूर करे॥\nजो ध्यावे फल पावे, दुःख बिनसे मन का।\nसुख सम्पत्ति घर आवे, कष्ट मिटे तन का॥\nमात-पिता तुम मेरे, शरण गहूं किसकी।\nतुम बिन और न दूजा, आस करूं जिसकी॥\nतुम पूरण परमात्मा, तुम अन्तर्यामी।\nपारब्रह्म परमेश्वर, तुम सबके स्वामी॥\nॐ जय जगदीश हरे, स्वामी जय जगदीश हरे॥",
        description = "Traditional evening prayer sung with family to express deep gratitude and cleanse the surrounding atmosphere."
    )
    val RAM_DHYAN = Bhajan(
        id = 4,
        title = "Raghupati Raghav Raja Ram (रघुपति राघव)",
        duration = "3.8 min",
        lyrics = "रघुपति राघव राजाराम, पतित पावन सीताराम॥\nसीताराम सीताराम, भज प्यारे तू सीताराम॥\nईश्वर अल्लाह तेरो नाम, सब को सन्मति दे भगवान॥\nरघुपति राघव राजाराम, पतित पावन सीताराम॥\nजय सीता राम, जय सीता राम, जय सीता राम॥",
        description = "Mahatma Gandhi's favourite meditative chant for universal love, social harmony, and peaceful breathing."
    )
    val HANUMAN_CHALISA = Bhajan(
        id = 5,
        title = "Shri Hanuman Chalisa (श्री हनुमान चालीसा)",
        duration = "6.5 min",
        lyrics = "श्री गुरु चरन सरोज रज, निज मनु मुकुरु सुधारि।\nबरनऊँ रघुबर बिमल जसु, जो दायकु फल चारि॥\nबुद्धिहीन तनु जानिके, सुमिरौं पवन-कुमार।\nबल बुधि बिद्या देहु मोहिं, हरहु कलेस बिकार॥\nजय हनुमान ज्ञान गुन सागर। जय कपीस तिहुँ लोक उजागर॥\nराम दूत अतुलित बल धामा। अंजनि-पुत्र पवनसुत नामा॥\nमहाबीर बिक्रम बजरंगी। कुमति निवार सुमति के संगी॥\nकंचन बरन बिराज सुबेसा। कानन कुंडल कुंचित केसा॥\nहाथ बज्र औ ध्वजा बिराजै। काँधे मूँज जनेऊ साजै॥\nशंकर सुवन केसरीनंदन। तेज प्रताप महा जग बन्दन॥\nविद्यावान गुनी अति चातुर। राम काज करिबे को आतुर॥\nप्रभु चरित्र सुनिबे को रसिया। राम लखन सीता मन बसिया॥\nसूक्ष्म रूप धरि सियहिं दिखावा। बिकट रूप धरि लंक जरावा॥\nभीम रूप धरि असुर संहारे। रामचंद्र के काज सँवारे॥\nलाय सजीवन लखन जियाये। श्रीरघुबीर हरषि उर लाये॥",
        description = "Powerful devotional verses dedicated to Lord Hanuman, providing absolute courage, mental strength, and protection."
    )
    val KRISHNA_GOVIND = Bhajan(
        id = 6,
        title = "Shree Krishna Govind (श्री कृष्ण गोविंद)",
        duration = "4.5 min",
        lyrics = "श्री कृष्ण गोविंद हरे मुरारी, हे नाथ नारायण वासुदेवा॥\nपितु मात स्वामी सखा हमारे, हे नाथ नारायण वासुदेवा॥\nश्री कृष्ण गोविंद हरे मुरारी, हे नाथ नारायण वासुदेवा॥\nअच्युतम् केशवम् रामनारायणम्, कृष्ण दामोदरम् वासुदेवम् भजे॥\nहे नाथ नारायण वासुदेवा, हे नाथ नारायण वासुदेवा॥",
        description = "An deeply emotional and comforting bhajan surrender song to Sri Krishna for stress relief and peace."
    )
    val SHIV_TANDAV = Bhajan(
        id = 7,
        title = "Shiv Tandav Stotram (शिव तांडव)",
        duration = "5.5 min",
        lyrics = "जटाटवीगलज्जलप्रवाहपावितस्थले गलेऽवलम्ब्य लम्बितां भुजङ्गतुङ्गमालिकाम्।\nडमड्डमड्डमड्डमन्निनादवड्डमर्वयं चकार चण्डताण्डवं तनोतु नः शिवः शिवम्॥\nजटाकटा हसंभ्रमभ्रमन्निलिंपनिर्झरी विलोलवीचिवल्लरी विराजमानमूर्धनि।\nधगद्धगद्धगज्ज्वलल्ललाटपट्टपावके किशोरचंद्रशेखरे रतिः प्रतिक्षणं मम॥\nधराधरेंद्रनंदिनीविलासबंधुबंधुर स्फुरद्दिगंतसंततिप्रमोदमानमानसे।\nकृपाकटाक्षधोरणीनिरुद्धदुर्धरापदि क्वचिद्दिगंबरे मनो विनोदमेतु वस्तुनि॥\nहर हर महादेव, हर हर महादेव, हर हर महादेव॥",
        description = "High-energy Sanskrit chanting praising Lord Shiva's cosmic dance, invoking willpower, energy, and inner fire."
    )
    val HARE_KRISHNA = Bhajan(
        id = 8,
        title = "Hare Krishna Mahamantra (हरे कृष्ण मंत्र)",
        duration = "4.0 min",
        lyrics = "हरे कृष्ण हरे कृष्ण, कृष्ण कृष्ण हरे हरे।\nहरे राम हरे राम, राम राम हरे हरे॥\nहरे कृष्ण हरे कृष्ण, कृष्ण कृष्ण हरे हरे।\nहरे राम हरे राम, राम राम हरे हरे॥\nहरे कृष्ण हरे कृष्ण, कृष्ण कृष्ण हरे हरे।\nहरे राम हरे राम, राम राम हरे हरे॥\nश्री कृष्ण शरणं मम, श्री कृष्ण शरणं मम॥",
        description = "The absolute sweet Mahamantra chant, bringing immediate joy, relaxation, and divine connection."
    )
}
