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

  DECLARE VersionTableCount INT;

  SELECT COUNT(1) INTO VersionTableCount
  FROM information_schema.tables
  WHERE table_schema = database()
    AND table_name = 'version';

  IF VersionTableCount = 0 THEN
    CREATE TABLE IF NOT EXISTS `version`
    (
      `type`  varchar(20) NOT NULL,
      `major` int         NOT NULL,
      `minor` int         NOT NULL,
      PRIMARY KEY (`type`)
    ) ENGINE = InnoDB
      DEFAULT CHARSET = latin1 ;

    CREATE TABLE IF NOT EXISTS `app_configuration`
    (
      `config_key`   varchar(255) NOT NULL,
      `config_value` varchar(255) DEFAULT NULL,
      `comment`      varchar(255) DEFAULT NULL,
      PRIMARY KEY (`config_key`),
      UNIQUE KEY `config_key` (`config_key`)
    ) ENGINE = InnoDB
      DEFAULT CHARSET = latin1;


    CREATE TABLE IF NOT EXISTS `users`
    (
      `user_id`      int(11)      NOT NULL AUTO_INCREMENT,
      `username`     varchar(255) NOT NULL,
      `display_name` varchar(255) NOT NULL,
      `last_login`   bigint(20) DEFAULT NULL,
      PRIMARY KEY (`user_id`),
      UNIQUE KEY `username` (`username`)
    ) ENGINE = InnoDB
      DEFAULT CHARSET = latin1;


    INSERT INTO `version` (type, major, minor) values ('DATABASE', 1, 0);

  END IF;

  -- DO NOT ADD ANYTHING AFTER THIS
END//
delimiter ;
CALL execute();
DROP PROCEDURE execute;
