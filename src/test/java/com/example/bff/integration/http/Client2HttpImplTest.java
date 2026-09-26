package com.example.bff.integration.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.bff.integration.request.ApiRequest;
import com.example.bff.integration.response.ApiResponse2;
import com.example.bff.testsupport.JsonFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;

@RestClientTest(
    components = Client2HttpImpl.class,
    properties = {
      "bff.client.type=http",
      "bff.client.api1.base-url=http://api1.test",
      "bff.client.api2.base-url=http://api2.test",
      "bff.client.api3.base-url=http://api3.test"
    })
@Import(HttpClientConfig.class)
class Client2HttpImplTest {

  @Autowired private Client2HttpImpl client;
  @Autowired private MockRestServiceServer server;

  @Test
  void idをパスに含めてGETで呼び出し_BFFで使わない項目は無視して変換する() {
    server
        .expect(requestTo("http://api2.test/api2/001"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                JsonFixtures.read("api-response2/normal.json"), MediaType.APPLICATION_JSON));

    ApiResponse2 response = client.execute2(ApiRequest.builder().id("001").build());

    assertThat(response)
        .isEqualTo(JsonFixtures.load("api-response2/normal.json", ApiResponse2.class));
    server.verify();
  }

  @Test
  void 外部APIが404を返した場合は例外を送出する() {
    server
        .expect(requestTo("http://api2.test/api2/999"))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    assertThatThrownBy(() -> client.execute2(ApiRequest.builder().id("999").build()))
        .isInstanceOf(HttpClientErrorException.NotFound.class);
  }
}
