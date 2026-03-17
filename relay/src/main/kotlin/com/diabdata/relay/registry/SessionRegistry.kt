package com.diabdata.relay.registry

import com.diabdata.relay.models.SessionClosedMessage
import com.diabdata.relay.models.RelaySession
import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

object SessionRegistry {

    private val sessions = ConcurrentHashMap<String, RelaySession>()
    private val tokenHashIndex = ConcurrentHashMap<String, String>()
    private val registerMutex = Mutex()
    private val cleanupMutex = Mutex()

    private val json = Json { encodeDefaults = true }

    // ═══════════════════════════════════════════
    //  REGISTER — Android app creates session
    // ═══════════════════════════════════════════

    suspend fun register(
        sessionId: String,
        tokenHash: String,
        mode: String,
        expiresAt: Long,
        appSocket: WebSocketSession
    ): Boolean {
        registerMutex.withLock {
            if (tokenHashIndex.containsKey(tokenHash)) return false

            val session = RelaySession(
                sessionId = sessionId,
                tokenHash = tokenHash,
                mode = mode,
                expiresAt = expiresAt,
                appSocket = appSocket
            )

            sessions[sessionId] = session
            tokenHashIndex[tokenHash] = sessionId
            return true
        }
    }

    // ═══════════════════════════════════════════
    //  UNREGISTER — Android app closes session
    // ═══════════════════════════════════════════

    suspend fun unregister(sessionId: String, reason: String = "USER_ENDED") {
        val session = sessions[sessionId] ?: return

        notifyClients(session, reason)

        tokenHashIndex.remove(session.tokenHash)
        sessions.remove(sessionId)
    }

    // ═══════════════════════════════════════════
    //  AUTH — Front connects using token hash
    // ═══════════════════════════════════════════

    fun authenticate(tokenHash: String, clientId: String, clientSocket: WebSocketSession): RelaySession? {
        val sessionId = tokenHashIndex[tokenHash] ?: return null
        val session = sessions[sessionId] ?: return null

        if (session.isExpired()) return null

        session.clientSockets[clientId] = clientSocket
        return session
    }

    // ═══════════════════════════════════════════
    //  FORWARD — Forwards request to app
    // ═══════════════════════════════════════════

    fun getAppSocket(tokenHash: String): WebSocketSession? {
        val sessionId = tokenHashIndex[tokenHash] ?: return null
        val session = sessions[sessionId] ?: return null
        if (session.isExpired()) return null
        return session.appSocket
    }

    // ═══════════════════════════════════════════
    //  RESPONSE — Forwards response to front
    // ═══════════════════════════════════════════

    fun getClientSocket(sessionId: String, clientId: String): WebSocketSession? {
        val session = sessions[sessionId] ?: return null
        return session.clientSockets[clientId]
    }

    // ═══════════════════════════════════════════
    //  DISCONNECTION of client
    // ═══════════════════════════════════════════

    fun removeClient(tokenHash: String, clientId: String) {
        val sessionId = tokenHashIndex[tokenHash] ?: return
        val session = sessions[sessionId] ?: return
        session.clientSockets.remove(clientId)
    }

    // ═══════════════════════════════════════════
    //  DISCONNECTION of android app
    // ═══════════════════════════════════════════

    suspend fun onAppDisconnected(sessionId: String) {
        unregister(sessionId, "APP_DISCONNECTED")
    }

    // ═══════════════════════════════════════════
    //  CLEAN expired sessions
    // ═══════════════════════════════════════════

    suspend fun cleanupExpired() {
        cleanupMutex.withLock {
            val expired = sessions.values.filter { it.isExpired() }
            expired.forEach { session ->
                unregister(session.sessionId, "SESSION_EXPIRED")
            }
        }
    }

    // ═══════════════════════════════════════════
    //  UTILS Notify clients
    // ═══════════════════════════════════════════

    private suspend fun notifyClients(session: RelaySession, reason: String) {
        val message = json.encodeToString(
            SessionClosedMessage.serializer(),
            SessionClosedMessage(reason = reason)
        )
        session.clientSockets.values.forEach { socket ->
            try {
                socket.send(Frame.Text(message))
                socket.close()
            } catch (_: java.io.IOException) {}
        }
    }

    // ═══════════════════════════════════════════
    //  STATS — For healthcheck
    // ═══════════════════════════════════════════

    fun stats(): Map<String, Int> = mapOf(
        "activeSessions" to sessions.size,
        "connectedClients" to sessions.values.sumOf { it.clientSockets.size }
    )
}