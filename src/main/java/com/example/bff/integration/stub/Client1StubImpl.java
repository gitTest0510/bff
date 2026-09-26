package com.example.bff.integration.stub;

import com.example.bff.integration.ClientType;
import com.example.bff.integration.request.ApiRequest;
import com.example.bff.integration.response.ApiResponse1;
import com.example.bff.service.Client1;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** 外部API_1 のスタブ. 固定値を返す. */
@Service
@ConditionalOnProperty(
    name = ClientType.API1_PROPERTY,
    havingValue = ClientType.STUB,
    matchIfMissing = true)
public class Client1StubImpl implements Client1 {
  @Override
  public ApiResponse1 execute1(ApiRequest apiRequest) {
    return new ApiResponse1("ApiResponse1");
  }
}
