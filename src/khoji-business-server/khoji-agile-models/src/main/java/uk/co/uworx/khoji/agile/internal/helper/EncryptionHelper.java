/*
 * Copyright 2026 UWorx Services.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE for the full license text.
 */


package uk.co.uworx.khoji.agile.internal.helper;

import lombok.extern.log4j.Log4j2;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

import static uk.co.uworx.khoji.agile.constants.EncryptionConstants.ENCRYPTION_ALGORITHM;
import static uk.co.uworx.khoji.agile.constants.EncryptionConstants.ENCRYPTION_KEY_ALGORITHM;
import static uk.co.uworx.khoji.agile.constants.EncryptionConstants.ENCRYPTION_TYPE;
import static uk.co.uworx.khoji.agile.constants.EncryptionConstants.ENCRYPT_KEY_ALIAS;
import static uk.co.uworx.khoji.agile.constants.EncryptionConstants.KEY_STORE_TYPE;
import static uk.co.uworx.khoji.agile.constants.EncryptionConstants.SALT_KEY_ALIAS;

@Log4j2
public class EncryptionHelper
{
  private static final IvParameterSpec ivParameterSpec = new IvParameterSpec(new byte[16]);
  private static volatile String initializedPath;
  private static volatile String initializedStorePassword;
  private static volatile String initializedKeyPassword;
  private static volatile KeyMaterial material;

  public static final class KeyMaterial
  {
    private final String secretKey;
    private final String salt;

    private KeyMaterial(String secretKey, String salt)
    {
      this.secretKey = secretKey;
      this.salt = salt;
    }

    private boolean sameKeys(KeyMaterial other)
    {
      return secretKey.equals(other.secretKey) && salt.equals(other.salt);
    }
  }

  /**
   * Loads key material from an external keystore. A repeat call with the same
   * path and passwords is a no-op. A different path or password fails closed.
   * Passwords are never included in the exception message.
   */
  public static synchronized void init(String keyStorePath, String keyStorePassword, String keyPassword)
  {
    requireSettings(keyStorePath, keyStorePassword, keyPassword);

    if (material != null)
    {
      if (keyStorePath.equals(initializedPath)
          && keyStorePassword.equals(initializedStorePassword)
          && keyPassword.equals(initializedKeyPassword))
      {
        return;
      }
      throw new IllegalStateException(
          "EncryptionHelper is already initialized with different keystore configuration.");
    }

    material = load(keyStorePath, keyStorePassword, keyPassword);
    initializedPath = keyStorePath;
    initializedStorePassword = keyStorePassword;
    initializedKeyPassword = keyPassword;
  }

  public static KeyMaterial load(String keyStorePath, String keyStorePassword, String keyPassword)
  {
    requireSettings(keyStorePath, keyStorePassword, keyPassword);
    String secretKey = readAlias(keyStorePath, keyStorePassword, keyPassword, ENCRYPT_KEY_ALIAS);
    String salt = readAlias(keyStorePath, keyStorePassword, keyPassword, SALT_KEY_ALIAS);
    return new KeyMaterial(secretKey, salt);
  }

  public static void requireRotated(KeyMaterial oldMaterial, KeyMaterial newMaterial)
  {
    if (oldMaterial == null || newMaterial == null || oldMaterial.sameKeys(newMaterial))
    {
      throw new IllegalStateException(
          "Old and new keystores hold the same key material. Generate a new keystore before migrating.");
    }
  }

  public static String reencrypt(String ciphertext, KeyMaterial from, KeyMaterial to)
  {
    if (ciphertext == null || ciphertext.isBlank())
    {
      return ciphertext;
    }
    String plain = decryptWith(ciphertext, from);
    if (plain == null)
    {
      throw new IllegalStateException("Stored token could not be decrypted with the old keystore.");
    }
    String encrypted = encrypt(plain, to);
    if (encrypted == null)
    {
      throw new IllegalStateException("Stored token could not be encrypted with the new keystore.");
    }
    return encrypted;
  }

  public static String encrypt(String strToEncrypt)
  {
    return encrypt(strToEncrypt, requireMaterial());
  }

  public static String encrypt(String strToEncrypt, KeyMaterial keyMaterial)
  {
    try
    {
      return encryptWith(strToEncrypt, keyMaterial);
    }
    catch (IllegalStateException exception)
    {
      throw exception;
    }
    catch (Exception exception)
    {
      log.error("Error while encrypting: ", exception);
    }
    return null;
  }

  public static String decrypt(String strToDecrypt)
  {
    return decrypt(strToDecrypt, requireMaterial());
  }

  public static String decrypt(String strToDecrypt, KeyMaterial keyMaterial)
  {
    return decryptWith(strToDecrypt, keyMaterial);
  }

  private static KeyMaterial requireMaterial()
  {
    if (material == null)
    {
      throw new IllegalStateException(
          "EncryptionHelper is not initialized. Set KEY_STORE_PATH, KEY_STORE_PASSWORD, and KEY_PASSWORD.");
    }
    return material;
  }

  private static void requireSettings(String keyStorePath, String keyStorePassword, String keyPassword)
  {
    if (keyStorePath == null || keyStorePath.isBlank())
    {
      throw new IllegalStateException("KEY_STORE_PATH is not configured.");
    }
    if (keyStorePassword == null || keyStorePassword.isBlank())
    {
      throw new IllegalStateException("KEY_STORE_PASSWORD is not configured.");
    }
    if (keyPassword == null || keyPassword.isBlank())
    {
      throw new IllegalStateException("KEY_PASSWORD is not configured.");
    }
  }

  private static String encryptWith(String value, KeyMaterial keyMaterial) throws Exception
  {
    Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
    cipher.init(Cipher.ENCRYPT_MODE, derive(keyMaterial), ivParameterSpec);
    return Base64.getEncoder().encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)));
  }

  private static String decryptWith(String value, KeyMaterial keyMaterial)
  {
    try
    {
      Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, derive(keyMaterial), ivParameterSpec);
      return new String(cipher.doFinal(Base64.getDecoder().decode(value)));
    }
    catch (IllegalStateException exception)
    {
      throw exception;
    }
    catch (Exception exception)
    {
      log.error("Error while decrypting: ", exception);
    }
    return null;
  }

  private static SecretKeySpec derive(KeyMaterial keyMaterial) throws NoSuchAlgorithmException, InvalidKeySpecException
  {
    SecretKeyFactory factory = SecretKeyFactory.getInstance(ENCRYPTION_KEY_ALGORITHM);
    KeySpec spec = new PBEKeySpec(keyMaterial.secretKey.toCharArray(), keyMaterial.salt.getBytes(), 65536, 128);
    SecretKey tmp = factory.generateSecret(spec);
    return new SecretKeySpec(tmp.getEncoded(), ENCRYPTION_TYPE);
  }

  private static String readAlias(String keyStorePath, String keyStorePassword, String keyPassword, String keyAlias)
  {
    Path path = Path.of(keyStorePath);
    if (!Files.isRegularFile(path))
    {
      throw new IllegalStateException("Keystore file not found at KEY_STORE_PATH.");
    }

    try (InputStream keystoreStream = Files.newInputStream(path))
    {
      KeyStore keystore = KeyStore.getInstance(KEY_STORE_TYPE);
      keystore.load(keystoreStream, keyStorePassword.toCharArray());
      if (!keystore.containsAlias(keyAlias))
      {
        throw new IllegalStateException("Keystore alias not found: " + keyAlias);
      }
      Key key = keystore.getKey(keyAlias, keyPassword.toCharArray());
      if (key == null)
      {
        throw new IllegalStateException("Keystore alias could not be unlocked: " + keyAlias);
      }
      return key.toString();
    }
    catch (IOException exception)
    {
      throw new IllegalStateException("Keystore could not be unlocked. Check KEY_STORE_PASSWORD.", exception);
    }
    catch (UnrecoverableKeyException exception)
    {
      throw new IllegalStateException("Keystore key could not be unlocked. Check KEY_PASSWORD.", exception);
    }
    catch (KeyStoreException | NoSuchAlgorithmException | CertificateException exception)
    {
      throw new IllegalStateException("Keystore could not be read at KEY_STORE_PATH.", exception);
    }
  }

}
