package com.example.bff.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ExternalApiPropertiesTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(ExternalApiConfig.class);

  @Test
  void 未指定のAPIはスタブになる() {
    runner.run(
        context -> {
          ExternalApiProperties properties = context.getBean(ExternalApiProperties.class);
          assertThat(properties.api1().type()).isEqualTo(ClientType.STUB);
          assertThat(properties.api2().type()).isEqualTo(ClientType.STUB);
          assertThat(properties.api3().type()).isEqualTo(ClientType.STUB);
        });
  }

  @Test
  void HTTPのAPIだけ接続先を指定すれば起動できる() {
    runner
        .withPropertyValues(
            "bff.client.api1.type=http", "bff.client.api1.base-url=http://api1.test")
        .run(
            context -> {
              ExternalApiProperties properties = context.getBean(ExternalApiProperties.class);
              assertThat(properties.api1().baseUrl()).isEqualTo(URI.create("http://api1.test"));
              assertThat(properties.api2().baseUrl()).isNull();
            });
  }

  @Test
  void HTTPなのに接続先が無い場合は起動エラーになる() {
    runner
        .withPropertyValues("bff.client.api2.type=http")
        .run(
            context ->
                assertThat(context)
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining("bff.client.api2")
                    .hasMessageContaining("type=http のときは base-url を指定してください"));
  }

  @Test
  void stubとhttp以外を指定した場合は起動エラーになる() {
    runner
        .withPropertyValues("bff.client.api3.type=htttp")
        .run(
            context ->
                assertThat(context)
                    .getFailure()
                    .rootCause()
                    .hasMessageContaining("bff.client.api3.type")
                    .hasMessageContaining("stub または http を指定してください"));
  }
}
