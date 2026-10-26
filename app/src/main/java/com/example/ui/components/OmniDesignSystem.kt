@file:OptIn(ExperimentalLayoutApi::class)

package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniAccentSoft
import com.example.ui.theme.OmniBg
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniDanger
import com.example.ui.theme.OmniInk
import com.example.ui.theme.OmniOnInk
import com.example.ui.theme.OmniOnAccent
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniSurface2
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusLg
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.RadiusSm
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusButton
import com.example.ui.theme.RadiusInput
import com.example.ui.theme.RadiusPill

/**
 * Back action for the tool currently on screen. Provided by the app shell so that every
 * tool header can show the Morph "←" button without threading callbacks through each screen.
 */
val LocalToolBack = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * 1. ToolHeader: Morph page header — optional back arrow, bold title and one muted subtitle line.
 */
@Composable
fun ToolHeader(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = LocalToolBack.current
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(44.dp)
                    .testTag("tool_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Indietro",
                    tint = OmniText
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = OmniText,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            if (description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmniTextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 2. Dropzone: Morph hero card — soft accent background with dashed outline, icon tile,
 * headline, format chips and a full-width primary button (plus an optional secondary action).
 */
@Composable
fun Dropzone(
    icon: ImageVector,
    headline: String,
    helpText: String,
    supportedFormats: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    tintColor: Color = OmniAccent,
    testTag: String = "dropzone_action_button",
    secondaryIcon: ImageVector? = null,
    secondaryLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    val accent = OmniAccent
    val dashColor = accent.copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(RadiusCard + 4.dp))
            .background(OmniAccentSoft.copy(alpha = 0.7f))
            .drawBehind {
                drawRoundRect(
                    color = dashColor,
                    cornerRadius = CornerRadius(24.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(RadiusCard))
                    .background(tintColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = headline,
                style = MaterialTheme.typography.titleLarge,
                color = OmniText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = helpText,
                style = MaterialTheme.typography.bodyMedium,
                color = OmniTextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                supportedFormats.split("·", ",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { format ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(RadiusPill))
                            .background(OmniSurface)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(format, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OmniText)
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OmniAccent,
                        contentColor = OmniOnAccent
                    ),
                    shape = RoundedCornerShape(RadiusMd),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag(testTag)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = actionLabel, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
                if (onSecondaryAction != null && secondaryIcon != null) {
                    Surface(
                        onClick = onSecondaryAction,
                        shape = RoundedCornerShape(RadiusMd),
                        color = OmniSurface,
                        modifier = Modifier.height(54.dp).widthIn(min = 54.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(secondaryIcon, contentDescription = secondaryLabel, tint = OmniAccent, modifier = Modifier.size(22.dp))
                            if (secondaryLabel != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(secondaryLabel, color = OmniAccent, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. ToolLayout: Adaptive container for all tools.
 * - Empty state: fills space with dropzone (no empty right panel).
 * - Loaded state (Tablet >= 600dp): Left file list + Right side panel (max 360dp) + Sticky Bottom Action Bar.
 * - Loaded state (Phone < 600dp): 2-Tab Switcher (Files vs Settings) + Sticky Bottom Action Bar.
 */
@Composable
fun ToolLayout(
    title: String,
    description: String,
    isEmpty: Boolean,
    emptyDropzone: @Composable () -> Unit,
    fileList: @Composable () -> Unit,
    optionsPanel: @Composable () -> Unit,
    actionBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    phoneTabTitles: List<String> = listOf("File", "Opzioni")
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        val isTablet = maxWidth >= 600.dp

        Column(modifier = Modifier.fillMaxSize()) {
            ToolHeader(title = title, description = description)

            if (isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    emptyDropzone()
                }
            } else {
                if (isTablet) {
                    // Tablet: Left file list + Right options panel (max 360dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            fileList()
                        }

                        Box(
                            modifier = Modifier
                                .width(340.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(RadiusCard))
                                .background(OmniSurface)
                                .border(1.dp, OmniBorder, RoundedCornerShape(RadiusCard))
                                .padding(16.dp)
                        ) {
                            optionsPanel()
                        }
                    }
                } else {
                    // Phone: 2-Tab Switcher
                    var phoneTab by rememberSaveable(title) { mutableIntStateOf(0) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        SegmentedControl(
                            options = phoneTabTitles,
                            selectedIndex = phoneTab,
                            onOptionSelected = { phoneTab = it },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            if (phoneTab == 0) {
                                fileList()
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(RadiusMd))
                                        .background(OmniSurface)
                                        .border(1.dp, OmniBorder, RoundedCornerShape(RadiusMd))
                                        .padding(16.dp)
                                ) {
                                    optionsPanel()
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                actionBar()
            }
        }
    }
}

/**
 * 4. SegmentedControl: Morph pill tabs — muted track, raised white thumb on the selected option.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(RadiusMd))
            .background(OmniSurface2)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (isSelected) Modifier.shadow(2.dp, RoundedCornerShape(RadiusSm)) else Modifier
                    )
                    .clip(RoundedCornerShape(RadiusSm))
                    .background(if (isSelected) OmniSurface else Color.Transparent)
                    .clickable { onOptionSelected(index) }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) OmniText else OmniTextMuted,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 5. RadioCard: Big selectable options (WAV vs M4A, wrappers, formats).
 */
@Composable
fun RadioCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(RadiusMd),
        color = if (isSelected) OmniAccentSoft else OmniSurface2,
        border = BorderStroke(1.dp, if (isSelected) OmniAccent else OmniBorder),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) OmniAccent else OmniText
                    )
                    if (badgeText != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(OmniAccentSoft)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OmniAccent
                            )
                        }
                    }
                }
                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        color = OmniTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(OmniAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = OmniOnAccent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * 6. OptionRow: Switches and settings rows (52x32 switch, 16sp label + 14sp description).
 */
@Composable
fun OptionRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = OmniText
            )
            if (description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = OmniTextMuted
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OmniOnAccent,
                checkedTrackColor = OmniAccent,
                uncheckedThumbColor = OmniTextMuted,
                uncheckedTrackColor = OmniSurface2,
                uncheckedBorderColor = OmniBorder
            )
        )
    }
}

/**
 * 7. OmniActionBar: Morph sticky bottom bar — bold summary (+ optional muted detail) on the left
 * and a 54dp primary button with a trailing arrow on the right.
 */
@Composable
fun OmniActionBar(
    summaryText: String,
    primaryButtonLabel: String,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    disabledHelperText: String? = null,
    testTag: String = "primary_action_button",
    detailText: String? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = OmniSurface,
        border = BorderStroke(1.dp, OmniBorder),
        shape = RoundedCornerShape(RadiusCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = summaryText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = OmniText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val secondLine = if (!isEnabled && disabledHelperText != null) disabledHelperText else detailText
                if (secondLine != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = secondLine,
                        fontSize = 13.sp,
                        color = OmniTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = onPrimaryAction,
                enabled = isEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OmniAccent,
                    contentColor = OmniOnAccent,
                    disabledContainerColor = OmniSurface2,
                    disabledContentColor = OmniTextMuted
                ),
                shape = RoundedCornerShape(RadiusMd),
                contentPadding = PaddingValues(horizontal = 22.dp),
                modifier = Modifier
                    .height(54.dp)
                    .testTag(testTag)
            ) {
                AnimatedContent(
                    targetState = primaryButtonLabel,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                    },
                    label = "action_bar_label_anim"
                ) { label ->
                    Text(
                        text = label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                if (isEnabled) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/**
 * 8. OmniSlider: 6dp track, 24dp thumb, accent fill, current value on right.
 */
@Composable
fun OmniSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    valueFormatter: (Float) -> String = { "${it.toInt()}" }
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = OmniText
            )
            Text(
                text = valueFormatter(value),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = OmniAccent
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = OmniAccent,
                activeTrackColor = OmniAccent,
                inactiveTrackColor = OmniSurface2
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 9. OmniProgressBanner: Morph dark progress card — big percentage, status line and slim bar.
 */
@Composable
fun OmniProgressBanner(
    progress: Float,
    statusText: String,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), label = "progress_banner")
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = OmniInk,
        shape = RoundedCornerShape(RadiusCard)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${(animated * 100).toInt()}",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = OmniOnInk
                )
                Text(
                    text = "%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OmniOnInk.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 5.dp, start = 1.dp)
                )
            }
            Spacer(modifier = Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statusText,
                    fontSize = 14.sp,
                    color = OmniOnInk,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { animated },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = Color(0xFF8EA2FF),
                    trackColor = OmniOnInk.copy(alpha = 0.15f),
                    drawStopIndicator = {}
                )
            }
            if (onCancel != null) {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onCancel) {
                    Text("Annulla", color = OmniOnInk, fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * 10. FileCard: Reusable card for loaded files with thumbnail/icon, name, size/details,
 * and actions (remove, rotate, reorder).
 */
@Composable
fun FileCard(
    name: String,
    details: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    thumbnail: (@Composable () -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    testTag: String = "file_card"
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMd))
            .testTag(testTag),
        color = OmniSurface,
        border = BorderStroke(1.dp, OmniBorder),
        shape = RoundedCornerShape(RadiusMd)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (thumbnail != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(RadiusSm))
                        .background(OmniSurface2),
                    contentAlignment = Alignment.Center
                ) {
                    thumbnail()
                }
                Spacer(modifier = Modifier.width(12.dp))
            } else if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(RadiusSm))
                        .background(OmniAccentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = OmniAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OmniText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (details.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = details,
                        fontSize = 13.sp,
                        color = OmniTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (trailingContent != null) {
                trailingContent()
            }

            if (onMoveUp != null || onMoveDown != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onMoveUp != null) {
                        IconButton(onClick = onMoveUp, modifier = Modifier.size(36.dp)) {
                            Text("▲", color = OmniTextMuted, fontSize = 13.sp)
                        }
                    }
                    if (onMoveDown != null) {
                        IconButton(onClick = onMoveDown, modifier = Modifier.size(36.dp)) {
                            Text("▼", color = OmniTextMuted, fontSize = 13.sp)
                        }
                    }
                }
            }

            if (onRemove != null) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Rimuovi file",
                        tint = OmniTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * 11. OmniToast: Floating feedback toast for Success / Error / Actions.
 */
@Composable
fun OmniToast(
    message: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMd)),
        color = OmniSurface2,
        border = BorderStroke(1.dp, if (isError) OmniDanger.copy(alpha = 0.5f) else OmniAccent.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(RadiusMd)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.TaskAlt,
                    contentDescription = null,
                    tint = if (isError) OmniDanger else OmniAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = OmniText,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (actionLabel != null && onAction != null) {
                    TextButton(
                        onClick = onAction,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = actionLabel,
                            color = if (isError) OmniDanger else OmniAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (onDismiss != null) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = OmniTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

