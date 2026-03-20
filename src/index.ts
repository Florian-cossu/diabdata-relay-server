import { randomUUIDv7 } from "bun";
import { registry } from "./registry";
import { WsRoleType, type WsData } from "./types/wsDataType";
import {
  ErrorType,
  MessageType,
  SessionInterruptionType,
} from "./types/messages";

const server = Bun.serve<WsData>({
  port: 8080,
  fetch(req, server) {
    const url = new URL(req.url);
    if (url.pathname === "/health") {
      return Response.json(registry.stats());
    } else if (url.pathname === "/ws/app") {
      server.upgrade(req, {
        data: {
          role: WsRoleType.APPLICATION,
          clientId: undefined,
        },
      });
    } else if (url.pathname === "/ws/client") {
      let clientId = randomUUIDv7();
      server.upgrade(req, {
        data: {
          role: WsRoleType.CLIENT,
          clientId: clientId,
        },
      });
    } else {
      return Response.error();
    }
  },
  websocket: {
    open(ws) {},
    message(ws, message) {
      let data;
      try {
        data = JSON.parse(message as string);
      } catch (e) {
        ws.close(1007, "Invalid frame payload data");
        return;
      }

      if (ws.data.role == WsRoleType.APPLICATION) {
        // Register the App
        if (data.type == MessageType.REGISTER) {
          const success = registry.register(
            data.sessionId,
            data.tokenHash,
            data.mode,
            data.expiresAt,
            ws,
          );

          if (success) {
            ws.data.sessionId = data.sessionId;
            ws.data.tokenHash = data.tokenHash;
            ws.send(
              JSON.stringify({
                type: MessageType.REGISTER_OK,
                sessionId: data.sessionId,
              }),
            );
          } else if (!success) {
            ws.send(
              JSON.stringify({
                type: MessageType.ERROR,
                reason: ErrorType.SESSION_ALREADY_EXISTS,
              }),
            );
          }
        }

        // Unregister the App
        else if (data.type == MessageType.UNREGISTER) {
          let sessionId = ws.data.sessionId;
          if (!sessionId) {
            ws.send(
              JSON.stringify({
                type: MessageType.ERROR,
                reason: ErrorType.SESSION_DOES_NOT_EXIST,
              }),
            );
            return;
          }

          let success = registry.unregister(
            sessionId,
            SessionInterruptionType.USER_ENDED,
          );

          if (success) {
            ws.send(
              JSON.stringify({
                type: MessageType.UNREGISTER_OK,
              }),
            );
          } else {
            ws.send(
              JSON.stringify({
                type: MessageType.ERROR,
                reason: ErrorType.SESSION_COULD_NOT_BE_TERMINATED,
              }),
            );
          }

          ws.close();
        }

        // Response to client request
        else if (data.type == MessageType.RESPONSE) {
          let sessionId = ws.data.sessionId;
          let requestId = data.requestId;
          let clientId = data.clientId;
          let payload = data.payload;

          if (!sessionId || !requestId || !clientId || !payload) return;

          let clientSocket = registry.getClientSocket(sessionId, clientId);

          if (!clientSocket) return;

          clientSocket.send(
            JSON.stringify({
              type: MessageType.RESPONSE,
              requestId,
              payload,
            }),
          );
        }
      } else if (ws.data.role == WsRoleType.CLIENT) {
        // Front authentication
        if (data.type == MessageType.AUTH) {
          if (!data.tokenHash) {
            ws.send(
              JSON.stringify({
                type: MessageType.AUTH_FAILED,
                reason: ErrorType.MISSING_TOKEN_HASH,
              }),
            );
            return;
          }

          if (!ws.data.clientId) return;

          let session = registry.authenticateClient(
            data.tokenHash,
            ws.data.clientId,
            ws,
          );

          if (!session) {
            ws.send(
              JSON.stringify({
                type: MessageType.AUTH_FAILED,
                reason: ErrorType.SESSION_COULD_NOT_BE_CREATED,
              }),
            );

            return;
          }

          ws.data.tokenHash = data.tokenHash;

          ws.send(
            JSON.stringify({
              type: MessageType.AUTH_OK,
              sessionMode: session.mode,
            }),
          );
        }

        // Request from front
        if (data.type == MessageType.REQUEST) {
          let tokenHash = ws.data.tokenHash;
          let requestId = data.requestId;
          let payload = data.payload;
          let clientId = ws.data.clientId;

          if (!payload || !tokenHash || !clientId) return;

          let appSocket = registry.getAppSocket(tokenHash);
          if (appSocket == null) {
            ws.send(
              JSON.stringify({
                type: MessageType.SESSION_CLOSED,
                reason: SessionInterruptionType.APP_DISCONNECTED,
              }),
            );
          } else {
            appSocket.send(
              JSON.stringify({
                type: MessageType.FORWARD,
                requestId,
                clientId,
                payload,
              }),
            );
          }
        }
      }
    },
    close(ws) {
      if (ws.data.role == WsRoleType.APPLICATION) {
        let session = ws.data.sessionId;

        if (!session) return;

        registry.onAppDisconnected(session);
      } else if (ws.data.role == WsRoleType.CLIENT) {
        // Disconnect front
        let tokenHash = ws.data.tokenHash;
        let clientId = ws.data.clientId;

        if (!tokenHash || !clientId) return;

        registry.removeClient(tokenHash, clientId);
      }
    },
  },
});

setInterval(() => {
  registry.cleanupExpired();
}, 60000);

console.log(`Listening on ${server.url}`);
