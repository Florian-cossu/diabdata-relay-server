package com.diabdata.relay

import com.diabdata.relay.registry.SessionRegistry
import com.diabdata.relay.routing.appSocket
import com.diabdata.relay.routing.clientSocket
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.minutes

fun main() {
    embeddedServer(Netty, port = 8080) {
        configurePlugins()
        configureRouting()
        startCleanupJob()
    }.start(wait = true)
}

fun Application.configurePlugins() {
    install(ContentNegotiation) {
        json(Json { encodeDefaults = true })
    }
    install(WebSockets) {
        pingPeriod = 30.seconds
        timeout = 60.seconds
        maxFrameSize = 65536
    }
}

fun Application.configureRouting() {
    routing {
        // Health check
        get("/health") {
            call.respond(SessionRegistry.stats())
        }

        // WebSocket endpoints
        appSocket()
        clientSocket()
    }
}

fun Application.startCleanupJob() {
    launch {
        while (isActive) {
            delay(1.minutes)
            SessionRegistry.cleanupExpired()
        }
    }
}