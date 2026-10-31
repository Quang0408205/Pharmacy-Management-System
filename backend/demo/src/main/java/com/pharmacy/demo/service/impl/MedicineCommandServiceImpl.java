package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.request.MedicineRequest;
import com.pharmacy.demo.dto.response.MedicineResponse;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.embedded.DataSource;
import com.pharmacy.demo.enums.MedicineStatus;
import com.pharmacy.demo.enums.PrescriptionType;
import com.pharmacy.demo.exception.BadRequestException;
import com.pharmacy.demo.exception.ConflictException;
import com.pharmacy.demo.exception.NotFoundException;
import com.pharmacy.demo.repository.CategoryRepository;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.ActivityLogger;
import com.pharmacy.demo.service.CodeGenerator;
import com.pharmacy.demo.service.CodeKind;
import com.pharmacy.demo.service.IdGenerator;
import com.pharmacy.demo.service.MedicineCommandService;
import com.pharmacy.demo.service.MedicineService;
import com.pharmacy.demo.util.MedicineUnits;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MedicineCommandServiceImpl implements MedicineCommandService {

    private static final Pattern GENERATED_CODE = Pattern.compile("^T(\\d+)$");
    private static final String MANUAL_SOURCE = "NHAP_TAY";

    private final MedicineRepository medicineRepository;
    private final CategoryRepository categoryRepository;
    private final MedicineService medicineService;
    private final CodeGenerator codeGenerator;
    private final ActivityLogger activityLogger;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @Transactional
    public MedicineResponse create(MedicineRequest request, Actor actor) {
        String code = normalizedCode(request);
        validate(request, code, null);
        Instant now = clock.instant();
        Medicine medicine = Medicine.builder()
                .id(idGenerator.newId())
                .unitConversions(new ArrayList<>())
                .dataSource(DataSource.builder()
                        .sourceType(MANUAL_SOURCE)
                        .collectedAt(now)
                        .verified(true)
                        .build())
                .status(MedicineStatus.ACTIVE)
                .createdAt(now)
                .build();
        apply(medicine, request, code, now);
        medicineRepository.save(medicine);
        raiseCounterFor(code);
        activityLogger.log(
                actor, "CREATE_MEDICINE", "MEDICINE", medicine.getId(), code, "Thêm thuốc " + medicine.getName());
        return medicineService.get(medicine.getId(), actor);
    }

    @Override
    @Transactional
    public MedicineResponse update(String id, MedicineRequest request, Actor actor) {
        Medicine medicine = find(id);
        String code = normalizedCode(request);
        validate(request, code, medicine.getId());
        apply(medicine, request, code, clock.instant());
        medicineRepository.save(medicine);
        activityLogger.log(
                actor, "UPDATE_MEDICINE", "MEDICINE", medicine.getId(), code, "Cập nhật thuốc " + medicine.getName());
        return medicineService.get(medicine.getId(), actor);
    }

    @Override
    @Transactional
    public MedicineResponse changeStatus(String id, MedicineStatus status, Actor actor) {
        Medicine medicine = find(id);
        if (medicine.getStatus() != status) {
            medicine.setStatus(status);
            medicine.setUpdatedAt(clock.instant());
            medicineRepository.save(medicine);
            activityLogger.log(
                    actor,
                    "UPDATE_MEDICINE_STATUS",
                    "MEDICINE",
                    medicine.getId(),
                    medicine.getCode(),
                    (status == MedicineStatus.ACTIVE ? "Kích hoạt" : "Ngừng kinh doanh") + " thuốc "
                            + medicine.getName());
        }
        return medicineService.get(medicine.getId(), actor);
    }

    @Override
    public String nextCode() {
        return codeGenerator.preview(CodeKind.MEDICINE);
    }

    private Medicine find(String id) {
        return medicineRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy thuốc."));
    }

    private void validate(MedicineRequest request, String code, String selfId) {
        if (!MedicineUnits.isAllowed(request.unit())) {
            throw new BadRequestException(
                    "Đơn vị tính không hợp lệ.", Map.of("unit", "Vui lòng chọn đơn vị tính trong danh sách."));
        }
        if (!categoryRepository.existsById(request.categoryId())) {
            throw new BadRequestException("Danh mục không tồn tại.", Map.of("categoryId", "Danh mục không tồn tại."));
        }
        boolean codeTaken = medicineRepository
                .findByCodeIgnoreCase(code)
                .filter(existing -> !existing.getId().equals(selfId))
                .isPresent();
        if (codeTaken) {
            throw new ConflictException("Mã thuốc đã tồn tại.", Map.of("code", "Mã thuốc đã tồn tại."));
        }
        String barcode = blankToNull(request.barcode());
        if (barcode != null
                && medicineRepository
                        .findByBarcode(barcode)
                        .filter(existing -> !existing.getId().equals(selfId))
                        .isPresent()) {
            throw new ConflictException(
                    "Mã vạch đã tồn tại.", Map.of("barcode", "Mã vạch đã được gán cho thuốc khác."));
        }
    }

    private void apply(Medicine medicine, MedicineRequest request, String code, Instant now) {
        medicine.setCode(code);
        medicine.setBarcode(blankToNull(request.barcode()));
        medicine.setName(request.name().trim());
        medicine.setGenericName(
                request.genericName() == null || request.genericName().isBlank()
                        ? request.name().trim()
                        : request.genericName().trim());
        medicine.setStrength(text(request.strength()));
        medicine.setDosageForm(text(request.dosageForm()));
        medicine.setUnit(request.unit().trim());
        medicine.setPackaging(text(request.packaging()));
        medicine.setManufacturer(request.manufacturer().trim());
        medicine.setCountry(text(request.country()));
        medicine.setRegistrationNumber(blankToNull(request.registrationNumber()));
        medicine.setCategoryId(request.categoryId());
        medicine.setPrice(request.price());
        medicine.setCostPrice(request.costPrice() == null ? 0L : request.costPrice());
        medicine.setPrescriptionType(
                request.prescriptionType() == null ? PrescriptionType.OTC : request.prescriptionType());
        medicine.setMinStock(request.minStock() == null ? 0 : request.minStock());
        medicine.setDescription(text(request.description()));
        medicine.setUpdatedAt(now);
    }

    private void raiseCounterFor(String code) {
        Matcher matcher = GENERATED_CODE.matcher(code);
        if (matcher.matches() && matcher.group(1).length() <= 9) {
            codeGenerator.raiseTo(CodeKind.MEDICINE, LocalDate.now(clock), Long.parseLong(matcher.group(1)));
        }
    }

    private static String normalizedCode(MedicineRequest request) {
        return request.code().trim().toUpperCase(Locale.ROOT);
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
