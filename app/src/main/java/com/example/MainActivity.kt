package com.example

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
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

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var viewModel: MainViewModel
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false

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

        // 1. Initialize Room Database and Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ReminderRepository(database.reminderDao())

        // 2. Initialize MainViewModel with Factory (Simple constructor injection)
        val factory = MainViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        // 3. Initialize native Text-To-Speech
        textToSpeech = TextToSpeech(this, this)

        // 4. Collect Text-To-Speech requests from ViewModel Flow
        lifecycleScope.launch {
            viewModel.speechEvent.collectLatest { textToSpeak ->
                if (ttsReady && textToSpeech != null) {
                    textToSpeech?.speak(
                        textToSpeak,
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
