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

    IF majVersion = 61 AND minVersion = 0
    THEN

				CREATE TABLE `run_step_attachment` (
                  `pk` int(11) NOT NULL,
                  `run_step_pk` int(11) NOT NULL,
                  PRIMARY KEY (`pk`),
                  KEY `fk_runStepAttachment_stepDef` (`run_step_pk`),
                  CONSTRAINT `fk_attachmentForRunStep` FOREIGN KEY (`pk`) REFERENCES `attachment` (`pk`) ON DELETE CASCADE ON UPDATE CASCADE,
                  CONSTRAINT `fk_runStepAttachment_stepDef` FOREIGN KEY (`run_step_pk`) REFERENCES `step_def` (`pk`) ON DELETE NO ACTION ON UPDATE NO ACTION
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

				CREATE TABLE `step_def_attachment` (
                  `pk` int(11) NOT NULL,
                  `step_def_pk` int(11) NOT NULL,
                  PRIMARY KEY (`pk`),
                  KEY `fk_stepDefAttachment_stepDef` (`step_def_pk`),
                  CONSTRAINT `fk_attachmentForStepDef` FOREIGN KEY (`pk`) REFERENCES `attachment` (`pk`) ON DELETE CASCADE ON UPDATE CASCADE,
                  CONSTRAINT `fk_stepDefAttachment_stepDef` FOREIGN KEY (`step_def_pk`) REFERENCES `step_def` (`pk`) ON DELETE NO ACTION ON UPDATE NO ACTION
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8;


				ALTER TABLE `attachment` ADD COLUMN `original_filename` VARCHAR(255) NOT NULL;
				ALTER TABLE `attachment` CHANGE COLUMN `path` `path` VARCHAR(255) NULL ;
                UPDATE attachment SET original_filename = filename;
                UPDATE attachment SET filename = CONCAT(REPLACE(path, "/", ''),filename);
                
        -- -----------------------------------------------------
        -- Update version numbers
        -- -----------------------------------------------------
        SET majVersion = 62;
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
