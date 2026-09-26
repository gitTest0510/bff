package com.example.bff.exception;

import java.util.concurrent.TimeoutException;
import lombok.Getter;

/** レスポンスの組み立てに欠かせない外部APIの結果が、呼び出しの失敗により得られなかったことを表す例外. */
@Getter
public class ExternalApiException extends RuntimeException {

    private final String apiName;

    public ExternalApiException(String apiName, Throwable cause) {
        super("外部API(" + apiName + ")の呼び出しに失敗しました", cause);
        this.apiName = apiName;
    }

    public boolean isTimeout() {
        return getCause() instanceof TimeoutException;
    }
}
