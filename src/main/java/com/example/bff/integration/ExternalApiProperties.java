package com.example.bff.integration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 外部APIの設定. API 毎にモード（スタブ / HTTP）と接続先を指定する.
 *
 * <p>タイムアウトは Spring Boot 標準の {@code spring.http.clients.connect-timeout} / {@code
 * spring.http.clients.read-timeout} で設定する.
 *
 * @param api1 外部API_1 の設定
 * @param api2 外部API_2 の設定
 * @param api3 外部API_3 の設定
 */
@Validated
@ConfigurationProperties("bff.external-api")
public record ExternalApiProperties(
    @Valid @DefaultValue Endpoint api1,
    @Valid @DefaultValue Endpoint api2,
    @Valid @DefaultValue Endpoint api3) {

  /**
   * @param mode 動かし方（{@value ExternalApiMode#STUB} / {@value ExternalApiMode#HTTP}）. 未設定ならスタブ
   * @param baseUrl ベースURL（例: https://api1.example.com）. mode が {@value ExternalApiMode#HTTP} のとき必須
   */
  public record Endpoint(
      @DefaultValue(ExternalApiMode.STUB)
          @Pattern(regexp = "(?i)stub|http", message = "stub または http を指定してください")
          String mode,
      URI baseUrl) {

    /** HTTP で呼び出すのに接続先が無い設定を、起動時にエラーにする. */
    @AssertTrue(message = "mode=http のときは base-url を指定してください")
    public boolean isBaseUrlConfigured() {
      return !ExternalApiMode.HTTP.equalsIgnoreCase(mode) || baseUrl != null;
    }
  }
}
