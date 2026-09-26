package com.example.bff.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bff.controller.request.Request;
import com.example.bff.controller.response.Response;
import com.example.bff.exception.ExternalApiException;
import com.example.bff.integration.request.ApiRequest3;
import com.example.bff.integration.response.ApiResponse1;
import com.example.bff.integration.response.ApiResponse2;
import com.example.bff.integration.response.ApiResponse3;
import com.example.bff.mapper.ExampleMapper;
import com.example.bff.orchestration.ApiCallProperties;
import com.example.bff.orchestration.ApiCaller;
import com.example.bff.testsupport.JsonFixtures;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MyServiceTest {

  @Mock private Api1Client api1Client;
  @Mock private Api2Client api2Client;
  @Mock private Api3Client api3Client;

  private MyService service;

  @BeforeEach
  void setUp() {
    ApiCaller apiCaller =
        new ApiCaller(
            Executors.newVirtualThreadPerTaskExecutor(),
            new ApiCallProperties(Duration.ofSeconds(1), 10));
    service = new MyService(api1Client, api2Client, api3Client, new ExampleMapper(), apiCaller);
  }

  @Test
  void 明細名ごとに外部API_3を1回だけ呼び出し明細に紐付ける() {
    when(api1Client.execute1(any()))
        .thenReturn(JsonFixtures.load("api-response1/normal.json", ApiResponse1.class));
    // 明細: main(1), sub(2), foo(3), main(4), bar(5)
    when(api2Client.execute2(any()))
        .thenReturn(JsonFixtures.load("api-response2/normal.json", ApiResponse2.class));
    // 外部API_3 はリクエストの明細名ごとに異なる結果を返す
    when(api3Client.execute3(any(ApiRequest3.class)))
        .thenAnswer(
            invocation -> {
              ApiRequest3 req = invocation.getArgument(0);
              return new ApiResponse3(
                  "title-" + req.getName(), new ApiResponse3.MyDetail3("a", "b", "c"));
            });

    Response response = service.execute(request("001"));

    verify(api1Client).execute1(argThat(req -> "001".equals(req.getId())));
    verify(api2Client).execute2(argThat(req -> "001".equals(req.getId())));
    // main が2件あっても外部API_3の呼び出しは明細名の種類数（main, sub, foo, bar）の4回
    verify(api3Client, times(4)).execute3(any());
    assertThat(response.getApiResponse1()).isEqualTo("res1");
    assertThat(response.getApiResponse2().getMain())
        .hasSize(2)
        .allSatisfy(
            item -> assertThat(item.getDetails().getFirst().getName()).isEqualTo("title-main"));
    assertThat(response.getApiResponse2().getSub().getFirst().getDetails().getFirst().getName())
        .isEqualTo("title-sub");
  }

  @Test
  void 外部API_1が失敗しても残りの結果でレスポンスを返す() {
    when(api1Client.execute1(any())).thenThrow(new IllegalStateException("down"));
    when(api2Client.execute2(any()))
        .thenReturn(
            new ApiResponse2("summary", List.of(new ApiResponse2.MyDetail("main", 1, "memo"))));

    Response response = service.execute(request("001"));

    assertThat(response.getApiResponse1()).isNull();
    assertThat(response.getApiResponse2().getSummary()).isEqualTo("summary");
    assertThat(response.getApiResponse2().getMain()).hasSize(1);
  }

  @Test
  void 外部API_2が失敗した場合は外部API_3を呼ばずにExternalApiExceptionを送出する() {
    when(api2Client.execute2(any())).thenThrow(new IllegalStateException("down"));

    assertThatThrownBy(() -> service.execute(request("001")))
        .isInstanceOfSatisfying(
            ExternalApiException.class, ex -> assertThat(ex.getApiName()).isEqualTo("API_2"));
    verify(api3Client, never()).execute3(any());
  }

  @Test
  void 外部API_3が一部失敗した場合はその明細のdetailsだけ空にする() {
    when(api2Client.execute2(any()))
        .thenReturn(
            new ApiResponse2(
                "summary",
                List.of(
                    new ApiResponse2.MyDetail("main", 1, "memo1"),
                    new ApiResponse2.MyDetail("sub", 2, "memo2"))));
    when(api3Client.execute3(argThat(req -> req != null && "main".equals(req.getName()))))
        .thenThrow(new IllegalStateException("down"));
    when(api3Client.execute3(argThat(req -> req != null && "sub".equals(req.getName()))))
        .thenReturn(new ApiResponse3("title-sub", new ApiResponse3.MyDetail3("a", "b", "c")));

    Response response = service.execute(request("001"));

    assertThat(response.getApiResponse2().getMain().getFirst().getDetails()).isEmpty();
    assertThat(response.getApiResponse2().getSub().getFirst().getDetails()).hasSize(1);
  }

  @Test
  void null要素や名前のない明細では外部API_3を呼び出さない() {
    when(api2Client.execute2(any()))
        .thenReturn(
            new ApiResponse2(
                "summary", Arrays.asList(null, new ApiResponse2.MyDetail(null, 1, "memo"))));

    Response response = service.execute(request("001"));

    verify(api3Client, never()).execute3(any());
    assertThat(response.getApiResponse2().getMain()).isEmpty();
    assertThat(response.getApiResponse2().getSub()).isEmpty();
    assertThat(response.getApiResponse2().getOther()).isEmpty();
  }

  @Test
  void 外部API_2の結果がnullでも例外にならない() {
    when(api2Client.execute2(any())).thenReturn(null);

    Response response = service.execute(request("001"));

    verify(api3Client, never()).execute3(any());
    assertThat(response.getApiResponse2().getSummary()).isNull();
  }

  @Test
  void 外部API_2の明細がnullでも例外にならない() {
    when(api2Client.execute2(any())).thenReturn(new ApiResponse2("summary", null));

    Response response = service.execute(request("001"));

    verify(api3Client, never()).execute3(any());
    assertThat(response.getApiResponse2().getSummary()).isEqualTo("summary");
  }

  private static Request request(String no) {
    return Request.builder().no(no).build();
  }
}
