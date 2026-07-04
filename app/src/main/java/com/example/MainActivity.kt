package com.example

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.ReminderRepository
import com.example.ui.screens.AppNavigation
import com.example.ui.theme.GenerationConnectTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.MainViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var viewModel: MainViewModel
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val grantedPost = permissions[android.Manifest.permission.POST_NOTIFICATIONS] ?: false
        val grantedCall = permissions[android.Manifest.permission.CALL_PHONE] ?: false
        val grantedActivity = permissions[android.Manifest.permission.ACTIVITY_RECOGNITION] ?: false
        Log.d("MainActivity", "Permissions updated: POST=$grantedPost, CALL=$grantedCall, RECOGNITION=$grantedActivity")
    }

    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null

    override fun onResume() {
        super.onResume()
        try {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            if (stepSensor != null) {
                sensorManager?.registerListener(sensorListener, stepSensor, SensorManager.SENSOR_DELAY_UI)
                Log.d("MainActivity", "Step counter sensor registered successfully")
            } else {
                Log.d("MainActivity", "Step counter sensor not available")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error registering step counter sensor", e)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            sensorManager?.unregisterListener(sensorListener)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error unregistering step counter", e)
        }
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null) return
            if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                val totalStepsSinceBoot = event.values[0].toInt()
                Log.d("MainActivity", "Step counter sensor changed: $totalStepsSinceBoot")
                viewModel.updateRealSteps(totalStepsSinceBoot)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    // Safe launcher for Speech-to-Text to prevent crashes on non-GMS low-end devices
    private val speechToTextLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val spokenTexts = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val firstCommand = spokenTexts?.firstOrNull()
            if (firstCommand != null) {
                viewModel.processVoiceCommand(firstCommand)
            }
        }
        viewModel.setIsListening(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request runtime permissions dynamically for calling, notification, and steps
        val permissionsToRequest = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionsToRequest.add(android.Manifest.permission.CALL_PHONE)
        permissionsToRequest.add(android.Manifest.permission.ACTIVITY_RECOGNITION)
        requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())

        // 1. Initialize Room Database, Repository, and Health Connect Manager
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ReminderRepository(database.reminderDao())
        val healthConnectManager = com.example.data.HealthConnectManager(applicationContext)

        // 2. Initialize MainViewModel with Factory (Simple constructor injection)
        val factory = MainViewModelFactory(repository, healthConnectManager, applicationContext)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        // SharedPreferences persistence for Onboarding and profile preferences
        val prefs = getSharedPreferences("SeniorCarePrefs", Context.MODE_PRIVATE)
        val completed = prefs.getBoolean("onboarding_completed", false)
        val isKids = prefs.getBoolean("is_kids_mode", false)
        val seniorName = prefs.getString("senior_name", "Satish Sharma") ?: "Satish Sharma"
        val seniorAge = prefs.getInt("senior_age", 72)
        val seniorBlood = prefs.getString("senior_blood", "O+") ?: "O+"
        val seniorEmergency = prefs.getString("senior_emergency", "+91 98765 43210") ?: "+91 98765 43210"

        if (completed) {
            viewModel.completeOnboarding(isKids, seniorName, seniorAge, seniorBlood, seniorEmergency)
        }

        // Schedule the first 5-minute hydration reminder alarm on launch
        com.example.receiver.AlarmReceiver.scheduleNextHydrationAlarm(this)

        lifecycleScope.launch {
            viewModel.isOnboardingCompleted.collectLatest { completedState ->
                if (completedState) {
                    prefs.edit().apply {
                        putBoolean("onboarding_completed", true)
                        putBoolean("is_kids_mode", viewModel.isKidsMode.value)
                        putString("senior_name", viewModel.seniorName.value)
                        putInt("senior_age", viewModel.seniorAge.value)
                        putString("senior_blood", viewModel.seniorBloodGroup.value)
                        putString("senior_emergency", viewModel.seniorEmergencyContact.value)
                        apply()
                    }
                }
            }
        }

        // Keep real-time edits synchronized continuously in SharedPreferences
        lifecycleScope.launch {
            viewModel.seniorName.collectLatest { name ->
                if (viewModel.isOnboardingCompleted.value) {
                    prefs.edit().putString("senior_name", name).apply()
                }
            }
        }
        lifecycleScope.launch {
            viewModel.seniorAge.collectLatest { age ->
                if (viewModel.isOnboardingCompleted.value) {
                    prefs.edit().putInt("senior_age", age).apply()
                }
            }
        }
        lifecycleScope.launch {
            viewModel.seniorBloodGroup.collectLatest { blood ->
                if (viewModel.isOnboardingCompleted.value) {
                    prefs.edit().putString("senior_blood", blood).apply()
                }
            }
        }
        lifecycleScope.launch {
            viewModel.seniorEmergencyContact.collectLatest { emergency ->
                if (viewModel.isOnboardingCompleted.value) {
                    prefs.edit().putString("senior_emergency", emergency).apply()
                }
            }
        }

        // 3. Initialize native Text-To-Speech
        textToSpeech = TextToSpeech(this, this)

        // 4. Collect Text-To-Speech requests from ViewModel Flow
        lifecycleScope.launch {
            viewModel.speechEvent.collectLatest { textToSpeak ->
                if (textToSpeak == "STOP_TTS") {
                    textToSpeech?.stop()
                    return@collectLatest
                }
                if (ttsReady && textToSpeech != null) {
                    var locale = Locale.getDefault()
                    var text = textToSpeak
                    var pitch = 1.0f
                    var rate = 1.0f
                    when {
                        textToSpeak.startsWith("[en]") -> {
                            locale = Locale.US
                            text = textToSpeak.substring(4)
                            pitch = 1.05f
                            rate = 0.95f
                        }
                        textToSpeak.startsWith("[hi]") -> {
                            locale = Locale("hi", "IN")
                            text = textToSpeak.substring(4)
                            pitch = 1.0f
                            rate = 0.88f // Calm story storytelling pace
                        }
                        textToSpeak.startsWith("[pa]") -> {
                            locale = Locale("pa", "IN")
                            text = textToSpeak.substring(4)
                            pitch = 1.0f
                            rate = 0.9f
                        }
                        textToSpeak.startsWith("[bhajan]") -> {
                            locale = Locale("hi", "IN")
                            text = textToSpeak.substring(8)
                            pitch = 0.82f // Deeper, spiritual resonance
                            rate = 0.65f  // Very slow, meditative singing/chanting tempo
                        }
                    }
                    try {
                        textToSpeech?.setPitch(pitch)
                        textToSpeech?.setSpeechRate(rate)
                        textToSpeech?.language = locale
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error setting TTS properties", e)
                    }
                    textToSpeech?.speak(
                        text,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "GenerationConnectTTS"
                    )
                }
            }
        }

        setContent {
            val isKidsModeState = viewModel.isKidsMode.collectAsState()
            GenerationConnectTheme(isKidsMode = isKidsModeState.value) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AppNavigation(
                        viewModel = viewModel,
                        onStartVoiceListening = { triggerSpeechToText() }
                    )
                }
            }
        }
    }

    // --- Start official Speech To Text overlay ---
    private fun triggerSpeechToText() {
        viewModel.setIsListening(true)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a command (e.g., 'story', 'dawa', 'bhajan')")
        }
        try {
            speechToTextLauncher.launch(intent)
        } catch (e: Exception) {
            viewModel.setIsListening(false)
            Toast.makeText(
                this,
                "Voice keyboard not available. Try manual typing fallback in the mic popup!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // --- TextToSpeech OnInit Callback ---
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to English if default system language isn't fully loaded
                textToSpeech?.setLanguage(Locale.US)
            }
            ttsReady = true
            // Friendly spoken onboarding announcement
            viewModel.speakText("Welcome to GenerationConnect! Tap the bottom microphone button at any time to speak to me.")
        } else {
            ttsReady = false
            Toast.makeText(this, "Text-to-Speech service initialization failed.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        if (textToSpeech != null) {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
        super.onDestroy()
    }
}
