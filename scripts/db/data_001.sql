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

  IF majVersion is null && minVersion is null THEN

    INSERT INTO `users` (`username`, `display_name`) VALUES ('user1', 'Default User 1');

    SET majVersion = 1;
    SET minVersion = 0;

    INSERT INTO version (type, major, minor) VALUES ('DATA', majVersion, minVersion);

  END IF;

  -- DO NOT ADD ANYTHING AFTER THIS
END//
delimiter ;
CALL execute();
DROP PROCEDURE execute;
