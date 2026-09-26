package com.example.bff.integration;

/**
 * 外部APIクライアントの実装の切り替え. API 毎に {@code bff.client.apiN.type} で指定する.
 *
 * <ul>
 *   <li>{@value #STUB}（既定）: 固定値を返すスタブ. 外部APIが無い環境での起動・テスト用
 *   <li>{@value #HTTP}: 実際に HTTP で外部APIを呼び出す
 * </ul>
 *
 * <p>{@code @ConditionalOnProperty} の引数にはコンパイル時定数が必要なため、プロパティ名を定数で持つ.
 */
public final class ClientType {

  public static final String API1_PROPERTY = "bff.client.api1.type";
  public static final String API2_PROPERTY = "bff.client.api2.type";
  public static final String API3_PROPERTY = "bff.client.api3.type";
  public static final String STUB = "stub";
  public static final String HTTP = "http";

  private ClientType() {}
}
