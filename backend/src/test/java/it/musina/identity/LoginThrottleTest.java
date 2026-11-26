package it.musina.identity;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LoginThrottleTest {
    @Test void attemptsExpireAndSuccessClearsTheLimit() {
        MutableClock clock = new MutableClock();
        LoginThrottle throttle = new LoginThrottle(clock);
        for (int i = 0; i < 10; i++) { throttle.failed("local"); }
        assertThat(throttle.blocked("local")).isTrue();
        assertThat(throttle.blocked("other")).isFalse();
        clock.now = clock.now.plusSeconds(901);
        assertThat(throttle.blocked("local")).isFalse();
        for (int i = 0; i < 10; i++) { throttle.failed("local"); }
        throttle.succeeded("local");
        assertThat(throttle.blocked("local")).isFalse();
    }

    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
