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

    IF majVersion = 70 AND minVersion = 0
    THEN

           ALTER TABLE `procedure_header`
	            ADD COLUMN `submitted_for_review_date` TIMESTAMP NULL DEFAULT NULL AFTER `creation_date`,
	            ADD COLUMN `approved_date` TIMESTAMP NULL DEFAULT NULL AFTER `submitted_for_review_date`,
	            ADD COLUMN `released_date` TIMESTAMP NULL DEFAULT NULL AFTER `approved_date`;

	        CREATE TABLE `procedure_details_history` (
	            `pk` INT(11) NOT NULL AUTO_INCREMENT,
	            `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
	            `description` VARCHAR(1024) NOT NULL,
	            `user_id` INT(11) NOT NULL,
	            `procedure_details_pk` INT(11) NOT NULL,
	            PRIMARY KEY (`pk`),
	            CONSTRAINT `fk_procedureDetailsHistory_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
	            CONSTRAINT `fk_procedureDetailsHistory_procedureDetails` FOREIGN KEY (`procedure_details_pk`) REFERENCES `procedure_details` (`pk`) ON UPDATE NO ACTION ON DELETE NO ACTION
            );



        -- -----------------------------------------------------
        -- Update version numbers
        -- -----------------------------------------------------
        SET majVersion = 71;
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
