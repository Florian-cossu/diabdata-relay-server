package com.diabdata.relay.routing

import com.diabdata.relay.models.*
import com.diabdata.relay.registry.SessionRegistry
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

fun Route.appSocket() {
    webSocket("/ws/app") {

        var registeredSessionId: String? = null

        try {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    val base = json.decodeFromString(BaseMessage.serializer(), text)

                    when (base.type) {

                        // ═══════════════════════════════════
                        //  REGISTER — Créer une session
                        // ═══════════════════════════════════
                        "REGISTER" -> {
                            val msg = json.decodeFromString(RegisterMessage.serializer(), text)

                            val success = SessionRegistry.register(
                                sessionId = msg.sessionId,
                                tokenHash = msg.tokenHash,
                                mode = msg.mode,
                                expiresAt = msg.expiresAt,
                                appSocket = this
                            )

                            if (success) {
                                registeredSessionId = msg.sessionId
                                val response = json.encodeToString(
                                    RegisterOkMessage.serializer(),
                                    RegisterOkMessage(sessionId = msg.sessionId)
                                )
                                send(Frame.Text(response))
                            } else {
                                send(Frame.Text("""{"type":"ERROR","reason":"SESSION_ALREADY_EXISTS"}"""))
                                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Session already exists"))
                            }
                        }

                        // ═══════════════════════════════════
                        //  RESPONSE — Réponse vers le front
                        // ═══════════════════════════════════
                        "RESPONSE" -> {
                            val msg = json.decodeFromString(AppResponseMessage.serializer(), text)
                            val sessionId = registeredSessionId ?: continue

                            val clientSocket = SessionRegistry.getClientSocket(sessionId, msg.clientId)
                            if (clientSocket != null) {
                                val response = json.encodeToString(
                                    ClientResponseMessage.serializer(),
                                    ClientResponseMessage(
                                        requestId = msg.requestId,
                                        payload = msg.payload
                                    )
                                )
                                clientSocket.send(Frame.Text(response))
                            }
                        }

                        // ═══════════════════════════════════
                        //  UNREGISTER — Fermer la session
                        // ═══════════════════════════════════
                        "UNREGISTER" -> {
                            val sessionId = registeredSessionId ?: continue
                            SessionRegistry.unregister(sessionId, "USER_ENDED")
                            registeredSessionId = null
                            close(CloseReason(CloseReason.Codes.NORMAL, "Session ended"))
                        }
                    }
                }
            }
        } finally {
            // L'app s'est déconnectée (volontairement ou non)
            registeredSessionId?.let { sessionId ->
                SessionRegistry.onAppDisconnected(sessionId)
            }
        }
    }
}