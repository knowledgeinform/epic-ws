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

  IF majVersion = 3 && minVersion = 0 THEN

    -- data_002.sql inserted dummy programs and subsystems but mistakenly
    -- assigned codes to the short_name column; here I correct the short_names.
    -- I also replace the codes with ones containing letters to remind us it's
    -- possible.
      UPDATE `program` SET `code` = 'DART' WHERE `short_name` = 'DART';

      UPDATE `subsystem` SET `code` = '001' WHERE `short_name` = 'THRM';
      UPDATE `subsystem` SET `code` = '002' WHERE `short_name` = 'GC';
      UPDATE `subsystem` SET `code` = '003' WHERE `short_name` = 'PROP';
      UPDATE `subsystem` SET `code` = '004' WHERE `short_name` = 'POW';
      UPDATE `subsystem` SET `code` = '005' WHERE `short_name` = 'HAR';
      UPDATE `subsystem` SET `code` = '006' WHERE `short_name` = 'RF';
      UPDATE `subsystem` SET `code` = '007' WHERE `short_name` = 'INST';
      UPDATE `subsystem` SET `code` = '008' WHERE `short_name` = 'MECH';
      UPDATE `subsystem` SET `code` = '009' WHERE `short_name` = 'AVI';
      UPDATE `subsystem` SET `code` = '010' WHERE `short_name` = 'EGSE';
      UPDATE `subsystem` SET `code` = '011' WHERE `short_name` = 'SIT';

    SET majVersion = 4;
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
