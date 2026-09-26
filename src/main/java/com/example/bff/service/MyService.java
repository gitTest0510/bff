package com.example.bff.service;

import com.example.bff.controller.request.Request;
import com.example.bff.controller.response.Response;
import com.example.bff.integration.request.ApiRequest;
import com.example.bff.integration.response.ApiResponse1;
import com.example.bff.integration.response.ApiResponse2;
import com.example.bff.mapper.ExampleMapper;
import com.example.bff.model.DetailContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
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
        // 外部API_2の明細ごとに外部API_3を呼び出し、明細と結果を紐付ける
        List<DetailContext> detailContexts = toDetailContexts(apiResponse2);

        return mapper.toResponse(apiResponse1, apiResponse2, detailContexts);
    }

    private List<DetailContext> toDetailContexts(ApiResponse2 apiResponse2) {
        if (Objects.isNull(apiResponse2) || Objects.isNull(apiResponse2.getMyDetails())) {
            return List.of();
        }

        // 名前のない明細は main / sub / other のいずれにも該当しないため、外部API_3を呼ばずに除外する
        return apiResponse2.getMyDetails().stream()
            .filter(Objects::nonNull)
            .filter(myDetail -> Objects.nonNull(myDetail.getName()))
            // 外部API_3複数回呼び出し（明細件数分の逐次呼び出し。並列化は今後検討）
            .map(myDetail -> new DetailContext(
                myDetail,
                client3.execute3(mapper.toApiRequest3(myDetail.getName()))))
            .toList();
    }
}
