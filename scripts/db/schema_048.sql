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

    IF majVersion = 47 AND minVersion = 0
    THEN

        -- add redlined_version to procedure details that's a FK to the run being redlined.
        ALTER TABLE `procedure_details`
            ADD COLUMN `redlined_version` INT(11) NULL DEFAULT NULL AFTER `hazard_description`,
            ADD CONSTRAINT `fk_procedureDetails_redlinedVersion` FOREIGN KEY (`redlined_version`)
                REFERENCES `procedure_details` (`pk`) ON UPDATE NO ACTION ON DELETE NO ACTION;

        -- new table - signatures for red and black lines. Has FKs to comment and parent second signature
        -- if the parent comment/signature is deleted, this signature should be too
        CREATE TABLE `black_red_line_signature` (
            `pk` INT(11) NOT NULL,
            `comment_fk` INT(11) NOT NULL,
            CONSTRAINT `fk_blackRedLineSignature_secondSignature` FOREIGN KEY (`pk`) REFERENCES `second_signature` (`pk`) ON UPDATE CASCADE ON DELETE CASCADE,
            CONSTRAINT `fk_blackRedLineSignature_comment` FOREIGN KEY (`comment_fk`) REFERENCES `comment` (`pk`) ON UPDATE CASCADE ON DELETE CASCADE
        )
            COLLATE='utf8_general_ci'
            ENGINE=InnoDB
        ;

        -- new table for redline comments - requires procedure detail FK, pk is fk to parent comment
        -- if parent comment is deleted, this should delete
        CREATE TABLE `red_line_comment` (
            `pk` INT(11) NOT NULL,
            `procedure_details_fk` INT(11) NOT NULL,
            `instruction_fk` INT(11) NULL,
            `step_group_def_fk` INT(11) NULL,
            `step_def_fk` INT(11) NULL,
            CONSTRAINT `fk_redLineComment_comment` FOREIGN KEY (`pk`) REFERENCES `comment` (`pk`) ON UPDATE CASCADE ON DELETE CASCADE,
            CONSTRAINT `fk_redLineComment_procedureDetails` FOREIGN KEY (`procedure_details_fk`) REFERENCES `procedure_details` (`pk`),
            CONSTRAINT `fk_redLineComment_procedureInstruction` FOREIGN KEY (`instruction_fk`) REFERENCES `procedure_instruction` (`pk`),
            CONSTRAINT `fk_redLineComment_stepGroupDef` FOREIGN KEY (`step_group_def_fk`) REFERENCES `step_group_def` (`pk`),
            CONSTRAINT `fk_redLineComment_stepDef` FOREIGN KEY (`step_def_fk`) REFERENCES `step_def` (`pk`)
        )
            COLLATE='utf8_general_ci'
            ENGINE=InnoDB
        ;

        -- -----------------------------------------------------
        -- Update version numbers
        -- -----------------------------------------------------
        SET majVersion = 48;
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
