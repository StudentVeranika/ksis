/**
 * Утилитный класс для работы с файлами.
 * Читает статические файлы из папки web/ (HTML, CSS, JS).
 */

package com.mindclash.utils;

import java.io.*;
import java.nio.file.*;
import java.net.URL;


public class FileUtils {


    public static String readWebFile(String fileName) {
        try {

            URL resource = FileUtils.class.getClassLoader().getResource("web/" + fileName);
            if (resource == null) {
                System.err.println(" Файл не найден: web/" + fileName);
                return null;
            }


            Path path = Paths.get(resource.toURI());
            return Files.readString(path);

        } catch (Exception e) {
            System.err.println(" Ошибка чтения файла " + fileName + ": " + e.getMessage());
            return null;
        }
    }


    public static String getMimeType(String fileName) {
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".css")) return "text/css";
        if (fileName.endsWith(".js")) return "application/javascript";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".ico")) return "image/x-icon";
        return "text/plain";
    }


    public static boolean fileExists(String fileName) {
        URL resource = FileUtils.class.getClassLoader().getResource("web/" + fileName);
        return resource != null;
    }
}