package hr.tvz.experimate.experimate.shared;

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
 * and writes files to the caller-supplied upload directory.
 *
 * <p>The upload directory is passed per-call so this service can be reused for
 * multiple distinct upload locations (profile photos, partner logos, ad images, etc.)
 * without needing a separate bean for each.</p>
 */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png", "image/jpeg", "image/webp"
    );

    /**
     * Validates and stores a multipart file in the given directory.
     * The original filename is never used — a UUID-based name is generated to
     * prevent collisions and path traversal attacks.
     *
     * @param file      the uploaded file from the HTTP request
     * @param uploadDir the target directory path (e.g. {@code "./uploads/partner-logos"})
     * @return the generated filename (e.g., {@code "a3f2e8b1-7c9d.png"}) stored on disk
     * @throws IllegalArgumentException if the file is empty or has a disallowed content type
     * @throws RuntimeException         if writing to disk fails
     */
    public String store(MultipartFile file, String uploadDir) {
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
     * @param filename  the filename returned by {@link #store(MultipartFile, String)}
     * @param uploadDir the directory from which the file should be loaded
     * @return a {@link FileSystemResource} pointing to the file on disk
     * @throws IllegalArgumentException if the filename contains path traversal characters
     */
    public Resource load(String filename, String uploadDir) {
        if (filename.contains("/") || filename.contains("\\") || filename.contains(".."))
            throw new IllegalArgumentException("Invalid filename: " + filename);
        return new FileSystemResource(Path.of(uploadDir, filename));
    }

    /**
     * Deletes a stored file from the given directory. Does nothing if the file does not exist.
     *
     * @param filename  the filename to delete
     * @param uploadDir the directory from which the file should be deleted
     * @throws RuntimeException if the filesystem operation fails
     */
    public void delete(String filename, String uploadDir) {
        try {
            Files.deleteIfExists(Path.of(uploadDir, filename));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + filename, e);
        }
    }
}
