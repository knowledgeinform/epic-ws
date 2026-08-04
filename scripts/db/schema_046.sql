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

    IF majVersion = 45 AND minVersion = 0
    THEN
      ALTER TABLE `procedure_details`
        CHANGE COLUMN `id` `id` VARCHAR(50) NULL DEFAULT NULL ;

      ALTER TABLE `comment`
        CHANGE COLUMN `comment_text` `comment_text` VARCHAR(2048) NOT NULL ;

      ALTER TABLE `procedure_instruction`
        CHANGE COLUMN `text` `text` VARCHAR(10000) NULL DEFAULT NULL ;

      ALTER TABLE `step_def`
        CHANGE COLUMN `instructions` `instructions` VARCHAR(5000) NULL DEFAULT NULL ;


      -- -----------------------------------------------------
        -- Update version numbers
        -- -----------------------------------------------------
        SET majVersion = 46;
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
