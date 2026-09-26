package com.example.bff.orchestration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@Configuration
@EnableConfigurationProperties(ApiCallProperties.class)
public class ApiCallConfig {

  /**
   * 外部API呼び出しは I/O 待ちが大半のため、仮想スレッドで並列実行する.
   *
   * <p>Executor を Bean にすると Spring Boot 標準の applicationTaskExecutor が無効になるため、ApiCaller の内部に閉じ込める.
   */
  @Bean
  public ApiCaller apiCaller(ApiCallProperties properties) {
    SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("api-call-");
    executor.setVirtualThreads(true);
    return new ApiCaller(executor, properties);
  }
}
