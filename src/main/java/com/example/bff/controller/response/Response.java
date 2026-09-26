package com.example.bff.controller.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@JsonPropertyOrder({
    "Apiレスポンス1",
    "Apiレスポンス2",
})
@Getter
@Builder
public class Response {
    private String Apiレスポンス1;
    private Apiレスポンス2 Apiレスポンス2;

    @JsonPropertyOrder({
        "main",
        "sub",
        "other",
        "details",
    })
    @Getter
    @Builder
    public static class Apiレスポンス2 {
        private String summary;
        private List<MainClazz> main;
        private List<SubClazz> sub;
        private List<OtherClazz> other;

        @Getter
        @Builder
        public static class MainClazz {
            private String name;
            private String price;
            private String memo;
            private List<Details> details;
        }

        @Getter
        @Builder
        public static class SubClazz {
            private String name;
            private String price;
            private String memo;
            private List<Details> details;
        }

        @Getter
        @Builder
        public static class OtherClazz {
            private String name;
            private String price;
            private String memo;
            private List<Details> details;
        }

        @Getter
        @Builder
        public static class Details {
            private String name;
            private String test;
            private String test2;
            private String test3;
        }
    }
}


