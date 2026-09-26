package com.example.bff.integration.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bff.testsupport.JsonFixtures;
import org.junit.jupiter.api.Test;

class ApiResponse3Test {

  @Test
  void ネストしたオブジェクトを含むテスト用JSONから生成できる() {
    ApiResponse3 response = JsonFixtures.load("api-response3/normal.json", ApiResponse3.class);

    assertThat(response)
        .isEqualTo(new ApiResponse3("title3", new ApiResponse3.MyDetail3("a", "b", "c")));
  }
}
