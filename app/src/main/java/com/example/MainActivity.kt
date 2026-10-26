package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.example.data.AppPreferences
import com.example.data.HistoryRepository
import com.example.ui.theme.OmniConvertTheme
import com.example.viewmodel.PdfStudioViewModel

class MainActivity : ComponentActivity() {

    private val pdfViewModel: PdfStudioViewModel by viewModels()

    /** Files shared to the app from another app; the UI asks which tool should receive them. */
    private val sharedFiles = mutableStateOf<SharedFiles?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPreferences.init(this)
        HistoryRepository.init(this)
        enableEdgeToEdge()

        if (savedInstanceState == null) handleShareIntent(intent)

        setContent {
            val themeMode by AppPreferences.themeMode.collectAsState()
            val reduceMotion by AppPreferences.reduceMotion.collectAsState()
            val accent by AppPreferences.accent.collectAsState()
            OmniConvertTheme(themeMode = themeMode, reduceMotion = reduceMotion, accent = accent) {
                OmniConvertApp(
                    pdfViewModel = pdfViewModel,
                    sharedFiles = sharedFiles.value,
                    onSharedFilesConsumed = { sharedFiles.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        val uris: List<Uri> = when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
            )
            Intent.ACTION_SEND_MULTIPLE -> (
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                ).orEmpty()
            Intent.ACTION_VIEW -> listOfNotNull(intent.data)
            else -> emptyList()
        }
        if (uris.isEmpty()) return
        val mime = intent.type
            ?: runCatching { contentResolver.getType(uris.first()) }.getOrNull()
            ?: "*/*"
        sharedFiles.value = SharedFiles(uris, mime)
    }
}
