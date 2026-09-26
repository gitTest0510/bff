package com.example.bff.controller.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 最終レスポンス.
 * JSON のキーは {@link JsonProperty} で明示し、Java のフィールド名とは切り離す.
 */
@JsonPropertyOrder({
    "Apiレスポンス1",
    "Apiレスポンス2",
})
@Getter
@Builder
public class Response {

    @JsonProperty("Apiレスポンス1")
    private String apiResponse1;

    @JsonProperty("Apiレスポンス2")
    private Section apiResponse2;

    @JsonPropertyOrder({
        "summary",
        "main",
        "sub",
        "other",
    })
    @Getter
    @Builder
    public static class Section {
        private String summary;
        private List<Item> main;
        private List<Item> sub;
        private List<Item> other;
    }

    /** main / sub / other 共通の明細. */
    @JsonPropertyOrder({
        "name",
        "price",
        "memo",
        "details",
    })
    @Getter
    @Builder
    public static class Item {
        private String name;
        private String price;
        private String memo;
        private List<Detail> details;
    }

    @JsonPropertyOrder({
        "name",
        "test",
        "test2",
        "test3",
    })
    @Getter
    @Builder
    public static class Detail {
        private String name;
        private String test;
        private String test2;
        private String test3;
    }
}
