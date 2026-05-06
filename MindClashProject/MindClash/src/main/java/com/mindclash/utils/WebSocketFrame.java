/**
 * Утилитный класс для работы с WebSocket-фреймами по RFC 6455.
 * Реализует:
 * - кодирование текстовых сообщений в WebSocket-фреймы
 * - декодирование полученных фреймов
 * - маскирование/демаскирование данных
 * - генерацию Sec-WebSocket-Accept для handshake
 */

package com.mindclash.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class WebSocketFrame {

    public static byte[] encodeTextFrame(String message) {
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        int payloadLen = payload.length;

        //  длина до 125 байт
        if (payloadLen <= 125) {
            byte[] frame = new byte[2 + payloadLen];
            frame[0] = (byte) 0x81; // FIN=1, opcode=1 (текст)
            frame[1] = (byte) payloadLen;
            System.arraycopy(payload, 0, frame, 2, payloadLen);
            return frame;
        }

        //  длина до 65535 байт
        if (payloadLen <= 65535) {
            byte[] frame = new byte[4 + payloadLen];
            frame[0] = (byte) 0x81;
            frame[1] = 126;
            frame[2] = (byte) ((payloadLen >> 8) & 0xFF);
            frame[3] = (byte) (payloadLen & 0xFF);
            System.arraycopy(payload, 0, frame, 4, payloadLen);
            return frame;
        }

        return new byte[0];
    }

    public static String decodeTextFrame(InputStream in) throws IOException {
        int firstByte = in.read();
        if (firstByte == -1) return null;

        int opcode = firstByte & 0x0F;

        int secondByte = in.read();
        boolean masked = (secondByte & 0x80) != 0;
        int payloadLen = secondByte & 0x7F;

        if (payloadLen == 126) {
            byte[] lenBytes = new byte[2];
            in.read(lenBytes);
            payloadLen = ((lenBytes[0] & 0xFF) << 8) | (lenBytes[1] & 0xFF);
        } else if (payloadLen == 127) {
            byte[] lenBytes = new byte[8];
            in.read(lenBytes);
            payloadLen = 0;
            for (int i = 0; i < 8; i++) {
                payloadLen = (payloadLen << 8) | (lenBytes[i] & 0xFF);
            }
        }

        byte[] maskKey = null;
        if (masked) {
            maskKey = new byte[4];
            in.read(maskKey);
        }

        byte[] payload = new byte[payloadLen];
        in.read(payload);

        if (masked) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] = (byte) (payload[i] ^ maskKey[i % 4]);
            }
        }

        // Обработка ping
        if (opcode == 0x9) {
            return "{\"type\":\"ping\"}";
        }

        return new String(payload, StandardCharsets.UTF_8);
    }

    public static String generateAcceptKey(String websocketKey) {
        try {
            String magic = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            String concat = websocketKey + magic;
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(concat.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}