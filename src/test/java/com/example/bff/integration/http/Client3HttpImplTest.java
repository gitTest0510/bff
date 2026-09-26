package com.example.bff.integration.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.bff.integration.request.ApiRequest3;
import com.example.bff.integration.response.ApiResponse3;
import com.example.bff.testsupport.JsonFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;

@RestClientTest(
    components = Client3HttpImpl.class,
    properties = {
      "bff.client.type=http",
      "bff.client.api1.base-url=http://api1.test",
      "bff.client.api2.base-url=http://api2.test",
      "bff.client.api3.base-url=http://api3.test"
    })
@Import(HttpClientConfig.class)
class Client3HttpImplTest {

  @Autowired private Client3HttpImpl client;
  @Autowired private MockRestServiceServer server;

  @Test
  void 明細名をJSONボディにしてPOSTで呼び出しレスポンスを変換する() {
    server
        .expect(requestTo("http://api3.test/api3"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("{\"name\":\"main\"}", JsonCompareMode.STRICT))
        .andRespond(
            withSuccess(
                JsonFixtures.read("api-response3/normal.json"), MediaType.APPLICATION_JSON));

    ApiResponse3 response = client.execute3(ApiRequest3.builder().name("main").build());

    assertThat(response)
        .isEqualTo(JsonFixtures.load("api-response3/normal.json", ApiResponse3.class));
    server.verify();
  }

  @Test
  void 外部APIが404を返した場合は該当データなしとしてnullを返す() {
    server.expect(requestTo("http://api3.test/api3")).andRespond(withStatus(HttpStatus.NOT_FOUND));

    assertThat(client.execute3(ApiRequest3.builder().name("none").build())).isNull();
  }

  @Test
  void 外部APIが5xxを返した場合は例外を送出する() {
    server.expect(requestTo("http://api3.test/api3")).andRespond(withServerError());

    assertThatThrownBy(() -> client.execute3(ApiRequest3.builder().name("main").build()))
        .isInstanceOf(HttpServerErrorException.class);
  }
}
