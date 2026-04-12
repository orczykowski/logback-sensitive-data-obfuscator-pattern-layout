# Logback Sensitive Data Obfuscator Pattern Layout

A lightweight library for masking **Personally Identifiable Information (PII)** and other sensitive data in application logs. Built as a Logback layout extension that works with standard pattern-based log output.

## Why You Need This

Logging sensitive data such as names, email addresses, phone numbers, ID card numbers, or authentication tokens is a common source of data leaks. Even in internal systems, unmasked PII in logs can lead to:

- **Regulatory violations** (GDPR/RODO, CCPA, HIPAA) with significant financial penalties
- **Security incidents** when logs are accessed by unauthorized parties or forwarded to third-party log aggregators
- **Privacy breaches** during debugging or incident response when logs are shared across teams

This library provides a drop-in Logback layout that intercepts log messages and masks sensitive field values before they are written, ensuring PII never reaches your log storage.

## Features

- Mask sensitive data in **JSON**, **key-value**, and **custom log formats**
- Two masking strategies: **full mask** or **shortcut** (preserves data shape for debugging)
- Built-in protection against **ReDoS attacks** with configurable regex timeout
- **Zero dependencies** beyond Logback Classic
- Fully configurable via standard Logback XML

## Requirements

- Java 25 or higher
- Logback Classic 1.5.x or higher

## Installation

### Maven

```xml
<dependency>
    <groupId>io.github.orczykowski</groupId>
    <artifactId>logback-sensitive-data-obfuscator-pattern-layout</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.orczykowski:logback-sensitive-data-obfuscator-pattern-layout:2.0.0'
```

## Masking Strategies

### MaskSensitiveDataLayout (Full Mask)

Replaces the entire sensitive value with a fixed mask string (default: `********`).

| Input                                          | Output                                          |
|------------------------------------------------|-------------------------------------------------|
| `email=[john.doe@example.com]`                 | `email=[********]`                               |
| `{"ssn":"123-45-6789"}`                        | `{"ssn":"********"}`                             |
| `firstName="Jane"`                             | `firstName="********"`                           |

Best for: **production environments** where no trace of PII should remain in logs.

### MaskSensitiveDataAsShortcutLayout (Shortcut Mask)

Replaces the sensitive value with a compact fingerprint in the format: `FIRST_CHAR-LENGTH-LAST_CHAR`.

| Input                                          | Output                                          |
|------------------------------------------------|-------------------------------------------------|
| `email=[john.doe@example.com]`                 | `email=[j-20-m]`                                 |
| `firstName=[Jane]`                             | `firstName=[J-4-e]`                              |
| `idCardNumber=[CC123456]`                      | `idCardNumber=[C-8-6]`                           |

Best for: **staging/debugging** where you need to correlate data across logs or verify value shapes without exposing actual PII.

## Quick Start

Add the layout to your `logback.xml`:

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="io.github.orczykowski.logback.obfuscator.MaskSensitiveDataLayout">
                <!-- Regex timeout protection (optional, default: 500ms) -->
                <regexTimeoutMillis>500</regexTimeoutMillis>

                <!-- Define detection patterns (must come before field names) -->
                <patternName>JSON</patternName>
                <patternName>EQUAL_AND_SQUARE_BRACKETS</patternName>

                <!-- Define which fields contain PII -->
                <fieldName>email</fieldName>
                <fieldName>firstName</fieldName>
                <fieldName>lastName</fieldName>
                <fieldName>ssn</fieldName>
                <fieldName>phoneNumber</fieldName>
                <fieldName>creditCardNumber</fieldName>

                <!-- Log pattern -->
                <pattern>%d{yyyy-MM-dd HH:mm:ss} %-5level %logger{36} - %msg%n</pattern>
            </layout>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

### Example: Masking PII in JSON Logs

Given the log statement:

```java
log.info("User registered: {}", new JSONObject(Map.of(
    "email", "john.doe@example.com",
    "firstName", "John",
    "lastName", "Doe",
    "role", "admin"
)));
```

**Output with `MaskSensitiveDataLayout`:**
```
User registered: {"email":"********","firstName":"********","lastName":"********","role":"admin"}
```

**Output with `MaskSensitiveDataAsShortcutLayout`:**
```
User registered: {"email":"j-20-m","firstName":"J-4-n","lastName":"D-3-e","role":"admin"}
```

### Example: Masking PII in Key-Value Logs

```java
log.info("Processing payment: userId=[{}] creditCardNumber=[{}] amount=[{}]",
    userId, creditCardNumber, amount);
```

**Output:**
```
Processing payment: userId=[********] creditCardNumber=[********] amount=[100.00]
```

Note: `amount` is not masked because it was not declared as a sensitive field.

### Example: Custom Mask String

```xml
<layout class="io.github.orczykowski.logback.obfuscator.MaskSensitiveDataLayout">
    <mask>***REDACTED***</mask>
    <patternName>JSON</patternName>
    <fieldName>email</fieldName>
    <pattern>%msg%n</pattern>
</layout>
```

### Example: Custom Detection Pattern

If your logs use a non-standard format, define a custom regex pattern:

```xml
<layout class="io.github.orczykowski.logback.obfuscator.MaskSensitiveDataLayout">
    <customPattern>[PROPERTY_NAME]->'([^']+)'</customPattern>
    <fieldName>email</fieldName>
    <pattern>%msg%n</pattern>
</layout>
```

This will mask values in logs like `email->'john@example.com'` into `email->'********'`.

The `[PROPERTY_NAME]` placeholder is replaced with each declared field name. The sensitive value must be captured in a regex group (parentheses).

## Configuration Reference

| Option                    | Description                                                                                                                                                                                                                           | Required |
|---------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| `<patternName>`           | Name of a built-in detection pattern. Must be declared **before** any `<fieldName>`. Multiple patterns can be added.                                                                                                                  | Yes*     |
| `<customPattern>`         | A custom regex pattern. Must contain `[PROPERTY_NAME]` placeholder and a capture group for the sensitive value. Follows Java regex syntax.                                                                                            | Yes*     |
| `<fieldName>`             | Name of a field/property/variable that contains PII or sensitive data.                                                                                                                                                                | Yes      |
| `<mask>`                  | Custom mask string (default: `********`). Only applicable to `MaskSensitiveDataLayout`.                                                                                                                                               | No       |
| `<regexTimeoutMillis>`    | Maximum time in milliseconds for regex evaluation per log message (default: `500`). Protects against catastrophic backtracking (ReDoS). Throws `RegexProcessingTimeoutException` if exceeded. Set to `-1` to disable timeout.           | No       |

*At least one `<patternName>` or `<customPattern>` is required.

### Built-in Detection Patterns

| Pattern Name                   | Log Format                            | Example                           |
|--------------------------------|---------------------------------------|-----------------------------------|
| `JSON`                         | `"fieldName":"value"`                 | `"email":"john@example.com"`      |
| `EQUAL_AND_SQUARE_BRACKETS`    | `fieldName=[value]`                   | `email=[john@example.com]`        |
| `EQUAL_AND_BRACKETS`           | `fieldName=(value)`                   | `email=(john@example.com)`        |
| `EQUAL_AND_DOUBLE_QUOTES`      | `fieldName="value"`                   | `email="john@example.com"`        |

## ReDoS Protection

Regular expressions used for pattern matching are protected against **Regular Expression Denial of Service (ReDoS)** attacks. Each regex evaluation is guarded by a configurable timeout (default: 500ms). If a regex takes longer than the allowed time (e.g., due to catastrophic backtracking on malicious input), a `RegexProcessingTimeoutException` is thrown, preventing the regex engine from hanging your application.

```xml
<!-- Increase timeout for complex log messages -->
<regexTimeoutMillis>1000</regexTimeoutMillis>

<!-- Or disable timeout entirely (not recommended for production) -->
<regexTimeoutMillis>-1</regexTimeoutMillis>
```

## Common PII Fields to Protect

Here is a non-exhaustive list of fields you should consider masking:

| Category           | Field Names                                                        |
|--------------------|--------------------------------------------------------------------|
| **Identity**       | `firstName`, `lastName`, `fullName`, `dateOfBirth`, `ssn`, `pesel` |
| **Contact**        | `email`, `phoneNumber`, `mobilePhone`, `address`                   |
| **Financial**      | `creditCardNumber`, `iban`, `accountNumber`, `cvv`                 |
| **Authentication** | `password`, `token`, `apiKey`, `sessionId`, `secret`               |
| **Documents**      | `idCardNumber`, `passportNumber`, `driverLicense`                  |

## How to Contribute

### Verify

- Run tests: `mvn test`
- Run mutation tests: `mvn test-compile org.pitest:pitest-maven:mutationCoverage`
- Build: `mvn install`

### Publish

1. Ensure you have configured the `central` server credentials in your `~/.m2/settings.xml`:

```xml
<server>
    <id>central</id>
    <username><!-- your Central Portal token username --></username>
    <password><!-- your Central Portal token password --></password>
</server>
```

2. Run: `mvn clean deploy`

This uses the `central-publishing-maven-plugin` to publish directly to Maven Central.

## Support

If you like this library and it helps you in your projects, I would really appreciate your support.

Maintaining open source takes time, and your support helps keep this project alive and improving.

<a href="https://buymeacoffee.com/tasior" target="_blank">
  <img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" width="217" height="60">
</a>

### [MIT License](https://opensource.org/licenses/MIT)
