-- ============================================================================
--  Envers audit-table test helpers (MySQL / MariaDB)
-- ============================================================================
--  Two stored procedures for local testing of the Envers auditing / backfill:
--
--    CALL clear_envers_audit_tables('_audit');   -- empties audit tables (keeps schema)
--    CALL drop_envers_audit_tables('_audit');    -- drops audit tables entirely
--
--  Both take the audit-table SUFFIX as a parameter so they work with a custom
--  `org.hibernate.envers.audit_table_suffix` (default in OpenMRS is `_audit`).
--
--  SAFETY: they only act on genuine Envers audit tables — tables whose name ends
--  with the given suffix AND that contain a `REV` column. That REV filter excludes
--  look-alike domain tables (e.g. the appointments module's own
--  `patient_appointment_audit`, which has no REV column).
--
--  NOTE: entities with a custom @AuditTable name that does NOT end with the suffix
--  (e.g. appointments' `patient_appointment_revisions`) are NOT matched by a
--  suffix-based run — drop/clear those by hand if needed.
--
--  Load these procedures once:
--    docker exec -i openmrs-distro-referenceapplication-db-1 \
--      mysql -uroot -p"$MYSQL_ROOT_PASSWORD" openmrs < scripts/audit_test_helpers.sql
--
--  Then call them:
--    docker exec openmrs-distro-referenceapplication-db-1 \
--      mysql -uroot -p"$MYSQL_ROOT_PASSWORD" openmrs -e "CALL drop_envers_audit_tables('_audit');"
--
--  Typical "test creation from scratch" loop:
--    1) CALL drop_envers_audit_tables('_audit');
--    2) (optional) also reset the backfill so it runs again:
--         DELETE FROM global_property WHERE property LIKE 'auditlogweb.backfillExistingData.%';
--         -- then set the flag you want, e.g.:
--         INSERT INTO global_property(property, property_value, uuid)
--           VALUES('auditlogweb.backfillExistingData.enabled','true',UUID());
--    3) restart the backend  -> core's EnversAuditTableInitializer recreates the
--       audit tables, then the module backfills them.
--
--  DRY RUN — preview which tables a given suffix would match before acting:
--    SELECT t.table_name
--    FROM information_schema.tables t
--    WHERE t.table_schema = DATABASE()
--      AND t.table_type = 'BASE TABLE'
--      AND RIGHT(t.table_name, CHAR_LENGTH('_audit')) = '_audit'
--      AND EXISTS (SELECT 1 FROM information_schema.columns c
--                  WHERE c.table_schema = DATABASE() AND c.table_name = t.table_name
--                    AND c.column_name = 'REV');
-- ============================================================================

DELIMITER $$

-- ----------------------------------------------------------------------------
--  clear_envers_audit_tables(suffix): DELETE all rows from every audit table
--  matching the suffix. Keeps the tables (and revision_entity) in place.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS clear_envers_audit_tables $$
CREATE PROCEDURE clear_envers_audit_tables(IN p_suffix VARCHAR(64))
BEGIN
    DECLARE v_done INT DEFAULT 0;
    DECLARE v_table VARCHAR(255);
    DECLARE v_count INT DEFAULT 0;
    DECLARE cur CURSOR FOR
        SELECT t.table_name
        FROM information_schema.tables t
        WHERE t.table_schema = DATABASE()
          AND t.table_type = 'BASE TABLE'
          AND CHAR_LENGTH(p_suffix) > 0
          AND RIGHT(t.table_name, CHAR_LENGTH(p_suffix)) = p_suffix
          AND EXISTS (
              SELECT 1 FROM information_schema.columns c
              WHERE c.table_schema = DATABASE()
                AND c.table_name = t.table_name
                AND c.column_name = 'REV'
          );
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    IF p_suffix IS NULL OR p_suffix = '' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'p_suffix must not be empty';
    END IF;

    SET FOREIGN_KEY_CHECKS = 0;
    OPEN cur;
    clear_loop: LOOP
        FETCH cur INTO v_table;
        IF v_done = 1 THEN
            LEAVE clear_loop;
        END IF;
        SET @sql = CONCAT('DELETE FROM `', v_table, '`');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SET v_count = v_count + 1;
    END LOOP;
    CLOSE cur;
    SET FOREIGN_KEY_CHECKS = 1;

    SELECT CONCAT('Cleared data from ', v_count, ' audit table(s) matching suffix "', p_suffix, '".') AS result;
END $$

-- ----------------------------------------------------------------------------
--  drop_envers_audit_tables(suffix): DROP every audit table matching the suffix.
--  Leaves revision_entity intact (drop it separately if you want a full reset).
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS drop_envers_audit_tables $$
CREATE PROCEDURE drop_envers_audit_tables(IN p_suffix VARCHAR(64))
BEGIN
    DECLARE v_done INT DEFAULT 0;
    DECLARE v_table VARCHAR(255);
    DECLARE v_count INT DEFAULT 0;
    DECLARE cur CURSOR FOR
        SELECT t.table_name
        FROM information_schema.tables t
        WHERE t.table_schema = DATABASE()
          AND t.table_type = 'BASE TABLE'
          AND CHAR_LENGTH(p_suffix) > 0
          AND RIGHT(t.table_name, CHAR_LENGTH(p_suffix)) = p_suffix
          AND EXISTS (
              SELECT 1 FROM information_schema.columns c
              WHERE c.table_schema = DATABASE()
                AND c.table_name = t.table_name
                AND c.column_name = 'REV'
          );
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    IF p_suffix IS NULL OR p_suffix = '' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'p_suffix must not be empty';
    END IF;

    SET FOREIGN_KEY_CHECKS = 0;
    OPEN cur;
    drop_loop: LOOP
        FETCH cur INTO v_table;
        IF v_done = 1 THEN
            LEAVE drop_loop;
        END IF;
        SET @sql = CONCAT('DROP TABLE IF EXISTS `', v_table, '`');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SET v_count = v_count + 1;
    END LOOP;
    CLOSE cur;
    SET FOREIGN_KEY_CHECKS = 1;

    SELECT CONCAT('Dropped ', v_count, ' audit table(s) matching suffix "', p_suffix, '".') AS result;
END $$

DELIMITER ;
