package com.example.bff.integration.http;

import com.example.bff.integration.ClientType;
import com.example.bff.integration.request.ApiRequest3;
import com.example.bff.integration.response.ApiResponse3;
import com.example.bff.service.Client3;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * 外部API_3 の HTTP クライアント. {@code POST {baseUrl}/api3}（JSON ボディ）
 *
 * <p>404 は「該当データなし」として null を返す. それ以外の 4xx / 5xx・通信エラー・タイムアウトは例外になり、呼び出し元（ApiCaller）が結果の扱いを決める.
 */
@Service
@ConditionalOnProperty(name = ClientType.PROPERTY, havingValue = ClientType.HTTP)
public class Client3HttpImpl implements Client3 {

  private final RestClient restClient;

  public Client3HttpImpl(RestClient.Builder builder, ExternalApiProperties properties) {
    this.restClient = builder.baseUrl(properties.api3().baseUrl().toString()).build();
  }

  @Override
  public ApiResponse3 execute3(ApiRequest3 apiRequest3) {
    try {
      return restClient
          .post()
          .uri("/api3")
          .contentType(MediaType.APPLICATION_JSON)
          .body(apiRequest3)
          .retrieve()
          .body(ApiResponse3.class);
    } catch (HttpClientErrorException.NotFound ex) {
      return null;
    }
  }
}
