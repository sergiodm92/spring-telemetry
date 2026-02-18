package dev.springtelescope;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TelescopeApiResponse<T> {
    private final boolean success;
    private final String message;
    private final T data;

    public static <T> TelescopeApiResponse<T> success(String message, T data) {
        return TelescopeApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> TelescopeApiResponse<T> error(String message, T data) {
        return TelescopeApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> TelescopeApiResponse<T> success(String message) {
        return TelescopeApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(null)
                .build();
    }

    public static <T> TelescopeApiResponse<T> error(String message) {
        return TelescopeApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}
