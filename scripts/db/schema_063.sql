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

    IF majVersion = 62 AND minVersion = 0
    THEN

            CREATE TABLE `run_approval` (
            	`pk` INT(11) NOT NULL AUTO_INCREMENT,
            	`approval_type` VARCHAR(45) NOT NULL,
            	`user_id` INT(11) NOT NULL,
	            `run_pk` INT(11) NOT NULL,
	            `is_approved` BIT(1) NULL,
	            `last_reminder_date` DATE NULL,
	            `approver_order` INT(11) NOT NULL,
	            `due_date` DATE NULL,
	            PRIMARY KEY (`pk`),
	            UNIQUE INDEX `runPk_userId_approvalType` (`approval_type`, `user_id`, `run_pk`),
	            CONSTRAINT `fk_runApproval_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
	            CONSTRAINT `fk_runApproval_run` FOREIGN KEY (`run_pk`) REFERENCES `run` (`pk`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

            CREATE TABLE `run_closeout_comment` (
	            `pk` INT(11) NOT NULL AUTO_INCREMENT,
	            `run_approval_pk` INT(11) NOT NULL,
	            PRIMARY KEY (`pk`),
	            CONSTRAINT `fk_runCloseoutComment_comment` FOREIGN KEY (`pk`) REFERENCES `comment` (`pk`),
	            CONSTRAINT `fk_runCloseoutComment_runApproval` FOREIGN KEY (`run_approval_pk`) REFERENCES `run_approval` (`pk`) ON UPDATE NO ACTION ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

            CREATE TABLE `run_closeout_comment_reply` (
	            `pk` INT(11) NOT NULL AUTO_INCREMENT,
	            `run_closeout_comment_pk` INT(11) NOT NULL,
	            PRIMARY KEY (`pk`),
	            CONSTRAINT `fk_runCloseoutCommentReply_comment` FOREIGN KEY (`pk`) REFERENCES `comment` (`pk`),
	            CONSTRAINT `fk_runCloseoutCommentReply_runCloseoutComment` FOREIGN KEY (`run_closeout_comment_pk`) REFERENCES `run_closeout_comment` (`pk`) ON UPDATE NO ACTION ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

        -- -----------------------------------------------------
        -- Update version numbers
        -- -----------------------------------------------------
        SET majVersion = 63;
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
