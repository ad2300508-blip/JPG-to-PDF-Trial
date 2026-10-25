package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniAccentSoft
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniOnAccent
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniSurface2
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusButton
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusLg
import com.example.ui.theme.RadiusSm
import com.example.ui.theme.TintAi
import com.example.ui.theme.TintAudio
import com.example.ui.theme.TintImage
import com.example.ui.theme.TintMerge
import com.example.ui.theme.TintPdf
import com.example.ui.theme.TintZip

data class TutorialStep(
    val stepIndex: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tintColor: Color,
    val highlights: List<Pair<String, String>>,
    val proTip: String
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OmniTutorialDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = remember {
        listOf(
            TutorialStep(
                stepIndex = 1,
                title = "Images to PDF",
                subtitle = "Smart orientation, book scan splitting & scan cleanup",
                icon = Icons.Default.PictureAsPdf,
                tintColor = TintPdf,
                highlights = listOf(
                    "Smart Auto Orientation" to "Horizontal images are automatically given landscape pages with zero vertical distortion.",
                    "Book Mode (Spread Split)" to "Split wide two-page book scans into left and right sequential pages with one tap.",
                    "Auto Contrast Clean" to "Removes yellow paper cast and enhances dark text contrast for crystal clear reading."
                ),
                proTip = "Pro tip: Select 'Original' page size if you want borderless PDFs matching the exact pixel dimensions of your pictures."
            ),
            TutorialStep(
                stepIndex = 2,
                title = "Merge PDF & Security",
                subtitle = "Combine documents and protect with 128-bit encryption",
                icon = Icons.AutoMirrored.Filled.MergeType,
                tintColor = TintMerge,
                highlights = listOf(
                    "Offline Merging" to "Fast, private document merging done 100% on-device without uploading to the cloud.",
                    "Order Management" to "Easily reorder files and preview total page counts before merging.",
                    "Password Protection" to "Encrypt merged documents with standard 128-bit password security."
                ),
                proTip = "Pro tip: Drag or use arrow keys to adjust file order before hitting 'Merge Documents'."
            ),
            TutorialStep(
                stepIndex = 3,
                title = "Images & Audio Lab",
                subtitle = "Modern formats, EXIF privacy & clean voice transcoding",
                icon = Icons.Default.Image,
                tintColor = TintImage,
                highlights = listOf(
                    "WebP Transcoding" to "WebP Lossy and Lossless compress up to 35% smaller than JPEG and PNG.",
                    "Strip EXIF Metadata" to "Removes GPS location and camera identifiers for safe sharing.",
                    "Voice Downmixing" to "Downmix stereo to mono for 50% smaller voice recordings and podcasts."
                ),
                proTip = "Pro tip: 128 kbps M4A is the optimal bitrate for spoken audio, tutorials, and interviews."
            ),
            TutorialStep(
                stepIndex = 4,
                title = "ZIP & AI Studio Context",
                subtitle = "Deflate archives, LLM wrappers & Gemini pipelines",
                icon = Icons.Default.AutoAwesome,
                tintColor = TintAi,
                highlights = listOf(
                    "Prompt Wrappers" to "Format notes and PDF chapters into Claude XML, ChatGPT System, or Gemini Brief.",
                    "Free-tier Gemini Hub" to "Integrated multi-model AI with automatic quota fallback and connection tests.",
                    "Multi-Doc Study Pipelines" to "Generate executive summaries, flashcards, quizzes, and critique reports."
                ),
                proTip = "Pro tip: Extract Markdown in 'Markdown for AI' and tap 'Send to Gemini' to analyze instantly!"
            )
        )
    }

    var currentStep by remember { mutableIntStateOf(0) }
    val step = steps[currentStep]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 640.dp)
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(RadiusLg))
                .testTag("omni_tutorial_dialog"),
            color = OmniSurface,
            border = BorderStroke(1.dp, OmniBorder),
            shape = RoundedCornerShape(RadiusLg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Top Header Row with Step indicator & Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(RadiusSm))
                                .background(OmniAccentSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = OmniAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "OmniConvert Studio Guide",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = OmniText
                            )
                            Text(
                                text = "Step ${currentStep + 1} of ${steps.size}",
                                fontSize = 12.sp,
                                color = OmniTextMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close guide",
                            tint = OmniTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Step Indicators (Pills)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    steps.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index <= currentStep) OmniAccent else OmniSurface2
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Animated Step Content
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                    },
                    label = "tutorial_step_content"
                ) { current ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Category Hero Tile & Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(RadiusCard))
                                    .background(current.tintColor.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = current.icon,
                                    contentDescription = null,
                                    tint = current.tintColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = current.title,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OmniText
                                )
                                Text(
                                    text = current.subtitle,
                                    fontSize = 13.sp,
                                    color = OmniTextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Highlights
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            current.highlights.forEach { (feature, explanation) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = OmniAccent,
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = feature,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = OmniText
                                        )
                                        Text(
                                            text = explanation,
                                            fontSize = 13.sp,
                                            color = OmniTextMuted,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Pro-Tip Callout
                        Surface(
                            shape = RoundedCornerShape(RadiusSm),
                            color = OmniSurface2,
                            border = BorderStroke(1.dp, OmniBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TipsAndUpdates,
                                    contentDescription = null,
                                    tint = OmniAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = current.proTip,
                                    fontSize = 12.sp,
                                    color = OmniText,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(RadiusButton),
                            border = BorderStroke(1.dp, OmniBorder),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("Previous", fontSize = 14.sp, color = OmniText)
                        }
                    } else {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("Skip Tour", fontSize = 14.sp, color = OmniTextMuted)
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < steps.size - 1) {
                                currentStep++
                            } else {
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(RadiusButton),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OmniAccent,
                            contentColor = OmniOnAccent
                        ),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("tutorial_next_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (currentStep < steps.size - 1) "Next" else "Start Converting",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (currentStep < steps.size - 1) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
