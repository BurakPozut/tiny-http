package org.example.tinyhttp;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.List;

import static org.example.tinyhttp.HttpParser.readFixedBytes;
import static org.example.tinyhttp.HttpParser.readLineCRLF;


public final class HttpRequest {
  private static final int MAX_REQUEST_LINE_BYTES = 8192; // 8KB
  private static final int MAX_TARGET_LENGTH = 4096; // 8KB
  private static final int MAX_HEADER_COUNT = 100; // prevent header bombs
  private static final int MAX_HEADER_LINE_BYTES = 8192; // 8KB per header line
  private static final int MAX_HEADERS_TOTAL_BYTES = 65536; // 64KB across all header lines
  private static final long MAX_BODY_BYTES = 1_000_000L; // 1 MB safety cap

  private final String method;
  private final String target;
  private final String version;
  private final HttpHeaders headers;
  private final byte[] body;

  private HttpRequest(String method, String target, String version, HttpHeaders headers, byte[] body) {
    this.method = method;
    this.target = target;
    this.version = version;
    this.headers = headers;
    this.body = body;
  }

  // Getters
  public String getMethod() { return method; }
  public String getTarget() { return target; }
  public String getVersion() { return version; }
  public HttpHeaders getHeaders() { return headers; }
  public byte[] getBody() { return body; }

  public static HttpRequest parse(BufferedInputStream in) throws IOException {
    // Read Request Line
    String requestLine = readLineCRLF(in, MAX_REQUEST_LINE_BYTES);
    
    if(requestLine == null || requestLine.isEmpty())
      throw new HttpExceptions.BadRequest("Empty Request Line");
    
    // Split into METHOD, TARGET, VERSION
    String[] parts = requestLine.split(" ", 3);
    if (parts.length != 3) 
      throw new HttpExceptions.BadRequest("Malformed Request Line");
    
    String method = parts[0];
    String target = parts[1];
    String version = parts[2];

    // Version Check
    if (!"HTTP/1.1".equals(version)) 
      throw new HttpExceptions.HttpVersionNotSupported("Only HTTP/1.1 supported");
    
    // Verify basic target sanity
    if (target.isEmpty() || !target.startsWith("/")) 
      throw new HttpExceptions.BadRequest("Target must start with '/'");

    if (target.length() > MAX_TARGET_LENGTH) 
        throw new HttpExceptions.HeaderTooLarge("Target too long");
    
    // 2. Read headers
    HttpHeaders headers = readHeaders(in);

    // 3. HTTP/1.1 requires Host
    List<String> hosts = headers.all("host");
    if (hosts.isEmpty())
        throw new HttpExceptions.BadRequest("Missing Host header");

    boolean allSame = hosts.stream().allMatch(h -> h.equals(hosts.get(0)));
    if (!allSame)
        throw new HttpExceptions.BadRequest("Multiple differing Host headers");

    // 4. Parse body semantics
    String cl = headers.first("content-length");
    String te = headers.first("transfer-encoding");

    if (cl != null && te != null)
        throw new HttpExceptions.BadRequest("Content-Length and Transfer-Encoding both present");

    if (te != null && !te.equalsIgnoreCase("identity"))
        throw new HttpExceptions.NotImplemented("Transfer-Encoding not implemented");

    Long contentLength = null;
    if (cl != null) {
        try {
            contentLength = Long.valueOf(cl);
            if (contentLength < 0 || contentLength > MAX_BODY_BYTES)
                throw new HttpExceptions.BadRequest("Invalid Content-Length");
        } catch (NumberFormatException e) {
            throw new HttpExceptions.BadRequest("Invalid Content-Length");
        }
    }

    // 5. Read body
    byte[] body = new byte[0];
    if (contentLength != null && contentLength > 0) {
      body = readFixedBytes(in, contentLength);
    }
    return new HttpRequest(method, target, version, headers, body);

  }

  private static HttpHeaders readHeaders(BufferedInputStream in) throws IOException {
    HttpHeaders headers = new HttpHeaders();
    int totalBytes = 0, count = 0;

    while (true) {
        String line = readLineCRLF(in);
        if (line == null)
            throw new HttpExceptions.BadRequest("Unexpected end of headers");
        if (line.isEmpty())
            break;

        int lineBytes = HttpHeaders.asciiLenWithCrlf(line);
        if (lineBytes > MAX_HEADER_LINE_BYTES)
            throw new HttpExceptions.HeaderTooLarge("Header line too large");
        totalBytes += lineBytes;
        if (totalBytes > MAX_HEADERS_TOTAL_BYTES)
            throw new HttpExceptions.HeaderTooLarge("Headers too large");
        if (++count > MAX_HEADER_COUNT)
            throw new HttpExceptions.HeaderTooLarge("Too many header fields");

        if (line.charAt(0) == ' ' || line.charAt(0) == '\t')
            throw new HttpExceptions.BadRequest("Obsolete header folding not allowed");

        int colon = line.indexOf(':');
        if (colon <= 0)
            throw new HttpExceptions.BadRequest("Malformed header (missing colon)");

        String name = line.substring(0, colon).trim();
        String value = line.substring(colon + 1).trim();
        if (name.isEmpty())
            throw new HttpExceptions.BadRequest("Empty header name");

        headers.add(name, value);
    }
    return headers;
}
}
