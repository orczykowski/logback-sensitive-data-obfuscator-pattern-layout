package io.github.orczykowski.logback.obfuscator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class AbstractSensitiveDataObfuscatorLayoutTest {

    TestImplementationSensitiveDataTextDecoratorTest subject;

    @BeforeEach
    void init() {
        subject = new TestImplementationSensitiveDataTextDecoratorTest();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowExceptionWhenTrySetCustomPatternAsNullOrBlank(final String prefix) {
        //given:
        final var expectedMessage = """
                Pattern have to be complies with java regexp and have to contains place holder
                [PROPERTY_NAME] where in log is sensitive value. The sensitive value must be a group in the sense of regular
                expressions, it have to be  surrounded by parentheses""";
        //expect:
        var ex = assertThrows(IncorrectConfigurationException.class, () -> subject.addCustomPattern(prefix));
        assertEquals(expectedMessage, ex.getMessage());
    }


    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"PROPERTY_NAME", "NAME", "[NAME]", "[PROPERTYNAME]", "[]"})
    void shouldThrowExceptionWhenPlaceholderIsIncorrect(final String incorrectPlaceHolder) {
        //given:
        final var expectedMessage = """
                Pattern have to be complies with java regexp and have to contains place holder
                [PROPERTY_NAME] where in log is sensitive value. The sensitive value must be a group in the sense of regular 
                expressions, it have to be  surrounded by parentheses""";
        //expect:
        var ex = assertThrows(IncorrectConfigurationException.class, () -> subject.addCustomPattern(incorrectPlaceHolder));
        assertEquals(expectedMessage, ex.getMessage());
    }


    @Test
    void shouldThrowExceptionWhenTryAddFieldNamesBeforePatterns() {
        //given:
        final var expectedMessage = """
                There is no pattern to detecting sensitive data added yet.
                Make sure the list of field names with sensitive fields is added after the patterns.""";
        //expect:
        var ex = assertThrows(IncorrectConfigurationException.class, () -> subject.addFieldName("something"));
        assertEquals(expectedMessage, ex.getMessage());
    }

    @ParameterizedTest
    @EnumSource
    void shouldAddAllPredefinedPatterns(SensitiveDataPatternFactory.SensitiveValuePatterns patternName) {
        //expect:
        Assertions.assertDoesNotThrow(() -> subject.addPatternName(patternName.name()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SOMETHING"})
    @NullAndEmptySource
    void shouldThrowExceptionWhenTryAddNonExistingPredefinedPatternName(String str) {
        //expect:
        var ex = assertThrows(IncorrectConfigurationException.class, () -> subject.addPatternName(str));
        Assertions.assertEquals("Unknown name. You can use the following predefined pattern names [JSON,EQUAL_AND_SQUARE_BRACKETS,EQUAL_AND_BRACKETS,EQUAL_AND_DOUBLE_QUOTES]", ex.getMessage());
    }

    @Test
    void shouldHaveDefaultRegexTimeout() {
        assertEquals(500, subject.regexTimeoutMillis);
    }

    @Test
    void shouldAllowSettingCustomRegexTimeout() {
        subject.setRegexTimeoutMillis(1000);
        assertEquals(1000, subject.regexTimeoutMillis);
    }

    @Test
    void shouldAllowDisablingTimeoutWithMinusOne() {
        subject.setRegexTimeoutMillis(-1);
        assertEquals(-1, subject.regexTimeoutMillis);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -2, -100})
    void shouldThrowExceptionWhenRegexTimeoutIsInvalid(int timeout) {
        var ex = assertThrows(IncorrectConfigurationException.class, () -> subject.setRegexTimeoutMillis(timeout));
        assertEquals("regexTimeoutMillis must be a positive integer or -1 to disable timeout", ex.getMessage());
    }

    static class TestImplementationSensitiveDataTextDecoratorTest extends AbstractSensitiveDataObfuscatorLayout {
        @Override
        protected String maskLogMessage(final String str) {
            //Not important for this test suite
            return null;
        }
    }
}