package io.github.orczykowski.logback.obfuscator;

public class IncorrectConfigurationException extends SensitiveDataObfuscatorException {
    IncorrectConfigurationException(final String msg) {
        super(msg);
    }
}
