@file:OptIn(ExperimentalLayoutApi::class)

package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Haptic
import com.example.ui.theme.LocalReduceMotion
import com.example.ui.theme.Motion
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniAccentOnInk
import com.example.ui.theme.OmniAccentSoft
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniDanger
import com.example.ui.theme.OmniInk
import com.example.ui.theme.OmniOnAccent
import com.example.ui.theme.OmniOnInk
import com.example.ui.theme.OmniSuccess
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniSurface2
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusMd
import com.example.ui.theme.RadiusPill
import com.example.ui.theme.RadiusSm
import com.example.ui.theme.enterAnimation
import com.example.ui.theme.motionSpec
import com.example.ui.theme.pressScale
import com.example.ui.theme.rememberHaptics
import kotlinx.coroutines.delay

/**
 * Back action for the tool currently on screen. Provided by the app shell so that every
 * tool header can show the Morph "←" button without threading callbacks through each screen.
 */
val LocalToolBack = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * 1. ToolHeader: Morph page header — optional back arrow, bold title and one muted subtitle line.
 * [actions] are shown on the right (e.g. a "more" menu).
 */
@Composable
fun ToolHeader(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = LocalToolBack.current,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            val interaction = remember { MutableInteractionSource() }
            IconButton(
                onClick = onBack,
                interactionSource = interaction,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(44.dp)
                    .pressScale(interaction, 0.85f)
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
        actions()
    }
}

/**
 * 2. Dropzone: Morph hero card — soft accent background with an animated dashed outline,
 * a gently floating icon tile, headline, format chips and a full-width primary button
 * (plus an optional secondary action).
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
    onSecondaryAction: (() -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null
) {
    val reduce = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "dropzone")
    val dashPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduce) 0f else 36f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "dash_phase"
    )
    val float by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduce) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = Motion.Standard), RepeatMode.Reverse),
        label = "icon_float"
    )
    val dashColor = OmniAccent.copy(alpha = 0.38f)
    val haptics = rememberHaptics()

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(RadiusCard + 4.dp))
            .background(
                Brush.verticalGradient(
                    listOf(OmniAccentSoft.copy(alpha = 0.85f), OmniAccentSoft.copy(alpha = 0.45f))
                )
            )
            .drawBehind {
                drawRoundRect(
                    color = dashColor,
                    cornerRadius = CornerRadius(24.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), dashPhase)
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
                    .enterAnimation(0)
                    .graphicsLayer { translationY = -6.dp.toPx() * float }
                    .shadow(10.dp * (0.6f + 0.4f * float), RoundedCornerShape(RadiusCard), ambientColor = tintColor, spotColor = tintColor)
                    .size(76.dp)
                    .clip(RoundedCornerShape(RadiusCard))
                    .background(Brush.linearGradient(listOf(tintColor, tintColor.copy(alpha = 0.82f)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = headline,
                style = MaterialTheme.typography.titleLarge,
                color = OmniText,
                textAlign = TextAlign.Center,
                modifier = Modifier.enterAnimation(1)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = helpText,
                style = MaterialTheme.typography.bodyMedium,
                color = OmniTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.enterAnimation(2)
            )

            Spacer(modifier = Modifier.height(14.dp))

            FlowRow(
                modifier = Modifier.enterAnimation(3),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .enterAnimation(4),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val interaction = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        haptics(Haptic.TAP)
                        onAction()
                    },
                    interactionSource = interaction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OmniAccent,
                        contentColor = OmniOnAccent
                    ),
                    shape = RoundedCornerShape(RadiusMd),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .pressScale(interaction)
                        .testTag(testTag)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = actionLabel, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
                if (onSecondaryAction != null && secondaryIcon != null) {
                    val secondaryInteraction = remember { MutableInteractionSource() }
                    Surface(
                        onClick = {
                            haptics(Haptic.TAP)
                            onSecondaryAction()
                        },
                        interactionSource = secondaryInteraction,
                        shape = RoundedCornerShape(RadiusMd),
                        color = OmniSurface,
                        modifier = Modifier
                            .height(54.dp)
                            .widthIn(min = 54.dp)
                            .pressScale(secondaryInteraction)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(secondaryIcon, contentDescription = secondaryLabel ?: "Fotocamera", tint = OmniAccent, modifier = Modifier.size(22.dp))
                            if (secondaryLabel != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(secondaryLabel, color = OmniAccent, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            if (footer != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.enterAnimation(5)) { footer() }
            }
        }
    }
}

/**
 * 3. ToolLayout: Adaptive container for all tools.
 * - Empty state: fills space with dropzone (no empty right panel).
 * - Loaded state (Tablet >= 600dp): Left file list + Right side panel (max 360dp) + Sticky Bottom Action Bar.
 * - Loaded state (Phone < 600dp): 2-Tab Switcher (Files vs Settings) + Sticky Bottom Action Bar.
 * Switching between empty and loaded, and between phone tabs, is animated.
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
    phoneTabTitles: List<String> = listOf("File", "Opzioni"),
    headerActions: @Composable RowScope.() -> Unit = {}
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        val isTablet = maxWidth >= 600.dp

        Column(modifier = Modifier.fillMaxSize()) {
            ToolHeader(title = title, description = description, actions = headerActions)

            AnimatedContent(
                targetState = isEmpty,
                transitionSpec = {
                    (fadeIn(tween(Motion.MEDIUM)) + scaleIn(initialScale = 0.97f, animationSpec = tween(Motion.MEDIUM, easing = Motion.EmphasizedDecelerate)))
                        .togetherWith(fadeOut(tween(Motion.SHORT)))
                        .using(SizeTransform(clip = false))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = "tool_layout_state"
            ) { empty ->
                if (empty) {
                    Box(modifier = Modifier.fillMaxSize()) { emptyDropzone() }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (isTablet) {
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
                                        .enterAnimation(1)
                                        .clip(RoundedCornerShape(RadiusCard))
                                        .background(OmniSurface)
                                        .border(1.dp, OmniBorder, RoundedCornerShape(RadiusCard))
                                        .padding(16.dp)
                                ) {
                                    optionsPanel()
                                }
                            }
                        } else {
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

                                AnimatedContent(
                                    targetState = phoneTab,
                                    transitionSpec = {
                                        val forward = targetState > initialState
                                        (slideInHorizontally(tween(Motion.MEDIUM, easing = Motion.EmphasizedDecelerate)) { w -> if (forward) w / 6 else -w / 6 } + fadeIn(tween(Motion.MEDIUM)))
                                            .togetherWith(slideOutHorizontally(tween(Motion.SHORT)) { w -> if (forward) -w / 8 else w / 8 } + fadeOut(tween(Motion.SHORT)))
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    label = "phone_tab"
                                ) { tab ->
                                    if (tab == 0) {
                                        Box(modifier = Modifier.fillMaxSize()) { fileList() }
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
                        Box(modifier = Modifier.enterAnimation(2, offsetDp = 28)) { actionBar() }
                    }
                }
            }
        }
    }
}

/**
 * 4. SegmentedControl: Morph pill tabs — muted track with a raised thumb that slides to the
 * selected option.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(RadiusMd))
            .background(OmniSurface2)
            .padding(4.dp)
    ) {
        val count = options.size.coerceAtLeast(1)
        val gap = 4.dp
        val segmentWidth = (maxWidth - gap * (count - 1)) / count
        val thumbOffset by animateDpAsState(
            targetValue = (segmentWidth + gap) * selectedIndex.coerceIn(0, count - 1),
            animationSpec = motionSpec(spring(dampingRatio = 0.75f, stiffness = 500f)),
            label = "segment_thumb"
        )
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(2.dp, RoundedCornerShape(RadiusSm))
                .clip(RoundedCornerShape(RadiusSm))
                .background(OmniSurface)
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            options.forEachIndexed { index, label ->
                val isSelected = index == selectedIndex
                val color by animateColorAsState(if (isSelected) OmniText else OmniTextMuted, label = "segment_label")
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(RadiusSm))
                        .clickable {
                            if (!isSelected) haptics(Haptic.TICK)
                            onOptionSelected(index)
                        }
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = color,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 5. RadioCard: Big selectable options (WAV vs M4A, wrappers, formats) with animated selection.
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
    val haptics = rememberHaptics()
    val interaction = remember { MutableInteractionSource() }
    val container by animateColorAsState(if (isSelected) OmniAccentSoft else OmniSurface2, label = "radio_bg")
    val borderColor by animateColorAsState(if (isSelected) OmniAccent else OmniBorder, label = "radio_border")
    Surface(
        onClick = {
            if (!isSelected) haptics(Haptic.TICK)
            onClick()
        },
        interactionSource = interaction,
        shape = RoundedCornerShape(RadiusMd),
        color = container,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .pressScale(interaction, 0.98f)
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
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) OmniSurface else OmniAccentSoft)
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
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(motionSpec(Motion.bouncy())) + fadeIn(),
                exit = scaleOut(tween(Motion.SHORT)) + fadeOut(tween(Motion.SHORT))
            ) {
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
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptics = rememberHaptics()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusSm))
            .clickable(enabled = enabled) {
                haptics(Haptic.TICK)
                onCheckedChange(!checked)
            }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) OmniText else OmniTextMuted
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
            enabled = enabled,
            onCheckedChange = {
                haptics(Haptic.TICK)
                onCheckedChange(it)
            },
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
 * and a 54dp primary button with a trailing arrow on the right. The summary animates when it changes.
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
    detailText: String? = null,
    secondaryAction: (@Composable () -> Unit)? = null
) {
    val haptics = rememberHaptics()
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = OmniSurface,
        border = BorderStroke(1.dp, OmniBorder),
        shape = RoundedCornerShape(RadiusCard),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                AnimatedText(
                    text = summaryText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = OmniText
                )
                val secondLine = if (!isEnabled && disabledHelperText != null) disabledHelperText else detailText
                if (secondLine != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    AnimatedText(
                        text = secondLine,
                        fontSize = 13.sp,
                        color = OmniTextMuted
                    )
                }
            }

            if (secondaryAction != null) {
                secondaryAction()
                Spacer(modifier = Modifier.width(8.dp))
            }

            val interaction = remember { MutableInteractionSource() }
            Button(
                onClick = {
                    haptics(Haptic.CONFIRM)
                    onPrimaryAction()
                },
                enabled = isEnabled,
                interactionSource = interaction,
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
                    .pressScale(interaction)
                    .testTag(testTag)
            ) {
                AnimatedContent(
                    targetState = primaryButtonLabel,
                    transitionSpec = {
                        (fadeIn(tween(Motion.SHORT)) + slideInVertically { it / 2 }) togetherWith
                            (fadeOut(tween(Motion.SHORT)) + slideOutVertically { -it / 2 })
                    },
                    label = "action_bar_label_anim"
                ) { label ->
                    Text(
                        text = label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1
                    )
                }
                AnimatedVisibility(visible = isEnabled) {
                    Row {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

/** Single-line text that slides the new value in from below when it changes (counters, summaries). */
@Composable
fun AnimatedText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    fontWeight: FontWeight? = null,
    color: Color = OmniText,
    textAlign: TextAlign? = null
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (slideInVertically(tween(Motion.MEDIUM, easing = Motion.EmphasizedDecelerate)) { it / 2 } + fadeIn(tween(Motion.MEDIUM)))
                .togetherWith(slideOutVertically(tween(Motion.SHORT)) { -it / 2 } + fadeOut(tween(Motion.SHORT)))
                .using(SizeTransform(clip = false))
        },
        modifier = modifier,
        label = "animated_text"
    ) { value ->
        Text(
            text = value,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign
        )
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
    steps: Int = 0,
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
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = OmniAccent,
                activeTrackColor = OmniAccent,
                inactiveTrackColor = OmniSurface2,
                activeTickColor = OmniOnAccent.copy(alpha = 0.6f),
                inactiveTickColor = OmniTextMuted.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 9. OmniProgressBanner: Morph dark progress card — counting percentage, status line and a slim
 * bar with a moving highlight.
 */
@Composable
fun OmniProgressBanner(
    progress: Float,
    statusText: String,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = motionSpec(spring(dampingRatio = 1f, stiffness = 120f)),
        label = "progress_banner"
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = OmniInk,
        shape = RoundedCornerShape(RadiusCard),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.widthIn(min = 64.dp)) {
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
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                AnimatedText(
                    text = statusText,
                    fontSize = 14.sp,
                    color = OmniOnInk,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                ShimmerProgressBar(
                    progress = animated,
                    color = OmniAccentOnInk,
                    trackColor = OmniOnInk.copy(alpha = 0.15f)
                )
            }
            if (onCancel != null) {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onCancel) {
                    Text("Annulla", color = OmniOnInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Rounded progress bar with a light sweep running across the filled part ("working" feel). */
@Composable
fun ShimmerProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = OmniAccent,
    trackColor: Color = OmniSurface2,
    height: Dp = 8.dp
) {
    val reduce = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    val sweep by transition.animateFloat(
        initialValue = -0.3f,
        targetValue = if (reduce) -0.3f else 1.3f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "shimmer_sweep"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor)
            .drawWithContent {
                val filled = size.width * progress.coerceIn(0f, 1f)
                if (filled > 0f) {
                    drawRoundRect(color = color, size = size.copy(width = filled), cornerRadius = CornerRadius(size.height / 2))
                    if (!reduce) {
                        val x = filled * sweep
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.45f), Color.Transparent),
                                startX = x - 60f,
                                endX = x + 60f
                            ),
                            size = size.copy(width = filled),
                            cornerRadius = CornerRadius(size.height / 2)
                        )
                    }
                }
            }
    )
}

/** Expand/collapse + fade wrapper used for banners and toasts appearing above a tool. */
@Composable
fun RevealVertically(visible: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandVertically(motionSpec(Motion.gentle())) + fadeIn(tween(Motion.MEDIUM)) + slideInVertically(motionSpec(Motion.gentle())) { -it / 3 },
        exit = shrinkVertically(tween(Motion.MEDIUM, easing = Motion.EmphasizedAccelerate)) + fadeOut(tween(Motion.SHORT))
    ) {
        content()
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
    iconTint: Color = OmniAccent,
    thumbnail: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    bottomContent: (@Composable () -> Unit)? = null,
    detailsColor: Color = OmniTextMuted,
    highlighted: Boolean = false,
    testTag: String = "file_card"
) {
    val interaction = remember { MutableInteractionSource() }
    val borderColor by animateColorAsState(if (highlighted) OmniAccent else OmniBorder, label = "file_card_border")
    val cardModifier = modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.pressScale(interaction, 0.98f) else Modifier)
        .testTag(testTag)
    val content: @Composable () -> Unit = {
        Column {
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
                    IconTile(icon = icon, tint = iconTint, size = 46.dp)
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
                        AnimatedText(
                            text = details,
                            fontSize = 13.sp,
                            color = detailsColor
                        )
                    }
                }

                if (trailingContent != null) {
                    trailingContent()
                }

                if (onMoveUp != null || onMoveDown != null) {
                    Column {
                        IconButton(onClick = { onMoveUp?.invoke() }, enabled = onMoveUp != null, modifier = Modifier.size(40.dp)) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = "Sposta su",
                                tint = if (onMoveUp != null) OmniTextMuted else OmniTextMuted.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = { onMoveDown?.invoke() }, enabled = onMoveDown != null, modifier = Modifier.size(40.dp)) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = "Sposta giù",
                                tint = if (onMoveDown != null) OmniTextMuted else OmniTextMuted.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
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
            if (bottomContent != null) {
                bottomContent()
            }
        }
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            interactionSource = interaction,
            modifier = cardModifier,
            color = OmniSurface,
            border = BorderStroke(if (highlighted) 1.5.dp else 1.dp, borderColor),
            shape = RoundedCornerShape(RadiusMd)
        ) { content() }
    } else {
        Surface(
            modifier = cardModifier,
            color = OmniSurface,
            border = BorderStroke(if (highlighted) 1.5.dp else 1.dp, borderColor),
            shape = RoundedCornerShape(RadiusMd)
        ) { content() }
    }
}

/** Header row above a file list: count + "Aggiungi" + "Svuota". */
@Composable
fun FileListHeader(
    title: String,
    onAdd: (() -> Unit)?,
    onClear: (() -> Unit)?,
    modifier: Modifier = Modifier,
    addLabel: String = "Aggiungi",
    addIcon: ImageVector = Icons.Default.FileUpload,
    extra: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedText(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = OmniText,
            modifier = Modifier.weight(1f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            extra()
            if (onAdd != null) {
                val interaction = remember { MutableInteractionSource() }
                Surface(
                    onClick = onAdd,
                    interactionSource = interaction,
                    shape = RoundedCornerShape(RadiusPill),
                    color = OmniAccentSoft,
                    modifier = Modifier
                        .height(36.dp)
                        .pressScale(interaction)
                        .testTag("list_add_more_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(addIcon, contentDescription = null, tint = OmniAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(addLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OmniAccent)
                    }
                }
            }
            if (onClear != null) {
                TextButton(onClick = onClear, modifier = Modifier.height(36.dp)) {
                    Text("Svuota", color = OmniTextMuted, fontSize = 13.sp)
                }
            }
        }
    }
}

/** Morph text field (filled surface, accent focus border) used in option panels. */
@Composable
fun MorphTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    testTag: String? = null
) {
    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = OmniText,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = OmniTextMuted, fontSize = 14.sp) },
            singleLine = true,
            isError = isError,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
            shape = RoundedCornerShape(RadiusSm),
            colors = morphTextFieldColors()
        )
        AnimatedVisibility(visible = supportingText != null) {
            Text(
                text = supportingText.orEmpty(),
                fontSize = 12.sp,
                color = if (isError) OmniDanger else OmniTextMuted,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
fun morphTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = OmniSurface2,
    unfocusedContainerColor = OmniSurface2,
    focusedBorderColor = OmniAccent,
    unfocusedBorderColor = OmniBorder,
    focusedTextColor = OmniText,
    unfocusedTextColor = OmniText,
    errorBorderColor = OmniDanger,
    errorContainerColor = OmniSurface2,
    cursorColor = OmniAccent
)

/**
 * 11. OmniToast: Floating feedback toast for Success / Error / Actions.
 * With [autoDismissMillis] it closes itself (calls [onDismiss], or [onAction] when there is no dismiss).
 */
@Composable
fun OmniToast(
    message: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    autoDismissMillis: Long? = null
) {
    if (autoDismissMillis != null) {
        LaunchedEffect(message) {
            delay(autoDismissMillis)
            (onDismiss ?: onAction)?.invoke()
        }
    }
    val accent = if (isError) OmniDanger else OmniSuccess
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .enterAnimation(0, key = message, offsetDp = -12),
        color = OmniSurface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(RadiusMd),
        shadowElevation = 4.dp
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
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.TaskAlt,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = OmniText,
                    fontWeight = FontWeight.Medium,
                    maxLines = 3,
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
                            fontWeight = FontWeight.Bold
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

/** Small rounded label (e.g. "Nuovo", "Consigliato"). */
@Composable
fun MorphBadge(text: String, modifier: Modifier = Modifier, color: Color = OmniAccent, container: Color = OmniAccentSoft) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(RadiusPill))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

/** Thin label + option content block used inside option panels. */
@Composable
fun OptionSection(title: String, hint: String? = null, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = OmniText,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
        if (hint != null) {
            AnimatedText(
                text = hint,
                fontSize = 13.sp,
                color = OmniTextMuted,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
