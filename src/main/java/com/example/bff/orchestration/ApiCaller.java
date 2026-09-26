package com.example.bff.orchestration;

import com.example.bff.exception.ExternalApiException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 * 外部API呼び出しを非同期で実行するための部品.
 *
 * <p>{@link #callOrFail} と {@link #callOrEmpty} はどちらも必ず外部APIを呼び出す. 違いは「呼び出しに失敗して結果が得られなかったときの扱い」で、
 * レスポンスの組み立てにその結果が欠かせないなら {@link #callOrFail}（処理全体を失敗させる）、無くても返せるなら {@link #callOrEmpty}
 * （結果なしで続行する）を使う.
 *
 * <p>呼び出し順序（依存関係）は CompletableFuture の合成で表現する.
 *
 * <ul>
 *   <li>独立した呼び出し: それぞれ {@link #callOrFail} / {@link #callOrEmpty} で開始すれば並列に実行される
 *   <li>依存する呼び出し: 前段の Future に {@code thenCompose} で後段の呼び出しをつなぐ
 *   <li>1件の結果から複数回呼び出す: {@link #fanOut}
 *   <li>すべての完了を待つ: {@link #awaitAll}
 * </ul>
 */
@Slf4j
public class ApiCaller {

  private final Executor executor;
  private final ApiCallProperties properties;

  public ApiCaller(Executor executor, ApiCallProperties properties) {
    this.executor = executor;
    this.properties = properties;
  }

  /**
   * 結果が欠かせない外部APIを呼び出す.
   *
   * <p>失敗・タイムアウトで結果が得られなかった場合は {@link ExternalApiException} で完了し、処理全体を失敗させる.
   */
  public <T> CompletableFuture<T> callOrFail(String apiName, Supplier<T> call) {
    return CompletableFuture.supplyAsync(call, executor)
        .orTimeout(properties.timeout().toMillis(), TimeUnit.MILLISECONDS)
        .exceptionally(
            ex -> {
              throw new ExternalApiException(apiName, unwrap(ex));
            });
  }

  /**
   * 結果が無くてもレスポンスを返せる外部APIを呼び出す.
   *
   * <p>失敗・タイムアウトで結果が得られなかった場合はログを出し、{@link Optional#empty()} で続行する.
   */
  public <T> CompletableFuture<Optional<T>> callOrEmpty(String apiName, Supplier<T> call) {
    return callOrFail(apiName, call)
        .thenApply(Optional::ofNullable)
        .exceptionally(
            ex -> {
              log.warn("外部API({})の呼び出しに失敗したため、結果なしで続行します", apiName, unwrap(ex));
              return Optional.empty();
            });
  }

  /**
   * キーごとに外部APIを並列で呼び出し、キーと結果の Map を返す.
   *
   * <ul>
   *   <li>同じキーは1回だけ呼び出す（null のキーは呼び出さない）
   *   <li>同時呼び出し数は {@link ApiCallProperties#fanOutConcurrency()} までに制限する
   *   <li>キーごとの結果は無くてもよいものとして扱う（{@link #callOrEmpty} と同じ）. 失敗したキー・結果が null のキーは Map に含めない
   * </ul>
   */
  public <K, V> CompletableFuture<Map<K, V>> fanOut(
      String apiName, Collection<K> keys, Function<K, V> call) {
    Set<K> distinctKeys = new LinkedHashSet<>(keys);
    distinctKeys.remove(null);
    Semaphore permits = new Semaphore(properties.fanOutConcurrency());

    List<CompletableFuture<Optional<Map.Entry<K, V>>>> futures =
        distinctKeys.stream()
            .map(
                key ->
                    callOrEmpty(apiName, () -> withPermit(permits, () -> call.apply(key)))
                        .thenApply(result -> result.map(value -> Map.entry(key, value))))
            .toList();

    return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
        .thenApply(
            ignored -> {
              Map<K, V> results = new LinkedHashMap<>();
              futures.forEach(
                  future ->
                      future
                          .join()
                          .ifPresent(entry -> results.put(entry.getKey(), entry.getValue())));
              return results;
            });
  }

  /**
   * すべての Future の完了を待つ. いずれかが失敗した時点で、他の完了を待たずにその例外を送出する.
   *
   * <p>注意: 失敗時も他の呼び出しはキャンセルされずバックグラウンドで最後まで実行される.
   */
  public void awaitAll(CompletableFuture<?>... futures) {
    CompletableFuture<Void> all = CompletableFuture.allOf(futures);
    CompletableFuture<Void> firstFailure = new CompletableFuture<>();
    for (CompletableFuture<?> future : futures) {
      future.whenComplete(
          (result, ex) -> {
            if (Objects.nonNull(ex)) {
              firstFailure.completeExceptionally(ex);
            }
          });
    }

    try {
      CompletableFuture.anyOf(all, firstFailure).join();
    } catch (CompletionException ex) {
      Throwable cause = unwrap(ex);
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new IllegalStateException(cause);
    }
  }

  private static <T> T withPermit(Semaphore permits, Supplier<T> call) {
    try {
      permits.acquire();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("同時実行数の制御中に割り込まれました", ex);
    }
    try {
      return call.get();
    } finally {
      permits.release();
    }
  }

  private static Throwable unwrap(Throwable ex) {
    Throwable current = ex;
    while ((current instanceof CompletionException || current instanceof ExecutionException)
        && Objects.nonNull(current.getCause())) {
      current = current.getCause();
    }
    return current;
  }
}
