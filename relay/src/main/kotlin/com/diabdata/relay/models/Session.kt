package com.diabdata.relay.models

import io.ktor.websocket.*

data class RelaySession(
    val sessionId: String,
    val tokenHash: String,
    val mode: String,
    val expiresAt: Long,
    var appSocket: WebSocketSession?,
    val clientSockets: MutableMap<String, WebSocketSession> = mutableMapOf()
) {
    fun isExpired(): Boolean = System.currentTimeMillis() / 1000 > expiresAt
}