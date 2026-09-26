package com.example.bff.integration;

/**
 * 外部APIをどのモードで動かすか. API 毎に {@code bff.external-api.apiN.mode} で指定する.
 *
 * <ul>
 *   <li>{@value #STUB}（既定）: 固定値を返すスタブ. 外部APIが無い環境での起動・テスト用
 *   <li>{@value #HTTP}: 実際に HTTP で外部APIを呼び出す
 * </ul>
 *
 * <p>実装クラスの {@code @ConditionalOnProperty} で使う. 引数にはコンパイル時定数が必要なため、プロパティ名と値を定数で持つ.
 *
 * <pre>
 *   &#64;ConditionalOnProperty(name = ExternalApiMode.API1, havingValue = ExternalApiMode.HTTP)
 *   → bff.external-api.api1.mode=http のとき、このクラスを Bean として登録する
 * </pre>
 */
public final class ExternalApiMode {

  /** 外部API_1 のモードを指定するプロパティ名. */
  public static final String API1 = "bff.external-api.api1.mode";

  /** 外部API_2 のモードを指定するプロパティ名. */
  public static final String API2 = "bff.external-api.api2.mode";

  /** 外部API_3 のモードを指定するプロパティ名. */
  public static final String API3 = "bff.external-api.api3.mode";

  public static final String STUB = "stub";
  public static final String HTTP = "http";

  private ExternalApiMode() {}
}
