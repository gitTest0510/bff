package com.example.bff.exception;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
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

    /**
     * 失敗の原因がタイムアウトかどうか.
     *
     * <p>ApiCaller のタイムアウト（{@link TimeoutException}）に加え、HTTP クライアントの接続・読み取りタイムアウトも対象にする. HTTP
     * クライアントの例外は ResourceAccessException 等に包まれるため、原因をたどって判定する.
     */
    public boolean isTimeout() {
        for (Throwable cause = getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof TimeoutException
                    || cause instanceof HttpTimeoutException
                    || cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
