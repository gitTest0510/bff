package com.example.bff.controller.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/**
 * 最終レスポンス. JSON のキーは {@link JsonProperty} で明示し、Java のフィールド名とは切り離す.
 *
 * <p>イミュータブル. 生成は各クラスの builder() からのみ行う. List 項目は {@link Singular} により、未設定なら空、設定時は変更不可のコピーを保持する.
 */
@JsonPropertyOrder({
  "Apiレスポンス1",
  "Apiレスポンス2",
})
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Response {

  @JsonProperty("Apiレスポンス1")
  String apiResponse1;

  @JsonProperty("Apiレスポンス2")
  Section apiResponse2;

  @JsonPropertyOrder({
    "summary", "main", "sub", "other",
  })
  @Value
  @Builder
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Section {
    String summary;

    @Singular("mainItem")
    List<Item> main;

    @Singular("subItem")
    List<Item> sub;

    @Singular("otherItem")
    List<Item> other;
  }

  /** main / sub / other 共通の明細. */
  @JsonPropertyOrder({
    "name", "price", "memo", "details",
  })
  @Value
  @Builder
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Item {
    String name;
    String price;
    String memo;
    @Singular List<Detail> details;
  }

  @JsonPropertyOrder({
    "name", "test", "test2", "test3",
  })
  @Value
  @Builder
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Detail {
    String name;
    String test;
    String test2;
    String test3;
  }
}
