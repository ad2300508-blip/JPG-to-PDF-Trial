package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * 1. ToolHeader: Minimal page header with title (28sp) + one muted line of description.
 * No breadcrumbs, category tags, or loaded count pills.
 */
@Composable
fun ToolHeader(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displayLarge,
            color = OmniText,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = OmniTextMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 2. Dropzone: Large empty state filling the entire content area with dashed border,
 * clear headline, helper text, format guide, and a prominent filled accent button.
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
    testTag: String = "dropzone_action_button"
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(RadiusCard)),
        color = OmniSurface,
        border = BorderStroke(1.5.dp, OmniBorder),
        shape = RoundedCornerShape(RadiusCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(RadiusCard))
                    .background(tintColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = headline,
                style = MaterialTheme.typography.titleLarge,
                color = OmniText,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = helpText,
                style = MaterialTheme.typography.bodyMedium,
                color = OmniTextMuted,
                modifier = Modifier.widthIn(max = 480.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = supportedFormats,
                style = MaterialTheme.typography.labelSmall,
                color = OmniAccent,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OmniAccent,
                    contentColor = OmniOnAccent
                ),
                shape = RoundedCornerShape(RadiusMd),
                modifier = Modifier
                    .height(52.dp)
                    .testTag(testTag)
            ) {
                Text(
                    text = actionLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
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
    phoneTabTitles: List<String> = listOf("Files", "Settings")
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
                                .clip(RoundedCornerShape(RadiusMd))
                                .background(OmniSurface)
                                .border(1.dp, OmniBorder, RoundedCornerShape(RadiusMd))
                                .padding(16.dp)
                        ) {
                            optionsPanel()
                        }
                    }
                } else {
                    // Phone: 2-Tab Switcher
                    var phoneTab by rememberSaveable { mutableIntStateOf(0) }

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
 * 4. SegmentedControl: 48dp tall option group.
 * Selected: accent-soft background + accent border + check.
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
            .height(48.dp)
            .clip(RoundedCornerShape(RadiusSm))
            .background(OmniSurface2)
            .border(1.dp, OmniBorder, RoundedCornerShape(RadiusSm))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(RadiusSm - 2.dp))
                    .background(if (isSelected) OmniAccentSoft else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) OmniAccent else Color.Transparent,
                        shape = RoundedCornerShape(RadiusSm - 2.dp)
                    )
                    .clickable { onOptionSelected(index) }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = OmniAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) OmniAccent else OmniTextMuted,
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
 * 7. OmniActionBar: Sticky bottom action bar with summary text on left and 52dp primary button on right.
 */
@Composable
fun OmniActionBar(
    summaryText: String,
    primaryButtonLabel: String,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    disabledHelperText: String? = null,
    testTag: String = "primary_action_button"
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMd)),
        color = OmniSurface,
        border = BorderStroke(1.dp, OmniBorder),
        shape = RoundedCornerShape(RadiusMd)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = summaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = OmniText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!isEnabled && disabledHelperText != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = disabledHelperText,
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
                shape = RoundedCornerShape(RadiusSm),
                modifier = Modifier
                    .height(52.dp)
                    .testTag(testTag)
            ) {
                Text(
                    text = primaryButtonLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
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
 * 9. OmniToast / FeedbackBanner:
 * Progress with percentage & Cancel, or Success / Error with actions.
 */
@Composable
fun OmniProgressBanner(
    progress: Float,
    statusText: String,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMd)),
        color = OmniSurface,
        border = BorderStroke(1.dp, OmniAccentSoft),
        shape = RoundedCornerShape(RadiusMd)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusText,
                    fontSize = 14.sp,
                    color = OmniText,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = OmniAccent
                    )
                    if (onCancel != null) {
                        Spacer(modifier = Modifier.width(10.dp))
                        TextButton(onClick = onCancel, modifier = Modifier.height(32.dp)) {
                            Text("Cancel", color = OmniDanger, fontSize = 13.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = OmniAccent,
                trackColor = OmniSurface2
            )
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
                        contentDescription = "Remove file",
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
                            contentDescription = "Dismiss",
                            tint = OmniTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

