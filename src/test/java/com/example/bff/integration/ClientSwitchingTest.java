package com.example.bff.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bff.integration.http.Client1HttpImpl;
import com.example.bff.integration.stub.Client2StubImpl;
import com.example.bff.integration.stub.Client3StubImpl;
import com.example.bff.service.Client1;
import com.example.bff.service.Client2;
import com.example.bff.service.Client3;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** API 毎に実装を切り替えられること. API_1 だけ HTTP、API_2 は明示的にスタブ、API_3 は application.properties のまま（スタブ）. */
@SpringBootTest(
    properties = {
      "bff.client.api1.type=http",
      "bff.client.api1.base-url=http://api1.test",
      "bff.client.api2.type=stub"
    })
class ClientSwitchingTest {

  @Autowired private Client1 client1;
  @Autowired private Client2 client2;
  @Autowired private Client3 client3;

  @Test
  void API毎に指定した実装が使われる() {
    assertThat(client1).isInstanceOf(Client1HttpImpl.class);
    assertThat(client2).isInstanceOf(Client2StubImpl.class);
    assertThat(client3).isInstanceOf(Client3StubImpl.class);
  }
}
