package com.fstv.player.network.models

import com.google.gson.annotations.SerializedName

data class RegisterResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("device_status") val deviceStatus: String?,
    @SerializedName("expires_at") val expiresAt: String?
)
