package com.example.bff.integration.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/** 外部API_1 / API_2 の共通リクエスト. */
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiRequest {

  String id;
}
