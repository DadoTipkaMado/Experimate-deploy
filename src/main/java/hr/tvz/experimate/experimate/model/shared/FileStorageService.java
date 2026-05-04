package hr.tvz.experimate.experimate.model.shared;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

/**
 * Handles persistence of user-uploaded image files on the local filesystem.
 * Validates content type and emptiness, generates collision-safe filenames via UUID,
 * and writes files to the configured upload directory.
 *
 * <p>The upload directory is configured via {@code app.upload.profile-photos-dir}
 * in application.properties and created automatically on first use.</p>
 */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png", "image/jpeg", "image/webp"
    );

    @Value("${app.upload.profile-photos-dir}")
    private String uploadDir;

    /**
     * Validates and stores a multipart file on the filesystem.
     * The original filename is never used — a UUID-based name is generated to
     * prevent collisions and path traversal attacks.
     *
     * @param file the uploaded file from the HTTP request
     * @return the generated filename (e.g., "a3f2e8b1-7c9d.png") stored on disk
     * @throws IllegalArgumentException if the file is empty or has a disallowed content type
     * @throws RuntimeException         if writing to disk fails
     */
    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException(
                    "File type not allowed. Accepted types: PNG, JPEG, WebP");
        }

        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + (extension != null ? "." + extension : "");

        Path directory = Path.of(uploadDir);
        try {
            Files.createDirectories(directory);
            file.transferTo(directory.resolve(filename));
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + filename, e);
        }

        return filename;
    }

    /**
     * Loads a stored file as a {@link Resource} suitable for an HTTP response body.
     *
     * @param filename the filename returned by {@link #store(MultipartFile)}
     * @return a {@link FileSystemResource} pointing to the file on disk
     */
    public Resource load(String filename) {
        if (filename.contains("/") || filename.contains("\\") || filename.contains(".."))
            throw new IllegalArgumentException("Invalid filename: " + filename);
        return new FileSystemResource(Path.of(uploadDir, filename));
    }

    /**
     * Deletes a stored file from the filesystem. Does nothing if the file does not exist.
     *
     * @param filename the filename to delete
     */
    public void delete(String filename) {
        try {
            Files.deleteIfExists(Path.of(uploadDir, filename));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + filename, e);
        }
    }
}
