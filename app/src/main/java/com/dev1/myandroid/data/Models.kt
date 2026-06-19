package com.dev1.myandroid.data

import com.google.gson.annotations.SerializedName

data class ApkManifest(
    @SerializedName("version") val version: String,
    @SerializedName("versionCode") val versionCode: Int,
    @SerializedName("buildAt") val buildAt: String,
    @SerializedName("apkUrl") val apkUrl: String,
    @SerializedName("apkSize") val apkSize: Long = 0L,
    @SerializedName("commitSha") val commitSha: String = "",
    @SerializedName("appName") val appName: String = "App",
    @SerializedName("notes") val notes: String = ""
)
