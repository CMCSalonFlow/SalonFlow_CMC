package com.example.salonflow.util;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.example.salonflow.exception.BadRequestException;

public final class ImageValidator {

    private static final long MAX_SIZE =
            10 * 1024 * 1024;

    private static final List<String> ALLOWED_TYPES =
            List.of(
                    "image/jpeg",
                    "image/png",
                    "image/webp"
            );

    public static void validate(
            MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new BadRequestException(
                    "File is empty");
        }

        if (file.getSize() > MAX_SIZE) {
            throw new BadRequestException(
                    "File exceeds 10MB");
        }

        if (!ALLOWED_TYPES.contains(
                file.getContentType())) {

            throw new BadRequestException(
                    "Only jpg/png/webp allowed");
        }
    }
}