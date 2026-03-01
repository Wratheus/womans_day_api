package com.womansday.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PhotoStorageServiceTest {

    private PhotoStorageService photoStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        photoStorageService = new PhotoStorageService();
        ReflectionTestUtils.setField(photoStorageService, "storageDir", tempDir.toString());
        photoStorageService.init();
    }

    @Test
    void store_shouldSaveFileAndReturnPath() throws IOException {
        byte[] data = "fake image data".getBytes();

        String filePath = photoStorageService.store(1L, "image/jpeg", data);

        assertNotNull(filePath);
        assertTrue(Files.exists(Path.of(filePath)));
        assertTrue(filePath.endsWith(".jpg"));
        assertArrayEquals(data, Files.readAllBytes(Path.of(filePath)));
    }

    @Test
    void store_shouldUsePngExtensionForPng() throws IOException {
        String filePath = photoStorageService.store(1L, "image/png", "data".getBytes());
        assertTrue(filePath.endsWith(".png"));
    }

    @Test
    void storeAvatar_shouldSaveInAvatarsDirectory() throws IOException {
        String filePath = photoStorageService.storeAvatar(1L, "image/jpeg", "data".getBytes());

        assertTrue(filePath.contains("avatars"));
        assertTrue(Files.exists(Path.of(filePath)));
    }

    @Test
    void load_shouldReturnFileContents() throws IOException {
        byte[] data = "test content".getBytes();
        String filePath = photoStorageService.store(1L, "image/jpeg", data);

        byte[] loaded = photoStorageService.load(filePath);
        assertArrayEquals(data, loaded);
    }

    @Test
    void load_shouldRejectPathOutsideStorageDir() {
        assertThrows(SecurityException.class,
                () -> photoStorageService.load("/etc/passwd"));
    }

    @Test
    void load_shouldRejectPathTraversal() {
        String maliciousPath = tempDir.toString() + "/../../etc/passwd";
        assertThrows(SecurityException.class,
                () -> photoStorageService.load(maliciousPath));
    }

    @Test
    void delete_shouldRemoveFile() throws IOException {
        String filePath = photoStorageService.store(1L, "image/jpeg", "data".getBytes());
        assertTrue(Files.exists(Path.of(filePath)));

        photoStorageService.delete(filePath);
        assertFalse(Files.exists(Path.of(filePath)));
    }

    @Test
    void delete_shouldRejectPathOutsideStorageDir() {
        assertThrows(SecurityException.class,
                () -> photoStorageService.delete("/etc/passwd"));
    }
}
