package com.womansday.api.service;

import com.womansday.api.entity.SubmissionMedia;
import com.womansday.api.entity.Task;
import com.womansday.api.entity.TaskSubmission;
import com.womansday.api.entity.User;
import com.womansday.api.repository.SubmissionMediaRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;

@Slf4j
@Service
public class MediaStorageService {

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

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter
            .ofPattern("dd.MM_HH-mm")
            .withZone(ZoneId.of("Europe/Moscow"));

    private final SubmissionMediaRepository mediaRepository;

    private Path storageRoot;

    @Value("${app.media.storage-dir}")
    private String storageDir;

    public MediaStorageService(SubmissionMediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

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

    public void streamAllAsZip(OutputStream out) throws IOException {
        List<SubmissionMedia> mediaList = mediaRepository.findAllApprovedWithDetails();

        Set<String> knownPaths = new HashSet<>();
        Set<String> usedNames = new HashSet<>();

        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            zos.setLevel(1);

            // 1) Одобренные файлы — папка по заданию, имя по автору
            for (SubmissionMedia media : mediaList) {
                knownPaths.add(media.getFilePath());

                Path filePath = resolveAndValidateKey(media.getFilePath());
                if (!Files.exists(filePath)) {
                    log.warn("Media file missing on disk: id={}, path={}", media.getId(), media.getFilePath());
                    continue;
                }

                TaskSubmission submission = media.getSubmission();
                Task task = submission.getTask();
                User submitter = submission.getSubmitter();

                String date = DATE_FMT.format(Instant.ofEpochMilli(submission.getCreatedAtEpoch()));
                String ext = getExtension(media.getFilePath());

                String folder = sanitize(task.getTitle());
                String fileName = sanitize(submitter.getLastName() + "_" + submitter.getFirstName())
                        + "_" + date;

                String entryName = folder + "/" + fileName + "." + ext;
                int counter = 1;
                while (usedNames.contains(entryName)) {
                    entryName = folder + "/" + fileName + "_" + counter + "." + ext;
                    counter++;
                }
                usedNames.add(entryName);

                zos.putNextEntry(new ZipEntry(entryName));
                Files.copy(filePath, zos);
                zos.closeEntry();
                zos.flush();
            }

            // 2) Орфаны — файлы на диске без записи в БД
            Path submissionsDir = storageRoot.resolve("submissions");
            if (Files.exists(submissionsDir)) {
                Files.walkFileTree(submissionsDir, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        String relativePath = storageRoot.relativize(file).toString();
                        if (!knownPaths.contains(relativePath)) {
                            String entryName = "_orphans/" + file.getFileName().toString();
                            int c = 1;
                            while (usedNames.contains(entryName)) {
                                String name = file.getFileName().toString();
                                String ext = getExtension(name);
                                String base = name.substring(0, name.length() - ext.length() - 1);
                                entryName = "_orphans/" + base + "_" + c + "." + ext;
                                c++;
                            }
                            usedNames.add(entryName);

                            zos.putNextEntry(new ZipEntry(entryName));
                            Files.copy(file, zos);
                            zos.closeEntry();
                            zos.flush();
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
    }

    private static String sanitize(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|\\s]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private static String getExtension(String path) {
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot + 1) : "bin";
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
