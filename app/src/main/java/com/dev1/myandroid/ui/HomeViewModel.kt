package com.dev1.myandroid.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dev1.myandroid.data.ApkManifest
import com.dev1.myandroid.data.ManifestRepository
import com.dev1.myandroid.installer.ApkInstaller
import com.dev1.myandroid.installer.DownloadState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class HomeUiState(
    val manifest: ApkManifest? = null,
    val installedVersionCode: Int = 0,
    val isLoading: Boolean = true,
    val downloadProgress: Int? = null,   // null = not downloading
    val downloadedFile: File? = null,
    val error: String? = null,
    val lastChecked: String = "Never"
)

val HomeUiState.hasUpdate: Boolean
    get() = manifest != null && manifest.versionCode > installedVersionCode

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ManifestRepository(app)
    private val installer = ApkInstaller(app)

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        // Collect installed versionCode from DataStore
        viewModelScope.launch {
            repo.installedVersionCode.collect { code ->
                _state.value = _state.value.copy(installedVersionCode = code)
            }
        }
        refresh()
        startAutoRefresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            repo.fetchLatest()
                .onSuccess { manifest ->
                    _state.value = _state.value.copy(
                        manifest = manifest,
                        isLoading = false,
                        lastChecked = java.text.SimpleDateFormat(
                            "h:mm a", java.util.Locale.getDefault()
                        ).format(java.util.Date())
                    )
                }
                .onFailure { err ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Could not reach server: ${err.message}"
                    )
                    Log.e("HomeViewModel", "Manifest fetch failed", err)
                }
        }
    }

    fun downloadAndInstall() {
        val manifest = _state.value.manifest ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(downloadProgress = 0, downloadedFile = null)
            val fileName = "${manifest.appName.replace(" ", "_")}_${manifest.version}.apk"
            val result = installer.download(manifest.apkUrl, fileName) { pct ->
                _state.value = _state.value.copy(downloadProgress = pct)
            }
            when (result) {
                is DownloadState.Done -> {
                    _state.value = _state.value.copy(
                        downloadProgress = null,
                        downloadedFile = result.file
                    )
                    installer.install(result.file)
                    repo.markInstalled(manifest.versionCode, manifest.version)
                }
                is DownloadState.Error -> {
                    _state.value = _state.value.copy(
                        downloadProgress = null,
                        error = "Download failed: ${result.message}"
                    )
                }
                is DownloadState.Progress -> { /* handled above */ }
            }
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(30_000L)
                refresh()
            }
        }
    }
}
