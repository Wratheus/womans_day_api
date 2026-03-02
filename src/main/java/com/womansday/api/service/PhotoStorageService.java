package com.womansday.api.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Service
public class PhotoStorageService {

    private static final Map<String, String> MIME_TO_EXT = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif");

    private Path storageRoot;

    @Value("${app.photos.storage-dir}")
    private String storageDir;

    @PostConstruct
    public void init() throws IOException {
        storageRoot = Paths.get(storageDir).toAbsolutePath().normalize();
        Files.createDirectories(storageRoot);
    }

    // Возвращаем НЕ абсолютный путь, а ключ
    public String storeSubmissionPhoto(Long submissionId, String contentType, byte[] data) throws IOException {
        String ext = requireAllowedExt(contentType);
        String filename = UUID.randomUUID() + "." + ext;
        String key = Paths.get("submissions", String.valueOf(submissionId), filename).toString();
        writeByKey(key, data);
        return key;
    }

    public String storeAvatar(Long userId, String contentType, byte[] data) throws IOException {
        String ext = requireAllowedExt(contentType);
        String filename = UUID.randomUUID() + "." + ext;
        String key = Paths.get("avatars", String.valueOf(userId), filename).toString();
        writeByKey(key, data);
        return key;
    }

    public byte[] loadByKey(String key) throws IOException {
        Path path = resolveAndValidateKey(key);
        return Files.readAllBytes(path);
    }

    public void deleteByKey(String key) throws IOException {
        Path path = resolveAndValidateKey(key);
        Files.deleteIfExists(path);
    }

    private void writeByKey(String key, byte[] data) throws IOException {
        Path path = resolveAndValidateKey(key);
        Files.createDirectories(path.getParent());
        Files.write(path, data,
                java.nio.file.StandardOpenOption.CREATE_NEW,
                java.nio.file.StandardOpenOption.WRITE);
    }

    private Path resolveAndValidateKey(String key) {
        // запрещаем абсолютные пути сразу
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

    private String requireAllowedExt(String contentType) {
        String ext = MIME_TO_EXT.get(contentType);
        if (ext == null) {
            throw new IllegalArgumentException("Unsupported content type: " + contentType);
        }
        return ext;
    }
}