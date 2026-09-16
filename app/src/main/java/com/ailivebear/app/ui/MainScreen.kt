package com.ailivebear.app.ui

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ailivebear.app.character.CharacterManager
import com.ailivebear.app.character.engine.CharacterRenderView

/**
 * The ONLY thing shown on the character screen: the live 3D character and
 * its background. No status bar, no chat UI, no FPS/debug overlay - do not
 * add anything to this composable beyond the render surface. If a
 * developer-mode overlay is ever needed, add it as a separate screen
 * rather than layering it on top of this one.
 */
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val characterManager = remember { CharacterManager(context) }
    val renderView = remember { CharacterRenderView(context, characterManager) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> renderView.onResume()
                Lifecycle.Event.ON_PAUSE -> renderView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            characterManager.destroy()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            renderView.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }
    )
}
