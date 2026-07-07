package com.smartflow.smestocksensebackend.excelimport;

import com.smartflow.smestocksensebackend.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class ExcelImportChecksumService {

    public String sha256(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("file is required.");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(file.getBytes()));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        } catch (Exception exception) {
            throw new BadRequestException("Cannot read uploaded file.", exception);
        }
    }
}
