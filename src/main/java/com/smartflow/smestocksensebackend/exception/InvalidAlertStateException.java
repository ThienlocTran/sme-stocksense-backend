package com.smartflow.smestocksensebackend.exception;

public class InvalidAlertStateException extends BadRequestException {
    public InvalidAlertStateException(String message) {
        super(message);
    }
}
