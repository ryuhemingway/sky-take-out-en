package com.sky.takeout.common;

public record ApiResponse<T>(Integer code, String msg, T data) {
    public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(1, "success", data); }
    public static ApiResponse<Void> ok() { return new ApiResponse<>(1, "success", null); }
    public static <T> ApiResponse<T> fail(String msg) { return new ApiResponse<>(0, msg, null); }
}
