package com.example.bff.integration.stub;

import com.example.bff.integration.ClientType;
import com.example.bff.integration.request.ApiRequest3;
import com.example.bff.integration.response.ApiResponse3;
import com.example.bff.service.Client3;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** 外部API_3 のスタブ. 固定値を返す. */
@Service
@ConditionalOnProperty(
    name = ClientType.PROPERTY,
    havingValue = ClientType.STUB,
    matchIfMissing = true)
public class Client3StubImpl implements Client3 {
  @Override
  public ApiResponse3 execute3(ApiRequest3 apiRequest3) {
    return new ApiResponse3("title3", new ApiResponse3.MyDetail3("test1", "test2", "test3"));
  }
}
