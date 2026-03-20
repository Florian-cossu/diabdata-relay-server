import type { ServerWebSocket } from "bun";
import { MessageType, SessionInterruptionType, type SessionClosedMessage, type SessionInterruptionTypes, type SharingMode } from "./types/messages";
import type { WsData } from "./types/wsDataType";

export interface Session {
    sessionId: string;
    tokenHash: string;
    mode: SharingMode;
    expiresAt: number;
    appSocket: ServerWebSocket<WsData>;
    clientSockets: Map<string, ServerWebSocket<WsData>>;
}

class SessionRegistry {
    private sessions = new Map<string, Session>()
    private tokenHashIndex = new Map<string, string>()

    // ═══════════════════════════════════════════
    //  REGISTER — Android app creates session
    // ═══════════════════════════════════════════

    public register ( sessionId: string, tokenHash: string, mode: SharingMode, expiresAt: number, appSocket: ServerWebSocket<WsData> ): boolean {
        if (this.tokenHashIndex.has(tokenHash)) return false

        let session: Session = {
            sessionId,
            tokenHash,
            mode,
            expiresAt,
            appSocket,
            clientSockets: new Map()
        }

        this.sessions.set(sessionId, session)
        this.tokenHashIndex.set(tokenHash, sessionId)

        return true
    }

    // ═══════════════════════════════════════════
    //  UNREGISTER — Android app closes session
    // ═══════════════════════════════════════════

    public unregister ( sessionId: string, reason: SessionInterruptionTypes = "USER_ENDED" ): boolean {
        if (!this.sessions.has(sessionId)) return false

        let session: Session = this.sessions.get(sessionId)!

        this.notifyClients(session, reason)

        this.tokenHashIndex.delete(session.tokenHash)
        this.sessions.delete(sessionId)

        return true
    }

    // ═══════════════════════════════════════════
    //  AUTH — Front connects using token hash
    // ═══════════════════════════════════════════

    public authenticateClient ( tokenHash: string, clientId: string, clientSocket: ServerWebSocket<WsData> ) {
        let session = this.getSessionByTokenHash(tokenHash)

        if (!session || this.isExpired(session.expiresAt)) return null

        session.clientSockets.set(clientId, clientSocket)
        return session
    }

    // ═══════════════════════════════════════════
    //  FORWARD — Forwards request to app
    // ═══════════════════════════════════════════

    public getAppSocket (tokenHash: string) {
        let session = this.getSessionByTokenHash(tokenHash)

        if (!session || this.isExpired(session.expiresAt)) return null

        return session.appSocket
    }

    // ═══════════════════════════════════════════
    //  RESPONSE — Forwards response to front
    // ═══════════════════════════════════════════

    public getClientSocket (sessionId: string, clientId: string) {
        let session = this.getSessionById(sessionId)

        if (!session || !session.clientSockets.get(clientId)) return null

        return session.clientSockets.get(clientId)
    }

    // ═══════════════════════════════════════════
    //  DISCONNECTION of client
    // ═══════════════════════════════════════════

    public removeClient (tokenHash: string, clientId: string) {
        let session = this.getSessionByTokenHash(tokenHash)

        if (!session) return null

        session.clientSockets.delete(clientId)
    }

    // ═══════════════════════════════════════════
    //  DISCONNECTION of android app
    // ═══════════════════════════════════════════

    public onAppDisconnected (sessionId: string) {
        this.unregister(sessionId, SessionInterruptionType.APP_DISCONNECTED)
    }

    // ═══════════════════════════════════════════
    //  UTILS
    // ═══════════════════════════════════════════
    // Notify clients

    private notifyClients( session: Session, reason: SessionInterruptionTypes ) {
        const message: SessionClosedMessage = {
            type: MessageType.SESSION_CLOSED,
            reason
        }

        session.clientSockets.forEach((client) => {
            try {
                client.send(JSON.stringify(message))
                client.close()
            } catch (e) {}
        })
    }

    // Clean expired sessions
    public cleanupExpired () {
        let expired: Session[] = []
        this.sessions.forEach(element => {
            if (this.isExpired(element.expiresAt)) {
                expired.push(element);
            }
        });

        expired.forEach(session => {
            this.unregister(session.sessionId, SessionInterruptionType.SESSION_EXPIRED)
        });
    }

    // Is expired
    private isExpired (expiresAt: number): boolean {
        return (Date.now() / 1000 > expiresAt)
    }

    // ═══════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════

    private getSessionByTokenHash (tokenHash: string) {
        if (!this.tokenHashIndex.has(tokenHash)) return null
        let sessionId = this.tokenHashIndex.get(tokenHash)!

        if (!this.sessions.has(sessionId)) return null
        let session = this.sessions.get(sessionId)!

        return session
    }

    private getSessionById (sessionId: string) {
        if (!this.sessions.has(sessionId)) return null
        let session = this.sessions.get(sessionId)!

        return session
    }

    // ═══════════════════════════════════════════
    //  STATS — For healthcheck
    // ═══════════════════════════════════════════

    public stats() {
        let activeSessions = this.sessions.size
        let connectedClients = 0

        this.sessions.forEach(element => {
            connectedClients += element.clientSockets.size
        });

        return {
            activeSessions,
            connectedClients
        }
    }
}

export const registry = new SessionRegistry()