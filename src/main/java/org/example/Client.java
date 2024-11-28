package org.example;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Client {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 8081;
    private static String clientColor;
    private static String nickname;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {

            setNickname(out, userInput, in);
            new Thread(new IncomingMessageHandler(in)).start();

            String userMessage;
            while (true) {
                userMessage = userInput.readLine();
                if (userMessage.equals("/chatMem")) {
                    chooseRecipient(out, in);
                } else {
                    out.println("broadcast:" + userMessage);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка в клиенте: " + e.getMessage());
        }
    }

    private static void setNickname(PrintWriter out, BufferedReader userInput, BufferedReader in) throws IOException {
        System.out.println("Введите ваш никнейм (пробелы будут заменены на '_'):");
        nickname = userInput.readLine().replace(" ", "_");
        clientColor = getRandomColor();
        if (!nickname.isEmpty()) {
            out.println(nickname + ":" + clientColor);
            System.out.println(in.readLine());
        } else {
            System.out.println("Неправильный никнейм. Попробуйте снова.");
            setNickname(out, userInput, in);
        }
    }

    private static String getRandomColor() {
        Random random = new Random();
        return String.format("38;2;%d;%d;%d", random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }

    private static void chooseRecipient(PrintWriter out, BufferedReader in) throws IOException {
        out.println("getClients");
        String clientsList = in.readLine();
        System.out.println("Введите никнейм собеседника:\n" + clientsList);

        String recipientName = new BufferedReader(new InputStreamReader(System.in)).readLine();

        if (recipientName.equalsIgnoreCase(nickname)) {
            System.out.println("Вы не можете отправить личное сообщение самому себе.");
            return;
        }

        System.out.println("Введите сообщение:");
        String message = new BufferedReader(new InputStreamReader(System.in)).readLine();
        out.println("private:" + recipientName + ":" + message);
    }

    private record IncomingMessageHandler(BufferedReader in) implements Runnable {

        public void run() {
                String message;
                try {
                    while ((message = in.readLine()) != null) {
                        String[] parts = message.split(": ", 2);
                        if (parts.length > 1) {
                            System.out.println("\u001B[" + clientColor + "m" + parts[0] + "\u001B[0m: " + parts[1]);
                        } else {
                            System.out.println(message);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Ошибка при получении сообщения: " + e.getMessage());
                }
            }
        }
}