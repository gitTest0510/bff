package com.example.bff.integration.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse3 {
    private String title;
    private MyDetail3 myDetail3;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyDetail3 {
        private String test;
        private String test2;
        private String test3;
    }
}
