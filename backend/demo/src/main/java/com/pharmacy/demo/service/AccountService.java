package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.request.ActivateAccountRequest;
import com.pharmacy.demo.dto.request.ResetPasswordRequest;
import com.pharmacy.demo.dto.response.ActivationInfoResponse;

public interface AccountService {

    ActivationInfoResponse describeActivation(String token);

    void activate(ActivateAccountRequest request);

    void validateResetToken(String token);

    void resetPassword(ResetPasswordRequest request);
}
