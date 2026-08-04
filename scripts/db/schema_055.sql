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

    IF majVersion = 54 AND minVersion = 0
    THEN

				-- Create user_messages table
				CREATE TABLE `user_messages` (
					`pk` INT(11) NOT NULL AUTO_INCREMENT,
					`user` INT(11) NOT NULL,
					`message_type` VARCHAR(45) NOT NULL,
					`time_last_sent` TIMESTAMP NOT NULL,
				  PRIMARY KEY (`pk`),
					UNIQUE KEY `user_message_type` (`user`, `message_type`),
					KEY `fk_userMessages_user_idx` (`user`),
					CONSTRAINT `fk_userMessages_user` FOREIGN KEY (`user`) REFERENCES `users` (`user_id`)
				);

				-- Add default email engine configuration values
				INSERT INTO `app_configuration` (config_key, config_value, comment)
				VALUES 
					("EMAIL_FROM_ADDR", "epic-noreply@bitbucket.jhuapl.edu", "The 'from' address for EPIC email notifications."),
					("EMAIL_SERVER_HOSTNAME", "mail.jhuapl.edu", "The email server that should be used to send email notifications."),
					("APP_CONTEXT_ROOT", "/EPIC", "The context root of EPIC. Used to help generate URLs."),
					("EMAIL_REMINDER_CHECK_FREQUENCY_MINUTES", "10", "The frequency EPIC should check for email reminders to send."),
					("EMAIL_REMINDER_THRESHOLD_MINUTES", "240", "How long should elapse since the last reminder before a new one is sent to a user.");	

        -- -----------------------------------------------------
        -- Update version numbers
        -- -----------------------------------------------------
        SET majVersion = 55;
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
