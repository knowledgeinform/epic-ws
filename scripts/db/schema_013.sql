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

    IF majVersion = 12 AND minVersion = 0
    THEN

		ALTER TABLE `step_def` 
		DROP FOREIGN KEY `fk_stepDef_procedureDefVersion`;
		ALTER TABLE `step_def` 
		DROP COLUMN `procedure_def_version_pk`,
		ADD COLUMN `step_group_def_pk` INT(11) NOT NULL AFTER `second_signature`,
		ADD INDEX `fk_stepDef_stepGroupDef_idx` (`step_group_def_pk` ASC),
		DROP INDEX `fk_stepDef_procedureDefVersion` ;
		
		ALTER TABLE `step_def` 
		ADD CONSTRAINT `fk_stepDef_stepGroupDef`
		  FOREIGN KEY (`step_group_def_pk`)
		  REFERENCES `step_group_def` (`pk`)
		  ON DELETE RESTRICT
		  ON UPDATE RESTRICT;


      SET majVersion = 13;
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
