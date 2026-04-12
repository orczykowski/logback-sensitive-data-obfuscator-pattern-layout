package io.github.orczykowski.logback.obfuscator;

import ch.qos.logback.classic.LoggerContext;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaskSensitiveDataAsShortcutLayoutTest {
    private static final Set<String> SENSITIVE_FIELDS = Set.of("firstName", "idCardNumber", "mobilePhone", "other");

    MaskSensitiveDataAsShortcutLayout subject;

    @BeforeEach
    void setup() {
        subject = new MaskSensitiveDataAsShortcutLayout();
        subject.setPattern("%m");
        subject.setContext(new LoggerContext());
        subject.start();
    }

    @Test
    void shouldMaskSensitiveDataWhenValuesAreInSquareBrackets() {
        // given:
        subject.addPatternName(SensitiveDataPatternFactory.SensitiveValuePatterns.EQUAL_AND_SQUARE_BRACKETS.name());
        addSensitiveFields();

        var log = """
                This is log with sensitive data: 
                firstName=[Gustaw], 
                idCardNumber=[CC123456]
                mobilePhone=[+48123123123]
                description=[something]
                other=(sth)""";

        var logEvent = ILoggingEventFactory.from(log);
        // when:
        var computedMaskedLog = subject.doLayout(logEvent);

        // then:
        var expectedLogWithMaskedSensitiveData = """
                This is log with sensitive data: 
                firstName=[G-6-w], 
                idCardNumber=[C-8-6]
                mobilePhone=[+-12-3]
                description=[something]
                other=(sth)""";
        assertEquals(expectedLogWithMaskedSensitiveData, computedMaskedLog);
    }

    @Test
    void shouldMaskSensitiveDataWhenValuesAreInNormalBrackets() {
        // given:
        subject.addPatternName(SensitiveDataPatternFactory.SensitiveValuePatterns.EQUAL_AND_BRACKETS.name());
        addSensitiveFields();
        var log = """
                This is log with sensitive data: 
                firstName=(Gustaw) 
                idCardNumber=(CC123456)
                mobilePhone=(+48123123123)
                description=(something)
                other=[sth]""";

        var logEvent = ILoggingEventFactory.from(log);
        // when:
        var computedMaskedLog = subject.doLayout(logEvent);

        // then:
        var expectedLogWithMaskedSensitiveData = """
                This is log with sensitive data: 
                firstName=(G-6-w)
                idCardNumber=(C-8-6)
                mobilePhone=(+-12-3)
                description=(something)
                other=[sth]""";
        assertEquals(expectedLogWithMaskedSensitiveData, computedMaskedLog);
    }

    @Test
    void shouldMaskSensitiveDataWhenValuesAreInDoubleQuotes() {
        // given:
        subject.addPatternName(SensitiveDataPatternFactory.SensitiveValuePatterns.EQUAL_AND_DOUBLE_QUOTES.name());
        addSensitiveFields();
        var log = """
                This is log with sensitive data: 
                firstName="Gustaw" 
                idCardNumber="CC123456"
                mobilePhone="+48123123123"
                description="something"
                other=[sth]""";

        var logEvent = ILoggingEventFactory.from(log);
        // when:
        var computedMaskedLog = subject.doLayout(logEvent);

        // then:
        var expectedLogWithMaskedSensitiveData = """
                This is log with sensitive data: 
                firstName="G-6-w"
                idCardNumber="C-8-6"
                mobilePhone="+-12-3"
                description="something"
                other=[sth]""";
        assertEquals(expectedLogWithMaskedSensitiveData, computedMaskedLog);
    }

    @Test
    void shouldMaskSensitiveDataValuesInJson() {
        // given:
        subject.addPatternName(SensitiveDataPatternFactory.SensitiveValuePatterns.JSON.name());
        addSensitiveFields();
        var log = asJson(Map.of("firstName", "Gustaw", "idCardNumber", "CC123456", "mobilePhone", "+48123123123", "nonSensitive", "test"));

        var logEvent = ILoggingEventFactory.from(log);
        // when:
        var computedMaskedLog = subject.doLayout(logEvent);

        // then:
        var expectedLogWithMaskedSensitiveData = asJson(Map.of("firstName", "G-6-w", "idCardNumber", "C-8-6", "mobilePhone", "+-12-3", "nonSensitive", "test"));
        assertEquals(expectedLogWithMaskedSensitiveData, computedMaskedLog);
    }

    @Test
    void shouldMaskNestedSensitiveDataValuesInJson() {
        // given:
        subject.addPatternName(SensitiveDataPatternFactory.SensitiveValuePatterns.JSON.name());
        addSensitiveFields();
        var log = asJson(Map.of("personalData", Map.of("firstName", "Gustaw", "idCardNumber", "CC123456"), "contactInfo", Map.of("mobilePhone", "+48123123123"), "nonSensitive", "test"));

        var logEvent = ILoggingEventFactory.from(log);
        // when:
        var computedMaskedLog = subject.doLayout(logEvent);

        // then:
        var expectedLogWithMaskedSensitiveData = asJson(Map.of("personalData", Map.of("firstName", "G-6-w", "idCardNumber", "C-8-6"), "contactInfo", Map.of("mobilePhone", "+-12-3"), "nonSensitive", "test"));
        assertEquals(expectedLogWithMaskedSensitiveData, computedMaskedLog);
    }

    @Test
    void shouldIgnoreValueWhenHasNoDecoration() {
        // given:
        subject.addPatternName(SensitiveDataPatternFactory.SensitiveValuePatterns.EQUAL_AND_SQUARE_BRACKETS.name());
        addSensitiveFields();

        var log = """
                This is some log:
                firstName=(Gustaw), 
                idCardNumber=CC123456""";
        var logEvent = ILoggingEventFactory.from(log);
        // when:
        var computedMaskedLog = subject.doLayout(logEvent);

        // then:
        assertEquals(log, computedMaskedLog);
    }

    @Test
    void shouldMaskDataUsingCustomPattern() {
        // given:
        subject.addCustomPattern("[PROPERTY_NAME]==>'([^']+)'");
        addSensitiveFields();

        var log = """
                This is log with sensitive data: 
                firstName==>'Gustaw'
                idCardNumber==>'CC123456'
                mobilePhone==>'+48123123123'
                description==>'something'
                other=[sth]""";

        var logEvent = ILoggingEventFactory.from(log);
        // when:
        var computedMaskedLog = subject.doLayout(logEvent);

        // then:
        var expectedLogWithMaskedSensitiveData = """
                This is log with sensitive data: 
                firstName==>'G-6-w'
                idCardNumber==>'C-8-6'
                mobilePhone==>'+-12-3'
                description==>'something'
                other=[sth]""";
        assertEquals(expectedLogWithMaskedSensitiveData, computedMaskedLog);
    }

    @Test
    void shouldMaskSensitiveDataWithAllAddedPatterns() {
        subject.addPatternName(SensitiveDataPatternFactory.SensitiveValuePatterns.JSON.name());
        subject.addPatternName(SensitiveDataPatternFactory.SensitiveValuePatterns.EQUAL_AND_SQUARE_BRACKETS.name());
        subject.addPatternName(SensitiveDataPatternFactory.SensitiveValuePatterns.EQUAL_AND_BRACKETS.name());
        addSensitiveFields();

        //and:
        var log = """
                payload={"firstName":"Gustaw"}
                mobilePhone=(+-12-3)
                description="something"
                other=[s-3-h]""";

        var logEvent = ILoggingEventFactory.from(log);
        // when:
        var computedMaskedLog = subject.doLayout(logEvent);

        // then:
        var expectedLogWithMaskedSensitiveData = """
                payload={"firstName":"G-6-w"}
                mobilePhone=(+-6-3)
                description="something"
                other=[s-5-h]""";
        assertEquals(expectedLogWithMaskedSensitiveData, computedMaskedLog);
    }

    @ParameterizedTest
    @EmptySource
    void shouldIgnoreEmptyString(String str) {
        //given:
        subject.addPatternName(SensitiveDataPatternFactory.SensitiveValuePatterns.EQUAL_AND_SQUARE_BRACKETS.name());
        addSensitiveFields();

        var logEvent = ILoggingEventFactory.from(str);
        //when:
        var result = subject.doLayout(logEvent);

        //then:
        assertEquals(str, result);
    }

    @Test
    void shouldNullStringForNullObjects() {
        //given:
        subject.addPatternName(SensitiveDataPatternFactory.SensitiveValuePatterns.EQUAL_AND_SQUARE_BRACKETS.name());
        addSensitiveFields();

        var logEvent = ILoggingEventFactory.from(null);
        //when:
        var result = subject.doLayout(logEvent);

        //then:
        assertEquals("null", result);
    }

    @Test
    void shouldMaskSuccessfullyWithCustomTimeout() {
        // given:
        subject.setRegexTimeoutMillis(2000);
        subject.addPatternName(SensitiveDataPatternFactory.SensitiveValuePatterns.EQUAL_AND_SQUARE_BRACKETS.name());
        addSensitiveFields();

        var log = "firstName=[Gustaw]";
        var logEvent = ILoggingEventFactory.from(log);

        // when:
        var result = subject.doLayout(logEvent);

        // then:
        assertEquals("firstName=[G-6-w]", result);
    }

    @Test
    void shouldMaskSuccessfullyWithDisabledTimeout() {
        // given:
        subject.setRegexTimeoutMillis(-1);
        subject.addPatternName(SensitiveDataPatternFactory.SensitiveValuePatterns.EQUAL_AND_SQUARE_BRACKETS.name());
        addSensitiveFields();

        var log = "firstName=[Gustaw]";
        var logEvent = ILoggingEventFactory.from(log);

        // when:
        var result = subject.doLayout(logEvent);

        // then:
        assertEquals("firstName=[G-6-w]", result);
    }

    private void addSensitiveFields() {
        SENSITIVE_FIELDS.forEach(subject::addFieldName);
    }

    protected static String asJson(Map<String, Object> map) {
        return new JSONObject(map).toString();
    }
}