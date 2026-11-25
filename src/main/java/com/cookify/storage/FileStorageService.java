package com.cookify.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
        "image/png", ".png",
        "image/jpeg", ".jpg",
        "image/webp", ".webp",
        "video/mp4", ".mp4",
        "video/webm", ".webm"
    );

    private final Path root;

    public FileStorageService(@Value("${cookify.storage.upload-dir}") String uploadDir) {
        this.root = Path.of(uploadDir);
    }

    public String store(MultipartFile file, Set<String> allowedContentTypes) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("File is empty.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedContentTypes.contains(contentType)) {
            throw new StorageException("Unsupported file type: " + contentType);
        }
        try {
            if (contentType.startsWith("image/") && ImageIO.read(new ByteArrayInputStream(file.getBytes())) == null) {
                throw new StorageException("File content does not match declared image type: " + contentType);
            }
        } catch (IOException e) {
            throw new StorageException("Failed to read file: " + e.getMessage());
        }
        String ext = EXTENSIONS_BY_CONTENT_TYPE.getOrDefault(contentType, "");
        String name = UUID.randomUUID() + ext;
        try {
            Files.createDirectories(root);
            Path target = root.resolve(name).normalize();
            if (!target.startsWith(root.normalize())) {
                throw new StorageException("Invalid path.");
            }
            file.transferTo(target);
            return name;
        } catch (IOException e) {
            throw new StorageException("Failed to store file: " + e.getMessage());
        }
    }
}
