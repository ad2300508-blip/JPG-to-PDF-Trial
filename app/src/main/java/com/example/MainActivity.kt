package com.example

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
}
