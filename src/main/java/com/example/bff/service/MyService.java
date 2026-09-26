package com.example.bff.service;

import com.example.bff.controller.request.Request;
import com.example.bff.controller.response.Response;
import com.example.bff.integration.request.ApiRequest;
import com.example.bff.integration.response.ApiResponse1;
import com.example.bff.integration.response.ApiResponse2;
import com.example.bff.integration.response.ApiResponse3;
import com.example.bff.mapper.ExampleMapper;
import com.example.bff.model.DetailContext;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class MyService {

    private final Client1 client1;
    private final Client2 client2;
    private final Client3 client3;
    private final ExampleMapper mapper;

    public Response execute(Request request) {

        // 共通的な外部APIリクエスト作成
        ApiRequest apiRequest = mapper.toApiRequest(request);

        // 外部API_1呼び出し
        ApiResponse1 apiResponse1 = client1.execute1(apiRequest);
        // 外部API_2呼び出し
        ApiResponse2 apiResponse2 = client2.execute2(apiRequest);

        List<DetailContext> apiResponse2with3 = new ArrayList<>();
        apiResponse2.getMyDetails()
            .forEach(record -> {
                // 外部API_3複数回呼び出し
                ApiResponse3 apiResponse3 = client3.execute3(mapper.toApiRequest3(record.getName()));

                apiResponse2with3.add(new DetailContext(apiResponse2, apiResponse3));
            });

        return mapper.toResponse(apiResponse1, apiResponse2with3);
    }
}
