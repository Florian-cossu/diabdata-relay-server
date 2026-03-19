export const MessageType = {
    REGISTER: "REGISTER",
    REGISTER_OK: "REGISTER_OK",
    UNREGISTER: "UNREGISTER",
    AUTH: "AUTH",
    AUTH_OK: "AUTH_OK",
    AUTH_FAILED: "AUTH_FAILED",
    REQUEST: "REQUEST",
    FORWARD: "FORWARD",
    RESPONSE: "RESPONSE",
    SESSION_CLOSED: "SESSION_CLOSED",
    ERROR: "ERROR"
} as const

export type MessageTypes = typeof MessageType[keyof typeof MessageType]

export const SessionInterruptionType = {
    USER_ENDED: "USER_ENDED",
    SESSION_EXPIRED: "SESSION_EXPIRED",
    APP_DISCONNECTED: "APP_DISCONNECTED"
} as const

export type SessionInterruptionTypes = typeof SessionInterruptionType[keyof typeof SessionInterruptionType]

export const SharingType = {
    COMPANION: "COMPANION",
    MEDICAL: "MEDICAL"
} as const

export type SharingMode = typeof SharingType[keyof typeof SharingType]

// ═══════════════════════════════════════════
//  ANDROID APP → RELAY
// ═══════════════════════════════════════════

export interface RegisterMessage {
    type: typeof MessageType.REGISTER;
    sessionId: string;
    tokenHash: string;
    mode: SharingMode;
    expiresAt: number
}

export interface UnregisterMessage {
    type: typeof MessageType.UNREGISTER;
    sessionId: string;
}

export interface AppResponseMessage {
    type: typeof MessageType.RESPONSE;
    requestId: string;
    clientId: string;
    payload: string;
}

// ═══════════════════════════════════════════
//  FRONT → RELAY
// ═══════════════════════════════════════════

export interface AuthMessage {
    type: typeof MessageType.AUTH;
    tokenHash: string;
}

export interface RequestMessage {
    type: typeof MessageType.REQUEST;
    requestId: string;
    payload: string;
}

// ═══════════════════════════════════════════
//  RELAY → ANDROID APP
// ═══════════════════════════════════════════

export interface RegisterOkMessage {
    type: typeof MessageType.REGISTER_OK;
    sessionId: string;
}

export interface ForwardMessage {
    type: typeof MessageType.FORWARD;
    requestId: string;
    clientId: string;
    payload: string;
}

// ═══════════════════════════════════════════
//  RELAI → FRONT
// ═══════════════════════════════════════════

export interface AuthOkMessage {
    type: typeof MessageType.AUTH_OK;
    mode: SharingMode;
}

export interface AuthFailedMessage {
    type: typeof MessageType.AUTH_FAILED;
    reason: string;
}

export interface ClientResponseMessage {
    type: typeof MessageType.RESPONSE;
    requestId: string;
    payload: string;
}

export interface SessionClosedMessage {
    type: typeof MessageType.SESSION_CLOSED;
    reason: string;
}

// ═══════════════════════════════════════════
//  UTILITAIRE
// ═══════════════════════════════════════════

export interface BaseMessage {
    type: MessageTypes;
}

export interface ErrorMessage {
    type: typeof MessageType.ERROR;
    reason: string;
}