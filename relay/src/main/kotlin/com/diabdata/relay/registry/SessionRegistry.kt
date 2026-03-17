package com.diabdata.relay.registry

import com.diabdata.relay.models.RelaySession
import com.diabdata.relay.models.SessionClosedMessage
import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

object SessionRegistry {

    private val sessions = ConcurrentHashMap<String, RelaySession>()
    private val tokenHashIndex = ConcurrentHashMap<String, String>() // tokenHash → sessionId
    private val cleanupMutex = Mutex()

    private val json = Json { encodeDefaults = true }

    // ═══════════════════════════════════════════
    //  REGISTER — Android app creates a session
    // ═══════════════════════════════════════════

    fun register(
        sessionId: String,
        tokenHash: String,
        mode: String,
        expiresAt: Long,
        appSocket: WebSocketSession
    ): Boolean {
        // Checks that there isn't already a session with the tokenhash
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

    // ════════════════════════════════════════════
    //  UNREGISTER — Android app closes the session
    // ════════════════════════════════════════════

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

        // Expiration check
        if (session.isExpired()) return null

        // Add client to session
        session.clientSockets[clientId] = clientSocket
        return session
    }

    // ═════════════════════════════════════════════
    //  FORWARD — Forwards front requests to the app
    // ═════════════════════════════════════════════

    fun getAppSocket(tokenHash: String): WebSocketSession? {
        val sessionId = tokenHashIndex[tokenHash] ?: return null
        val session = sessions[sessionId] ?: return null
        if (session.isExpired()) return null
        return session.appSocket
    }

    // ═══════════════════════════════════════════════
    //  RESPONSE — Forwards apps response to the front
    // ═══════════════════════════════════════════════

    fun getClientSocket(sessionId: String, clientId: String): WebSocketSession? {
        val session = sessions[sessionId] ?: return null
        return session.clientSockets[clientId]
    }

    // ═════════════════════
    //  Client disconnection
    // ═════════════════════

    fun removeClient(tokenHash: String, clientId: String) {
        val sessionId = tokenHashIndex[tokenHash] ?: return
        val session = sessions[sessionId] ?: return
        session.clientSockets.remove(clientId)
    }

    // ══════════════════════════
    //  Android app disconnection
    // ══════════════════════════

    suspend fun onAppDisconnected(sessionId: String) {
        unregister(sessionId, "APP_DISCONNECTED")
    }

    // ═══════════════════════════════════════════
    //  Expired sessions cleanup
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
    //  UTILITAIRE — Notify client
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
            } catch (_: Exception) {
                // Client déjà déconnecté, on ignore
            }
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