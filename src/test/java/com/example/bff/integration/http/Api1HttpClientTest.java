package com.example.bff.integration.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.bff.integration.ExternalApiConfig;
import com.example.bff.integration.request.ApiRequest;
import com.example.bff.integration.response.ApiResponse1;
import com.example.bff.testsupport.JsonFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;

@RestClientTest(
    components = Api1HttpClient.class,
    properties = {
      "bff.external-api.api1.mode=http",
      "bff.external-api.api1.base-url=http://api1.test"
    })
@Import(ExternalApiConfig.class)
class Api1HttpClientTest {

  @Autowired private Api1HttpClient client;
  @Autowired private MockRestServiceServer server;

  @Test
  void idをクエリパラメータにしてGETで呼び出しレスポンスを変換する() {
    server
        .expect(requestTo("http://api1.test/api1?id=001"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                JsonFixtures.read("api-response1/normal.json"), MediaType.APPLICATION_JSON));

    ApiResponse1 response = client.execute1(ApiRequest.builder().id("001").build());

    assertThat(response).isEqualTo(new ApiResponse1("res1"));
    server.verify();
  }

  @Test
  void 外部APIが5xxを返した場合は例外を送出する() {
    server.expect(requestTo("http://api1.test/api1?id=001")).andRespond(withServerError());

    assertThatThrownBy(() -> client.execute1(ApiRequest.builder().id("001").build()))
        .isInstanceOf(HttpServerErrorException.class);
  }
}
