package com.fstv.player.network.models

import com.google.gson.annotations.SerializedName

data class CheckResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("customer_name") val customerName: String?,
    @SerializedName("playlist_url") val playlistUrl: String?,
    @SerializedName("playlist_type") val playlistType: String?,
    @SerializedName("expires_at") val expiresAt: String?,
    @SerializedName("days_remaining") val daysRemaining: Int?
)
