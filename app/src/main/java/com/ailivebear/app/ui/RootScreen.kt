package com.ailivebear.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ailivebear.app.settings.SettingsRepository
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Hosts the two screens (character, settings) and the horizontal swipe
 * gesture between them. No hamburger menu, no bottom nav - gesture only,
 * plus a small back arrow inside Settings for accessibility.
 *
 * Both screens stay composed simultaneously (just slid off-screen) rather
 * than one being torn down - so the character keeps idling underneath
 * while Settings is open, and CharacterManager/the Filament engine isn't
 * recreated on every swipe.
 */
@Composable
fun RootScreen(settingsRepository: SettingsRepository) {
    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val screenWidthPx = with(density) { screenWidthDp.dp.toPx() }

    // 0f = character fully shown, 1f = settings fully shown.
    var openFraction by remember { mutableFloatStateOf(0f) }
    var dragAccumulatorPx by remember { mutableFloatStateOf(0f) }

    val animatedFraction by animateFloatAsState(
        targetValue = openFraction,
        animationSpec = tween(durationMillis = 260),
        label = "settingsSlide"
    )

    // Requires a deliberate drag past ~28% of the screen width before it
    // commits to switching screens, so an incidental touch during LIVE
    // doesn't flip the screen.
    val dragThresholdPx = screenWidthPx * 0.28f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulatorPx = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulatorPx += dragAmount
                    },
                    onDragEnd = {
                        openFraction = if (abs(dragAccumulatorPx) > dragThresholdPx) {
                            if (dragAccumulatorPx > 0) 1f else 0f
                        } else {
                            (openFraction + dragAccumulatorPx / screenWidthPx)
                                .roundToInt()
                                .toFloat()
                                .coerceIn(0f, 1f)
                        }
                    }
                )
            }
    ) {
        // Character screen slides fully out to the left as settings comes in.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset((-animatedFraction * screenWidthPx).roundToInt(), 0) }
        ) {
            MainScreen()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(((1f - animatedFraction) * screenWidthPx).roundToInt(), 0) }
        ) {
            com.ailivebear.app.settings.SettingsScreen(
                repository = settingsRepository,
                onBack = { openFraction = 0f }
            )
        }
    }
}
