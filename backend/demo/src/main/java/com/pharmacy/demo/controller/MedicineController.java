package com.pharmacy.demo.controller;

import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.request.ChangeMedicineStatusRequest;
import com.pharmacy.demo.dto.request.MedicineRequest;
import com.pharmacy.demo.dto.response.CodePreviewResponse;
import com.pharmacy.demo.dto.response.MedicineResponse;
import com.pharmacy.demo.enums.MedicineStatus;
import com.pharmacy.demo.enums.PrescriptionType;
import com.pharmacy.demo.enums.StockStatus;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.MedicineCommandService;
import com.pharmacy.demo.service.MedicineImageService;
import com.pharmacy.demo.service.MedicineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Thuốc", description = "Danh mục thuốc kinh doanh và tồn khả dụng.")
@RestController
@RequestMapping("/api/v1/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;
    private final MedicineCommandService commandService;
    private final MedicineImageService imageService;

    @Operation(summary = "Danh sách thuốc kèm tồn khả dụng")
    @GetMapping
    public PageResponse<MedicineResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) MedicineStatus status,
            @RequestParam(required = false) StockStatus stockStatus,
            @RequestParam(required = false) PrescriptionType prescriptionType,
            @RequestParam(defaultValue = "false") boolean sellable,
            @RequestParam(defaultValue = "false") boolean inStock,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal Actor actor) {
        MedicineService.MedicineFilter filter = new MedicineService.MedicineFilter(
                q, categoryId, status, stockStatus, prescriptionType, sellable, inStock);
        return medicineService.search(filter, PageQuery.of(page, size, sort), actor);
    }

    @Operation(summary = "Gợi ý nhà sản xuất đã có trên thuốc (Admin)")
    @GetMapping("/manufacturers")
    @PreAuthorize("hasRole('ADMIN')")
    public List<String> manufacturers(
            @RequestParam(required = false) String q, @RequestParam(defaultValue = "50") int limit) {
        return medicineService.manufacturers(q, Math.max(1, Math.min(limit, 200)));
    }

    @Operation(summary = "Xem trước mã thuốc kế tiếp (Admin)")
    @GetMapping("/next-code")
    @PreAuthorize("hasRole('ADMIN')")
    public CodePreviewResponse nextCode() {
        return new CodePreviewResponse(commandService.nextCode());
    }

    @Operation(summary = "Chi tiết thuốc")
    @GetMapping("/{id}")
    public MedicineResponse get(@PathVariable String id, @AuthenticationPrincipal Actor actor) {
        return medicineService.get(id, actor);
    }

    @Operation(summary = "Tạo thuốc (Admin)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public MedicineResponse create(@Valid @RequestBody MedicineRequest request, @AuthenticationPrincipal Actor actor) {
        return commandService.create(request, actor);
    }

    @Operation(summary = "Sửa thuốc (Admin)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public MedicineResponse update(
            @PathVariable String id,
            @Valid @RequestBody MedicineRequest request,
            @AuthenticationPrincipal Actor actor) {
        return commandService.update(id, request, actor);
    }

    @Operation(summary = "Ảnh thuốc")
    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> image(@PathVariable String id) {
        MedicineImageService.StoredImage image = imageService.load(id);
        return ResponseEntity.ok()
                .contentType(image.mediaType())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePrivate())
                .body(image.resource());
    }

    @Operation(summary = "Tải ảnh thuốc lên (Admin)")
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public MedicineResponse uploadImage(
            @PathVariable String id, @RequestPart("image") MultipartFile image, @AuthenticationPrincipal Actor actor) {
        return imageService.upload(id, image, actor);
    }

    @Operation(summary = "Xóa ảnh thuốc (Admin)")
    @DeleteMapping("/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public MedicineResponse deleteImage(@PathVariable String id, @AuthenticationPrincipal Actor actor) {
        return imageService.remove(id, actor);
    }

    @Operation(summary = "Đổi trạng thái kinh doanh (Admin)")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public MedicineResponse changeStatus(
            @PathVariable String id,
            @Valid @RequestBody ChangeMedicineStatusRequest request,
            @AuthenticationPrincipal Actor actor) {
        return commandService.changeStatus(id, request.status(), actor);
    }
}
