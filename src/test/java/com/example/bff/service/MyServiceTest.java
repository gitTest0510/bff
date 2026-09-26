package com.example.bff.service;

import com.example.bff.controller.request.Request;
import com.example.bff.controller.response.Response;
import com.example.bff.integration.request.ApiRequest3;
import com.example.bff.integration.response.ApiResponse1;
import com.example.bff.integration.response.ApiResponse2;
import com.example.bff.integration.response.ApiResponse3;
import com.example.bff.mapper.ExampleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyServiceTest {

    @Mock
    private Client1 client1;
    @Mock
    private Client2 client2;
    @Mock
    private Client3 client3;

    private MyService service;

    @BeforeEach
    void setUp() {
        service = new MyService(client1, client2, client3, new ExampleMapper());
    }

    @Test
    void 外部API_2の明細ごとに外部API_3を呼び出して結果を紐付ける() {
        when(client1.execute1(any())).thenReturn(new ApiResponse1("res1"));
        when(client2.execute2(any())).thenReturn(new ApiResponse2("summary", List.of(
            new ApiResponse2.MyDetail("main", 1, "memo1"),
            new ApiResponse2.MyDetail("sub", 2, "memo2"))));
        when(client3.execute3(any(ApiRequest3.class))).thenAnswer(invocation -> {
            ApiRequest3 req = invocation.getArgument(0);
            return new ApiResponse3("title-" + req.getName(), new ApiResponse3.MyDetail3("a", "b", "c"));
        });

        Response response = service.execute(request("001"));

        verify(client1).execute1(argThat(req -> "001".equals(req.getId())));
        verify(client2).execute2(argThat(req -> "001".equals(req.getId())));
        verify(client3, times(2)).execute3(any());
        assertThat(response.getApiResponse1()).isEqualTo("res1");
        assertThat(response.getApiResponse2().getMain().getFirst().getDetails().getFirst().getName())
            .isEqualTo("title-main");
        assertThat(response.getApiResponse2().getSub().getFirst().getDetails().getFirst().getName())
            .isEqualTo("title-sub");
    }

    @Test
    void null要素や名前のない明細では外部API_3を呼び出さない() {
        when(client2.execute2(any())).thenReturn(new ApiResponse2("summary", Arrays.asList(
            null,
            new ApiResponse2.MyDetail(null, 1, "memo"))));

        Response response = service.execute(request("001"));

        verify(client3, never()).execute3(any());
        assertThat(response.getApiResponse2().getMain()).isEmpty();
        assertThat(response.getApiResponse2().getSub()).isEmpty();
        assertThat(response.getApiResponse2().getOther()).isEmpty();
    }

    @Test
    void 外部API_2の結果がnullでも例外にならない() {
        when(client2.execute2(any())).thenReturn(null);

        Response response = service.execute(request("001"));

        verify(client3, never()).execute3(any());
        assertThat(response.getApiResponse2().getSummary()).isNull();
    }

    @Test
    void 外部API_2の明細がnullでも例外にならない() {
        when(client2.execute2(any())).thenReturn(new ApiResponse2("summary", null));

        Response response = service.execute(request("001"));

        verify(client3, never()).execute3(any());
        assertThat(response.getApiResponse2().getSummary()).isEqualTo("summary");
    }

    private static Request request(String no) {
        Request request = new Request();
        request.setNo(no);
        return request;
    }
}
