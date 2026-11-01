package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.config.AppProperties;
import com.pharmacy.demo.dto.response.BulkImageResultResponse;
import com.pharmacy.demo.dto.response.MedicineResponse;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.enums.BulkImageStatus;
import com.pharmacy.demo.exception.BadRequestException;
import com.pharmacy.demo.exception.NotFoundException;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.ActivityLogger;
import com.pharmacy.demo.service.MedicineImageService;
import com.pharmacy.demo.service.MedicineService;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MedicineImageServiceImpl implements MedicineImageService {

    private static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final int MAX_FILES = 300;
    private static final int MAX_DIMENSION = 600;
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
        PreparedImage image = prepare(read(file));
        if (image == null) {
            throw invalid();
        }
        store(medicine, image);
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
    public BulkImageResultResponse uploadBulk(List<MultipartFile> files, boolean overwrite, Actor actor) {
        List<IncomingImage> incoming = expand(files);
        List<BulkImageResultResponse.Item> items = new ArrayList<>();
        Set<String> handled = new HashSet<>();
        for (IncomingImage entry : incoming) {
            items.add(handleIncoming(entry, overwrite, handled));
        }
        int saved = count(items, BulkImageStatus.SAVED);
        activityLogger.log(
                actor,
                "BULK_UPLOAD_MEDICINE_IMAGE",
                "MEDICINE",
                "bulk",
                "bulk",
                "Tải ảnh thuốc hàng loạt: " + saved + "/" + items.size() + " ảnh được lưu");
        return new BulkImageResultResponse(
                saved,
                count(items, BulkImageStatus.SKIPPED_EXISTING),
                count(items, BulkImageStatus.NO_MATCH),
                count(items, BulkImageStatus.INVALID),
                items);
    }

    private BulkImageResultResponse.Item handleIncoming(IncomingImage entry, boolean overwrite, Set<String> handled) {
        Medicine medicine = entry.bytes() == null ? null : match(entry.key());
        if (entry.bytes() == null) {
            return item(entry, BulkImageStatus.INVALID, null, INVALID_MESSAGE);
        }
        if (medicine == null) {
            return item(
                    entry, BulkImageStatus.NO_MATCH, null, "Tên file không khớp mã thuốc, mã vạch hay số đăng ký nào.");
        }
        if (!handled.add(medicine.getId())) {
            return item(
                    entry, BulkImageStatus.SKIPPED_EXISTING, medicine, "Thuốc này đã có ảnh khác trong cùng lần tải.");
        }
        if (medicine.getImageUrl() != null && !overwrite) {
            return item(entry, BulkImageStatus.SKIPPED_EXISTING, medicine, "Thuốc đã có ảnh, bỏ qua.");
        }
        PreparedImage image = prepare(entry.bytes());
        if (image == null) {
            return item(entry, BulkImageStatus.INVALID, medicine, INVALID_MESSAGE);
        }
        store(medicine, image);
        return item(entry, BulkImageStatus.SAVED, medicine, "Đã lưu ảnh.");
    }

    private static BulkImageResultResponse.Item item(
            IncomingImage entry, BulkImageStatus status, Medicine medicine, String message) {
        return new BulkImageResultResponse.Item(
                entry.fileName(),
                status,
                medicine == null ? null : medicine.getCode(),
                medicine == null ? null : medicine.getName(),
                message);
    }

    private static int count(List<BulkImageResultResponse.Item> items, BulkImageStatus status) {
        return (int) items.stream().filter(entry -> entry.status() == status).count();
    }

    private Medicine match(String key) {
        if (key.isBlank()) {
            return null;
        }
        return medicineRepository
                .findByCodeIgnoreCase(key)
                .or(() -> medicineRepository.findByBarcode(key))
                .or(() -> medicineRepository.findFirstByRegistrationNumberIgnoreCase(key))
                .orElse(null);
    }

    private static List<IncomingImage> expand(List<MultipartFile> files) {
        List<IncomingImage> result = new ArrayList<>();
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("Chưa chọn ảnh nào.", Map.of("files", "Chưa chọn ảnh nào."));
        }
        for (MultipartFile file : files) {
            String name = baseName(file.getOriginalFilename());
            if (name.toLowerCase(Locale.ROOT).endsWith(".zip")) {
                readZip(file, result);
            } else {
                result.add(incoming(name, file.getSize() > MAX_BYTES ? null : readQuietly(file)));
            }
            if (result.size() > MAX_FILES) {
                throw new BadRequestException(
                        "Mỗi lần chỉ tải tối đa " + MAX_FILES + " ảnh.", Map.of("files", "Quá số ảnh cho phép."));
            }
        }
        return result;
    }

    private static void readZip(MultipartFile file, List<IncomingImage> result) {
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = baseName(entry.getName());
                if (entry.isDirectory() || entry.getName().startsWith("__MACOSX") || name.startsWith(".")) {
                    continue;
                }
                byte[] content = zip.readNBytes((int) MAX_BYTES + 1);
                result.add(incoming(name, content.length > MAX_BYTES ? null : content));
                if (result.size() > MAX_FILES) {
                    return;
                }
            }
        } catch (IOException exception) {
            throw new BadRequestException("Không đọc được file zip.", Map.of("files", "File zip không hợp lệ."));
        }
    }

    private static IncomingImage incoming(String fileName, byte[] bytes) {
        int dot = fileName.lastIndexOf('.');
        String key = (dot > 0 ? fileName.substring(0, dot) : fileName).trim();
        return new IncomingImage(fileName, key, bytes);
    }

    private static String baseName(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    private static byte[] readQuietly(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            return null;
        }
    }

    private void store(Medicine medicine, PreparedImage image) {
        Path directory = directory();
        try {
            Files.createDirectories(directory);
            deleteFiles(medicine.getId());
            Path target =
                    directory.resolve(medicine.getId() + "." + image.type().extension());
            Path temporary = directory.resolve(medicine.getId() + ".upload");
            Files.write(temporary, image.bytes());
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        medicine.setImageUrl("/api/v1/medicines/" + medicine.getId() + "/image?v="
                + clock.instant().toEpochMilli());
        medicine.setUpdatedAt(clock.instant());
        medicineRepository.save(medicine);
    }

    private static PreparedImage prepare(byte[] content) {
        ImageType type = detect(content);
        if (type == null) {
            return null;
        }
        if (type == TYPES.get(2)) {
            return new PreparedImage(content, type);
        }
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(content));
            if (source == null) {
                return new PreparedImage(content, type);
            }
            int longest = Math.max(source.getWidth(), source.getHeight());
            if (longest <= MAX_DIMENSION) {
                return new PreparedImage(content, type);
            }
            double scale = (double) MAX_DIMENSION / longest;
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
            boolean png = type == TYPES.get(1);
            BufferedImage resized =
                    new BufferedImage(width, height, png ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = resized.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            if (!png) {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, width, height);
            }
            graphics.drawImage(source, 0, 0, width, height, null);
            graphics.dispose();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(resized, png ? "png" : "jpg", output);
            return new PreparedImage(output.toByteArray(), type);
        } catch (IOException exception) {
            return new PreparedImage(content, type);
        }
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

    private record PreparedImage(byte[] bytes, ImageType type) {}

    private record IncomingImage(String fileName, String key, byte[] bytes) {}
}
