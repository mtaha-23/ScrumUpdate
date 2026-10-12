/*
 * Copyright 2026 UWorx Services.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE for the full license text.
 */


package uk.co.uworx.khoji.agile.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KeystoreConfigTest
{
  private static final String TEST_PASSWORD = "test-only-password";

  @TempDir
  Path tempDir;

  @Test
  void missingKeyStorePasswordFails()
  {
    KeystoreConfig config = configured(null, TEST_PASSWORD, tempDir.resolve("missing.jck").toString());
    IllegalStateException error = assertThrows(IllegalStateException.class, config::validateAndInitialize);
    assertNamesSettingWithoutSecret(error, "KEY_STORE_PASSWORD");
  }

  @Test
  void blankKeyStorePasswordFails()
  {
    KeystoreConfig config = configured("   ", TEST_PASSWORD, tempDir.resolve("missing.jck").toString());
    IllegalStateException error = assertThrows(IllegalStateException.class, config::validateAndInitialize);
    assertNamesSettingWithoutSecret(error, "KEY_STORE_PASSWORD");
  }

  @Test
  void missingKeyPasswordFails()
  {
    KeystoreConfig config = configured(TEST_PASSWORD, null, tempDir.resolve("missing.jck").toString());
    IllegalStateException error = assertThrows(IllegalStateException.class, config::validateAndInitialize);
    assertNamesSettingWithoutSecret(error, "KEY_PASSWORD");
  }

  @Test
  void blankKeyPasswordFails()
  {
    KeystoreConfig config = configured(TEST_PASSWORD, "", tempDir.resolve("missing.jck").toString());
    IllegalStateException error = assertThrows(IllegalStateException.class, config::validateAndInitialize);
    assertNamesSettingWithoutSecret(error, "KEY_PASSWORD");
  }

  @Test
  void missingKeystoreFileFails()
  {
    KeystoreConfig config = configured(TEST_PASSWORD, TEST_PASSWORD, tempDir.resolve("absent.jck").toString());
    IllegalStateException error = assertThrows(IllegalStateException.class, config::validateAndInitialize);
    assertTrue(error.getMessage().contains("KEY_STORE_PATH"));
    assertFalse(error.getMessage().contains(TEST_PASSWORD));
  }

  private static KeystoreConfig configured(String storePassword, String keyPassword, String path)
  {
    KeystoreConfig config = new KeystoreConfig();
    config.keyStorePassword = storePassword;
    config.keyPassword = keyPassword;
    config.keyStorePath = path;
    return config;
  }

  private static void assertNamesSettingWithoutSecret(IllegalStateException error, String setting)
  {
    assertTrue(error.getMessage().contains(setting));
    assertFalse(error.getMessage().contains(TEST_PASSWORD));
  }
}
