package com.noe.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.noe.app.data.Reminders
import com.noe.app.ui.NoeApp
import com.noe.app.ui.NotesViewModel
import com.noe.app.ui.theme.NoeTheme

class MainActivity : ComponentActivity() {

    private val viewModel: NotesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Only react to the launching intent on a fresh start, not after rotation.
        if (savedInstanceState == null) handleLaunch(intent)

        setContent {
            NoeTheme(settings = viewModel.settings) {
                NoeApp(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunch(intent)
    }

    override fun onStop() {
        // Make sure whatever is being typed is saved if the app gets killed in the background.
        viewModel.commitEditor()
        super.onStop()
    }

    private fun handleLaunch(intent: Intent?) {
        if (intent == null) return
        val noteId = intent.getStringExtra(Reminders.EXTRA_NOTE_ID)
        val sharedText =
            if (intent.action == Intent.ACTION_SEND) intent.getStringExtra(Intent.EXTRA_TEXT) else null
        viewModel.handleLaunch(noteId, sharedText)
    }
}
