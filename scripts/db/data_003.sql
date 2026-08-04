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

  IF majVersion = 2 && minVersion = 0 THEN

      INSERT INTO `subsystem` (`name`, `short_name`) VALUES ('Thermal', 'THRM');
      INSERT INTO `subsystem` (`name`, `short_name`) VALUES ('Guidance and Control', 'GC');
      INSERT INTO `subsystem` (`name`, `short_name`) VALUES ('Propulsion', 'PROP');
      INSERT INTO `subsystem` (`name`, `short_name`) VALUES ('Power', 'POW');
      INSERT INTO `subsystem` (`name`, `short_name`) VALUES ('Harness', 'HAR');
      INSERT INTO `subsystem` (`name`, `short_name`) VALUES ('RF', 'RF');
      INSERT INTO `subsystem` (`name`, `short_name`) VALUES ('Instrument', 'INST');
      INSERT INTO `subsystem` (`name`, `short_name`) VALUES ('Mechanical', 'MECH');
      INSERT INTO `subsystem` (`name`, `short_name`) VALUES ('Avionics', 'AVI');
      INSERT INTO `subsystem` (`name`, `short_name`) VALUES ('EGSE', 'EGSE');
      INSERT INTO `subsystem` (`name`, `short_name`) VALUES ('System I&T', 'SIT');

    SET majVersion = 3;
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
