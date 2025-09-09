package org.example.tinyhttp;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.example.tinyhttp.HttpExceptions.BadRequest;
import org.example.tinyhttp.HttpExceptions.HeaderTooLarge;
import org.example.tinyhttp.HttpExceptions.HttpVersionNotSupported;
import org.example.tinyhttp.HttpExceptions.LineTooLong;
import org.example.tinyhttp.HttpExceptions.NotImplemented;
import org.example.tinyhttp.routes.GetRoutes;
import org.example.tinyhttp.routes.PostRoutes;

public final class Main {
    private static final int PORT = 8080;

    private Main() {
    }

    public static void main(String[] args) {
        System.out.println("[tiny-http] listening on http://localhost:" + PORT);

        try (ServerSocket server = new ServerSocket(PORT)) {
            server.setReuseAddress(true);

            // 🔁 Keep serving connections forever (one at a time for now)
            while (true) {
                try (Socket client = server.accept()) {
                    System.out.println("[tiny-http] client connected: " + client.getRemoteSocketAddress());
                    client.setSoTimeout(10_000);
                    handle(client);
                }

            }
        } catch (IOException e) {
            System.err.println("[tiny-http] error: " + e.getMessage());
        }
        System.out.println("[tiny-http] done (accepted one connection)");

    }

    private static void handle(Socket client) throws IOException {
        OutputStream out = client.getOutputStream();
        BufferedInputStream bufferedIn = new BufferedInputStream(client.getInputStream());

        try {
            HttpRequest request = HttpRequest.parse(bufferedIn);
            switch (request.getMethod()) {
                case "GET" -> GetRoutes.handle(request.getTarget(), out);
                case "POST" -> PostRoutes.handle(request.getTarget(), out, request.getHeaders(), request.getBody());
                default -> throw new AssertionError();
            }

        } catch (BadRequest e) {
            HttpResponses.writeText(out, 400, "Bad Request", e.getMessage() + "\n");
        } catch (HeaderTooLarge e) {
            HttpResponses.writeText(out, 431, "Request Header Fields Too Large", e.getMessage() + "\n");
        } catch (NotImplemented e) {
            HttpResponses.writeText(out, 501, "Not Implemented", e.getMessage() + "\n");
        } catch (HttpVersionNotSupported e) {
            HttpResponses.writeText(out, 505, "HTTP Version Not Supported", e.getMessage() + "\n");
        } catch (LineTooLong e) {
            HttpResponses.writeText(out, 430, "", e.getMessage() + "\n");
        }

    }
}

/*
 * Commands to test
 * 
 # GET /hello
printf 'GET /hello HTTP/1.1\r\nHost: x\r\n\r\n' | nc localhost 8080

# POST /echo with small text
printf 'POST /echo HTTP/1.1\r\nHost: x\r\nContent-Type: text/plain\r\nContent-Length: 5\r\n\r\nhello' | nc localhost 8080

# Body too large (should 400)
python3 - <<'PY' | nc localhost 8080
b = b'a' * 1000001
print("POST /echo HTTP/1.1\r\nHost: x\r\nContent-Length: %d\r\n\r\n" % len(b), end='')
import sys; sys.stdout.flush(); sys.stdout.buffer.write(b)
PY

# Incomplete body (send less than CL) -> 400
printf 'POST /echo HTTP/1.1\r\nHost: x\r\nContent-Length: 10\r\n\r\nshort' | nc localhost 8080
 */