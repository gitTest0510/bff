package com.example.bff.integration.stub;

import com.example.bff.integration.ExternalApiMode;
import com.example.bff.integration.request.ApiRequest;
import com.example.bff.integration.response.ApiResponse1;
import com.example.bff.service.Api1Client;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 外部API_1 のスタブ.
 *
 * <p>最終レスポンスのどこに API_1 の結果が入ったか分かるよう、受け取った id を埋め込んだ値を返す（例: {@code API_1(id=001)}）.
 */
@Service
@ConditionalOnProperty(
    name = ExternalApiMode.API1,
    havingValue = ExternalApiMode.STUB,
    matchIfMissing = true)
public class Api1StubClient implements Api1Client {
  @Override
  public ApiResponse1 execute1(ApiRequest apiRequest) {
    return new ApiResponse1("API_1(id=" + apiRequest.getId() + ")");
  }
}
