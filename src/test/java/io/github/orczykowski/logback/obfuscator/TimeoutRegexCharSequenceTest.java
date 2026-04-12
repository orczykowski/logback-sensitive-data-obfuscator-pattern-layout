package io.github.orczykowski.logback.obfuscator;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class TimeoutRegexCharSequenceTest {

    private static final Pattern SIMPLE_PATTERN = Pattern.compile("hello (\\w+)");

    @Test
    void shouldDelegateCharAtToInnerSequence() {
        var subject = new TimeoutRegexCharSequence("hello", 5000, SIMPLE_PATTERN);

        assertEquals('h', subject.charAt(0));
        assertEquals('e', subject.charAt(1));
        assertEquals('o', subject.charAt(4));
    }

    @Test
    void shouldDelegateLengthToInnerSequence() {
        var subject = new TimeoutRegexCharSequence("hello", 5000, SIMPLE_PATTERN);

        assertEquals(5, subject.length());
    }

    @Test
    void shouldReturnTimeoutRegexCharSequenceFromSubSequence() {
        var subject = new TimeoutRegexCharSequence("hello world", 5000, SIMPLE_PATTERN);

        var sub = subject.subSequence(0, 5);

        assertInstanceOf(TimeoutRegexCharSequence.class, sub);
        assertEquals("hello", sub.toString());
    }

    @Test
    void shouldDelegateToStringToInnerSequence() {
        var subject = new TimeoutRegexCharSequence("hello", 5000, SIMPLE_PATTERN);

        assertEquals("hello", subject.toString());
    }

    @Test
    void shouldWorkWithStringBuilder() {
        var inner = new StringBuilder("test value");
        var subject = new TimeoutRegexCharSequence(inner, 5000, SIMPLE_PATTERN);

        assertEquals('t', subject.charAt(0));
        assertEquals(10, subject.length());
        assertEquals("test value", subject.toString());
    }

    @Test
    void shouldThrowRegexTimeoutExceptionWhenTimeoutExpires() throws InterruptedException {
        var subject = new TimeoutRegexCharSequence("hello", 1, SIMPLE_PATTERN);
        Thread.sleep(10);

        assertThrows(RegexProcessingTimeoutException.class, () -> subject.charAt(0));
    }

    @Test
    void shouldNotThrowWhenWithinTimeout() {
        var subject = new TimeoutRegexCharSequence("hello", 5000, SIMPLE_PATTERN);

        assertDoesNotThrow(() -> subject.charAt(0));
    }

    @Test
    void shouldNotTimeoutForFastRegex() {
        var subject = new TimeoutRegexCharSequence("hello world", 5000, SIMPLE_PATTERN);

        Matcher matcher = SIMPLE_PATTERN.matcher(subject);

        assertTrue(matcher.find());
        assertEquals("world", matcher.group(1));
    }

    @Test
    void shouldIncludePatternInExceptionMessage() throws InterruptedException {
        var pattern = Pattern.compile("test-pattern");
        var subject = new TimeoutRegexCharSequence("hello", 1, pattern);
        Thread.sleep(10);

        var ex = assertThrows(RegexProcessingTimeoutException.class, () -> subject.charAt(0));
        assertTrue(ex.getMessage().contains("test-pattern"));
    }
}
