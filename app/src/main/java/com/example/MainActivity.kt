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
import androidx.compose.runtime.mutableIntStateOf
import com.example.data.AppPreferences
import com.example.data.HistoryRepository
import com.example.ui.theme.OmniConvertTheme
import com.example.viewmodel.PdfStudioViewModel

class MainActivity : ComponentActivity() {

    private val pdfViewModel: PdfStudioViewModel by viewModels()

    /** Incremented whenever images are shared to the app, so the UI opens "Immagini in PDF". */
    private val openPdfToolSignal = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPreferences.init(this)
        HistoryRepository.init(this)
        enableEdgeToEdge()

        if (savedInstanceState == null) handleShareIntent(intent)

        setContent {
            val themeMode by AppPreferences.themeMode.collectAsState()
            OmniConvertTheme(themeMode = themeMode) {
                OmniConvertApp(
                    pdfViewModel = pdfViewModel,
                    openPdfToolSignal = openPdfToolSignal.intValue
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
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                if (uri != null) {
                    pdfViewModel.addImages(listOf(uri))
                    openPdfToolSignal.intValue++
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                if (!uris.isNullOrEmpty()) {
                    pdfViewModel.addImages(uris)
                    openPdfToolSignal.intValue++
                }
            }
        }
    }
}
