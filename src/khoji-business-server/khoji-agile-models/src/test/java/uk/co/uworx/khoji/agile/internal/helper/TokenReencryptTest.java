/*
 * Copyright 2026 UWorx Services.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE for the full license text.
 */


package uk.co.uworx.khoji.agile.internal.helper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TokenReencryptTest
{
  private static final String STORE_PASSWORD = "temp-store-password";
  private static final String KEY_PASSWORD = "temp-key-password";

  @TempDir
  Path tempDir;

  @Test
  void reencryptsTokenWithTheNewKeystore()
  {
    EncryptionHelper.KeyMaterial oldMaterial = material("old.jck");
    EncryptionHelper.KeyMaterial newMaterial = material("new.jck");
    String ciphertext = EncryptionHelper.encrypt("tenant-token", oldMaterial);

    String rotated = EncryptionHelper.reencrypt(ciphertext, oldMaterial, newMaterial);

    assertEquals("tenant-token", EncryptionHelper.decrypt(rotated, newMaterial));
    assertThrows(IllegalStateException.class, () -> EncryptionHelper.reencrypt(rotated, oldMaterial, newMaterial));
  }

  @Test
  void leavesBlankTokensUntouched()
  {
    EncryptionHelper.KeyMaterial oldMaterial = material("old-blank.jck");
    EncryptionHelper.KeyMaterial newMaterial = material("new-blank.jck");

    assertNull(EncryptionHelper.reencrypt(null, oldMaterial, newMaterial));
    assertEquals("  ", EncryptionHelper.reencrypt("  ", oldMaterial, newMaterial));
  }

  @Test
  void wrongOldPasswordFailsWithoutEchoingThePassword()
  {
    Path keystore = tempDir.resolve("locked.jck");
    KeystoreFiles.create(keystore, STORE_PASSWORD, KEY_PASSWORD);

    IllegalStateException error = assertThrows(
        IllegalStateException.class,
        () -> EncryptionHelper.load(keystore.toString(), "wrong-store-password", KEY_PASSWORD));

    assertTrue(error.getMessage().contains("KEY_STORE_PASSWORD"));
    assertFalse(error.getMessage().contains("wrong-store-password"));
    assertFalse(error.getMessage().contains(KEY_PASSWORD));
  }

  private EncryptionHelper.KeyMaterial material(String fileName)
  {
    Path keystore = tempDir.resolve(fileName);
    KeystoreFiles.create(keystore, STORE_PASSWORD, KEY_PASSWORD);
    return EncryptionHelper.load(keystore.toString(), STORE_PASSWORD, KEY_PASSWORD);
  }
}
