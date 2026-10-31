package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.config.AppProperties;
import com.pharmacy.demo.dto.response.MedicineResponse;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.exception.BadRequestException;
import com.pharmacy.demo.exception.NotFoundException;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.ActivityLogger;
import com.pharmacy.demo.service.MedicineImageService;
import com.pharmacy.demo.service.MedicineService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MedicineImageServiceImpl implements MedicineImageService {

    private static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final String INVALID_MESSAGE = "Ảnh phải là JPG, PNG hoặc WebP và không quá 2MB.";
    private static final List<ImageType> TYPES = List.of(
            new ImageType("jpg", MediaType.IMAGE_JPEG),
            new ImageType("png", MediaType.IMAGE_PNG),
            new ImageType("webp", MediaType.parseMediaType("image/webp")));

    private final MedicineRepository medicineRepository;
    private final MedicineService medicineService;
    private final ActivityLogger activityLogger;
    private final AppProperties properties;
    private final Clock clock;

    @Override
    public MedicineResponse upload(String medicineId, MultipartFile file, Actor actor) {
        Medicine medicine = find(medicineId);
        byte[] content = read(file);
        ImageType type = detect(content);
        if (type == null) {
            throw invalid();
        }
        Path directory = directory();
        try {
            Files.createDirectories(directory);
            deleteFiles(medicineId);
            Path target = directory.resolve(medicineId + "." + type.extension());
            Path temporary = directory.resolve(medicineId + ".upload");
            Files.write(temporary, content);
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        medicine.setImageUrl("/api/v1/medicines/" + medicineId + "/image?v="
                + clock.instant().toEpochMilli());
        medicine.setUpdatedAt(clock.instant());
        medicineRepository.save(medicine);
        activityLogger.log(
                actor,
                "UPLOAD_MEDICINE_IMAGE",
                "MEDICINE",
                medicine.getId(),
                medicine.getCode(),
                "Cập nhật ảnh thuốc " + medicine.getName());
        return medicineService.get(medicineId, actor);
    }

    @Override
    public MedicineResponse remove(String medicineId, Actor actor) {
        Medicine medicine = find(medicineId);
        deleteFiles(medicineId);
        if (medicine.getImageUrl() != null) {
            medicine.setImageUrl(null);
            medicine.setUpdatedAt(clock.instant());
            medicineRepository.save(medicine);
            activityLogger.log(
                    actor,
                    "DELETE_MEDICINE_IMAGE",
                    "MEDICINE",
                    medicine.getId(),
                    medicine.getCode(),
                    "Xóa ảnh thuốc " + medicine.getName());
        }
        return medicineService.get(medicineId, actor);
    }

    @Override
    public StoredImage load(String medicineId) {
        find(medicineId);
        for (ImageType type : TYPES) {
            Path path = directory().resolve(medicineId + "." + type.extension());
            if (Files.isRegularFile(path)) {
                return new StoredImage(new FileSystemResource(path), type.mediaType());
            }
        }
        throw new NotFoundException("Thuốc chưa có ảnh.");
    }

    private Medicine find(String id) {
        return medicineRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy thuốc."));
    }

    private Path directory() {
        return Path.of(properties.storage().medicineImageDirectory())
                .toAbsolutePath()
                .normalize();
    }

    private void deleteFiles(String medicineId) {
        for (ImageType type : TYPES) {
            try {
                Files.deleteIfExists(directory().resolve(medicineId + "." + type.extension()));
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }
    }

    private static byte[] read(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw invalid();
        }
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static ImageType detect(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return TYPES.get(0);
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 'P'
                && bytes[2] == 'N'
                && bytes[3] == 'G'
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A) {
            return TYPES.get(1);
        }
        if (bytes.length >= 12
                && "RIFF".equals(new String(bytes, 0, 4, StandardCharsets.US_ASCII))
                && "WEBP".equals(new String(bytes, 8, 4, StandardCharsets.US_ASCII))) {
            return TYPES.get(2);
        }
        return null;
    }

    private static BadRequestException invalid() {
        return new BadRequestException(INVALID_MESSAGE, Map.of("image", INVALID_MESSAGE));
    }

    private record ImageType(String extension, MediaType mediaType) {}
}
