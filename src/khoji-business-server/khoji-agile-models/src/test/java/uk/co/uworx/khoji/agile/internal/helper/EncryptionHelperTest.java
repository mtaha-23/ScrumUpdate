/*
 * Copyright 2026 UWorx Services.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE for the full license text.
 */


package uk.co.uworx.khoji.agile.internal.helper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.co.uworx.khoji.agile.constants.EncryptionConstants;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EncryptionHelperTest
{
  private static final String STORE_PASSWORD = "temp-store-password";
  private static final String KEY_PASSWORD = "temp-key-password";

  @TempDir
  Path tempDir;

  @Test
  void missingKeystoreFileFailsWithoutReturningNull() throws Exception
  {
    IsolatedHelper helper = new IsolatedHelper();
    IllegalStateException error = assertThrows(
        IllegalStateException.class,
        () -> helper.init(tempDir.resolve("absent.jck").toString(), STORE_PASSWORD, KEY_PASSWORD));
    assertTrue(error.getMessage().contains("KEY_STORE_PATH"));
    assertFalse(error.getMessage().contains(STORE_PASSWORD));
    assertFalse(error.getMessage().contains(KEY_PASSWORD));
  }

  @Test
  void loadsTemporaryKeystoreAndRoundTrips() throws Exception
  {
    Path keystore = tempDir.resolve("test-keystore.jck");
    writeTestKeystore(keystore);

    IsolatedHelper helper = new IsolatedHelper();
    helper.init(keystore.toString(), STORE_PASSWORD, KEY_PASSWORD);
    String encrypted = helper.encrypt("tenant-token");

    assertEquals("tenant-token", helper.decrypt(encrypted));
  }

  private static void writeTestKeystore(Path keystore) throws Exception
  {
    KeyStore store = KeyStore.getInstance(EncryptionConstants.KEY_STORE_TYPE);
    store.load(null, STORE_PASSWORD.toCharArray());
    store.setKeyEntry(
        EncryptionConstants.ENCRYPT_KEY_ALIAS,
        secret("test-encryption-secret"),
        KEY_PASSWORD.toCharArray(),
        null);
    store.setKeyEntry(
        EncryptionConstants.SALT_KEY_ALIAS,
        secret("test-salt-secret"),
        KEY_PASSWORD.toCharArray(),
        null);
    try (OutputStream out = Files.newOutputStream(keystore))
    {
      store.store(out, STORE_PASSWORD.toCharArray());
    }
  }

  private static SecretKey secret(String value) throws Exception
  {
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    return new SecretKeySpec(
        factory.generateSecret(new javax.crypto.spec.PBEKeySpec(
            value.toCharArray(),
            "test-salt".getBytes(StandardCharsets.UTF_8),
            1024,
            128)).getEncoded(),
        "AES");
  }

  /**
   * Each test gets a fresh EncryptionHelper class so static initialization
   * does not leak between tests and production code needs no reset method.
   */
  private static final class IsolatedHelper
  {
    private final Object helper;
    private final Method init;
    private final Method encrypt;
    private final Method decrypt;

    private IsolatedHelper() throws Exception
    {
      Class<?> type = new IsolatedClassLoader().loadClass(EncryptionHelper.class.getName());
      helper = null;
      init = type.getMethod("init", String.class, String.class, String.class);
      encrypt = type.getMethod("encrypt", String.class);
      decrypt = type.getMethod("decrypt", String.class);
    }

    private void init(String path, String storePassword, String keyPassword) throws Exception
    {
      try
      {
        init.invoke(helper, path, storePassword, keyPassword);
      }
      catch (java.lang.reflect.InvocationTargetException exception)
      {
        rethrow(exception);
      }
    }

    private String encrypt(String value) throws Exception
    {
      return (String) encrypt.invoke(helper, value);
    }

    private String decrypt(String value) throws Exception
    {
      return (String) decrypt.invoke(helper, value);
    }

    private static void rethrow(java.lang.reflect.InvocationTargetException exception) throws Exception
    {
      Throwable cause = exception.getCause();
      if (cause instanceof Exception thrown)
      {
        throw thrown;
      }
      throw exception;
    }
  }

  private static final class IsolatedClassLoader extends ClassLoader
  {
    private IsolatedClassLoader()
    {
      super(EncryptionHelper.class.getClassLoader());
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
      if (name.equals(EncryptionHelper.class.getName()) || name.startsWith(EncryptionHelper.class.getName() + "$"))
      {
        synchronized (getClassLoadingLock(name))
        {
          Class<?> loaded = findLoadedClass(name);
          if (loaded == null)
          {
            try
            {
              String resource = name.replace('.', '/') + ".class";
              byte[] bytes = EncryptionHelper.class.getClassLoader().getResourceAsStream(resource).readAllBytes();
              loaded = defineClass(name, bytes, 0, bytes.length);
            }
            catch (Exception exception)
            {
              throw new ClassNotFoundException(name, exception);
            }
          }
          if (resolve)
          {
            resolveClass(loaded);
          }
          return loaded;
        }
      }
      return super.loadClass(name, resolve);
    }
  }
}
