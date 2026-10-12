/*
 * Copyright 2026 UWorx Services.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE for the full license text.
 */


package uk.co.uworx.khoji.agile.internal.helper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KeystoreFilesTest
{
  private static final String STORE_PASSWORD = "temp-store-password";
  private static final String KEY_PASSWORD = "temp-key-password";

  @TempDir
  Path tempDir;

  @Test
  void createsRandomKeyMaterialThatRoundTrips()
  {
    Path first = tempDir.resolve("one.jck");
    Path second = tempDir.resolve("two.jck");
    KeystoreFiles.create(first, STORE_PASSWORD, KEY_PASSWORD);
    KeystoreFiles.create(second, STORE_PASSWORD, KEY_PASSWORD);

    EncryptionHelper.KeyMaterial firstMaterial = EncryptionHelper.load(first.toString(), STORE_PASSWORD, KEY_PASSWORD);
    EncryptionHelper.KeyMaterial secondMaterial = EncryptionHelper.load(second.toString(), STORE_PASSWORD, KEY_PASSWORD);
    String encryptedWithFirst = EncryptionHelper.encrypt("tenant-token", firstMaterial);
    String encryptedWithSecond = EncryptionHelper.encrypt("tenant-token", secondMaterial);

    assertNotEquals(encryptedWithFirst, encryptedWithSecond);
    assertEquals("tenant-token", EncryptionHelper.decrypt(encryptedWithFirst, firstMaterial));
    EncryptionHelper.requireRotated(firstMaterial, secondMaterial);
    assertThrows(IllegalStateException.class, () -> EncryptionHelper.requireRotated(firstMaterial, firstMaterial));
  }

  @Test
  void refusesToOverwriteAnExistingKeystore()
  {
    Path keystore = tempDir.resolve("existing.jck");
    KeystoreFiles.create(keystore, STORE_PASSWORD, KEY_PASSWORD);

    IllegalStateException error = assertThrows(
        IllegalStateException.class,
        () -> KeystoreFiles.create(keystore, STORE_PASSWORD, KEY_PASSWORD));

    assertTrue(error.getMessage().contains("KEY_STORE_PATH"));
    assertFalse(error.getMessage().contains(STORE_PASSWORD));
    assertFalse(error.getMessage().contains(KEY_PASSWORD));
  }

  @Test
  void failedWriteLeavesNoFileSoRetryWorks() throws Exception
  {
    Path keystore = tempDir.resolve("retry.jck");

    IOException error = assertThrows(IOException.class, () -> KeystoreFiles.writeAtomically(keystore, out -> {
      throw new IOException("disk full");
    }));

    assertEquals("disk full", error.getMessage());
    assertFalse(Files.exists(keystore));
    assertFalse(Files.exists(keystore.resolveSibling("retry.jck.tmp")));

    KeystoreFiles.create(keystore, STORE_PASSWORD, KEY_PASSWORD);
    assertTrue(Files.isRegularFile(keystore));
  }

  @Test
  void blankPasswordFailsWithoutEchoingThePassword()
  {
    IllegalStateException error = assertThrows(
        IllegalStateException.class,
        () -> KeystoreFiles.create(tempDir.resolve("missing.jck"), "   ", KEY_PASSWORD));

    assertTrue(error.getMessage().contains("KEY_STORE_PASSWORD"));
    assertFalse(error.getMessage().contains(KEY_PASSWORD));
  }
}
