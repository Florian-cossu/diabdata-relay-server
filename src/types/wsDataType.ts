import type { SharingMode } from "./messages";

export const WsRoleType = {
    CLIENT: "CLIENT",
    APPLICATION: "APPLICATION"
} as const

export type WsRoleTypes = typeof WsRoleType[keyof typeof WsRoleType]

export type WsData = {
    role: WsRoleTypes;
    clientId?: string;
    sessionId?: string;
    tokenHash?: string;
}