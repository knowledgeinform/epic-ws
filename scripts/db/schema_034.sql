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

    IF majVersion = 33 AND minVersion = 0
    THEN

        CREATE TABLE `testing_phase` (
                                         `pk` INT NOT NULL AUTO_INCREMENT,
                                         `name` VARCHAR(255) NULL DEFAULT NULL,
                                         `short_name` VARCHAR(255) NULL DEFAULT NULL,
                                         `code` VARCHAR(255) NULL DEFAULT NULL,
                                         PRIMARY KEY (`pk`),
                                         UNIQUE INDEX `name` (`name`),
                                         UNIQUE INDEX `short_name` (`short_name`),
                                         UNIQUE INDEX `code` (`code`)
        )
            COLLATE='latin1_swedish_ci'
            ENGINE=InnoDB
        ;

        ALTER TABLE `run`
            ADD COLUMN `testing_phase_fk` INT(11) NOT NULL AFTER `description`,
            ADD CONSTRAINT `fk_run_testingPhase` FOREIGN KEY (`testing_phase_fk`) REFERENCES `testing_phase` (`pk`);

        ALTER TABLE `procedure_def_version`
            CHANGE COLUMN `procedure_id` `id` TEXT NULL DEFAULT NULL AFTER `status`;

        -- -----------------------------------------------------
        -- Update version numbers
        -- -----------------------------------------------------
        SET majVersion = 34;
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
