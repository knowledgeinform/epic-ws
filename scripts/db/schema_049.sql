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

    IF majVersion = 48 AND minVersion = 0
    THEN

      CREATE TABLE `user_procedure_detail` (
                                             `user_id` int NOT NULL,
                                             `procedure_detail_pk` int NOT NULL,
                                             PRIMARY KEY (`user_id`,`procedure_detail_pk`),
                                             KEY `procedure_detail_pk` (`procedure_detail_pk`),
                                             CONSTRAINT `user_procedure_detail_FK1`
                                               FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
                                             CONSTRAINT `user_procedure_detail_FK2`
                                               FOREIGN KEY (`procedure_detail_pk`) REFERENCES `procedure_details` (`pk`)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8;


      -- update the table with "current favorites"
      REPLACE INTO user_procedure_detail
      SELECT author, procedure_details_pk FROM procedure_header
      UNION
      SELECT author, procedure_details_pk FROM run;

        -- -----------------------------------------------------
        -- Update version numbers
        -- -----------------------------------------------------
        SET majVersion = 49;
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
