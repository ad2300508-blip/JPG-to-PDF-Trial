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
import com.example.ui.components.CrashReportDialog
import com.example.ui.theme.OmniConvertTheme
import com.example.util.CrashReporter
import com.example.viewmodel.PdfStudioViewModel

class MainActivity : ComponentActivity() {

    private val pdfViewModel: PdfStudioViewModel by viewModels()

    /** Files shared to the app from another app; the UI asks which tool should receive them. */
    private val sharedFiles = mutableStateOf<SharedFiles?>(null)

    /** Tool asked for by a launcher shortcut (its [com.example.ui.home.OmniTool] name). */
    private val toolRequest = mutableStateOf<String?>(null)

    /** Report of a crash from the previous run, shown once so the user can send it. */
    private val crashReport = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashReporter.install(this)
        AppPreferences.init(this)
        HistoryRepository.init(this)
        enableEdgeToEdge()
        if (savedInstanceState == null) crashReport.value = CrashReporter.pending(this)

        if (savedInstanceState == null) handleShareIntent(intent)
        AppShortcuts.publish(this)

        setContent {
            val themeMode by AppPreferences.themeMode.collectAsState()
            val appReduceMotion by AppPreferences.reduceMotion.collectAsState()
            val systemReduceMotion by AppPreferences.systemReduceMotion.collectAsState()
            val reduceMotion = appReduceMotion || systemReduceMotion
            val accent by AppPreferences.accent.collectAsState()
            val pureBlack by AppPreferences.pureBlack.collectAsState()
            val dynamicAccent by AppPreferences.dynamicAccent.collectAsState()
            OmniConvertTheme(
                themeMode = themeMode,
                reduceMotion = reduceMotion,
                accent = accent,
                pureBlack = pureBlack,
                dynamicAccent = dynamicAccent
            ) {
                OmniConvertApp(
                    pdfViewModel = pdfViewModel,
                    sharedFiles = sharedFiles.value,
                    onSharedFilesConsumed = { sharedFiles.value = null },
                    toolRequest = toolRequest.value,
                    onToolRequestConsumed = { toolRequest.value = null },
                    canShowWhatsNew = crashReport.value == null
                )
                crashReport.value?.let { report ->
                    CrashReportDialog(
                        report = report,
                        onDismiss = {
                            CrashReporter.clear(this)
                            crashReport.value = null
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AppPreferences.refreshSystemMotion(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == AppShortcuts.ACTION_OPEN_TOOL) {
            toolRequest.value = intent.getStringExtra(AppShortcuts.EXTRA_TOOL)
            return
        }
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
        sharedFiles.value = SharedFiles(uris, mime, openInReader = intent.action == Intent.ACTION_VIEW && mime == "application/pdf")
    }
}
