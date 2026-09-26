package com.example.bff.service;

import com.example.bff.controller.request.Request;
import com.example.bff.controller.response.Response;
import com.example.bff.integration.request.ApiRequest;
import com.example.bff.integration.response.ApiResponse1;
import com.example.bff.integration.response.ApiResponse2;
import com.example.bff.integration.response.ApiResponse3;
import com.example.bff.mapper.ExampleMapper;
import com.example.bff.model.ExampleAggregate;
import com.example.bff.orchestration.ApiCaller;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MyService {

    private final Client1 client1;
    private final Client2 client2;
    private final Client3 client3;
    private final ExampleMapper mapper;
    private final ApiCaller apiCaller;

    public Response execute(Request request) {
        // 1. 外部APIを依存関係どおりに呼び出し、結果を集約する
        ExampleAggregate aggregate = fetch(mapper.toApiRequest(request));
        // 2. 集約した結果だけを使って最終レスポンスに変換する
        return mapper.toResponse(aggregate);
    }

    /**
     * 外部APIの依存関係.
     *
     * <pre>
     *   API_1 ─────────────────────────────┐
     *   API_2 ──→ API_3 × 明細名の種類数 ───┴──→ ExampleAggregate
     * </pre>
     *
     * <p>API_1 と API_2 は互いに独立しているため並列に呼び出す. API_3 は API_2 の結果に依存するため、API_2 の完了後に呼び出す.
     *
     * <p>結果が得られなかった場合の扱い:
     *
     * <ul>
     *   <li>API_1: 無くても返せる（Apiレスポンス1 を null にして続行）
     *   <li>API_2: 欠かせない（レスポンスの骨格になるため、処理全体をエラーにする）
     *   <li>API_3: 無くても返せる（取得できなかった明細の details を空にして続行）
     * </ul>
     */
    private ExampleAggregate fetch(ApiRequest apiRequest) {
        CompletableFuture<Optional<ApiResponse1>> api1 =
                apiCaller.callOrEmpty("API_1", () -> client1.execute1(apiRequest));
        CompletableFuture<ApiResponse2> api2 =
                apiCaller.callOrFail("API_2", () -> client2.execute2(apiRequest));
        CompletableFuture<Map<String, ApiResponse3>> api3 =
                api2.thenCompose(
                        apiResponse2 ->
                                apiCaller.fanOut(
                                        "API_3",
                                        detailNames(apiResponse2),
                                        name -> client3.execute3(mapper.toApiRequest3(name))));

        apiCaller.awaitAll(api1, api2, api3);

        return ExampleAggregate.builder()
                .apiResponse1(api1.join().orElse(null))
                .apiResponse2(api2.join())
                .apiResponse3ByName(api3.join())
                .build();
    }

    private static List<String> detailNames(ApiResponse2 apiResponse2) {
        if (Objects.isNull(apiResponse2) || Objects.isNull(apiResponse2.getMyDetails())) {
            return List.of();
        }
        return apiResponse2.getMyDetails().stream()
                .filter(Objects::nonNull)
                .map(ApiResponse2.MyDetail::getName)
                .filter(Objects::nonNull)
                .toList();
    }
}
