package org.project.exception;

public class UnsupportedCurrencyException extends RuntimeException{
    public UnsupportedCurrencyException(String currency) {
        super("Unsupported or unknown target currency: " + currency);
    }
}
