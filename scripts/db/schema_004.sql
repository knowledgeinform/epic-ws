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

  IF majVersion = 3 AND minVersion = 0 THEN


    -- -----------------------------------------------------
    -- Alter Table `template_def_version`
    -- -----------------------------------------------------
    ALTER TABLE `template_def_version`
      CHANGE COLUMN `status` `status` VARCHAR(15) NOT NULL DEFAULT 'DRAFT' ;

    -- -----------------------------------------------------
    -- Alter Table `template_header`
    -- -----------------------------------------------------
    ALTER TABLE `template_header`
      DROP FOREIGN KEY `fk_tempateHeader_templateDefVersion`;

    ALTER TABLE `template_header`
      CHANGE COLUMN `author` `author` INT(11) NOT NULL ,
      DROP INDEX `fk_tempateHeader_templateDefVersion` ,
      ADD INDEX `fk_templateHeader_templateDefVersion` (`template_def_version_pk` ASC),
      ADD INDEX `fk_templateHeader_users_idx` (`author` ASC);

    ALTER TABLE `template_header`
      ADD CONSTRAINT `fk_templateHeader_templateDefVersion`
        FOREIGN KEY (`template_def_version_pk`)
          REFERENCES `template_def_version` (`pk`),
      ADD CONSTRAINT `fk_templateHeader_users`
        FOREIGN KEY (`author`)
          REFERENCES `users` (`user_id`)
          ON DELETE RESTRICT
          ON UPDATE RESTRICT;


    -- update the schema version to 6.0
    SET majVersion = 4;
    SET minVersion = 0;
    UPDATE version SET major = majVersion, minor = minVersion WHERE type = 'DATABASE';

  END IF;

  -- DO NOT ADD ANYTHING AFTER THIS
END//
delimiter ;
CALL execute();
DROP PROCEDURE execute;
