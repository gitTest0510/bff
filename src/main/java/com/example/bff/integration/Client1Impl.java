package com.example.bff.integration;

import com.example.bff.integration.request.ApiRequest;
import com.example.bff.integration.response.ApiResponse1;
import com.example.bff.service.Client1;
import org.springframework.stereotype.Service;

@Service
public class Client1Impl implements Client1 {
    @Override
    public ApiResponse1 execute1(ApiRequest apiRequest) {
        return new ApiResponse1("ApiResponse1");
    }
}
