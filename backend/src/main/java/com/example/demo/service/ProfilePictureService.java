package com.example.demo.service;

import com.example.demo.api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Stores validated profile pictures outside the application classpath. */
@Service
public class ProfilePictureService {
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private final Path storageDirectory;
    private final UserService users;

    public ProfilePictureService(UserService users, @Value("${app.upload-dir:uploads/profile-pictures}") String uploadDirectory) {
        this.users = users;
        this.storageDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create profile picture storage", exception);
        }
    }

    public String save(long userId, MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE_BYTES) throw invalidPicture();
        ImageType imageType = imageType(file);
        String fileName = UUID.randomUUID() + "." + imageType.extension();
        Path destination = storageDirectory.resolve(fileName).normalize();
        if (!destination.startsWith(storageDirectory)) throw new IllegalStateException("Invalid profile picture path");
        try (InputStream input = file.getInputStream()) {
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "picture_upload_failed", "Profile picture could not be saved");
        }
        String pictureUrl = "/uploads/profile-pictures/" + fileName;
        users.updatePictureUrl(userId, pictureUrl);
        return pictureUrl;
    }

    private ImageType imageType(MultipartFile file) {
        byte[] header = new byte[12];
        try (InputStream input = file.getInputStream()) {
            if (input.readNBytes(header, 0, header.length) != header.length) throw invalidPicture();
        } catch (IOException exception) {
            throw invalidPicture();
        }
        if ((header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff) return ImageType.JPEG;
        if ((header[0] & 0xff) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
            && header[4] == 0x0d && header[5] == 0x0a && header[6] == 0x1a && header[7] == 0x0a) return ImageType.PNG;
        if (header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
            && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') return ImageType.WEBP;
        throw invalidPicture();
    }

    private ApiException invalidPicture() {
        return new ApiException(HttpStatus.BAD_REQUEST, "invalid_picture", "Profile picture must be a JPEG, PNG, or WebP file up to 5 MB");
    }

    private enum ImageType {
        JPEG("jpg"), PNG("png"), WEBP("webp");
        private final String extension;
        ImageType(String extension) { this.extension = extension; }
        String extension() { return extension; }
    }
}
