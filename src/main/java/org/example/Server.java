package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Server {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8081;
    private static final Set<ClientHandler> clientHandlers = new HashSet<>();
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) {
        System.out.println("Сервер запущен на " + HOST + ":" + PORT);
        logger.info("Сервер запущен на {}:{}", HOST, PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            logger.error("Ошибка при запуске сервера: ", e);
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket socket;
        private PrintWriter out;
        private String clientName;
        private String clientColor;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                synchronized (clientHandlers) {
                    clientHandlers.add(this);
                }

                String[] nameAndColor = in.readLine().split(":");
                clientName = nameAndColor[0];
                clientColor = nameAndColor[1];
                out.println("Добро пожаловать, " + clientName + "!");

                broadcastMessage(clientName + " присоединился к чату.");
                logger.info("Пользователь {} присоединился к чату.", clientName);

                String message;
                while ((message = in.readLine()) != null) {
                    handleMessage(message);
                }
            } catch (IOException e) {
                logger.error("Ошибка при обработке сообщения: ", e);
            } finally {
                cleanup();
            }
        }

        private void handleMessage(String message) {
            if (message.startsWith("private:")) {
                sendPrivateMessage(message);
            } else if (message.startsWith("getClients")) {
                sendClientList();
            } else {
                broadcastMessage(clientName + ": " + message.substring("broadcast:".length()));
            }
        }

        private void cleanup() {
            try {
                socket.close();
            } catch (IOException e) {
                logger.error("Ошибка при закрытии сокета: ", e);
            }
            synchronized (clientHandlers) {
                clientHandlers.remove(this);
            }
            broadcastMessage(clientName + " покинул чат.");
            logger.info("Пользователь {} покинул чат.", clientName);
        }

        private void broadcastMessage(String message) {
            synchronized (clientHandlers) {
                for (ClientHandler handler : clientHandlers) {
                    handler.out.println("\u001B[" + clientColor + "m" + message + "\u001B[0m");
                }
            }
        }

        private void sendPrivateMessage(String message) {
            String[] parts = message.split(":", 3);
            String recipientName = parts[1];
            String privateMessage = clientName + " (личное): " + parts[2];

            synchronized (clientHandlers) {
                for (ClientHandler handler : clientHandlers) {
                    if (handler.clientName.equals(recipientName)) {
                        handler.out.println("\u001B[" + clientColor + "m" + privateMessage + "\u001B[0m");
                        logger.info("Личное сообщение от {} к {}: {}", clientName, recipientName, parts[2]);
                        return;
                    }
                }
                out.println("Пользователь с никнеймом " + recipientName + " не найден.");
            }
        }

        private void sendClientList() {
            StringBuilder clientsList = new StringBuilder();
            synchronized (clientHandlers) {
                for (ClientHandler handler : clientHandlers) {
                    clientsList.append(handler.clientName).append("\n");
                }
            }
            out.println(clientsList.toString().trim());
        }
    }
}