package com.example.bff.controller;

import com.example.bff.controller.request.Request;
import com.example.bff.controller.response.Response;
import com.example.bff.service.MyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExampleController.class)
class ExampleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MyService service;

    @Test
    void クエリパラメータを受け取りレスポンスをJSONで返却する() throws Exception {
        Response response = Response.builder()
            .apiResponse1("res1")
            .apiResponse2(Response.Section.builder()
                .summary("summary")
                .main(List.of())
                .sub(List.of())
                .other(List.of())
                .build())
            .build();
        when(service.execute(any(Request.class))).thenReturn(response);

        mockMvc.perform(get("/example").param("no", "001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.['Apiレスポンス1']").value("res1"))
            .andExpect(jsonPath("$.['Apiレスポンス2'].summary").value("summary"));

        verify(service).execute(argThat(request -> "001".equals(request.getNo())));
    }

    @Test
    void noが未指定の場合は400をProblemDetail形式で返却する() throws Exception {
        mockMvc.perform(get("/example"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.status").value(400));

        verify(service, never()).execute(any());
    }

    @Test
    void 予期しない例外の場合は500をProblemDetail形式で返却する() throws Exception {
        when(service.execute(any(Request.class))).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(get("/example").param("no", "001"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.detail").value("予期しないエラーが発生しました"));
    }
}
