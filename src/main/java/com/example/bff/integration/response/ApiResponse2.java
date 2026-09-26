package com.example.bff.integration.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse2 {
    private String summary;
    private List<MyDetail> myDetails;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyDetail {
        private String name;
        private int price;
        private String memo;
    }
}
