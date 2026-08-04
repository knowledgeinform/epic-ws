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

    IF majVersion = 13 AND minVersion = 0
    THEN

		ALTER TABLE  `step_def` 
		ADD COLUMN `step_name` VARCHAR(255) NULL AFTER `step_group_def_pk`,
		ADD COLUMN `require_witness` BIT(1) NULL DEFAULT NULL AFTER `step_name`,
		ADD COLUMN `hazardous` BIT(1) NULL DEFAULT NULL AFTER `require_witness`,
		ADD COLUMN `mandatory_inspection` BIT(1) NULL DEFAULT NULL AFTER `hazardous`,
		CHANGE COLUMN `description` `instructions` VARCHAR(100) NULL DEFAULT NULL ;


      SET majVersion = 14;
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
