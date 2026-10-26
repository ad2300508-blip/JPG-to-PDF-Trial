package com.example.ui.theme

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.example.data.AppPreferences
import kotlinx.coroutines.delay

/** True when the user asked for reduced motion in Impostazioni: animations snap instead of moving. */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Motion tokens shared by every screen so transitions feel like one system. */
object Motion {
    const val SHORT = 160
    const val MEDIUM = 280
    const val LONG = 420

    /** Material "emphasized decelerate": fast start, gentle landing. */
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    fun <T> bouncy(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)
    fun <T> gentle(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
    fun <T> snappy(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium)
}

/** Animation spec that collapses to an instant snap when reduced motion is on. */
@Composable
fun <T> motionSpec(spec: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> =
    if (LocalReduceMotion.current) snap() else spec

@Composable
fun <T> tweenSpec(durationMillis: Int, delayMillis: Int = 0): AnimationSpec<T> =
    if (LocalReduceMotion.current) snap() else tween(durationMillis, delayMillis, Motion.Standard)

/**
 * Springy "press" feedback: the element shrinks slightly while touched and bounces back on release.
 * Pass the same [interactionSource] given to the clickable/Surface.
 */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.96f): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = motionSpec(spring(dampingRatio = 0.45f, stiffness = 700f)),
        label = "press_scale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Fade + rise entrance, staggered by [index] (first ~12 items), played once per [key].
 * Ideal for cards appearing on a screen.
 */
@Composable
fun Modifier.enterAnimation(index: Int = 0, key: Any = Unit, offsetDp: Int = 18): Modifier {
    val reduce = LocalReduceMotion.current
    var visible by rememberSaveable(key) { mutableStateOf(reduce) }
    LaunchedEffect(key) {
        if (!visible) {
            delay(index.coerceAtMost(12) * 45L)
            visible = true
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (reduce) snap() else spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "enter"
    )
    val offsetPx = with(LocalDensity.current) { offsetDp.dp.toPx() }
    return this.graphicsLayer {
        alpha = progress.coerceIn(0f, 1f)
        translationY = (1f - progress) * offsetPx
        val s = 0.96f + 0.04f * progress
        scaleX = s
        scaleY = s
    }
}

enum class Haptic { TAP, CONFIRM, REJECT, TICK }

/** Haptic feedback that honours the "Vibrazione" setting. */
@Composable
fun rememberHaptics(): (Haptic) -> Unit {
    val view = LocalView.current
    return remember(view) {
        { type: Haptic ->
            if (AppPreferences.haptics.value) {
                val constant = when (type) {
                    Haptic.TAP -> HapticFeedbackConstants.VIRTUAL_KEY
                    Haptic.TICK -> HapticFeedbackConstants.CLOCK_TICK
                    Haptic.CONFIRM -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
                    Haptic.REJECT -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
                }
                view.performHapticFeedback(constant)
            }
        }
    }
}
