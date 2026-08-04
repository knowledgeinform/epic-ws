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

  IF majVersion = 1 && minVersion = 0 THEN

      INSERT INTO `program` (`name`, `short_name`) VALUES ('Double Asteroid Redirection Test', 'DART');

    SET majVersion = 2;
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
