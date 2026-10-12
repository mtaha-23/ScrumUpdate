/*
 * Copyright 2026 UWorx Services.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE for the full license text.
 */


package uk.co.uworx.khoji.agile.constants;

public class EncryptionConstants
{
  // Details for Encryption key in keystore
  public static final String ENCRYPT_KEY_ALIAS = "ssapitokenkey";
  // Details for salt Value stored in key store
  public static final String SALT_KEY_ALIAS = "ssapitokensalt";

  public static final String ENCRYPTION_KEY_ALGORITHM = "PBKDF2WithHmacSHA256";

  public static final String ENCRYPTION_TYPE = "AES";
  public static final String ENCRYPTION_ALGORITHM = "AES/CBC/PKCS5Padding";
  public static final String KEY_STORE_TYPE = "JCEKS";
}
