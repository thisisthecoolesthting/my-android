package com.dev1.myandroid.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "my_android_prefs")

class ManifestRepository(private val context: Context) {

    private val api = ApiClient.manifest

    private object Keys {
        val INSTALLED_VERSION_CODE = intPreferencesKey("installed_version_code")
        val LAST_KNOWN_VERSION = stringPreferencesKey("last_known_version")
    }

    /** Fetch the latest manifest from VPS. Returns null on error. */
    suspend fun fetchLatest(): Result<ApkManifest> = runCatching {
        api.getManifest()
    }

    val installedVersionCode: Flow<Int> = context.dataStore.data
        .map { it[Keys.INSTALLED_VERSION_CODE] ?: 0 }

    suspend fun markInstalled(versionCode: Int, version: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.INSTALLED_VERSION_CODE] = versionCode
            prefs[Keys.LAST_KNOWN_VERSION] = version
        }
    }
}
