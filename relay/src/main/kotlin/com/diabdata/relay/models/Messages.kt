package com.diabdata.relay.models

import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════
//  ANDROID APP → RELAI
// ═══════════════════════════════════════════

@Serializable
data class RegisterMessage(
    val type: String = "REGISTER",
    val sessionId: String,
    val tokenHash: String,
    val mode: String,
    val expiresAt: Long
)

@Serializable
data class UnregisterMessage(
    val type: String = "UNREGISTER",
    val sessionId: String
)

@Serializable
data class AppResponseMessage(
    val type: String = "RESPONSE",
    val requestId: String,
    val clientId: String,
    val payload: String
)

// ═══════════════════════════════════════════
//  FRONT → RELAI
// ═══════════════════════════════════════════

@Serializable
data class AuthMessage(
    val type: String = "AUTH",
    val tokenHash: String
)

@Serializable
data class RequestMessage(
    val type: String = "REQUEST",
    val requestId: String,
    val payload: String       // blob chiffré en base64
)

// ═══════════════════════════════════════════
//  RELAY → ANDROID APP
// ═══════════════════════════════════════════

@Serializable
data class RegisterOkMessage(
    val type: String = "REGISTER_OK",
    val sessionId: String
)

@Serializable
data class ForwardMessage(
    val type: String = "FORWARD",
    val requestId: String,
    val clientId: String,
    val payload: String
)

// ═══════════════════════════════════════════
//  RELAY → FRONT
// ═══════════════════════════════════════════

@Serializable
data class AuthOkMessage(
    val type: String = "AUTH_OK",
    val mode: String
)

@Serializable
data class AuthFailedMessage(
    val type: String = "AUTH_FAILED",
    val reason: String
)

@Serializable
data class ClientResponseMessage(
    val type: String = "RESPONSE",
    val requestId: String,
    val payload: String
)

@Serializable
data class SessionClosedMessage(
    val type: String = "SESSION_CLOSED",
    val reason: String        // "USER_ENDED", "SESSION_EXPIRED", "APP_DISCONNECTED"
)

// ═══════════════════════════
//  UTILS — Parse message type
// ═══════════════════════════

@Serializable
data class BaseMessage(
    val type: String
)