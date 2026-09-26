package com.example.bff.controller.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class RequestTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void noが空の場合はバリデーションエラーになる() {
    Set<ConstraintViolation<Request>> violations =
        validator.validate(Request.builder().no(" ").build());

    assertThat(violations)
        .singleElement()
        .satisfies(v -> assertThat(v.getPropertyPath()).hasToString("no"));
  }

  @Test
  void noが指定されていればバリデーションエラーにならない() {
    assertThat(validator.validate(Request.builder().no("001").build())).isEmpty();
  }

  @Test
  void JSONボディからJacksonで生成できる() {
    Request request = JsonMapper.builder().build().readValue("{\"no\":\"001\"}", Request.class);

    assertThat(request.getNo()).isEqualTo("001");
  }
}
