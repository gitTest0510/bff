package com.example.bff.integration.http;

import com.example.bff.integration.ClientType;
import com.example.bff.integration.ExternalApiProperties;
import com.example.bff.integration.request.ApiRequest;
import com.example.bff.integration.response.ApiResponse1;
import com.example.bff.service.Client1;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 外部API_1 の HTTP クライアント. {@code GET {baseUrl}/api1?id={id}}
 *
 * <p>4xx / 5xx・通信エラー・タイムアウトは例外になり、呼び出し元（ApiCaller）が結果の扱いを決める.
 */
@Service
@ConditionalOnProperty(name = ClientType.API1_PROPERTY, havingValue = ClientType.HTTP)
public class Client1HttpImpl implements Client1 {

  private final RestClient restClient;

  /**
   * Spring Boot が用意する RestClient.Builder を使う. Boot の Jackson 設定・タイムアウト設定（spring.http.clients.*）が
   * 適用される.
   */
  public Client1HttpImpl(RestClient.Builder builder, ExternalApiProperties properties) {
    this.restClient = builder.baseUrl(properties.api1().baseUrl().toString()).build();
  }

  @Override
  public ApiResponse1 execute1(ApiRequest apiRequest) {
    return restClient
        .get()
        .uri(uri -> uri.path("/api1").queryParam("id", apiRequest.getId()).build())
        .retrieve()
        .body(ApiResponse1.class);
  }
}
