package io.gravitee.management.apis;

import com.intuit.karate.junit5.Karate;

// Dev companion class to only run Apis related test suite
public class ApisRunner {
    @Karate.Test
    Karate testUsers() {
        return Karate.run("apis").relativeTo(getClass());
    }
}
