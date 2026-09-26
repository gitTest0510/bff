package com.example.bff.integration.stub;

import com.example.bff.integration.ExternalApiMode;
import com.example.bff.integration.request.ApiRequest;
import com.example.bff.integration.response.ApiResponse2;
import com.example.bff.service.Api2Client;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 外部API_2 のスタブ.
 *
 * <p>最終レスポンスのどこに API_2 の結果が入ったか分かるよう、受け取った id を埋め込んだ値を返す（例: {@code API_2(id=001)}）.
 *
 * <p>明細は main / sub / other の振り分けと、同じ明細名（main）が複数あるケースを確認できるように並べる. 明細名は後続の API_3 の呼び出しに使われる.
 */
@Service
@ConditionalOnProperty(
    name = ExternalApiMode.API2,
    havingValue = ExternalApiMode.STUB,
    matchIfMissing = true)
public class Api2StubClient implements Api2Client {
  @Override
  public ApiResponse2 execute2(ApiRequest apiRequest) {
    String source = "API_2(id=" + apiRequest.getId() + ")";
    return new ApiResponse2(
        source,
        List.of(
            new ApiResponse2.MyDetail("main", 100, source + " 明細1"),
            new ApiResponse2.MyDetail("sub", 200, source + " 明細2"),
            new ApiResponse2.MyDetail("main", 300, source + " 明細3"),
            new ApiResponse2.MyDetail("other-a", 400, source + " 明細4"),
            new ApiResponse2.MyDetail("other-b", 500, source + " 明細5")));
  }
}
