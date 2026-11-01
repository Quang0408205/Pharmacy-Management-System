package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.response.BulkImageResultResponse;
import com.pharmacy.demo.dto.response.MedicineResponse;
import com.pharmacy.demo.security.Actor;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

public interface MedicineImageService {

    MedicineResponse upload(String medicineId, MultipartFile file, Actor actor);

    BulkImageResultResponse uploadBulk(List<MultipartFile> files, boolean overwrite, Actor actor);

    MedicineResponse remove(String medicineId, Actor actor);

    StoredImage load(String medicineId);

    record StoredImage(Resource resource, MediaType mediaType) {}
}
