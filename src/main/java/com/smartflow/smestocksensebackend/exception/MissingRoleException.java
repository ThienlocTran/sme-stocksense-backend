package com.smartflow.smestocksensebackend.exception;

public class MissingRoleException extends RuntimeException {

    public MissingRoleException() {
        super("Tai khoan chua duoc gan vai tro.");
    }

    public MissingRoleException(String message) {
        super(message);
    }
}
