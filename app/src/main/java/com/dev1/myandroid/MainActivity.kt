package com.dev1.myandroid

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.dev1.myandroid.data.PollWorker
import com.dev1.myandroid.ui.HomeScreen
import com.dev1.myandroid.ui.HomeViewModel
import com.dev1.myandroid.ui.theme.MyAndroidTheme

class MainActivity : ComponentActivity() {

    private val vm: HomeViewModel by viewModels()

    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* ignore result — app works without notifications */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Start background polling
        PollWorker.schedule(this)

        setContent {
            MyAndroidTheme {
                val state by vm.state.collectAsState()
                HomeScreen(
                    state = state,
                    onRefresh = vm::refresh,
                    onInstall = vm::downloadAndInstall
                )
            }
        }
    }
}
