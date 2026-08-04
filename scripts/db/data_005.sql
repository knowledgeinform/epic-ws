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

    IF majVersion = 4 && minVersion = 0 THEN

        INSERT INTO `testing_phase` (`name`, `short_name`, `code`) VALUES ('Pre-I&T', 'PREI', '000');
        INSERT INTO `testing_phase` (`name`, `short_name`, `code`) VALUES ('Spacecraft Integration', 'SPCI', '001');
        INSERT INTO `testing_phase` (`name`, `short_name`, `code`) VALUES ('SC Environmental Test', 'SET', '002');
        INSERT INTO `testing_phase` (`name`, `short_name`, `code`) VALUES ('Launch', 'LNCH', '003');

        SET majVersion = 5;
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
