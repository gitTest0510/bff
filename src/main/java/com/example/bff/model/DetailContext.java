package com.example.bff.model;

import com.example.bff.integration.response.ApiResponse2;
import com.example.bff.integration.response.ApiResponse3;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 外部API_2 の明細1件と、その明細をキーに呼び出した外部API_3 の結果の組.
 */
@Getter
@AllArgsConstructor
public class DetailContext {
    private final ApiResponse2.MyDetail myDetail;
    private final ApiResponse3 apiResponse3;
}
