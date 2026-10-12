/*
 * Copyright 2026 UWorx Services.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE for the full license text.
 */


package uk.co.uworx.khoji.agile.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TokenKeystoreMigrationTest
{
  private static final String TEST_PASSWORD = "test-only-password";

  @Test
  void missingOldKeystoreSettingsFail()
  {
    IllegalStateException error = assertThrows(
        IllegalStateException.class,
        () -> TokenKeystoreMigration.requireOldSettings(null, TEST_PASSWORD, TEST_PASSWORD));

    assertTrue(error.getMessage().contains("OLD_KEY_STORE_PATH"));
    assertFalse(error.getMessage().contains(TEST_PASSWORD));
  }

  @Test
  void blankOldPasswordFails()
  {
    IllegalStateException error = assertThrows(
        IllegalStateException.class,
        () -> TokenKeystoreMigration.requireOldSettings("old.jck", "   ", TEST_PASSWORD));

    assertTrue(error.getMessage().contains("OLD_KEY_STORE_PASSWORD"));
    assertFalse(error.getMessage().contains(TEST_PASSWORD));
  }

  @Test
  void quotesASafeIdentifierAndRejectsSql()
  {
    assertEquals("\"tenant_users\"", TokenKeystoreMigration.quoted("tenant_users"));

    IllegalStateException error = assertThrows(
        IllegalStateException.class,
        () -> TokenKeystoreMigration.quoted("tenant_users;drop"));

    assertTrue(error.getMessage().contains("unexpected table name"));
  }
}
