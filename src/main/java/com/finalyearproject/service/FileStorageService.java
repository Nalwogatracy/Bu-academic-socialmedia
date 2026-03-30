package com.finalyearproject.service;

import com.finalyearproject.model.Attachment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    // Set upload dir in application.properties:
    // app.upload.dir=uploads
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Saves a MultipartFile to disk and returns a populated Attachment entity.
     */
    public Attachment storeFile(MultipartFile file) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        // Get original filename and extension
        String originalFileName = file.getOriginalFilename();
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toLowerCase();
        }

        // Generate unique stored filename to avoid conflicts
        String storedFileName = UUID.randomUUID().toString() + "." + extension;
        Path targetPath = uploadPath.resolve(storedFileName);

        // Save the file
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Build Attachment entity
        Attachment attachment = new Attachment();
        attachment.setFileName(originalFileName);
        attachment.setStoredFileName(storedFileName);
        attachment.setFilePath(targetPath.toString());
        attachment.setFileType(extension);
        attachment.setFileSize(file.getSize());
        attachment.setDownloadCount(0);

        return attachment;
    }

    /**
     * Loads a stored file as a Spring Resource for download/streaming.
     */
    public Resource loadFileAsResource(String storedFileName) throws MalformedURLException {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path filePath = uploadPath.resolve(storedFileName).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        }
        throw new RuntimeException("File not found: " + storedFileName);
    }

    /**
     * Determines content type from file extension for proper HTTP response headers.
     */
    public String getContentType(String fileType) {
        return switch (fileType.toLowerCase()) {
            case "pdf"  -> "application/pdf";
            case "pptx", "ppt" -> "application/vnd.ms-powerpoint";
            case "docx", "doc" -> "application/msword";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png"  -> "image/png";
            case "gif"  -> "image/gif";
            case "mp4"  -> "video/mp4";
            case "avi"  -> "video/x-msvideo";
            case "mov"  -> "video/quicktime";
            case "zip"  -> "application/zip";
            default     -> "application/octet-stream";
        };
    }
    public Attachment getAttachment(String storedFileName) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path filePath = uploadPath.resolve(storedFileName).normalize();

        if (Files.exists(filePath)) {
            // Optionally return a minimal Attachment object
            Attachment a = new Attachment();
            a.setStoredFileName(storedFileName);
            a.setFilePath(filePath.toString());
            return a;
        }
        throw new RuntimeException("File not found: " + storedFileName);
    }
}