package com.example.bff.integration.stub;

import com.example.bff.integration.ClientType;
import com.example.bff.integration.request.ApiRequest;
import com.example.bff.integration.response.ApiResponse2;
import com.example.bff.service.Client2;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** 外部API_2 のスタブ. 固定値を返す. */
@Service
@ConditionalOnProperty(
    name = ClientType.API2_PROPERTY,
    havingValue = ClientType.STUB,
    matchIfMissing = true)
public class Client2StubImpl implements Client2 {
  @Override
  public ApiResponse2 execute2(ApiRequest apiRequest) {
    return new ApiResponse2(
        "ApiResponse2",
        List.of(
            new ApiResponse2.MyDetail("ApiResponse2_name1", 1, "ApiResponse2_memo1"),
            new ApiResponse2.MyDetail("ApiResponse2_name2", 2, "ApiResponse2_memo2"),
            new ApiResponse2.MyDetail("main", 3, "ApiResponse2_memo3"),
            new ApiResponse2.MyDetail("main", 4, "ApiResponse2_memo4"),
            new ApiResponse2.MyDetail("sub", 5, "ApiResponse2_memo5"),
            new ApiResponse2.MyDetail("main", 6, "ApiResponse2_memo6")));
  }
}
