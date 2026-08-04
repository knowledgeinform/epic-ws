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

    IF majVersion = 72 AND minVersion = 0
    THEN

          CREATE Table `program_roles` (
            `pk` int(11) NOT NULL AUTO_INCREMENT,
            `name` varchar(32) NOT NULL,
            PRIMARY KEY (`pk`)
          );

          CREATE Table `procedure_change_types` (
            `pk` int(11) NOT NULL AUTO_INCREMENT,
            `name` varchar(32) NOT NULL,
            `is_enabled` BIT(1) NOT NULL DEFAULT b'1',
            `accepts_all_signatures` BIT(1) NOT NULL DEFAULT b'0',
            `description` varchar(100) NULL,
            PRIMARY KEY (`pk`)
          );

          -- Ideally these data entry statements would be in a data_*.sql file, but they are incorporated here
          -- because the schema/data updates have to happen in a certain order and we didn't want to create
          -- multiple schema/data files for all the changes.
          INSERT INTO procedure_change_types (name, is_enabled, accepts_all_signatures, description)
            VALUES ("Non-specific", false, true, "Non-specific change type; primarily used for pre-v1.1.0 comments");

          CREATE TABLE `roster` (
            `user_id` int(11) NOT NULL,
            `program_pk` int(11) NOT NULL,
            `role_pk` int(11) NOT NULL,
            PRIMARY KEY (`user_id`, `program_pk`, `role_pk`),
            CONSTRAINT `fk_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
            CONSTRAINT `fk_program_pk` FOREIGN KEY (`program_pk`) REFERENCES `program` (`pk`),
            CONSTRAINT `fk_role_pk` FOREIGN KEY (`role_pk`) REFERENCES `program_roles` (`pk`)
          );

          CREATE TABLE `program_role_to_change_type` (
            `program_role_pk` int(11) NOT NULL,
            `procedure_change_type_pk` int(11) NOT NULL,
            CONSTRAINT `fk_program_role_pk` FOREIGN KEY (`program_role_pk`) REFERENCES `program_roles` (`pk`),
            CONSTRAINT `fk_procedure_change_type_pk` FOREIGN KEY (`procedure_change_type_pk`) REFERENCES `procedure_change_types` (`pk`)
          );

          CREATE TABLE `program_required_roles` (
            `program_pk` int(11) NOT NULL,
            `program_role_pk` int(11) NOT NULL,
            CONSTRAINT `fk_program_role_pk2` FOREIGN KEY (`program_role_pk`) REFERENCES `program_roles` (`pk`),
            CONSTRAINT `fk_program_pk1` FOREIGN KEY (`program_pk`) REFERENCES `program` (`pk`)
          );

          ALTER TABLE `red_line_comment`
            ADD COLUMN `procedure_change_type` int(11) NOT NULL AFTER `step_def_fk`;

          UPDATE `red_line_comment`
            SET `procedure_change_type` = (SELECT `pk` FROM `procedure_change_types` WHERE `name` = "Non-specific");

          ALTER TABLE `red_line_comment`
            ADD CONSTRAINT `fk_procedure_change_type` FOREIGN KEY (`procedure_change_type`)
              REFERENCES `procedure_change_types` (`pk`);

          ALTER TABLE `black_line_comment`
            ADD COLUMN `procedure_change_type` int(11) NOT NULL AFTER `step_def_fk`;

          UPDATE `black_line_comment`
            SET `procedure_change_type` = (SELECT `pk` FROM `procedure_change_types` WHERE `name` = "Non-specific");

          ALTER TABLE `black_line_comment`
            -- NB: Constraints on separate tables cannot have identical names. 
            ADD CONSTRAINT `fk_procedure_change_type1` FOREIGN KEY (`procedure_change_type`)
              REFERENCES `procedure_change_types` (`pk`);

          ALTER TABLE `black_red_line_signature`
            ADD COLUMN `program_role_pk` int(11) AFTER `comment_fk`,
            ADD UNIQUE `one_signature_per_role_and_comment` (`comment_fk`, `program_role_pk`),
            ADD CONSTRAINT `fk_program_role_pk1` FOREIGN KEY (`program_role_pk`) 
              REFERENCES `program_roles` (`pk`);


        -- -----------------------------------------------------
        -- Update version numbers
        -- -----------------------------------------------------
        SET majVersion = 73;
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
