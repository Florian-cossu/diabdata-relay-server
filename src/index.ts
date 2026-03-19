import { randomUUIDv7 } from "bun";
import { registry } from "./registry";
import { WsRoleType, type WsData } from "./types/wsDataType";
import { MessageType } from "./types/messages";

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
    open(ws) { },
    message(ws, message) {
      if (ws.data.role == WsRoleType.APPLICATION) {
        let data = JSON.parse(message as string);
        if (data.type == MessageType.REGISTER) {
          const success = registry
            .register(
              data.sessionId,
              data.tokenHash,
              data.mode,
              data.expiresAt,
              ws,
            )

          if (success) {
            ws.data.sessionId = data.sessionId;
            ws.data.tokenHash = data.tokenHash;
            ws.send(JSON.stringify({
               type: MessageType.REGISTER_OK,
               sessionId: data.sessionId
            }));
          } else if (!success) {
            ws.send(JSON.stringify({
                type: MessageType.ERROR,
                reason: "SESSION_ALREADY_EXISTS"
            }));
          }
        }
      }
    },
  },
});
