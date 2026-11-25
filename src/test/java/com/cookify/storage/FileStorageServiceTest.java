package com.cookify.storage;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class FileStorageServiceTest {

    private final FileStorageService storage = new FileStorageService("./build/test-uploads");
    private static final Set<String> IMAGES = Set.of("image/png", "image/jpeg");

    // minimal 1x1 PNG
    static final byte[] PNG_1x1 = java.util.Base64.getDecoder().decode(
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

    @Test
    void storesValidImageAndReturnsPath() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "avatar", "pic.png", "image/png", PNG_1x1);
        String path = storage.store(file, IMAGES);
        assertThat(path).isNotBlank();
        assertThat(path).endsWith(".png");
        assertThat(Files.exists(Path.of("./build/test-uploads").resolve(path))).isTrue();
    }

    @Test
    void rejectsDisallowedType() {
        MockMultipartFile file = new MockMultipartFile(
            "avatar", "evil.exe", "application/octet-stream", new byte[]{1});
        assertThatThrownBy(() -> storage.store(file, IMAGES))
            .isInstanceOf(StorageException.class);
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
            "avatar", "empty.png", "image/png", new byte[]{});
        assertThatThrownBy(() -> storage.store(file, IMAGES))
            .isInstanceOf(StorageException.class);
    }

    @Test
    void rejectsSpoofedImage() {
        MockMultipartFile file = new MockMultipartFile(
            "avatar", "pic.png", "image/png", new byte[]{1, 2, 3, 4});
        assertThatThrownBy(() -> storage.store(file, IMAGES))
            .isInstanceOf(StorageException.class);
    }

    @Test
    void storesVideoByTypeOnly() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "v", "clip.mp4", "video/mp4", new byte[]{0, 0, 0, 24});
        String path = storage.store(file, Set.of("video/mp4"));
        assertThat(path).endsWith(".mp4");
        assertThat(Files.exists(Path.of("./build/test-uploads").resolve(path))).isTrue();
    }
}
