package com.example.bff.integration;

/**
 * 外部APIクライアントの実装の切り替え. {@code bff.client.type} で指定する.
 *
 * <ul>
 *   <li>{@value #STUB}（既定）: 固定値を返すスタブ. 外部APIが無い環境での起動・テスト用
 *   <li>{@value #HTTP}: 実際に HTTP で外部APIを呼び出す
 * </ul>
 */
public final class ClientType {

  public static final String PROPERTY = "bff.client.type";
  public static final String STUB = "stub";
  public static final String HTTP = "http";

  private ClientType() {}
}
