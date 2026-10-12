/*
 * Copyright 2026 UWorx Services.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE for the full license text.
 */


package uk.co.uworx.khoji.agile.internal.helper;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.security.SecureRandom;

import static uk.co.uworx.khoji.agile.constants.EncryptionConstants.ENCRYPT_KEY_ALIAS;
import static uk.co.uworx.khoji.agile.constants.EncryptionConstants.KEY_STORE_TYPE;
import static uk.co.uworx.khoji.agile.constants.EncryptionConstants.SALT_KEY_ALIAS;

public final class KeystoreFiles
{
  private static final SecureRandom RANDOM = new SecureRandom();

  private KeystoreFiles()
  {
  }

  public static void create(Path path, String storePassword, String keyPassword)
  {
    if (path == null)
    {
      throw new IllegalStateException("KEY_STORE_PATH is not configured.");
    }
    if (storePassword == null || storePassword.isBlank())
    {
      throw new IllegalStateException("KEY_STORE_PASSWORD is not configured.");
    }
    if (keyPassword == null || keyPassword.isBlank())
    {
      throw new IllegalStateException("KEY_PASSWORD is not configured.");
    }
    if (Files.exists(path))
    {
      throw new IllegalStateException("Keystore already exists at KEY_STORE_PATH. Refusing to overwrite.");
    }

    try
    {
      Path parent = path.getParent();
      if (parent != null)
      {
        Files.createDirectories(parent);
      }
      KeyStore store = KeyStore.getInstance(KEY_STORE_TYPE);
      store.load(null, storePassword.toCharArray());
      store.setKeyEntry(ENCRYPT_KEY_ALIAS, randomSecret(), keyPassword.toCharArray(), null);
      store.setKeyEntry(SALT_KEY_ALIAS, randomSecret(), keyPassword.toCharArray(), null);
      writeAtomically(path, out -> store.store(out, storePassword.toCharArray()));
    }
    catch (IllegalStateException exception)
    {
      throw exception;
    }
    catch (Exception exception)
    {
      throw new IllegalStateException("Keystore could not be written at KEY_STORE_PATH.", exception);
    }
  }

  public static void main(String[] args)
  {
    String path = System.getenv("KEY_STORE_PATH");
    String storePassword = System.getenv("KEY_STORE_PASSWORD");
    String keyPassword = System.getenv("KEY_PASSWORD");
    if (path == null || path.isBlank())
    {
      throw new IllegalStateException("KEY_STORE_PATH is not configured.");
    }
    create(Path.of(path), storePassword, keyPassword);
    System.out.println("Wrote a new keystore to KEY_STORE_PATH.");
  }

  static void writeAtomically(Path path, OutputWriter writer) throws Exception
  {
    Path temp = path.resolveSibling(path.getFileName().toString() + ".tmp");
    try
    {
      try (OutputStream out = Files.newOutputStream(
          temp,
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING))
      {
        writer.write(out);
      }
      try
      {
        Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE);
      }
      catch (AtomicMoveNotSupportedException exception)
      {
        Files.move(temp, path);
      }
    }
    catch (Exception exception)
    {
      try
      {
        Files.deleteIfExists(temp);
      }
      catch (IOException deleteFailure)
      {
        exception.addSuppressed(deleteFailure);
      }
      throw exception;
    }
  }

  private static SecretKey randomSecret()
  {
    byte[] bytes = new byte[16];
    RANDOM.nextBytes(bytes);
    return new SecretKeySpec(bytes, "AES");
  }

  interface OutputWriter
  {
    void write(OutputStream out) throws Exception;
  }
}
