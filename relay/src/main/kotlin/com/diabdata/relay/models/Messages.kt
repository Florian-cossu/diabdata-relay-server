package com.diabdata.relay.models

import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════
//  CONSTANTES DE TYPES
// ═══════════════════════════════════════════

object MessageType {
    const val REGISTER = "REGISTER"
    const val REGISTER_OK = "REGISTER_OK"
    const val UNREGISTER = "UNREGISTER"
    const val AUTH = "AUTH"
    const val AUTH_OK = "AUTH_OK"
    const val AUTH_FAILED = "AUTH_FAILED"
    const val REQUEST = "REQUEST"
    const val FORWARD = "FORWARD"
    const val RESPONSE = "RESPONSE"
    const val SESSION_CLOSED = "SESSION_CLOSED"
    const val ERROR = "ERROR"
}

// ═══════════════════════════════════════════
//  ANDROID APP → RELAI
// ═══════════════════════════════════════════

@Serializable
data class RegisterMessage(
    val type: String = MessageType.REGISTER,
    val sessionId: String,
    val tokenHash: String,
    val mode: String,
    val expiresAt: Long
)

@Serializable
data class UnregisterMessage(
    val type: String = MessageType.UNREGISTER,
    val sessionId: String
)

@Serializable
data class AppResponseMessage(
    val type: String = MessageType.RESPONSE,
    val requestId: String,
    val clientId: String,
    val payload: String
)

// ═══════════════════════════════════════════
//  FRONT → RELAI
// ═══════════════════════════════════════════

@Serializable
data class AuthMessage(
    val type: String = MessageType.AUTH,
    val tokenHash: String
)

@Serializable
data class RequestMessage(
    val type: String = MessageType.REQUEST,
    val requestId: String,
    val payload: String
)

// ═══════════════════════════════════════════
//  RELAI → ANDROID APP
// ═══════════════════════════════════════════

@Serializable
data class RegisterOkMessage(
    val type: String = MessageType.REGISTER_OK,
    val sessionId: String
)

@Serializable
data class ForwardMessage(
    val type: String = MessageType.FORWARD,
    val requestId: String,
    val clientId: String,
    val payload: String
)

// ═══════════════════════════════════════════
//  RELAI → FRONT
// ═══════════════════════════════════════════

@Serializable
data class AuthOkMessage(
    val type: String = MessageType.AUTH_OK,
    val mode: String
)

@Serializable
data class AuthFailedMessage(
    val type: String = MessageType.AUTH_FAILED,
    val reason: String
)

@Serializable
data class ClientResponseMessage(
    val type: String = MessageType.RESPONSE,
    val requestId: String,
    val payload: String
)

@Serializable
data class SessionClosedMessage(
    val type: String = MessageType.SESSION_CLOSED,
    val reason: String
)

// ═══════════════════════════════════════════
//  UTILITAIRE
// ═══════════════════════════════════════════

@Serializable
data class BaseMessage(
    val type: String
)