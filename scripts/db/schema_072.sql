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

    IF majVersion = 71 AND minVersion = 0
    THEN

            -- Create history table to where data will be moved. Two temporary columns for the history tables pks
	        CREATE TABLE `history` (
	            `pk` INT(11) NOT NULL AUTO_INCREMENT,
	            `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
	            `description` VARCHAR(1024) NOT NULL,
	            `user_id` INT(11) NOT NULL,
	            `step_def_pk` INT(11) DEFAULT NULL,
	            `procedure_details_pk` INT(11) DEFAULT NULL,
	            PRIMARY KEY (`pk`),
	            CONSTRAINT `fk_history_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
	            CONSTRAINT `fk_history_stepDef` FOREIGN KEY (`step_def_pk`) REFERENCES `step_def` (`pk`) ON UPDATE NO ACTION ON DELETE NO ACTION,
	            CONSTRAINT `fk_history_procedureDetails` FOREIGN KEY (`procedure_details_pk`) REFERENCES `procedure_details` (`pk`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
                COLLATE='utf8_general_ci'
                ENGINE=InnoDB
            ;

            INSERT INTO `history` (`timestamp`, `description`, `user_id`, `step_def_pk`)
                SELECT `timestamp`, `description`, `user_id`, `step_def_id` FROM `run_step_history`;

            INSERT INTO `history` (`timestamp`, `description`, `user_id`, `procedure_details_pk`)
                SELECT `timestamp`, `description`, `user_id`, `procedure_details_pk` FROM `procedure_details_history`;

            DROP TABLE `run_step_history`;

            DROP TABLE `procedure_details_history`;



        -- -----------------------------------------------------
        -- Update version numbers
        -- -----------------------------------------------------
        SET majVersion = 72;
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
