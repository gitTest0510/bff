package com.example.bff.integration.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/** 外部API_3 のリクエスト. */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiRequest3 {

  /** 外部API_2 の明細名. */
  String name;
}
