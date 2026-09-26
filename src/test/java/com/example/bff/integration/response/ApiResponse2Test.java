package com.example.bff.integration.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ApiResponse2Test {

    @Test
    void JSONからJacksonで生成できる() {
        String json =
                """
                {"summary":"s","myDetails":[{"name":"main","price":1,"memo":"m"},null]}
                """;

        ApiResponse2 response = JsonMapper.builder().build().readValue(json, ApiResponse2.class);

        assertThat(response.summary()).isEqualTo("s");
        assertThat(response.myDetails())
                .containsExactly(new ApiResponse2.MyDetail("main", 1, "m"), null);
    }

    @Test
    void 渡したリストを後から変更しても影響を受けず取得したリストも変更できない() {
        List<ApiResponse2.MyDetail> source = new ArrayList<>();
        source.add(new ApiResponse2.MyDetail("main", 1, "m"));
        source.add(null);

        ApiResponse2 response = new ApiResponse2("s", source);
        source.clear();

        assertThat(response.myDetails()).hasSize(2);
        assertThatThrownBy(() -> response.myDetails().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void 明細リストがnullの場合はnullのまま保持する() {
        assertThat(new ApiResponse2("s", null).myDetails()).isNull();
    }
}
