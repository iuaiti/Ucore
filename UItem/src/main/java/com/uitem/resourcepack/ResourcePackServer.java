package com.uitem.resourcepack;

import com.uitem.UItemPlugin;
import com.uitem.util.SchedulerUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class ResourcePackServer {

    private final UItemPlugin plugin;
    private HttpServer server;
    private int port;
    private boolean running;

    public ResourcePackServer(UItemPlugin plugin) {
        this.plugin = plugin;
    }

    public void start(int port) throws IOException {
        if (running) {
            stop();
        }
        this.port = port;

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/" + plugin.getName().toLowerCase(), new PackHandler());
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        running = true;

        plugin.getLogger().info("[UItem] Resource pack server running at http://0.0.0.0:" + port + "/" + plugin.getName().toLowerCase());
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            // Bug fix: shutdown executor to prevent thread leak when server restarts
            if (server.getExecutor() instanceof ExecutorService es) {
                es.shutdownNow();
            }
            server = null;
        }
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    public String getPackUrl() {
        return "http://" + getLocalIp() + ":" + port + "/" + plugin.getName().toLowerCase();
    }

    private String getLocalIp() {
        try {
            var addr = java.net.InetAddress.getLocalHost();
            return addr.getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private class PackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            File packFile = plugin.getResourcePackManager().getLastZipFile();
            if (packFile == null || !packFile.exists()) {
                String response = "Resource pack has not been generated. Use /uitem rp generate";
                exchange.sendResponseHeaders(503, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            long fileLength = packFile.length();
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + packFile.getName() + "\"");

            byte[] hash = plugin.getResourcePackManager().getLastPackHash();
            if (hash != null) {
                exchange.getResponseHeaders().set("X-Pack-Hash-SHA1", bytesToHex(hash));
            }

            exchange.sendResponseHeaders(200, fileLength);
            try (FileInputStream fis = new FileInputStream(packFile);
                 OutputStream os = exchange.getResponseBody()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
            }
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
