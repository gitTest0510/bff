package com.example.bff.integration;

import com.example.bff.integration.request.ApiRequest3;
import com.example.bff.integration.response.ApiResponse3;
import com.example.bff.service.Client3;
import org.springframework.stereotype.Service;

@Service
public class Client3Impl implements Client3 {
    @Override
    public ApiResponse3 execute3(ApiRequest3 apiRequest3) {
        return new ApiResponse3("title3", new ApiResponse3.MyDetail3("test1", "test2", "test3"));
    }
}
