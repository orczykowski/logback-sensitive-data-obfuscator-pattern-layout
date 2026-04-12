package io.github.orczykowski.logback.obfuscator;

public class SensitiveDataObfuscatorException extends RuntimeException {

    SensitiveDataObfuscatorException(final String message) {
        super(message);
    }
}
