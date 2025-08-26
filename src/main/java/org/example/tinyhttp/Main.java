package org.example.tinyhttp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public final class Main 
{
    private Main() {}
    public static void main( String[] args )
    {
        int port = 8080;
        System.out.println("[tiny-http] listening on http://localhost:" + port);

        try(ServerSocket server = new ServerSocket(port)) {
            server.setReuseAddress(true);
            try(Socket client = server.accept()) {
                System.out.println("[tiny-http] client connected: " + client.getRemoteSocketAddress());
            }
            
        } catch (IOException e) {
            System.err.println("[tiny-http] error: " + e.getMessage());
        }
        System.out.println("[tiny-http] done (accepted one connection)");

    }
}
