package io.github.orczykowski.logback.obfuscator;

import ch.qos.logback.classic.LoggerContext;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaskSensitiveDataLayoutTest {
    private static final Set<String> SENSITIVE_FIELDS = Set.of("firstName", "idCardNumber", "mobilePhone", "other");

    MaskSensitiveDataLayout subject;

    @BeforeEach
    void setup() {
        subject = new MaskSensitiveDataLayout();
        subject.setPattern("%m");
        subject.setContext(new LoggerContext());
        subject.start();
    }

    @Test
    void shouldThrowExceptionWhenTrySetMaskAsNull() {
        //given

        //expect:
        Assertions.assertThrows(IncorrectConfigurationException.class, () -> subject.addMask(null));
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
        var computedMaskedLog = (String) subject.doLayout(logEvent);

        // then:
        var expectedLogWithMaskedSensitiveData = """
                This is log with sensitive data: 
                firstName=[********], 
                idCardNumber=[********]
                mobilePhone=[********]
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
        var computedMaskedLog = (String) subject.doLayout(logEvent);

        // then:
        var expectedLogWithMaskedSensitiveData = """
                This is log with sensitive data: 
                firstName=(********)
                idCardNumber=(********)
                mobilePhone=(********)
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
        var computedMaskedLog = (String) subject.doLayout(logEvent);

        // then:
        var expectedLogWithMaskedSensitiveData = """
                This is log with sensitive data: 
                firstName="********"
                idCardNumber="********"
                mobilePhone="********"
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
        var computedMaskedLog = (String) subject.doLayout(logEvent);

        // then:
        var expectedLogWithMaskedSensitiveData = asJson(Map.of("firstName", "********", "idCardNumber", "********", "mobilePhone", "********", "nonSensitive", "test"));
        assertEquals(expectedLogWithMaskedSensitiveData, computedMaskedLog);
    }

    @Test
    void shouldUseCustomMask() {
        // given:
        var customMask = "xxxxxx";

        subject.addMask(customMask);
        subject.addPatternName(SensitiveDataPatternFactory.SensitiveValuePatterns.JSON.name());
        addSensitiveFields();
        var log = asJson(Map.of("firstName", "Gustaw"));
        var logEvent = ILoggingEventFactory.from(log);
        // when:
        var computedMaskedLog = (String) subject.doLayout(logEvent);

        // then:
        var expectedLogWithMaskedSensitiveData = asJson(Map.of("firstName", customMask));
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
        var computedMaskedLog = (String) subject.doLayout(logEvent);

        // then:
        var expectedLogWithMaskedSensitiveData = asJson(Map.of("personalData", Map.of("firstName", "********", "idCardNumber", "********"), "contactInfo", Map.of("mobilePhone", "********"), "nonSensitive", "test"));
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
        var computedMaskedLog = (String) subject.doLayout(logEvent);

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
        var computedMaskedLog = (String) subject.doLayout(logEvent);

        // then:
        var expectedLogWithMaskedSensitiveData = """
                This is log with sensitive data: 
                firstName==>'********'
                idCardNumber==>'********'
                mobilePhone==>'********'
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
        var computedMaskedLog = (String) subject.doLayout(logEvent);

        // then:
        var expectedLogWithMaskedSensitiveData = """
                payload={"firstName":"********"}
                mobilePhone=(********)
                description="something"
                other=[********]""";
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
        var result = (String) subject.doLayout(logEvent);

        // then:
        assertEquals("firstName=[********]", result);
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
        var result = (String) subject.doLayout(logEvent);

        // then:
        assertEquals("firstName=[********]", result);
    }

    private void addSensitiveFields() {
        SENSITIVE_FIELDS.forEach(subject::addFieldName);
    }

    protected static String asJson(Map<String, Object> map) {
        return new JSONObject(map).toString();
    }
}