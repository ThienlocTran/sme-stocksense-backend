package com.smartflow.smestocksensebackend.exception;

public class AccountInactiveException extends RuntimeException {

    public AccountInactiveException() {
        super("Tài khoản không hoạt động hoặc đã bị khóa.");
    }
}
