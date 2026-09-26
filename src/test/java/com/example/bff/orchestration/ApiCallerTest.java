package com.example.bff.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.bff.exception.ExternalApiException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class ApiCallerTest {

  private final ApiCaller apiCaller = apiCaller(Duration.ofSeconds(1), 10);

  @Test
  void callOrFailは成功時に結果を返す() {
    CompletableFuture<String> future = apiCaller.callOrFail("API", () -> "ok");

    apiCaller.awaitAll(future);

    assertThat(future.join()).isEqualTo("ok");
  }

  @Test
  void callOrFailは失敗時にExternalApiExceptionを送出する() {
    CompletableFuture<String> future =
        apiCaller.callOrFail(
            "API",
            () -> {
              throw new IllegalStateException("down");
            });

    assertThatThrownBy(() -> apiCaller.awaitAll(future))
        .isInstanceOfSatisfying(
            ExternalApiException.class,
            ex -> {
              assertThat(ex.getApiName()).isEqualTo("API");
              assertThat(ex.isTimeout()).isFalse();
              assertThat(ex).hasRootCauseInstanceOf(IllegalStateException.class);
            });
  }

  @Test
  void callOrFailはタイムアウト時にタイムアウトとして送出する() {
    ApiCaller shortTimeout = apiCaller(Duration.ofMillis(50), 10);
    CompletableFuture<String> future =
        shortTimeout.callOrFail("API", () -> sleepAndReturn(1000, "late"));

    assertThatThrownBy(() -> shortTimeout.awaitAll(future))
        .isInstanceOfSatisfying(
            ExternalApiException.class,
            ex -> {
              assertThat(ex.isTimeout()).isTrue();
              assertThat(ex.getCause()).isInstanceOf(TimeoutException.class);
            });
  }

  @Test
  void callOrEmptyは失敗時に空の結果で続行する() {
    CompletableFuture<Optional<String>> future =
        apiCaller.callOrEmpty(
            "API",
            () -> {
              throw new IllegalStateException("down");
            });

    apiCaller.awaitAll(future);

    assertThat(future.join()).isEmpty();
  }

  @Test
  void callOrEmptyは結果がnullの場合に空を返す() {
    CompletableFuture<Optional<String>> future = apiCaller.callOrEmpty("API", () -> null);

    assertThat(future.join()).isEmpty();
  }

  @Test
  void fanOutは同じキーを1回だけ呼び出しnullのキーは呼び出さない() {
    Map<String, AtomicInteger> callCounts = new ConcurrentHashMap<>();

    Map<String, String> results =
        apiCaller
            .fanOut(
                "API",
                Arrays.asList("a", "b", "a", null, "a"),
                key -> {
                  callCounts.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
                  return key.toUpperCase();
                })
            .join();

    assertThat(results).containsExactly(Map.entry("a", "A"), Map.entry("b", "B"));
    assertThat(callCounts.get("a")).hasValue(1);
    assertThat(callCounts.get("b")).hasValue(1);
  }

  @Test
  void fanOutは失敗したキーと結果がnullのキーを結果に含めない() {
    Map<String, String> results =
        apiCaller
            .fanOut(
                "API",
                List.of("ok", "error", "null"),
                key ->
                    switch (key) {
                      case "error" -> throw new IllegalStateException("down");
                      case "null" -> null;
                      default -> key;
                    })
            .join();

    assertThat(results).containsOnlyKeys("ok");
  }

  @Test
  void fanOutは同時呼び出し数を制限する() {
    ApiCaller limited = apiCaller(Duration.ofSeconds(5), 2);
    AtomicInteger running = new AtomicInteger();
    AtomicInteger maxRunning = new AtomicInteger();

    limited
        .fanOut(
            "API",
            List.of("1", "2", "3", "4", "5", "6"),
            key -> {
              maxRunning.accumulateAndGet(running.incrementAndGet(), Math::max);
              sleepAndReturn(50, key);
              running.decrementAndGet();
              return key;
            })
        .join();

    assertThat(maxRunning).hasValueLessThanOrEqualTo(2);
  }

  @Test
  void fanOutはキーが空の場合に空のMapを返す() {
    assertThat(apiCaller.fanOut("API", List.<String>of(), key -> key).join()).isEmpty();
  }

  @Test
  void awaitAllはいずれかが失敗した時点で他の完了を待たずに例外を送出する() {
    ApiCaller longTimeout = apiCaller(Duration.ofSeconds(10), 10);
    CompletableFuture<String> slow =
        longTimeout.callOrFail("SLOW", () -> sleepAndReturn(3000, "slow"));
    CompletableFuture<String> failing =
        longTimeout.callOrFail(
            "FAIL",
            () -> {
              throw new IllegalStateException("down");
            });

    long start = System.nanoTime();
    assertThatThrownBy(() -> longTimeout.awaitAll(slow, failing))
        .isInstanceOfSatisfying(
            ExternalApiException.class, ex -> assertThat(ex.getApiName()).isEqualTo("FAIL"));

    assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(2));
    assertThat(slow).isNotDone();
  }

  @Test
  void awaitAllは実行時例外以外の失敗をIllegalStateExceptionで包んで送出する() {
    CompletableFuture<String> future = CompletableFuture.failedFuture(new Exception("checked"));

    assertThatThrownBy(() -> apiCaller.awaitAll(future))
        .isInstanceOf(IllegalStateException.class)
        .hasCauseInstanceOf(Exception.class);
  }

  @Test
  void 呼び出しごとにAPI名と結果と所要時間をログに出す(CapturedOutput output) {
    apiCaller.awaitAll(apiCaller.callOrFail("API_1", () -> "ok"));

    assertThat(output).containsPattern("外部API呼び出し api=API_1 result=成功 elapsed=\\d+ms");
  }

  @Test
  void 結果がnullの場合は結果なしとしてログに出す(CapturedOutput output) {
    apiCaller.callOrEmpty("API_1", () -> null).join();

    assertThat(output).contains("外部API呼び出し api=API_1 result=成功(結果なし)");
  }

  @Test
  void 失敗した場合は原因をログに出す(CapturedOutput output) {
    apiCaller
        .callOrEmpty(
            "API_1",
            () -> {
              throw new IllegalStateException("down");
            })
        .join();

    assertThat(output)
        .containsPattern(
            "外部API呼び出し api=API_1 result=失敗 elapsed=\\d+ms"
                + " cause=java.lang.IllegalStateException: down");
  }

  @Test
  void タイムアウトした場合はタイムアウトとしてログに出す(CapturedOutput output) {
    ApiCaller shortTimeout = apiCaller(Duration.ofMillis(50), 10);

    shortTimeout.callOrEmpty("API_1", () -> sleepAndReturn(1000, "late")).join();

    assertThat(output).contains("外部API呼び出し api=API_1 result=失敗(タイムアウト)");
  }

  @Test
  void fanOutはキーごとにどのデータに対する呼び出しかをログに出す(CapturedOutput output) {
    apiCaller.fanOut("API_3", List.of("main", "sub"), key -> key).join();

    assertThat(output)
        .contains("外部API呼び出し api=API_3 key=main result=成功")
        .contains("外部API呼び出し api=API_3 key=sub result=成功");
  }

  private static ApiCaller apiCaller(Duration timeout, int fanOutConcurrency) {
    return new ApiCaller(
        Executors.newVirtualThreadPerTaskExecutor(),
        new ApiCallProperties(timeout, fanOutConcurrency));
  }

  private static <T> T sleepAndReturn(long millis, T value) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
    return value;
  }
}
