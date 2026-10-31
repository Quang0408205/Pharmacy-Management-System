package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.request.SaveDraftRequest;
import com.pharmacy.demo.dto.response.InvoiceDraftResponse;
import com.pharmacy.demo.security.Actor;
import java.util.List;

public interface InvoiceDraftService {

    List<InvoiceDraftResponse> list(Actor actor);

    InvoiceDraftResponse save(SaveDraftRequest request, Actor actor);

    void delete(String id, Actor actor);
}
