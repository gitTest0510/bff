package com.example.bff.service;

import com.example.bff.integration.request.ApiRequest;
import com.example.bff.integration.response.ApiResponse1;

public interface Client1 {

    ApiResponse1 execute1(ApiRequest apiRequest);
}
