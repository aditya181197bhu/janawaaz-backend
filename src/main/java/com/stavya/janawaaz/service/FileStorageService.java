package com.stavya.janawaaz.service;

import com.stavya.janawaaz.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Stores uploads on local disk (S3 later). Adapted from the Stavya backend. */
@Service
public class FileStorageService {

    public enum Kind { PHOTO, VOICE }

    private static final long MAX_PHOTO_BYTES = 20L * 1024 * 1024;
    private static final long MAX_VOICE_BYTES = 5L * 1024 * 1024;

    private static final Set<String> PHOTO_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".heic", ".heif");
    private static final Set<String> VOICE_EXTENSIONS = Set.of(".m4a", ".aac", ".mp4", ".wav", ".ogg", ".mp3");

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public String store(MultipartFile file, Kind kind) {
        if (file.isEmpty()) {
            throw new ApiException("File is empty", HttpStatus.BAD_REQUEST);
        }
        long maxBytes = kind == Kind.PHOTO ? MAX_PHOTO_BYTES : MAX_VOICE_BYTES;
        if (file.getSize() > maxBytes) {
            throw new ApiException("File too large (max " + maxBytes / (1024 * 1024) + "MB)", HttpStatus.BAD_REQUEST);
        }

        String extension = extensionOf(file.getOriginalFilename(), kind);
        Set<String> allowed = kind == Kind.PHOTO ? PHOTO_EXTENSIONS : VOICE_EXTENSIONS;
        if (!allowed.contains(extension)) {
            throw new ApiException("File type " + extension + " is not allowed for " + kind, HttpStatus.BAD_REQUEST);
        }
        verifyMagicBytes(file, kind);

        String subdirectory = kind == Kind.PHOTO ? "photos" : "voice";
        try {
            Path dir = Paths.get(uploadDir, subdirectory);
            Files.createDirectories(dir);
            String storedName = UUID.randomUUID() + extension;
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(storedName));
            }
            return "/uploads/" + subdirectory + "/" + storedName;
        } catch (IOException e) {
            throw new ApiException("Failed to store file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void verifyMagicBytes(MultipartFile file, Kind kind) {
        byte[] h = new byte[12];
        int n;
        try (InputStream in = file.getInputStream()) {
            n = in.readNBytes(h, 0, h.length);
        } catch (IOException e) {
            throw new ApiException("Could not read file", HttpStatus.BAD_REQUEST);
        }
        boolean ok = kind == Kind.PHOTO ? looksLikeImage(h, n) : looksLikeAudio(h, n);
        if (!ok) {
            throw new ApiException("File content does not match a " + kind.name().toLowerCase(Locale.ROOT),
                    HttpStatus.BAD_REQUEST);
        }
    }

    private static boolean looksLikeImage(byte[] h, int n) {
        if (n < 4) return false;
        boolean jpeg = (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
        boolean png = (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
        boolean webp = n >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
        return jpeg || png || webp || hasFtyp(h, n);
    }

    private static boolean looksLikeAudio(byte[] h, int n) {
        if (n < 4) return false;
        boolean adtsAac = (h[0] & 0xFF) == 0xFF && ((h[1] & 0xF6) == 0xF0);
        boolean wav = n >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'A' && h[10] == 'V' && h[11] == 'E';
        boolean ogg = h[0] == 'O' && h[1] == 'g' && h[2] == 'g' && h[3] == 'S';
        boolean mp3 = (h[0] == 'I' && h[1] == 'D' && h[2] == '3') || ((h[0] & 0xFF) == 0xFF && (h[1] & 0xE0) == 0xE0);
        return hasFtyp(h, n) || adtsAac || wav || ogg || mp3;
    }

    /** MP4-family containers (HEIC photos, M4A audio) carry "ftyp" at offset 4. */
    private static boolean hasFtyp(byte[] h, int n) {
        return n >= 8 && h[4] == 'f' && h[5] == 't' && h[6] == 'y' && h[7] == 'p';
    }

    private static String extensionOf(String filename, Kind kind) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        }
        return kind == Kind.PHOTO ? ".jpg" : ".m4a";
    }
}
