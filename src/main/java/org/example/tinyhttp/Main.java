package org.example.tinyhttp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class Main {
    private static final int PORT = 8080;

    // Safety limits
    private static final int MAX_REQUEST_LINE_BYTES = 8192; // 8KB
    private static final int MAX_TARGET_LENGTH = 4096; // 8KB

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
        InputStream in = client.getInputStream();
        OutputStream out = client.getOutputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.US_ASCII));

        String requestLine = readRequestLine(reader);
        if (requestLine == null || requestLine.isEmpty()) {
            writeText(out, 400, "Bad Request", "Empty Request Line\n");
            return;
        }

        // Split into METHOD, TARGET, VERSION
        String[] parts = requestLine.split(" ", 3);
        if (parts.length != 3) {
            writeText(out, 400, "Bad Request", "Malformed Request Line\n");
            return;
        }

        String method = parts[0];
        String target = parts[1];
        String version = parts[2];

        // Version Check
        if (!"HTTP/1.1".equals(version)) {
            writeText(out, 505, "HTTP Versoin Not Supported", "Only HTTP/1.1 supported\n");
            return;
        }

        // Verify basic target sanity (we’ll do real URL parsing later)
        if (target.isEmpty() || !target.startsWith("/")) {
            writeText(out, 400, "Bad Request", "Target must start with '/'\n");
            return;
        }

        if (target.length() > MAX_TARGET_LENGTH) {
            writeText(out, 414, "URI Too Long", "Target too long\n");
            return;
        }

        // For step 2 we ignore the headers/body. Just echo the target back
        String body = "Method: " + method + "\n" +
                "Target: " + target + "\n" +
                "Version" + version + "\n";
        writeText(out, 200, "OK", body);

    }

    /*
     * Read exactly one line for the request line, enforcing a max line budget.
     * BufferedReader#readLine handles CRLF and LF; We enforce size manually.
     */
    private static String readRequestLine(BufferedReader reader) throws IOException {
        reader.mark(MAX_REQUEST_LINE_BYTES + 1);
        String line = reader.readLine(); // automaticly rips th CRLF (/r/n)

        if (line == null)
            return null;

        // Enforce Limit: if exceeded, bail before parsing
        if (line.getBytes(StandardCharsets.US_ASCII).length > MAX_REQUEST_LINE_BYTES) {
            // reset so we can still write a response, then consume and close
            reader.reset();
            throw new LineTooLongException();
        }

        return line;
    }

    private static void writeText(OutputStream out, int status, String reason, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 " + status + " " + reason + "\r\n" +
                "Content-Type: text/plain; charset=utf-8\r\n" +
                "Content-Length: " + body.length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";

        out.write(headers.getBytes(StandardCharsets.US_ASCII));
        out.write(body);
        out.flush();
    }

    // Minimal cheked exception to signal an overlong request line.
    private static final class LineTooLongException extends IOException {
    }
}
