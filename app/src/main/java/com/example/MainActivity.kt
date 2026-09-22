package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.DramaRepository
import com.example.ui.MainTvApp
import com.example.ui.theme.HongguoTvTheme
import com.example.util.ClipboardObserver
import com.example.viewmodel.MainTvViewModel

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var repository: DramaRepository
    private lateinit var viewModel: MainTvViewModel
    private lateinit var clipboardObserver: ClipboardObserver

    private var currentClipboardTextState by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep TV screen awake during shortplay viewing
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        database = AppDatabase.getDatabase(this)
        repository = DramaRepository(database)

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainTvViewModel(repository) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)[MainTvViewModel::class.java]

        clipboardObserver = ClipboardObserver(this) { detectedText ->
            currentClipboardTextState = detectedText
            viewModel.onClipboardDetected(detectedText)
        }

        setContent {
            HongguoTvTheme {
                MainTvApp(
                    viewModel = viewModel,
                    repository = repository,
                    currentClipboardText = currentClipboardTextState,
                    onTriggerCheckClipboard = {
                        clipboardObserver.checkClipboard()
                        currentClipboardTextState = clipboardObserver.getDirectCurrentText()
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        clipboardObserver.start()
    }

    override fun onResume() {
        super.onResume()
        clipboardObserver.checkClipboard()
        currentClipboardTextState = clipboardObserver.getDirectCurrentText()
    }

    override fun onStop() {
        super.onStop()
        clipboardObserver.stop()
    }
}
