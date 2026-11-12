package com.noe.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.noe.app.ui.screens.EditorScreen
import com.noe.app.ui.screens.HomeScreen
import com.noe.app.ui.screens.SettingsScreen

private enum class Target { Home, Editor, Settings }

private class EditorHolder {
    var last: EditorState? = null
}

@Composable
fun NoeApp(vm: NotesViewModel) {
    val editor = vm.editor
    val holder = remember { EditorHolder() }
    if (editor != null) holder.last = editor

    val target = when {
        editor != null -> Target.Editor
        vm.showSettings -> Target.Settings
        else -> Target.Home
    }

    // Kept here so the note list keeps its scroll position while you edit a note.
    val gridState = rememberLazyStaggeredGridState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        AnimatedContent(
            targetState = target,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                (fadeIn(tween(240, delayMillis = 60)) + scaleIn(initialScale = 0.94f, animationSpec = tween(280))) togetherWith
                    fadeOut(tween(120))
            },
            label = "screens",
        ) { screen ->
            when (screen) {
                Target.Home -> HomeScreen(vm, gridState)
                Target.Settings -> SettingsScreen(vm)
                Target.Editor -> holder.last?.let { EditorScreen(vm, it) }
            }
        }
    }
}
