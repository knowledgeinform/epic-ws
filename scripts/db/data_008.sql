-- =======================================================================================

-- !! IMPORTANT !!
-- !! Only add changes to the end of the MyProcedure.
-- !! Each change should be within an IF statement to ensure the change will only run if
-- !! the database is at the appropriate 'static data' version.


-- =======================================================================================
DROP PROCEDURE IF EXISTS execute;
delimiter //
CREATE PROCEDURE execute()
BEGIN
    -- declare and set variable to store current db & version;
    DECLARE db varchar(20);
    DECLARE majVersion INT;
    DECLARE minVersion INT;

    SELECT database() INTO db;
    SELECT major, minor FROM version WHERE type = 'DATA' INTO majVersion, minVersion;

    IF majVersion = 7 && minVersion = 0 THEN

				-- Set email reminders to go out 1x a day (set threshold to 1440 minutes).
				UPDATE app_configuration
				SET config_value=1440
				WHERE config_key="EMAIL_REMINDER_THRESHOLD_MINUTES";

        SET majVersion = 8;
        SET minVersion = 0;

        UPDATE version
        SET major = majVersion,
            minor = minVersion
        WHERE type = 'DATA';

    END IF;

    -- DO NOT ADD ANYTHING AFTER THIS
END//
delimiter ;
CALL execute();
DROP PROCEDURE execute;
