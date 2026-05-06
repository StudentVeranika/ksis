/**
 * Построитель HTTP-ответов.
 * Формирует полный HTTP-ответ из статуса, заголовков и тела.
 */

package com.mindclash.utils;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;


 // для построения HTTP ответов

public class HttpResponseBuilder {

    private int statusCode;
    private String statusText;
    private Map<String, String> headers;
    private String body;

    public HttpResponseBuilder() {
        this.headers = new HashMap<>();
        this.statusCode = 200;
        this.statusText = "OK";

        addHeader("Server", "MindClash/1.0");
        addHeader("Date", getCurrentDate());
        addHeader("Connection", "close");
    }

    private String getCurrentDate() {

        return DateTimeFormatter.RFC_1123_DATE_TIME
                .format(ZonedDateTime.now(ZoneId.of("GMT")));
    }


    public HttpResponseBuilder setStatus(int code, String text) {
        this.statusCode = code;
        this.statusText = text;
        return this;
    }


    public HttpResponseBuilder addHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }


    public HttpResponseBuilder setBody(String body, String contentType) {
        this.body = body;
        addHeader("Content-Type", contentType);
        addHeader("Content-Length", String.valueOf(body.getBytes().length));
        return this;
    }


    public HttpResponseBuilder setHtmlBody(String html) {
        return setBody(html, "text/html; charset=utf-8");
    }


    public HttpResponseBuilder setJsonBody(String json) {
        return setBody(json, "application/json; charset=utf-8");
    }


    public String build() {
        StringBuilder response = new StringBuilder();

        // Статусная строка
        response.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusText).append("\r\n");

        // Заголовки
        for (Map.Entry<String, String> header : headers.entrySet()) {
            response.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }

        // Пустая строка между заголовками и телом
        response.append("\r\n");

        // Тело
        if (body != null) {
            response.append(body);
        }

        return response.toString();
    }


    public static String ok(String html) {
        return new HttpResponseBuilder()
                .setHtmlBody(html)
                .build();
    }


    public static String notFound() {
        String html = "<html><body><h1>404 Not Found</h1><p>Страница не найдена</p></body></html>";
        return new HttpResponseBuilder()
                .setStatus(404, "Not Found")
                .setHtmlBody(html)
                .build();
    }


    public static String serverError(String message) {
        String html = "<html><body><h1>500 Internal Server Error</h1><p>" + message + "</p></body></html>";
        return new HttpResponseBuilder()
                .setStatus(500, "Internal Server Error")
                .setHtmlBody(html)
                .build();
    }


    public static String badRequest(String message) {
        String html = "<html><body><h1>400 Bad Request</h1><p>" + message + "</p></body></html>";
        return new HttpResponseBuilder()
                .setStatus(400, "Bad Request")
                .setHtmlBody(html)
                .build();
    }


    public static String redirect(String location) {
        return new HttpResponseBuilder()
                .setStatus(302, "Found")
                .addHeader("Location", location)
                .build();
    }
}