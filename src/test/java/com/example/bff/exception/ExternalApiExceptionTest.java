package com.example.bff.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

class ExternalApiExceptionTest {

  @Test
  void ApiCallerのタイムアウトはタイムアウトと判定する() {
    assertThat(new ExternalApiException("API", new TimeoutException()).isTimeout()).isTrue();
  }

  @Test
  void HTTPクライアントの例外に包まれたタイムアウトもタイムアウトと判定する() {
    assertThat(
            new ExternalApiException(
                    "API",
                    new ResourceAccessException(
                        "I/O error", new HttpTimeoutException("request timed out")))
                .isTimeout())
        .isTrue();
    assertThat(
            new ExternalApiException(
                    "API",
                    new ResourceAccessException(
                        "I/O error", new SocketTimeoutException("Read timed out")))
                .isTimeout())
        .isTrue();
  }

  @Test
  void タイムアウト以外の失敗はタイムアウトと判定しない() {
    assertThat(new ExternalApiException("API", new IllegalStateException()).isTimeout()).isFalse();
    assertThat(new ExternalApiException("API", null).isTimeout()).isFalse();
  }
}
