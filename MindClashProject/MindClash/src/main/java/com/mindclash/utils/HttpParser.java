package com.mindclash.utils;

import java.util.*;

//парсер http-запросов
public class HttpParser {

    private String method;
    private String path;
    private String version;
    private Map<String, String> headers;
    private String body;

    public HttpParser(String requestText) {
        this.headers = new HashMap<>();
        parse(requestText);
    }

    private void parse(String requestText) {
        if (requestText == null || requestText.isEmpty()) return;

        String[] lines = requestText.split("\r\n");
        if (lines.length == 0) return;

        //  GET /index.html HTTP/1.1
        String[] firstLine = lines[0].split(" ");
        if (firstLine.length >= 2) {
            method = firstLine[0];
            path = firstLine[1];
            if (firstLine.length >= 3) {
                version = firstLine[2];
            }
        }


        int i = 1;
        while (i < lines.length && !lines[i].isEmpty()) {
            String line = lines[i];
            int colonPos = line.indexOf(':');
            if (colonPos > 0) {
                String key = line.substring(0, colonPos).trim();
                String value = line.substring(colonPos + 1).trim();
                headers.put(key.toLowerCase(), value);
            }
            i++;
        }

        // тело
        i++;
        if (i < lines.length) {
            StringBuilder bodyBuilder = new StringBuilder();
            while (i < lines.length) {
                bodyBuilder.append(lines[i]);
                if (i < lines.length - 1) bodyBuilder.append("\r\n");
                i++;
            }
            body = bodyBuilder.toString();
        }
    }

    // Геттеры
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public String getVersion() { return version; }
    public Map<String, String> getHeaders() { return headers; }
    public String getBody() { return body; }


    public String getHeader(String name) {
        return headers.get(name.toLowerCase());
    }

    @Override
    public String toString() {
        return "HttpParser{" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", version='" + version + '\'' +
                ", headers=" + headers +
                ", body='" + body + '\'' +
                '}';
    }
}