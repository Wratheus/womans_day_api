package com.womansday.api.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;

@Service
public class PhotoStorageService {

    private static final Map<String, String> IMAGE_MIME_TO_EXT = Map.ofEntries(
            Map.entry("image/jpeg", "jpg"),
            Map.entry("image/jpg", "jpg"),
            Map.entry("image/pjpeg", "jpg"),
            Map.entry("image/png", "png"),
            Map.entry("image/webp", "webp"),
            Map.entry("image/gif", "gif"),
            Map.entry("image/heic", "heic"),
            Map.entry("image/heif", "heif"));

    private static final Map<String, String> MIME_TO_EXT = Map.ofEntries(
            Map.entry("image/jpeg", "jpg"),
            Map.entry("image/jpg", "jpg"),
            Map.entry("image/pjpeg", "jpg"),
            Map.entry("image/png", "png"),
            Map.entry("image/webp", "webp"),
            Map.entry("image/gif", "gif"),
            Map.entry("image/heic", "heic"),
            Map.entry("image/heif", "heif"),
            Map.entry("video/mp4", "mp4"),
            Map.entry("video/quicktime", "mov"),
            Map.entry("video/x-msvideo", "avi"),
            Map.entry("video/webm", "webm"),
            Map.entry("audio/mpeg", "mp3"),
            Map.entry("audio/ogg", "ogg"),
            Map.entry("audio/wav", "wav"),
            Map.entry("application/pdf", "pdf"));

    private Path storageRoot;

    @Value("${app.media.storage-dir}")
    private String storageDir;

    @PostConstruct
    public void init() throws IOException {
        storageRoot = Paths.get(storageDir).toAbsolutePath().normalize();
        Files.createDirectories(storageRoot);
    }

    public String storeSubmissionFile(Long submissionId, String contentType, InputStream data) throws IOException {
        String ext = resolveExt(contentType);
        String filename = UUID.randomUUID() + "." + ext;
        String key = Paths.get("submissions", String.valueOf(submissionId), filename).toString();
        writeByKey(key, data);
        return key;
    }

    public String storeAvatar(Long userId, String contentType, InputStream data) throws IOException {
        String ext = requireImageExt(contentType);
        String filename = UUID.randomUUID() + "." + ext;
        String key = Paths.get("avatars", String.valueOf(userId), filename).toString();
        writeByKey(key, data);
        return key;
    }

    public byte[] loadByKey(String key) throws IOException {
        Path path = resolveAndValidateKey(key);
        return Files.readAllBytes(path);
    }

    public Path resolvePathByKey(String key) {
        return resolveAndValidateKey(key);
    }

    public void deleteByKey(String key) throws IOException {
        Path path = resolveAndValidateKey(key);
        Files.deleteIfExists(path);
    }

    private void writeByKey(String key, InputStream data) throws IOException {
        Path path = resolveAndValidateKey(key);
        Files.createDirectories(path.getParent());

        try (OutputStream out = Files.newOutputStream(path, CREATE_NEW, WRITE)) {
            data.transferTo(out);
        }
    }

    private Path resolveAndValidateKey(String key) {
        Path relative = Paths.get(key);
        if (relative.isAbsolute()) {
            throw new SecurityException("Absolute paths are not allowed");
        }

        Path resolved = storageRoot.resolve(relative).normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw new SecurityException("Access denied: path outside storage directory");
        }
        return resolved;
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null)
            return null;
        return contentType.split(";")[0].trim().toLowerCase();
    }

    private String resolveExt(String contentType) {
        contentType = normalizeContentType(contentType);
        if (contentType == null)
            return "bin";

        String known = MIME_TO_EXT.get(contentType);
        if (known != null)
            return known;

        if (contentType.contains("/")) {
            String sub = contentType.split("/")[1].trim();
            if (!sub.isBlank())
                return sub;
        }
        return "bin";
    }

    private String requireImageExt(String contentType) {
        contentType = normalizeContentType(contentType);
        if (contentType == null) {
            throw new IllegalArgumentException("Content type is null");
        }

        String ext = IMAGE_MIME_TO_EXT.get(contentType);
        if (ext == null) {
            throw new IllegalArgumentException("Unsupported content type: " + contentType);
        }
        return ext;
    }
}