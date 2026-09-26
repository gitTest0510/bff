package com.example.bff.controller.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResponseTest {

    @Test
    void List項目を設定しない場合は空のリストになる() {
        Response.Section section = Response.Section.builder().build();

        assertThat(section.getMain()).isEmpty();
        assertThat(section.getSub()).isEmpty();
        assertThat(section.getOther()).isEmpty();
        assertThat(Response.Item.builder().build().getDetails()).isEmpty();
    }

    @Test
    void 渡したリストを後から変更しても影響を受けず取得したリストも変更できない() {
        List<Response.Item> source = new ArrayList<>();
        source.add(Response.Item.builder().name("a").build());

        Response.Section section = Response.Section.builder().main(source).build();
        source.add(Response.Item.builder().name("b").build());

        assertThat(section.getMain()).extracting(Response.Item::getName).containsExactly("a");
        assertThatThrownBy(() -> section.getMain().add(Response.Item.builder().build()))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
