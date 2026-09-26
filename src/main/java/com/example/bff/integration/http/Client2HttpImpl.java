package com.example.bff.integration.http;

import com.example.bff.integration.ClientType;
import com.example.bff.integration.ExternalApiProperties;
import com.example.bff.integration.request.ApiRequest;
import com.example.bff.integration.response.ApiResponse2;
import com.example.bff.service.Client2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 外部API_2 の HTTP クライアント. {@code GET {baseUrl}/api2/{id}}
 *
 * <p>4xx / 5xx・通信エラー・タイムアウトは例外になり、呼び出し元（ApiCaller）が結果の扱いを決める.
 */
@Service
@ConditionalOnProperty(name = ClientType.API2_PROPERTY, havingValue = ClientType.HTTP)
public class Client2HttpImpl implements Client2 {

  private final RestClient restClient;

  public Client2HttpImpl(RestClient.Builder builder, ExternalApiProperties properties) {
    this.restClient = builder.baseUrl(properties.api2().baseUrl().toString()).build();
  }

  @Override
  public ApiResponse2 execute2(ApiRequest apiRequest) {
    return restClient
        .get()
        .uri("/api2/{id}", apiRequest.getId())
        .retrieve()
        .body(ApiResponse2.class);
  }
}
