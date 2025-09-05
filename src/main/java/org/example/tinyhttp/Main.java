package org.example.tinyhttp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.example.tinyhttp.HttpExceptions.*;
import org.example.tinyhttp.HttpExceptions.BadRequest;
import org.example.tinyhttp.HttpExceptions.HeaderTooLarge;
import org.example.tinyhttp.HttpExceptions.HttpVersionNotSupported;
import org.example.tinyhttp.HttpExceptions.LineTooLong;
import org.example.tinyhttp.HttpExceptions.NotImplemented;
import static org.example.tinyhttp.HttpResponses.writeText;

public final class Main {
    private static final int PORT = 8080;

    // Safety limits
    private static final int MAX_REQUEST_LINE_BYTES = 8192; // 8KB
    private static final int MAX_TARGET_LENGTH = 4096; // 8KB
    private static final int MAX_HEADER_COUNT = 100; // prevent header bombs
    private static final int MAX_HEADER_LINE_BYTES = 8192; // 8KB per header line
    private static final int MAX_HEADERS_TOTAL_BYTES = 65536; // 64KB across all header lines
    private static final long MAX_BODY_BYTES = 1_000_000L; // 1 MB safety cap

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

        try {

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
                throw new HttpVersionNotSupported("Only HTTP/1.1 supported");
            }

            // Verify basic target sanity (we’ll do real URL parsing later)
            if (target.isEmpty() || !target.startsWith("/")) {
                throw new BadRequest("Target must start with '/'");
            }

            if (target.length() > MAX_TARGET_LENGTH) {
                throw new HeaderTooLarge("Target too long");
            }

            // --- 2 ) Headers
            HttpHeaders headers = readHeaders(reader);

            // ---- 3) HTTP/1.1 requires Host ----
            List<String> hosts = headers.all("host");
            // List<String> hosts = headers.getOrDefault("host", Collections.emptyList());
            if (hosts.isEmpty())
                throw new BadRequest("Missing Host header");
            boolean allSame = hosts.stream().allMatch(h -> h.equals(hosts.get(0)));

            if (!allSame)
                throw new BadRequest("Multiple differing Host headers");

            // ---- 4) Pre-parse body semantics (do NOT read body yet) ----
            String cl = headers.first("content-length");
            String te = headers.first("transfer-encoding");

            if (cl != null && te != null)
                throw new BadRequest("Content-Length and Transfer-Encoding both present");

            if (te != null && !te.equalsIgnoreCase("identity"))
                throw new NotImplemented("Transfer-Encoding not implemented");

            Long contentLength = null;
            if (cl != null) {
                try {
                    contentLength = Long.parseLong(cl);
                    if (contentLength < 0 || contentLength > 10_000_000L)
                        throw new BadRequest("Invalid Content-Length");
                } catch (NumberFormatException e) {
                    throw new BadRequest("Invalid Content-Length");
                }
            }

            byte[] body = new byte[0];
            if(contentLength != null && contentLength > 0)
                body = readFixedBytes(in, contentLength);

            // ---- 5) response: echo basics + some headers ----
            String response_body = ""
                    + "Method: " + method + "\n"
                    + "Target: " + target + "\n"
                    + "Version: " + version + "\n"
                    + "Host: " + (hosts.isEmpty() ? "-> none" : hosts.get(0)) + "\n"
                    + "User-Agent: " + (headers.first("user-agent", "-> none")) + "\n"
                    + "Content-Length (req): " + (contentLength == null ? "-> none" : contentLength) + "\n";

            writeText(out, 200, "OK", response_body);
        } catch (BadRequest e) {
            writeText(out, 400, "Bad Request", e.getMessage() + "\n");
        } catch (HeaderTooLarge e) {
            writeText(out, 431, "Request Header Fields Too Large", e.getMessage() + "\n");
        } catch (NotImplemented e) {
            writeText(out, 501, "Not Implemented", e.getMessage() + "\n");
        } catch (HttpVersionNotSupported e) {
            writeText(out, 505, "HTTP Version Not Supported", e.getMessage() + "\n");
        } catch (LineTooLong e) {
            writeText(out, 430, "", e.getMessage() + "\n");
        }

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
            throw new LineTooLong("Requst Line Too Long");
        }

        return line;
    }

    private static HttpHeaders readHeaders(BufferedReader reader) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        int totalBytes = 0, count = 0;

        while (true) {
            String line = reader.readLine();
            if (line == null)
                throw new BadRequest("Unexpected end of headers");
            if (line.isEmpty())
                break;

            int lineBytes = HttpHeaders.asciiLenWithCrlf(line);
            if (lineBytes > MAX_HEADER_LINE_BYTES)
                throw new HeaderTooLarge("Header line too large");
            totalBytes += lineBytes;
            if (totalBytes > MAX_HEADERS_TOTAL_BYTES)
                throw new HeaderTooLarge("Headers too large");
            if (++count > MAX_HEADER_COUNT)
                throw new HeaderTooLarge("Too many header fields");

            if (line.charAt(0) == ' ' || line.charAt(0) == '\t')
                throw new BadRequest("Obsolete header folding not allowed");

            int colon = line.indexOf(':');
            if (colon <= 0)
                throw new BadRequest("Malformed header (missing colon)");

            String name = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();
            if (name.isEmpty())
                throw new BadRequest("Empty header name");

            headers.add(name, value);
        }
        return headers;
    }

    private static byte[] readFixedBytes(InputStream in, long length) throws
    IOException, BadRequest{
        if(length > Integer.MAX_VALUE)
            throw new BadRequest("Body too large");

        int toRead = (int) length;
        byte[] buff = in.readNBytes(toRead);
        // Client closed early
        if(buff.length != toRead)
            throw new BadRequest("Incoplete Request Body");
        return buff;
    }
}

/*
 * Commands to test
 * 
 * # Valid GET (HTTP/1.1 requires Host)
 * printf 'GET /hello HTTP/1.1\r\nHost: localhost\r\n\r\n' | nc localhost 8080
 * 
 * # Missing Host -> 400
 * printf 'GET /hello HTTP/1.1\r\n\r\n' | nc localhost 8080
 * 
 * # Multiple Host different -> 400
 * printf 'GET / HTTP/1.1\r\nHost: a\r\nHost: b\r\n\r\n' | nc localhost 8080
 * 
 * # Header too large -> 431
 * python3 - <<'PY' | nc localhost 8080
 * print("GET / HTTP/1.1\r\nHost: x\r\nX-Big: " + "a"*9000 + "\r\n\r\n")
 * PY
 * 
 * # Transfer-Encoding (not implemented yet) -> 501
 * printf 'POST / HTTP/1.1\r\nHost: x\r\nTransfer-Encoding: chunked\r\n\r\n' |
 * nc localhost 8080
 * 
 * # With Content-Length (we ignore body for now)
 * printf 'POST /echo HTTP/1.1\r\nHost: x\r\nContent-Length: 5\r\n\r\nhello' |
 * nc localhost 8080
 */