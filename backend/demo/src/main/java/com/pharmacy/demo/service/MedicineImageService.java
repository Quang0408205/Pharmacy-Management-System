package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.response.MedicineResponse;
import com.pharmacy.demo.security.Actor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

public interface MedicineImageService {

    MedicineResponse upload(String medicineId, MultipartFile file, Actor actor);

    MedicineResponse remove(String medicineId, Actor actor);

    StoredImage load(String medicineId);

    record StoredImage(Resource resource, MediaType mediaType) {}
}
