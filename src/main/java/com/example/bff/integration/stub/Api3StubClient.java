package com.example.bff.integration.stub;

import com.example.bff.integration.ExternalApiMode;
import com.example.bff.integration.request.ApiRequest3;
import com.example.bff.integration.response.ApiResponse3;
import com.example.bff.service.Api3Client;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 外部API_3 のスタブ.
 *
 * <p>API_2 のどの明細に紐付いたか分かるよう、受け取った明細名を埋め込んだ値を返す（例: {@code API_3(name=main)}）.
 */
@Service
@ConditionalOnProperty(
    name = ExternalApiMode.API3,
    havingValue = ExternalApiMode.STUB,
    matchIfMissing = true)
public class Api3StubClient implements Api3Client {
  @Override
  public ApiResponse3 execute3(ApiRequest3 apiRequest3) {
    String source = "API_3(name=" + apiRequest3.getName() + ")";
    return new ApiResponse3(
        source, new ApiResponse3.MyDetail3(source + ".test", source + ".test2", source + ".test3"));
  }
}
