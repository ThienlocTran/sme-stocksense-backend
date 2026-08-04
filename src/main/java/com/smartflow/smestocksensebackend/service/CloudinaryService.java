package com.smartflow.smestocksensebackend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryService.class);
    private final Cloudinary cloudinary;
    private final String env;

    public CloudinaryService(Cloudinary cloudinary, @Value("${cloudinary.env:dev}") String env) {
        this.cloudinary = cloudinary;
        this.env = env;
    }

    public Map<String, Object> uploadAvatar(MultipartFile file, Long employeeId) throws IOException {
        if (env == null || "test".equals(env) || cloudinary.config.cloudName == null || cloudinary.config.cloudName.isBlank() || "${CLOUDINARY_CLOUD_NAME}".equals(cloudinary.config.cloudName)) {
             throw new RuntimeException("Cloudinary chưa được cấu hình. Vui lòng cập nhật biến môi trường.");
        }
        
        String folderPath = "sme-stocksense/" + env + "/avatars";
        String publicId = "employee_" + employeeId + "_" + System.currentTimeMillis();
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folderPath,
                    "public_id", publicId,
                    "transformation", "c_thumb,g_face,h_256,w_256",
                    "resource_type", "image"
            ));
            return uploadResult;
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("cloud_name is disabled")) {
                throw new RuntimeException("Cloudinary chưa được cấu hình hoặc key không hợp lệ. Vui lòng kiểm tra lại cấu hình.");
            }
            throw e;
        }
    }

    public void deleteAvatarByPublicId(String publicId) {
        if (publicId == null || publicId.isBlank()) return;
        
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Deleted old avatar on Cloudinary: {}", publicId);
        } catch (Exception e) {
            log.warn("Could not delete old avatar from Cloudinary: {}", e.getMessage());
        }
    }
}
