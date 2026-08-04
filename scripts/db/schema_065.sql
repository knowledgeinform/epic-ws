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

    IF majVersion = 64 AND minVersion = 0
    THEN

            ALTER TABLE `run`
                ADD COLUMN `closeout_submitter` INT(11) NULL DEFAULT NULL AFTER `closeout_completed_date`,
	            ADD CONSTRAINT `fk_run_users_closeout_submitter` FOREIGN KEY (`closeout_submitter`) REFERENCES `users` (`user_id`);

            CREATE TABLE `run_closeout_sticky_comment` (
	            `pk` INT(11) NOT NULL,
	            `instruction_pk` INT(11) NULL,
	            `step_group_pk` INT(11) NULL,
	            `step_pk` INT(11) NULL,
	            `run_pd_pk` INT(11) NULL,
	            `is_complete` BIT NOT NULL DEFAULT b'0',
	            CONSTRAINT `fk_runCloseoutStickyComment_comment` FOREIGN KEY (`pk`) REFERENCES `comment` (`pk`),
	            CONSTRAINT `fk_runCloseoutStickyComment_procedureInstruction` FOREIGN KEY (`instruction_pk`) REFERENCES `procedure_instruction` (`pk`),
	            CONSTRAINT `fk_runCloseoutStickyComment_stepGroup` FOREIGN KEY (`step_group_pk`) REFERENCES `step_group_def` (`pk`),
	            CONSTRAINT `fk_runCloseoutStickyComment_step` FOREIGN KEY (`step_pk`) REFERENCES `step_def` (`pk`),
	            CONSTRAINT `fk_runCloseoutStickyComment_procedureDetails` FOREIGN KEY (`run_pd_pk`) REFERENCES `procedure_details` (`pk`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8;


        -- -----------------------------------------------------
        -- Update version numbers
        -- -----------------------------------------------------
        SET majVersion = 65;
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
