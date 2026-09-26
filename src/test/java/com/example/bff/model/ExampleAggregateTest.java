package com.example.bff.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.bff.integration.response.ApiResponse3;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExampleAggregateTest {

  @Test
  void apiResponse3ByNameを設定しない場合は空のMapになる() {
    ExampleAggregate aggregate = ExampleAggregate.builder().build();

    assertThat(aggregate.getApiResponse1()).isNull();
    assertThat(aggregate.getApiResponse2()).isNull();
    assertThat(aggregate.getApiResponse3ByName()).isEmpty();
  }

  @Test
  void 渡したMapを後から変更しても影響を受けず取得したMapも変更できない() {
    Map<String, ApiResponse3> source = new HashMap<>();
    source.put("main", new ApiResponse3("t", null));

    ExampleAggregate aggregate = ExampleAggregate.builder().apiResponse3ByName(source).build();
    source.put("sub", new ApiResponse3("t2", null));

    assertThat(aggregate.getApiResponse3ByName()).containsOnlyKeys("main");
    assertThatThrownBy(() -> aggregate.getApiResponse3ByName().put("x", null))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
