package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.MainStudioScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ArchiveViewModel
import com.example.viewmodel.AudioLabViewModel
import com.example.viewmodel.ImageLabViewModel
import com.example.viewmodel.PdfMergeViewModel
import com.example.viewmodel.PdfViewModel

class MainActivity : ComponentActivity() {
  private val imagePdfViewModel: PdfViewModel by viewModels()
  private val pdfMergeViewModel: PdfMergeViewModel by viewModels()
  private val imageLabViewModel: ImageLabViewModel by viewModels()
  private val audioLabViewModel: AudioLabViewModel by viewModels()
  private val archiveViewModel: ArchiveViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleIncomingShareIntent(intent)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          MainStudioScreen(
            imagePdfViewModel = imagePdfViewModel,
            pdfMergeViewModel = pdfMergeViewModel,
            imageLabViewModel = imageLabViewModel,
            audioLabViewModel = audioLabViewModel,
            archiveViewModel = archiveViewModel
          )
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIncomingShareIntent(intent)
  }

  private fun handleIncomingShareIntent(intent: Intent?) {
    if (intent == null) return
    try {
      when (intent.action) {
        Intent.ACTION_SEND -> {
          val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
          } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
          }
          uri?.let { imagePdfViewModel.addImages(this, listOf(it)) }
        }
        Intent.ACTION_SEND_MULTIPLE -> {
          val uris: ArrayList<Uri>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
          } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
          }
          uris?.let { if (it.isNotEmpty()) imagePdfViewModel.addImages(this, it) }
        }
      }
    } catch (_: Exception) { }
  }
}
