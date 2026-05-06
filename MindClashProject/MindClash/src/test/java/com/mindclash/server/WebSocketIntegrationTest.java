package com.mindclash.server;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WebSocketIntegrationTest {

    private static WebSocketServer webSocketServer;
    private static Thread serverThread;
    private static final int WS_PORT = 8082;
    private static final String WS_URL = "ws://localhost:" + WS_PORT;

    @BeforeAll
    static void startServer() throws Exception {

        webSocketServer = new WebSocketServer(WS_PORT);
        serverThread = new Thread(() -> webSocketServer.start());
        serverThread.start();
        Thread.sleep(2000);
    }

    @AfterAll
    static void stopServer() {
        if (webSocketServer != null) {
            webSocketServer.stop();
        }
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }


    @Test
    @Order(1)
    void testWebSocketConnection() throws Exception {
        CountDownLatch connectionLatch = new CountDownLatch(1);
        CountDownLatch closeLatch = new CountDownLatch(1);

        WebSocketClient client = new WebSocketClient(new URI(WS_URL)) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                connectionLatch.countDown();
            }

            @Override
            public void onMessage(String message) {}

            @Override
            public void onClose(int code, String reason, boolean remote) {
                closeLatch.countDown();
            }

            @Override
            public void onError(Exception ex) {}
        };

        client.connect();
        boolean connected = connectionLatch.await(5, TimeUnit.SECONDS);

        assertTrue(connected, "Соединение должно установиться");
        assertTrue(client.isOpen(), "Клиент должен быть в открытом состоянии");

        client.close();
        boolean closed = closeLatch.await(5, TimeUnit.SECONDS);
        assertTrue(closed, "Соединение должно закрыться");
    }


    @Test
    @Order(2)
    void testLoginWithCorrectCredentials() throws Exception {
        CountDownLatch responseLatch = new CountDownLatch(1);

        WebSocketClient client = new WebSocketClient(new URI(WS_URL)) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                String loginMsg = "{\"type\":\"login\",\"username\":\"vera\",\"password\":\"1234\"}";
                send(loginMsg);
            }

            @Override
            public void onMessage(String message) {
                if (message.contains("login_success")) {
                    responseLatch.countDown();
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {}

            @Override
            public void onError(Exception ex) {}
        };

        client.connect();
        boolean responseReceived = responseLatch.await(5, TimeUnit.SECONDS);

        assertTrue(responseReceived, "Должен быть получен login_success");

        client.close();
        Thread.sleep(500);
    }


    @Test
    @Order(3)
    void testLoginWithWrongCredentials() throws Exception {
        CountDownLatch responseLatch = new CountDownLatch(1);

        WebSocketClient client = new WebSocketClient(new URI(WS_URL)) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                String loginMsg = "{\"type\":\"login\",\"username\":\"vera\",\"password\":\"wrong\"}";
                send(loginMsg);
            }

            @Override
            public void onMessage(String message) {
                if (message.contains("login_error")) {
                    responseLatch.countDown();
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {}

            @Override
            public void onError(Exception ex) {}
        };

        client.connect();
        boolean responseReceived = responseLatch.await(5, TimeUnit.SECONDS);

        assertTrue(responseReceived, "Должен быть получен login_error");

        client.close();
        Thread.sleep(500);
    }


    @Test
    @Order(4)
    void testSendMessageWithoutAuth() throws Exception {
        CountDownLatch responseLatch = new CountDownLatch(1);

        WebSocketClient client = new WebSocketClient(new URI(WS_URL)) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                String inviteMsg = "{\"type\":\"invite\",\"toUsername\":\"sofa\",\"difficulty\":1}";
                send(inviteMsg);
            }

            @Override
            public void onMessage(String message) {
                if (message.contains("error") && message.contains("Не авторизован")) {
                    responseLatch.countDown();
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {}

            @Override
            public void onError(Exception ex) {}
        };

        client.connect();
        boolean responseReceived = responseLatch.await(5, TimeUnit.SECONDS);

        assertTrue(responseReceived, "Должна быть получена ошибка о неавторизованном запросе");

        client.close();
        Thread.sleep(500);
    }

    @Test
    @Order(5)
    void testMultipleMessageExchange() throws Exception {
        CountDownLatch authLatch = new CountDownLatch(1);
        CountDownLatch pingLatch = new CountDownLatch(1);

        WebSocketClient client = new WebSocketClient(new URI(WS_URL)) {
            private boolean authenticated = false;

            @Override
            public void onOpen(ServerHandshake handshake) {
                String loginMsg = "{\"type\":\"login\",\"username\":\"vera\",\"password\":\"1234\"}";
                send(loginMsg);
            }

            @Override
            public void onMessage(String message) {
                if (!authenticated && message.contains("login_success")) {
                    authenticated = true;
                    authLatch.countDown();
                    send("{\"type\":\"ping\"}");
                } else if (message.contains("pong")) {
                    pingLatch.countDown();
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {}

            @Override
            public void onError(Exception ex) {}
        };

        client.connect();

        boolean authReceived = authLatch.await(5, TimeUnit.SECONDS);
        assertTrue(authReceived, "Должна быть получена авторизация");

        boolean pongReceived = pingLatch.await(5, TimeUnit.SECONDS);
        assertTrue(pongReceived, "Должен быть получен ответ на ping");

        client.close();
        Thread.sleep(500);
    }


    @Test
    @Order(6)
    void testConnectionClose() throws Exception {
        CountDownLatch openLatch = new CountDownLatch(1);
        CountDownLatch closeLatch = new CountDownLatch(1);

        WebSocketClient client = new WebSocketClient(new URI(WS_URL)) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                openLatch.countDown();
            }

            @Override
            public void onMessage(String message) {}

            @Override
            public void onClose(int code, String reason, boolean remote) {
                closeLatch.countDown();
            }

            @Override
            public void onError(Exception ex) {}
        };

        client.connect();
        boolean opened = openLatch.await(5, TimeUnit.SECONDS);
        assertTrue(opened, "Соединение должно открыться");


        Thread.sleep(500);


        client.close();


        boolean closed = closeLatch.await(5, TimeUnit.SECONDS);
        assertTrue(closed, "Соединение должно закрыться");


        assertFalse(client.isOpen(), "Клиент не должен быть в открытом состоянии");
    }
}