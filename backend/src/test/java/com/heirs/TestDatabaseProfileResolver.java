package com.heirs;

import org.springframework.test.context.ActiveProfilesResolver;

public class TestDatabaseProfileResolver implements ActiveProfilesResolver {
  @Override
  public String[] resolve(Class<?> testClass) {
    if (!Boolean.getBoolean("heirs.test.mysql")) return new String[] {"test"};
    String url = System.getenv("TEST_DB_URL");
    if (url != null && !url.matches("jdbc:mysql://[^/]+/heirs_test(?:\\?.*)?")) {
      throw new IllegalArgumentException(
          "MySQL tests require a dedicated database named heirs_test");
    }
    return new String[] {"mysql-test"};
  }
}
