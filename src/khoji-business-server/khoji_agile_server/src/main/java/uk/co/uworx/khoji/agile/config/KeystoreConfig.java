/*
 * Copyright 2026 UWorx Services.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE for the full license text.
 */


package uk.co.uworx.khoji.agile.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.co.uworx.khoji.agile.internal.helper.EncryptionHelper;

/**
 * Fail-closed keystore settings. Applies in every profile, including dev.
 * Messages name the missing setting and never include secret values.
 */
@Component
public class KeystoreConfig
{
  @Value("${KEY_STORE_PATH:/run/secrets/khoji-keystore.jck}")
  public String keyStorePath;

  @Value("${KEY_STORE_PASSWORD:}")
  public String keyStorePassword;

  @Value("${KEY_PASSWORD:}")
  public String keyPassword;

  @PostConstruct
  void validateAndInitialize()
  {
    if (!StringUtils.hasText(keyStorePassword))
    {
      throw new IllegalStateException(
          "KEY_STORE_PASSWORD is not configured. Set it before starting KBS.");
    }
    if (!StringUtils.hasText(keyPassword))
    {
      throw new IllegalStateException(
          "KEY_PASSWORD is not configured. Set it before starting KBS.");
    }
    if (!StringUtils.hasText(keyStorePath))
    {
      throw new IllegalStateException(
          "KEY_STORE_PATH is not configured. Set it before starting KBS.");
    }
    EncryptionHelper.init(keyStorePath, keyStorePassword, keyPassword);
  }
}
