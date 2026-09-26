package com.example.bff.integration.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiRequest3 {

    /** 外部API_2 の明細名. */
    private String name;
}
