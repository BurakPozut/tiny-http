package org.example.tinyhttp;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class Main 
{
    private Main() {}
    public static void main( String[] args )
    {
        int port = 8080;
        System.out.println("[tiny-http] listening on http://localhost:" + port);

        try(ServerSocket server = new ServerSocket(port)) {
            server.setReuseAddress(true);

            // 🔁 Keep serving connections forever (one at a time for now)
            while(true){
                try(Socket client = server.accept()) {
                    System.out.println("[tiny-http] client connected: " + client.getRemoteSocketAddress());
                    client.setSoTimeout(10_000);
                    writeFixedHttpResponse(client.getOutputStream());
                }
                
            }
            } catch (IOException e) {
                System.err.println("[tiny-http] error: " + e.getMessage());
        }
        System.out.println("[tiny-http] done (accepted one connection)");

    }

    private static void writeFixedHttpResponse(OutputStream out) throws IOException {
        String body = "Hello From Tiny HTTP";
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

         // HTTP/1.1 requires CRLF between lines and a blank line before the body.
         String headers =
         "HTTP/1.1 200 OK\r\n" +
         "Content-Type: text/plain; charset=utf-8\r\n" +
         "Content-Length: " + bodyBytes.length + "\r\n" +
         "Connection: close\r\n" +   // keep it simple for 1B
         "\r\n";

         out.write(headers.getBytes(StandardCharsets.US_ASCII));
         out.write(bodyBytes);
         out.flush();
    }
}
