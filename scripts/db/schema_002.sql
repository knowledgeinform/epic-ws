-- =======================================================================================

-- !! IMPORTANT !!
-- !! Only add changes to the end of the MyProcedure.
-- !! Each change should be within an IF statement to ensure the change will only run if 
-- !! the database is at the appropriate 'static data' version.

-- !! Each IF statement for a change should always evaluate its major and minor versions
-- !! to the values from the prior update.
-- !! Example:
/*
  DB maj version = 3 AND min version = 45
  Next alteration should check the following for being applied
  IF majVersion = 3 AND minVersion = 45 THEN
    ....make changes....
  END IF;

*/

-- =======================================================================================
DROP PROCEDURE IF EXISTS execute;
delimiter //
CREATE PROCEDURE execute()
BEGIN

  DECLARE db varchar(20);
  DECLARE majVersion INT;
  DECLARE minVersion INT;

  SELECT database() INTO db;
  SELECT major, minor FROM version WHERE type = 'DATABASE' INTO majVersion, minVersion;

  IF majVersion = 1 AND minVersion = 0 THEN


    -- -----------------------------------------------------
    -- Table `program`
    -- -----------------------------------------------------
    CREATE TABLE IF NOT EXISTS `program`
    (
      `pk`         int(11) NOT NULL AUTO_INCREMENT,
      `name`       varchar(255) DEFAULT NULL,
      `short_name` varchar(255) DEFAULT NULL,
      PRIMARY KEY (`pk`),
      UNIQUE KEY `name_UNIQUE` (`name`),
      UNIQUE KEY `shortName_UNIQUE` (`short_name`)
    ) ENGINE = InnoDB
      DEFAULT CHARSET = latin1;

    -- -----------------------------------------------------
    -- Table `template_def`
    -- -----------------------------------------------------
    CREATE TABLE IF NOT EXISTS `template_def`
    (
      `pk`          int(11) NOT NULL AUTO_INCREMENT,
      `description` varchar(255) DEFAULT NULL,
      `name`        varchar(255) DEFAULT NULL,
      `program_pk`  int(11) NOT NULL,
      PRIMARY KEY (`pk`),
      UNIQUE KEY `name_UNIQUE` (`name`),
      CONSTRAINT `fk_templateDef_program` FOREIGN KEY (`program_pk`) REFERENCES `program` (`pk`)
    ) ENGINE = InnoDB
      DEFAULT CHARSET = latin1;


    -- -----------------------------------------------------
    -- Table `template_def_version`
    -- -----------------------------------------------------
    CREATE TABLE IF NOT EXISTS `template_def_version`
    (
      `pk`              int(11) NOT NULL AUTO_INCREMENT,
      `version`         int(11) DEFAULT NULL,
      `template_def_pk` int(11) NOT NULL,
      PRIMARY KEY (`pk`),
      UNIQUE KEY `version_template_UNIQUE` (`template_def_pk`, `version`),
      CONSTRAINT `fk_templateDefVersion_templateDef` FOREIGN KEY (`template_def_pk`) REFERENCES `template_def` (`pk`)
    ) ENGINE = InnoDB
      DEFAULT CHARSET = latin1;


    -- -----------------------------------------------------
    -- Table `tempate_header`
    -- -----------------------------------------------------
    CREATE TABLE IF NOT EXISTS `template_header`
    (
      `pk`                      int(11) NOT NULL AUTO_INCREMENT,
      `text`                    varchar(255) DEFAULT NULL,
      `template_def_version_pk` int(11) NOT NULL,
      PRIMARY KEY (`pk`),
      KEY `fk_tempateHeader_templateDefVersion` (`template_def_version_pk`),
      CONSTRAINT `fk_tempateHeader_templateDefVersion` FOREIGN KEY (`template_def_version_pk`) REFERENCES `template_def_version` (`pk`)
    ) ENGINE = InnoDB
      DEFAULT CHARSET = latin1;


    -- -----------------------------------------------------
    -- Table `step_def`
    -- -----------------------------------------------------
    CREATE TABLE IF NOT EXISTS `step_def`
    (
      `pk`                      int(11) NOT NULL AUTO_INCREMENT,
      `description`             varchar(255) DEFAULT NULL,
      `display_order`           double       DEFAULT NULL,
      `second_signature`        bit(1)       DEFAULT NULL,
      `template_def_version_pk` int(11) NOT NULL,
      PRIMARY KEY (`pk`),
      KEY `fk_stepDef_templateDefVersion` (`template_def_version_pk`),
      CONSTRAINT `fk_stepDef_templateDefVersion` FOREIGN KEY (`template_def_version_pk`) REFERENCES `template_def_version` (`pk`)
    ) ENGINE = InnoDB
      DEFAULT CHARSET = latin1;


    -- -----------------------------------------------------
    -- Table `step_file_upload`
    -- -----------------------------------------------------
    CREATE TABLE IF NOT EXISTS `step_file_upload`
    (
      `pk` int(11) NOT NULL,
      PRIMARY KEY (`pk`),
      CONSTRAINT `fk_pk_stepFileUpload_stepDef` FOREIGN KEY (`pk`) REFERENCES `step_def` (`pk`) ON DELETE CASCADE ON UPDATE CASCADE
    ) ENGINE = InnoDB
      DEFAULT CHARSET = latin1;


    -- -----------------------------------------------------
    -- Table `step_single_value`
    -- -----------------------------------------------------
    CREATE TABLE IF NOT EXISTS `step_single_value`
    (
      `pk` int(11) NOT NULL,
      PRIMARY KEY (`pk`),
      CONSTRAINT `fk_pk_stepSingleValue_stepDef` FOREIGN KEY (`pk`) REFERENCES `step_def` (`pk`) ON DELETE CASCADE ON UPDATE CASCADE
    ) ENGINE = InnoDB
      DEFAULT CHARSET = latin1;


    -- -----------------------------------------------------
    -- Table `step_table`
    -- -----------------------------------------------------
    CREATE TABLE IF NOT EXISTS `step_table`
    (
      `pk` int(11) NOT NULL,
      PRIMARY KEY (`pk`),
      CONSTRAINT `fk_pk_stepTable_stepDef` FOREIGN KEY (`pk`) REFERENCES `step_def` (`pk`) ON DELETE CASCADE ON UPDATE CASCADE
    ) ENGINE = InnoDB
      DEFAULT CHARSET = latin1;


    -- -----------------------------------------------------
    -- Table `step_table_row`
    -- -----------------------------------------------------
    CREATE TABLE IF NOT EXISTS `step_table_row`
    (
      `pk`            int(11) NOT NULL AUTO_INCREMENT,
      `row_number`    int(11) DEFAULT NULL,
      `step_table_pk` int(11) NOT NULL,
      PRIMARY KEY (`pk`),
      UNIQUE KEY `stepTable_rowNumber_UNIQUE` (`step_table_pk`, `row_number`),
      CONSTRAINT `fk_stepTableRow_stepTable` FOREIGN KEY (`step_table_pk`) REFERENCES `step_table` (`pk`) ON DELETE CASCADE ON UPDATE CASCADE
    ) ENGINE = InnoDB
      DEFAULT CHARSET = latin1;


    -- -----------------------------------------------------
    -- Table `step_table_cell`
    -- -----------------------------------------------------
    CREATE TABLE IF NOT EXISTS `step_table_cell`
    (
      `pk`                 int(11) NOT NULL AUTO_INCREMENT,
      `cell_index`         int(11)      DEFAULT NULL,
      `editable`           bit(1)       DEFAULT NULL,
      `non_editable_value` varchar(255) DEFAULT NULL,
      `step_table_row_pk`  int(11) NOT NULL,
      PRIMARY KEY (`pk`),
      UNIQUE KEY `row_cellIndex_UNIQUE` (`step_table_row_pk`, `cell_index`),
      CONSTRAINT `fk_stepTableCell_stepTableRow` FOREIGN KEY (`step_table_row_pk`) REFERENCES `step_table_row` (`pk`) ON DELETE CASCADE ON UPDATE CASCADE
    ) ENGINE = InnoDB
      DEFAULT CHARSET = latin1;


    -- -----------------------------------------------------
    -- Table `run`
    -- -----------------------------------------------------
    CREATE TABLE IF NOT EXISTS `run`
    (
      `pk`                      int(11) NOT NULL AUTO_INCREMENT,
      `run_number`              int(11) DEFAULT NULL,
      `template_def_version_pk` int(11) NOT NULL,
      PRIMARY KEY (`pk`),
      UNIQUE KEY `template_runNumber_UNIQUE` (`run_number`, `template_def_version_pk`),
      KEY `fk_run_templateDefVersion` (`template_def_version_pk`),
      CONSTRAINT `fk_run_templateDefVersion` FOREIGN KEY (`template_def_version_pk`) REFERENCES `template_def_version` (`pk`)
    ) ENGINE = InnoDB
      DEFAULT CHARSET = latin1;


    -- -----------------------------------------------------
    -- Table `equipment_list`
    -- -----------------------------------------------------
    CREATE TABLE IF NOT EXISTS `equipment_list`
    (
      `pk`            int(11) NOT NULL AUTO_INCREMENT,
      `name`          varchar(255) DEFAULT NULL,
      `serial_number` varchar(255) DEFAULT NULL,
      `run_pk`        int(11) NOT NULL,
      PRIMARY KEY (`pk`),
      KEY `fk_equipmentList_run` (`run_pk`),
      CONSTRAINT `fk_equipmentList_run` FOREIGN KEY (`run_pk`) REFERENCES `run` (`pk`)
    ) ENGINE = InnoDB
      DEFAULT CHARSET = latin1;


    -- -----------------------------------------------------
    -- Table `run_step_results`
    -- -----------------------------------------------------
    CREATE TABLE IF NOT EXISTS `run_step_results`
    (
      `pk`          int(11) NOT NULL AUTO_INCREMENT,
      `run_pk`      int(11) NOT NULL,
      `step_def_pk` int(11) NOT NULL,
      PRIMARY KEY (`pk`),
      UNIQUE KEY `run_step_unique` (`run_pk`, `step_def_pk`),
      KEY `fk_runStepResults_stepDef` (`step_def_pk`),
      CONSTRAINT `fk_runStepResults_run` FOREIGN KEY (`run_pk`) REFERENCES `run` (`pk`),
      CONSTRAINT `fk_runStepResults_stepDef` FOREIGN KEY (`step_def_pk`) REFERENCES `step_def` (`pk`)
    ) ENGINE = InnoDB
      DEFAULT CHARSET = latin1;


    -- -----------------------------------------------------
    -- Table `run_step_file_result`
    -- -----------------------------------------------------
    CREATE TABLE IF NOT EXISTS `run_step_file_result`
    (
      `file_path` varchar(255) DEFAULT NULL,
      `pk`        int(11) NOT NULL,
      PRIMARY KEY (`pk`),
      CONSTRAINT `fk_pk_runStepFileResult_runStepResult` FOREIGN KEY (`pk`) REFERENCES `run_step_results` (`pk`)
    ) ENGINE = InnoDB
      DEFAULT CHARSET = latin1;


    -- -----------------------------------------------------
    -- Table `run_step_single_result`
    -- -----------------------------------------------------
    CREATE TABLE IF NOT EXISTS `run_step_single_result`
    (
      `value` varchar(255) DEFAULT NULL,
      `pk`    int(11) NOT NULL,
      PRIMARY KEY (`pk`),
      CONSTRAINT `fk_pk_runStepSingleResult_runStepResult` FOREIGN KEY (`pk`) REFERENCES `run_step_results` (`pk`)
    ) ENGINE = InnoDB
      DEFAULT CHARSET = latin1;


    -- -----------------------------------------------------
    -- Table `run_step_table_result`
    -- -----------------------------------------------------
    CREATE TABLE IF NOT EXISTS `run_step_table_cell_result`
    (
      `value`              varchar(255) DEFAULT NULL,
      `pk`                 int(11) NOT NULL,
      `step_table_cell_pk` int(11) NOT NULL,
      PRIMARY KEY (`pk`),
      UNIQUE KEY `run_cell_UNIQUE` (`step_table_cell_pk`, `pk`),
      CONSTRAINT `fk_pk_runStepTableCellResult_runStepResult` FOREIGN KEY (`pk`) REFERENCES `run_step_results` (`pk`),
      CONSTRAINT `fk_runStepTableCellResult_stepTableCell` FOREIGN KEY (`step_table_cell_pk`) REFERENCES `step_table_cell` (`pk`)
    ) ENGINE = InnoDB
      DEFAULT CHARSET = latin1;


    -- -----------------------------------------------------
    -- Table `run_step_comment`
    -- -----------------------------------------------------
    CREATE TABLE IF NOT EXISTS `run_step_comment`
    (
      `pk`                  int(11) NOT NULL AUTO_INCREMENT,
      `comment`             varchar(255) DEFAULT NULL,
      `is_nonconformance`   bit(1)       DEFAULT NULL,
      `timestamp`           bigint(20)   DEFAULT NULL,
      `run_step_results_pk` int(11) NOT NULL,
      PRIMARY KEY (`pk`),
      KEY `fk_runStepComment_runStepResults` (`run_step_results_pk`),
      CONSTRAINT `fk_runStepComment_runStepResults` FOREIGN KEY (`run_step_results_pk`) REFERENCES `run_step_results` (`pk`)
    ) ENGINE = InnoDB
      DEFAULT CHARSET = latin1;


    -- -----------------------------------------------------
    -- Table `run_step_result_history`
    -- -----------------------------------------------------
    CREATE TABLE IF NOT EXISTS `run_step_result_history`
    (
      `pk`                  int(11) NOT NULL AUTO_INCREMENT,
      `description`         varchar(255) DEFAULT NULL,
      `timestamp`           bigint(20)   DEFAULT NULL,
      `run_step_results_pk` int(11) NOT NULL,
      PRIMARY KEY (`pk`),
      KEY `fk_runStepResultHistory_runStepResults` (`run_step_results_pk`),
      CONSTRAINT `fk_runStepResultHistory_runStepResults` FOREIGN KEY (`run_step_results_pk`) REFERENCES `run_step_results` (`pk`)
    ) ENGINE = InnoDB
      DEFAULT CHARSET = latin1;


    -- update the schema version to 6.0
    SET majVersion = 2;
    SET minVersion = 0;
    UPDATE version SET major = majVersion, minor = minVersion WHERE type = 'DATABASE';

  END IF;

  -- DO NOT ADD ANYTHING AFTER THIS
END//
delimiter ;
CALL execute();
DROP PROCEDURE execute;
