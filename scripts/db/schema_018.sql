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

    IF majVersion = 17 AND minVersion = 0
    THEN


      -- -----------------------------------------------------
      -- Table `subsystem`
      -- -----------------------------------------------------
      CREATE TABLE IF NOT EXISTS `subsystem` (
        `pk` int(11) NOT NULL AUTO_INCREMENT,
        `name` varchar(255) DEFAULT NULL,
        `short_name` varchar(255) DEFAULT NULL,
        PRIMARY KEY (`pk`),
        UNIQUE KEY `name_UNIQUE` (`name`),
        UNIQUE KEY `shortName_UNIQUE` (`short_name`)
      ) ENGINE=InnoDB DEFAULT CHARSET=latin1;

      -- -----------------------------------------------------
      -- Table `procedure_def`
      -- -----------------------------------------------------
      ALTER TABLE `procedure_def`
        ADD COLUMN `subsystem_pk` int(11) NOT NULL;
      
      -- -----------------------------------------------------
      -- Table `procedure_def_version`
      -- -----------------------------------------------------
      ALTER TABLE `procedure_def_version`
        ADD COLUMN `procedure_id` TEXT;

      -- -----------------------------------------------------
      -- Update version numbers
      -- -----------------------------------------------------
      SET majVersion = 18;
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
