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
    DECLARE lastSubsystem INT;

    SELECT database() INTO db;
    SELECT major, minor FROM version WHERE type = 'DATABASE' INTO majVersion, minVersion;

    IF majVersion = 18 AND minVersion = 0
    THEN


      -- -----------------------------------------------------
      -- Table `procedure_def`
      -- -----------------------------------------------------
      SELECT max(`pk`) FROM `subsystem` INTO lastSubsystem;
      UPDATE `procedure_def` SET `subsystem_pk` = lastSubsystem;

      ALTER TABLE `procedure_def`
        ADD INDEX `fk_procedureDef_subsystem` (`subsystem_pk` ASC);

      ALTER TABLE `procedure_def`
        ADD CONSTRAINT `fk_procedureDef_subsystem`
      FOREIGN KEY (`subsystem_pk`)
      REFERENCES `subsystem` (`pk`);

      -- -----------------------------------------------------
      -- Update version numbers
      -- -----------------------------------------------------
      SET majVersion = 19;
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
