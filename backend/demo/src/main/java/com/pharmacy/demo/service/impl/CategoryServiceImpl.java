package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.request.CategoryRequest;
import com.pharmacy.demo.dto.response.CategoryResponse;
import com.pharmacy.demo.entity.Category;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.enums.CategoryStatus;
import com.pharmacy.demo.exception.ConflictException;
import com.pharmacy.demo.exception.NotFoundException;
import com.pharmacy.demo.mapper.CategoryMapper;
import com.pharmacy.demo.repository.CategoryRepository;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.ActivityLogger;
import com.pharmacy.demo.service.CategoryService;
import com.pharmacy.demo.service.IdGenerator;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final String DEFAULT_COLOR = "#0d9488";
    private static final String DUPLICATE_CODE_MESSAGE = "Mã danh mục đã tồn tại.";

    private final CategoryRepository categoryRepository;
    private final MedicineRepository medicineRepository;
    private final CategoryMapper categoryMapper;
    private final ActivityLogger activityLogger;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    public List<CategoryResponse> list() {
        Map<String, Long> counts = medicineRepository.findAll().stream()
                .collect(Collectors.groupingBy(Medicine::getCategoryId, Collectors.counting()));
        return categoryRepository.findAll(Sort.by("id")).stream()
                .map(category -> categoryMapper.toResponse(category, counts.getOrDefault(category.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request, Actor actor) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        requireUniqueCode(code, null);
        Instant now = clock.instant();
        Category category = categoryRepository.save(Category.builder()
                .id(idGenerator.newId())
                .code(code)
                .name(request.name().trim())
                .description(text(request.description()))
                .color(colorOf(request.color(), DEFAULT_COLOR))
                .status(CategoryStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());
        activityLogger.log(
                actor,
                "CREATE_CATEGORY",
                "CATEGORY",
                category.getId(),
                category.getCode(),
                "Thêm danh mục " + category.getName());
        return categoryMapper.toResponse(category, 0);
    }

    @Override
    @Transactional
    public CategoryResponse update(String id, CategoryRequest request, Actor actor) {
        Category category = find(id);
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        requireUniqueCode(code, category.getId());
        category.setCode(code);
        category.setName(request.name().trim());
        category.setDescription(text(request.description()));
        category.setColor(colorOf(request.color(), category.getColor()));
        category.setUpdatedAt(clock.instant());
        categoryRepository.save(category);
        activityLogger.log(
                actor,
                "UPDATE_CATEGORY",
                "CATEGORY",
                category.getId(),
                category.getCode(),
                "Cập nhật danh mục " + category.getName());
        return categoryMapper.toResponse(category, medicineRepository.countByCategoryId(category.getId()));
    }

    @Override
    @Transactional
    public void delete(String id, Actor actor) {
        Category category = find(id);
        if (medicineRepository.existsByCategoryId(category.getId())) {
            throw new ConflictException(
                    "Không thể xóa danh mục đang có thuốc. Hãy chuyển thuốc sang danh mục khác trước.");
        }
        categoryRepository.delete(category);
        activityLogger.log(
                actor,
                "DELETE_CATEGORY",
                "CATEGORY",
                category.getId(),
                category.getCode(),
                "Xóa danh mục " + category.getName());
    }

    private Category find(String id) {
        return categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục."));
    }

    private void requireUniqueCode(String code, String selfId) {
        boolean taken = categoryRepository
                .findByCodeIgnoreCase(code)
                .filter(existing -> !existing.getId().equals(selfId))
                .isPresent();
        if (taken) {
            throw new ConflictException(DUPLICATE_CODE_MESSAGE, Map.of("code", DUPLICATE_CODE_MESSAGE));
        }
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static String colorOf(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
