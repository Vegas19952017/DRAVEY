package com.dravey.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.dravey.app.ui.*
import com.dravey.app.ui.theme.DraveyTheme

class MainActivity : ComponentActivity() {

    private val requestAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled gracefully in ViewModel */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request microphone permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            DraveyTheme {
                val vm: DraveyViewModel = viewModel()
                val screen by vm.currentScreen.collectAsStateWithLifecycle()

                when (screen) {
                    is Screen.Main -> MainScreen(vm)
                    is Screen.Wizard -> WizardScreen(vm)
                    is Screen.ManualEntry -> ManualEntryScreen(vm)
                    is Screen.Preview -> PreviewScreen(vm)
                    is Screen.History -> HistoryScreen(vm)
                    is Screen.Settings -> SettingsScreen(vm)
                }
            }
        }
    }
}
