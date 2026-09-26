package com.example.bff;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BffApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void スタブの外部APIを通して最終レスポンスを返却できる() throws Exception {
        mockMvc.perform(get("/example").param("no", "001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.['Apiレスポンス1']").value("ApiResponse1"))
            .andExpect(jsonPath("$.['Apiレスポンス2'].summary").value("ApiResponse2"))
            .andExpect(jsonPath("$.['Apiレスポンス2'].main.length()").value(3))
            .andExpect(jsonPath("$.['Apiレスポンス2'].sub.length()").value(1))
            .andExpect(jsonPath("$.['Apiレスポンス2'].other.length()").value(2))
            .andExpect(jsonPath("$.['Apiレスポンス2'].main[0].details[0].name").value("title3"));
    }
}
