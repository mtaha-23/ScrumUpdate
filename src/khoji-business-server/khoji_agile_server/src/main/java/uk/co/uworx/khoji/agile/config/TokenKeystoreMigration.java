/*
 * Copyright 2026 UWorx Services.
 * Licensed under the Apache License, Version 2.0.
 * See LICENSE for the full license text.
 */


package uk.co.uworx.khoji.agile.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import uk.co.uworx.khoji.agile.internal.helper.EncryptionHelper;
import uk.co.uworx.khoji.agile.internal.helper.EncryptionHelper.KeyMaterial;

import java.util.List;
import java.util.Map;

@Component
@Log4j2
@ConditionalOnProperty(name = "KEY_STORE_MIGRATE", havingValue = "true")
public class TokenKeystoreMigration implements ApplicationRunner, Ordered
{
  private final JdbcTemplate jdbc;
  private final PlatformTransactionManager transactions;
  private final String oldPath;
  private final String oldStorePassword;
  private final String oldKeyPassword;
  private final String newPath;
  private final String newStorePassword;
  private final String newKeyPassword;

  public TokenKeystoreMigration(
      JdbcTemplate jdbc,
      PlatformTransactionManager transactions,
      @Value("${OLD_KEY_STORE_PATH:}") String oldPath,
      @Value("${OLD_KEY_STORE_PASSWORD:}") String oldStorePassword,
      @Value("${OLD_KEY_PASSWORD:}") String oldKeyPassword,
      @Value("${KEY_STORE_PATH:/run/secrets/khoji-keystore.jck}") String newPath,
      @Value("${KEY_STORE_PASSWORD:}") String newStorePassword,
      @Value("${KEY_PASSWORD:}") String newKeyPassword)
  {
    this.jdbc = jdbc;
    this.transactions = transactions;
    this.oldPath = oldPath;
    this.oldStorePassword = oldStorePassword;
    this.oldKeyPassword = oldKeyPassword;
    this.newPath = newPath;
    this.newStorePassword = newStorePassword;
    this.newKeyPassword = newKeyPassword;
  }

  static void requireOldSettings(String path, String storePassword, String keyPassword)
  {
    if (!StringUtils.hasText(path) || !StringUtils.hasText(storePassword) || !StringUtils.hasText(keyPassword))
    {
      throw new IllegalStateException(
          "KEY_STORE_MIGRATE is set but OLD_KEY_STORE_PATH, OLD_KEY_STORE_PASSWORD, or OLD_KEY_PASSWORD is not configured.");
    }
  }

  static String quoted(String identifier)
  {
    if (identifier == null || !identifier.matches("[A-Za-z_][A-Za-z0-9_]*"))
    {
      throw new IllegalStateException("Refusing to migrate an unexpected table name.");
    }
    return "\"" + identifier + "\"";
  }

  @Override
  public int getOrder()
  {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  public void run(ApplicationArguments args)
  {
    requireOldSettings(oldPath, oldStorePassword, oldKeyPassword);
    KeyMaterial oldMaterial = EncryptionHelper.load(oldPath, oldStorePassword, oldKeyPassword);
    KeyMaterial newMaterial = EncryptionHelper.load(newPath, newStorePassword, newKeyPassword);
    EncryptionHelper.requireRotated(oldMaterial, newMaterial);
    Integer updated = new TransactionTemplate(transactions).execute(status -> migrate(oldMaterial, newMaterial));
    log.info("Re-encrypted {} stored token rows.", updated);
  }

  private int migrate(KeyMaterial oldMaterial, KeyMaterial newMaterial)
  {
    List<Map<String, Object>> tables = jdbc.queryForList(
        "SELECT c.table_schema, c.table_name "
            + "FROM information_schema.columns c "
            + "JOIN information_schema.tables t "
            + "ON t.table_schema = c.table_schema AND t.table_name = c.table_name "
            + "WHERE c.column_name = 'jwt_source_token' "
            + "AND t.table_type = 'BASE TABLE' "
            + "AND c.table_schema NOT IN ('pg_catalog', 'information_schema') "
            + "AND EXISTS ("
            + "SELECT 1 FROM information_schema.columns r "
            + "WHERE r.table_schema = c.table_schema "
            + "AND r.table_name = c.table_name "
            + "AND r.column_name = 'jwt_source_refresh_token')");
    if (tables.isEmpty())
    {
      throw new IllegalStateException(
          "KEY_STORE_MIGRATE is set but no jwt_source_token column was found.");
    }

    int updated = 0;
    for (Map<String, Object> table : tables)
    {
      String schema = quoted(String.valueOf(table.get("table_schema")));
      String name = quoted(String.valueOf(table.get("table_name")));
      String qualified = schema + "." + name;
      jdbc.execute("LOCK TABLE " + qualified + " IN SHARE ROW EXCLUSIVE MODE");
      List<Map<String, Object>> rows = jdbc.queryForList(
          "SELECT ctid::text AS row_id, jwt_source_token, jwt_source_refresh_token FROM "
              + qualified + " FOR UPDATE");
      for (Map<String, Object> row : rows)
      {
        String token = EncryptionHelper.reencrypt(stringValue(row.get("jwt_source_token")), oldMaterial, newMaterial);
        String refresh = EncryptionHelper.reencrypt(
            stringValue(row.get("jwt_source_refresh_token")), oldMaterial, newMaterial);
        jdbc.update(
            "UPDATE " + qualified
                + " SET jwt_source_token = ?, jwt_source_refresh_token = ? WHERE ctid = CAST(? AS tid)",
            token,
            refresh,
            row.get("row_id"));
        updated++;
      }
    }
    return updated;
  }

  private static String stringValue(Object value)
  {
    return value == null ? null : value.toString();
  }
}
