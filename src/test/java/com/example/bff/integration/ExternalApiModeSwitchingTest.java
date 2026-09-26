package com.example.bff.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bff.integration.http.Api1HttpClient;
import com.example.bff.integration.stub.Api2StubClient;
import com.example.bff.integration.stub.Api3StubClient;
import com.example.bff.service.Api1Client;
import com.example.bff.service.Api2Client;
import com.example.bff.service.Api3Client;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** API 毎に実装を切り替えられること. API_1 だけ HTTP、API_2 は明示的にスタブ、API_3 は application.properties のまま（スタブ）. */
@SpringBootTest(
    properties = {
      "bff.external-api.api1.mode=http",
      "bff.external-api.api1.base-url=http://api1.test",
      "bff.external-api.api2.mode=stub"
    })
class ExternalApiModeSwitchingTest {

  @Autowired private Api1Client api1Client;
  @Autowired private Api2Client api2Client;
  @Autowired private Api3Client api3Client;

  @Test
  void API毎に指定した実装が使われる() {
    assertThat(api1Client).isInstanceOf(Api1HttpClient.class);
    assertThat(api2Client).isInstanceOf(Api2StubClient.class);
    assertThat(api3Client).isInstanceOf(Api3StubClient.class);
  }
}
