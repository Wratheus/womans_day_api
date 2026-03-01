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
            "image/gif", "gif"
    );

    @Value("${app.photos.storage-dir}")
    private String storageDir;

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(storageDir));
    }

    public String store(Long submissionId, String contentType, byte[] data) throws IOException {
        String ext = MIME_TO_EXT.getOrDefault(contentType, "jpg");
        String filename = UUID.randomUUID() + "." + ext;
        Path dir = Paths.get(storageDir, String.valueOf(submissionId));
        Files.createDirectories(dir);
        Path filePath = dir.resolve(filename);
        Files.write(filePath, data);
        return filePath.toString();
    }

    public String storeAvatar(Long userId, String contentType, byte[] data) throws IOException {
        String ext = MIME_TO_EXT.getOrDefault(contentType, "jpg");
        String filename = UUID.randomUUID() + "." + ext;
        Path dir = Paths.get(storageDir, "avatars", String.valueOf(userId));
        Files.createDirectories(dir);
        Path filePath = dir.resolve(filename);
        Files.write(filePath, data);
        return filePath.toString();
    }

    public byte[] load(String filePath) throws IOException {
        validatePath(filePath);
        return Files.readAllBytes(Paths.get(filePath));
    }

    public void delete(String filePath) throws IOException {
        validatePath(filePath);
        Files.deleteIfExists(Paths.get(filePath));
    }

    private void validatePath(String filePath) {
        Path normalized = Paths.get(filePath).normalize();
        if (!normalized.startsWith(Paths.get(storageDir).normalize())) {
            throw new SecurityException("Access denied: path outside storage directory");
        }
    }
}
