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

    IF majVersion = 34 AND minVersion = 0
    THEN

        ALTER TABLE `run`
                DROP INDEX `procedure_runNumber_UNIQUE`,
                DROP COLUMN `run_number`;

        ALTER TABLE `procedure_def_version`
            DROP FOREIGN KEY `fk_procedureDefVersion_procedureDef`,
            DROP INDEX `version_procedure_UNIQUE`,
            ADD COLUMN `run_number` INT NULL DEFAULT NULL AFTER `edit_type`;

        ALTER TABLE `procedure_def_version`
            ADD UNIQUE `version_procedure_run_UNIQUE`(`version`, `procedure_def_pk`, `run_number`),
            ADD CONSTRAINT `fk_procedureDefVersion_procedureDef` FOREIGN KEY (`procedure_def_pk`) REFERENCES `procedure_def` (`pk`);

        -- -----------------------------------------------------
        -- Update version numbers
        -- -----------------------------------------------------
        SET majVersion = 35;
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
