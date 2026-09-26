package com.example.bff.service;

import com.example.bff.integration.request.ApiRequest;
import com.example.bff.integration.response.ApiResponse2;

public interface Api2Client {

  ApiResponse2 execute2(ApiRequest apiRequest);
}
