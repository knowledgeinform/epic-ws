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

  IF majVersion = 9 AND minVersion = 0 THEN


    -- -----------------------------------------------------
    -- Alter table `procedure_approvals`
    -- -----------------------------------------------------
    ALTER TABLE `procedure_approvals`
      DROP FOREIGN KEY `fk_procedureApprovals_templateDef`;
    ALTER TABLE `procedure_approvals`
      CHANGE COLUMN `template_def_pk` `template_def_version_pk` INT(11) NOT NULL ,
      ADD INDEX `fk_procedureApprovals_templateDefVersion_idx` (`template_def_version_pk` ASC),
      DROP INDEX `fk_procedureApprovals_templateDef_idx` ;

    ALTER TABLE `procedure_approvals`
      ADD CONSTRAINT `fk_procedureApprovals_templateDefVersion`
        FOREIGN KEY (`template_def_version_pk`)
          REFERENCES `template_def_version` (`pk`);


    -- update the schema version to 6.0
    SET majVersion = 10;
    SET minVersion = 0;
    UPDATE version SET major = majVersion, minor = minVersion WHERE type = 'DATABASE';

  END IF;

  -- DO NOT ADD ANYTHING AFTER THIS
END//
delimiter ;
CALL execute();
DROP PROCEDURE execute;
