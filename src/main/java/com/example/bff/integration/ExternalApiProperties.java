package com.example.bff.integration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 外部APIクライアントの設定. API 毎に実装（スタブ / HTTP）と接続先を指定する.
 *
 * <p>タイムアウトは Spring Boot 標準の {@code spring.http.clients.connect-timeout} / {@code
 * spring.http.clients.read-timeout} で設定する.
 *
 * @param api1 外部API_1 の設定
 * @param api2 外部API_2 の設定
 * @param api3 外部API_3 の設定
 */
@Validated
@ConfigurationProperties("bff.client")
public record ExternalApiProperties(
    @Valid @DefaultValue Endpoint api1,
    @Valid @DefaultValue Endpoint api2,
    @Valid @DefaultValue Endpoint api3) {

  /**
   * @param type 実装の種類（{@value ClientType#STUB} / {@value ClientType#HTTP}）. 未設定ならスタブ
   * @param baseUrl ベースURL（例: https://api1.example.com）. type が {@value ClientType#HTTP} のとき必須
   */
  public record Endpoint(
      @DefaultValue(ClientType.STUB)
          @Pattern(regexp = "(?i)stub|http", message = "stub または http を指定してください")
          String type,
      URI baseUrl) {

    /** HTTP で呼び出すのに接続先が無い設定を、起動時にエラーにする. */
    @AssertTrue(message = "type=http のときは base-url を指定してください")
    public boolean isBaseUrlConfigured() {
      return !ClientType.HTTP.equalsIgnoreCase(type) || baseUrl != null;
    }
  }
}
