package com.smartflow.smestocksensebackend.exception;

public class MissingRoleException extends RuntimeException {

    public MissingRoleException() {
        super("Tài khoản chưa được gán vai trò.");
    }
}
