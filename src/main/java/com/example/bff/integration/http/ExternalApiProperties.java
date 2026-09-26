package com.example.bff.integration.http;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 外部APIの接続先. {@code bff.client.type=http} のときだけ使う.
 *
 * <p>タイムアウトは Spring Boot 標準の {@code spring.http.clients.connect-timeout} / {@code
 * spring.http.clients.read-timeout} で設定する.
 *
 * @param api1 外部API_1 の接続先
 * @param api2 外部API_2 の接続先
 * @param api3 外部API_3 の接続先
 */
@Validated
@ConfigurationProperties("bff.client")
public record ExternalApiProperties(
    @Valid @NotNull Endpoint api1, @Valid @NotNull Endpoint api2, @Valid @NotNull Endpoint api3) {

  /**
   * @param baseUrl ベースURL（例: https://api1.example.com）
   */
  public record Endpoint(@NotNull URI baseUrl) {}
}
