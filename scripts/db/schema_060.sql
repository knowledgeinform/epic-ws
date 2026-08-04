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

    IF majVersion = 59 AND minVersion = 0
    THEN

				CREATE TABLE `attachment` (
                  `pk` int(11) NOT NULL AUTO_INCREMENT,
                  `path` varchar(255) NOT NULL,
                  `filename` varchar(255) NOT NULL,
                  `isImage` bit(1) NOT NULL,
                  PRIMARY KEY (`pk`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8;


                CREATE TABLE `procedure_attachment` (
                  `pk` int(11) NOT NULL,
                  `procedure_header_pk` int(11) NOT NULL,
                  PRIMARY KEY (`pk`),
                  KEY `fk_procedureAttachment_procedureHeader` (`procedure_header_pk`),
                  CONSTRAINT `fk_attachment` FOREIGN KEY (`pk`) REFERENCES `attachment` (`pk`) ON DELETE CASCADE ON UPDATE CASCADE,
                  CONSTRAINT `fk_procedureAttachment_procedureHeader` FOREIGN KEY (`procedure_header_pk`) REFERENCES `procedure_header` (`pk`) ON DELETE NO ACTION ON UPDATE NO ACTION
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

                INSERT INTO `app_configuration` (`config_key`, `config_value`, `comment`) VALUES ('UPLOAD_ROOT_DIR', '/project/epic/attachments', 'The root directory where file attachments are stored.');

        -- -----------------------------------------------------
        -- Update version numbers
        -- -----------------------------------------------------
        SET majVersion = 60;
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
