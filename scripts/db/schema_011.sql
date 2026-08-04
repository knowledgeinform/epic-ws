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

  IF majVersion = 10 AND minVersion = 0
  THEN
    ALTER TABLE `template_def`
      RENAME TO `procedure_def`;

    ALTER TABLE `procedure_def`
      DROP INDEX `fk_templateDef_program`,
      ADD INDEX `fk_procedureDef_program` (`program_pk` ASC);

    ALTER TABLE `procedure_def`
      DROP FOREIGN KEY `fk_templateDef_program`;
    ALTER TABLE `procedure_def`
      ADD CONSTRAINT `fk_procedureDef_program`
        FOREIGN KEY (`program_pk`)
          REFERENCES `program` (`pk`);

    ALTER TABLE `template_def_version`
      RENAME TO `procedure_def_version` ;

    ALTER TABLE `procedure_def_version`
      DROP FOREIGN KEY `fk_templateDefVersion_templateDef`;
    ALTER TABLE `procedure_def_version`
      CHANGE COLUMN `template_def_pk` `procedure_def_pk` INT(11) NOT NULL ,
      DROP INDEX `version_template_UNIQUE` ,
      ADD UNIQUE INDEX `version_procedure_UNIQUE` (`procedure_def_pk` ASC, `version` ASC);
    ALTER TABLE `procedure_def_version`
      ADD CONSTRAINT `fk_procedureDefVersion_procedureDef`
        FOREIGN KEY (`procedure_def_pk`)
          REFERENCES `procedure_def` (`pk`);

    ALTER TABLE `template_header`
      RENAME TO `procedure_header` ;

    ALTER TABLE `procedure_header`
      DROP FOREIGN KEY `fk_templateHeader_templateDefVersion`,
      DROP FOREIGN KEY `fk_templateHeader_users`;
    ALTER TABLE `procedure_header`
      CHANGE COLUMN `template_def_version_pk` `procedure_def_version_pk` INT(11) NOT NULL ,
      DROP INDEX `fk_templateHeader_templateDefVersion` ,
      ADD INDEX `fk_procedureHeader_procedureDefVersion` (`procedure_def_version_pk` ASC),
      DROP INDEX `fk_templateHeader_users_idx` ,
      ADD INDEX `fk_procedureHeader_users_idx` (`author` ASC);
    ALTER TABLE `procedure_header`
      ADD CONSTRAINT `fk_procedureHeader_procedureDefVersion`
        FOREIGN KEY (`procedure_def_version_pk`)
          REFERENCES `procedure_def_version` (`pk`),
      ADD CONSTRAINT `fk_procedureHeader_users`
        FOREIGN KEY (`author`)
          REFERENCES `users` (`user_id`);

    ALTER TABLE `run`
      DROP FOREIGN KEY `fk_run_templateDefVersion`;
    ALTER TABLE `run`
      CHANGE COLUMN `template_def_version_pk` `procedure_def_version_pk` INT(11) NOT NULL ,
      DROP INDEX `template_runNumber_UNIQUE` ,
      ADD UNIQUE INDEX `procedure_runNumber_UNIQUE` (`run_number` ASC, `procedure_def_version_pk` ASC),
      DROP INDEX `fk_run_templateDefVersion` ,
      ADD INDEX `fk_run_procedureDefVersion` (`procedure_def_version_pk` ASC);
    ALTER TABLE `run`
      ADD CONSTRAINT `fk_run_templateDefVersion`
        FOREIGN KEY (`procedure_def_version_pk`)
          REFERENCES `procedure_def_version` (`pk`);

    ALTER TABLE `run`
      DROP FOREIGN KEY `fk_run_templateDefVersion`;
    ALTER TABLE `run`
      ADD CONSTRAINT `fk_run_procedureDefVersion`
        FOREIGN KEY (`procedure_def_version_pk`)
          REFERENCES `procedure_def_version` (`pk`);

    ALTER TABLE `step_def`
      DROP FOREIGN KEY `fk_stepDef_templateDefVersion`;
    ALTER TABLE `step_def`
      CHANGE COLUMN `template_def_version_pk` `procedure_def_version_pk` INT(11) NOT NULL ,
      DROP INDEX `fk_stepDef_templateDefVersion` ,
      ADD INDEX `fk_stepDef_procedureDefVersion` (`procedure_def_version_pk` ASC);
    ALTER TABLE `step_def`
      ADD CONSTRAINT `fk_stepDef_procedureDefVersion`
        FOREIGN KEY (`procedure_def_version_pk`)
          REFERENCES `procedure_def_version` (`pk`);

    ALTER TABLE `procedure_approvals`
      DROP FOREIGN KEY `fk_procedureApprovals_templateDefVersion`;
    ALTER TABLE `procedure_approvals`
      CHANGE COLUMN `template_def_version_pk` `procedure_def_version_pk` INT(11) NOT NULL ,
      DROP INDEX `fk_procedureApprovals_templateDefVersion_idx` ,
      ADD INDEX `fk_procedureApprovals_procedureDefVersion_idx` (`procedure_def_version_pk` ASC);
    ALTER TABLE `procedure_approvals`
      ADD CONSTRAINT `fk_procedureApprovals_procedureDefVersion`
        FOREIGN KEY (`procedure_def_version_pk`)
          REFERENCES `procedure_def_version` (`pk`);


    SET majVersion = 11;
    SET minVersion = 0;
    UPDATE version
    SET major = majVersion,
        minor = minVersion
    WHERE type = 'DATABASE';

  END IF;

  -- DO NOT ADD ANYTHING AFTER THIS
END//
delimiter ;
CALL execute();
DROP PROCEDURE execute;
