package com.diabdata.relay.routing

import com.diabdata.relay.models.*
import com.diabdata.relay.registry.SessionRegistry
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import java.util.UUID

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

fun Route.clientSocket() {
    webSocket("/ws/client") {

        var authenticatedTokenHash: String? = null
        val clientId = UUID.randomUUID().toString()

        try {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    val base = json.decodeFromString(BaseMessage.serializer(), text)

                    when (base.type) {

                        // ═══════════════════════════════════
                        //  AUTH — Se connecter à une session
                        // ═══════════════════════════════════
                        "AUTH" -> {
                            val msg = json.decodeFromString(AuthMessage.serializer(), text)

                            val session = SessionRegistry.authenticate(
                                tokenHash = msg.tokenHash,
                                clientId = clientId,
                                clientSocket = this
                            )

                            if (session != null) {
                                authenticatedTokenHash = msg.tokenHash
                                val response = json.encodeToString(
                                    AuthOkMessage.serializer(),
                                    AuthOkMessage(mode = session.mode)
                                )
                                send(Frame.Text(response))
                            } else {
                                val response = json.encodeToString(
                                    AuthFailedMessage.serializer(),
                                    AuthFailedMessage(reason = "INVALID_TOKEN_OR_EXPIRED")
                                )
                                send(Frame.Text(response))
                                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Auth failed"))
                            }
                        }

                        // ═══════════════════════════════════
                        //  REQUEST — Demande vers l'app Android
                        // ═══════════════════════════════════
                        "REQUEST" -> {
                            val tokenHash = authenticatedTokenHash ?: continue
                            val msg = json.decodeFromString(RequestMessage.serializer(), text)

                            val appSocket = SessionRegistry.getAppSocket(tokenHash)
                            if (appSocket != null) {
                                val forward = json.encodeToString(
                                    ForwardMessage.serializer(),
                                    ForwardMessage(
                                        requestId = msg.requestId,
                                        clientId = clientId,
                                        payload = msg.payload
                                    )
                                )
                                appSocket.send(Frame.Text(forward))
                            }
                        }
                    }
                }
            }
        } finally {
            // Le client s'est déconnecté
            authenticatedTokenHash?.let { tokenHash ->
                SessionRegistry.removeClient(tokenHash, clientId)
            }
        }
    }
}