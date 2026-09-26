package com.example.bff.testsupport;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * テスト用 JSON ファイル（src/test/resources/fixtures 配下）から外部APIのレスポンスを生成する.
 *
 * <p>外部APIのレスポンスは項目数が多くなりがちなため、テストでコンストラクタを並べて組み立てる代わりに、実際のレスポンスに近い JSON を置いて読み込む. 境界値（null
 * 要素など）のケースはテストコード内で個別に組み立てる.
 */
public final class JsonFixtures {

  /** 外部APIのレスポンスには BFF で使わない項目も含まれるため、未知の項目は無視する. */
  private static final JsonMapper MAPPER =
      JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

  private JsonFixtures() {}

  /**
   * JSON ファイルを読み込んで指定の型に変換する.
   *
   * @param path fixtures ディレクトリからの相対パス（例: {@code "api-response2/normal.json"}）
   */
  public static <T> T load(String path, Class<T> type) {
    return MAPPER.readValue(read(path), type);
  }

  /**
   * JSON ファイルを文字列のまま読み込む. 外部APIのモック（MockRestServiceServer 等）のレスポンス本文に使う.
   *
   * @param path fixtures ディレクトリからの相対パス（例: {@code "api-response2/normal.json"}）
   */
  public static String read(String path) {
    String resource = "/fixtures/" + path;
    try (InputStream in = JsonFixtures.class.getResourceAsStream(resource)) {
      return new String(
          Objects.requireNonNull(in, () -> "テスト用 JSON が見つかりません: " + resource).readAllBytes(),
          StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}
