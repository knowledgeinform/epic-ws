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

    IF majVersion = 29 AND minVersion = 0
    THEN

        -- Drop the second_signature column - not being used
        ALTER TABLE `step_def`
            DROP COLUMN `second_signature`;

        -- -----------------------------------------------------
        -- Table `step_checkbox`
        -- -----------------------------------------------------
        CREATE TABLE IF NOT EXISTS `step_checkbox`
        (
            `pk` int(11) NOT NULL,
            PRIMARY KEY (`pk`),
            CONSTRAINT `fk_pk_stepCheckbox_stepDef` FOREIGN KEY (`pk`) REFERENCES `step_def` (`pk`) ON DELETE CASCADE ON UPDATE CASCADE
        ) ENGINE = InnoDB
          DEFAULT CHARSET = latin1;


        -- -----------------------------------------------------
        -- Update version numbers
        -- -----------------------------------------------------
        SET majVersion = 30;
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