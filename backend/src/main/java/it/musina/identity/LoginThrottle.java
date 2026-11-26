package it.musina.identity;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

final class LoginThrottle {
    private static final int MAX_FAILURES = 10;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private final Clock clock;
    private final Map<String, Attempt> attempts = new LinkedHashMap<>();

    LoginThrottle(Clock clock) { this.clock = clock; }

    synchronized boolean blocked(String address) {
        prune();
        Attempt attempt = attempts.get(address);
        return attempt != null && attempt.failures() >= MAX_FAILURES;
    }

    synchronized void failed(String address) {
        prune();
        if (!attempts.containsKey(address) && attempts.size() >= 1024) {
            attempts.remove(attempts.keySet().iterator().next());
        }
        attempts.compute(address, (key, previous) -> previous == null
                ? new Attempt(clock.instant(), 1) : new Attempt(previous.started(), previous.failures() + 1));
    }

    synchronized void succeeded(String address) { attempts.remove(address); }

    private void prune() {
        Instant cutoff = clock.instant().minus(WINDOW);
        attempts.values().removeIf(attempt -> !attempt.started().isAfter(cutoff));
    }

    private record Attempt(Instant started, int failures) { }
}
