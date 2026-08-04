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

    IF majVersion = 32 AND minVersion = 0
    THEN

        ALTER TABLE `procedure_def_version`
            ADD COLUMN `original_procedure_def_version_fk` INT(11) NULL DEFAULT NULL AFTER `procedure_id`,
            ADD COLUMN `edit_type` VARCHAR(15) NOT NULL DEFAULT 'ORIGINAL' AFTER `original_procedure_def_version_fk`,
#             ADD COLUMN `run_fk` INT(11) NULL DEFAULT NULL AFTER `edit_type`,
            ADD CONSTRAINT `fk_procedureDefVersion_originalProcedureDefVersion` FOREIGN KEY (`original_procedure_def_version_fk`) REFERENCES `procedure_def_version` (`pk`);
#             ADD CONSTRAINT `fk_procedureDefVersion_run` FOREIGN KEY (`run_fk`) REFERENCES `run` (`pk`);

        ALTER TABLE `procedure_instruction`
            ADD COLUMN `edit_type` VARCHAR(15) NOT NULL DEFAULT 'ORIGINAL' AFTER `procedure_def_version_pk`;

        ALTER TABLE `step_group_def`
            ADD COLUMN `edit_type` VARCHAR(15) NOT NULL DEFAULT 'ORIGINAL' AFTER `parent_step_group_def_pk`;

        ALTER TABLE `step_def`
            ADD COLUMN `edit_type` VARCHAR(15) NOT NULL DEFAULT 'ORIGINAL' AFTER `type`,
            ADD COLUMN `run_value_saved_timestamp` TIMESTAMP NULL AFTER `edit_type`;

        ALTER TABLE `step_table`
            ADD COLUMN `run_value` INT(11) NULL DEFAULT NULL AFTER `pk`,
            ADD CONSTRAINT `fk_stepTable_runValueStepTable` FOREIGN KEY (`run_value`) REFERENCES `step_table` (`pk`);

        ALTER TABLE `step_checkbox`
            ADD COLUMN `run_value` BIT(1) NULL DEFAULT NULL AFTER `pk`;

        ALTER TABLE `step_single_value`
            ADD COLUMN `run_value` VARCHAR(255) NULL DEFAULT NULL AFTER `pk`;

        CREATE TABLE `comment` (
            `pk` INT(11) NOT NULL AUTO_INCREMENT,
            `comment_timestamp` TIMESTAMP NOT NULL,
            `comment_text` TEXT NOT NULL,
            `comment_type` VARCHAR(50) NOT NULL,
            PRIMARY KEY (`pk`)
        )
            COLLATE='latin1_swedish_ci'
            ENGINE=InnoDB
        ;

        CREATE TABLE `black_line_comment` (
            `pk` INT(11) NOT NULL,
            `procedure_def_version_fk` INT(11) NULL DEFAULT NULL,
            `step_group_def_fk` INT(11) NULL DEFAULT NULL,
            `step_def_fk` INT(11) NULL DEFAULT NULL,
            `procedure_instruction_fk` INT(11) NULL DEFAULT NULL,
            PRIMARY KEY (`pk`),
            CONSTRAINT `fk_pk_blackLineComment_comment` FOREIGN KEY (`pk`) REFERENCES `comment` (`pk`),
            CONSTRAINT `fk_blackLineComment_procedureDefVersion` FOREIGN KEY (`procedure_def_version_fk`) REFERENCES `procedure_def_version` (`pk`),
            CONSTRAINT `fk_blackLineComment_stepGroupDef` FOREIGN KEY (`step_group_def_fk`) REFERENCES `step_group_def` (`pk`),
            CONSTRAINT `fk_blackLineComment_stepDef` FOREIGN KEY (`step_def_fk`) REFERENCES `step_def` (`pk`),
            CONSTRAINT `fk_blackLineComment_procedureInstruction` FOREIGN KEY (`procedure_instruction_fk`) REFERENCES `procedure_instruction` (`pk`)
        )
            COLLATE='latin1_swedish_ci'
            ENGINE=InnoDB
        ;

        ALTER TABLE `approval_comment`
            DROP COLUMN `comment_date`,
            DROP COLUMN `comment`,
            ADD CONSTRAINT `fk_pk_approvalComment_comment` FOREIGN KEY (`pk`) REFERENCES `comment` (`pk`);

        ALTER TABLE `approval_comment_reply`
            DROP COLUMN `reply`,
            DROP COLUMN `replyDate`,
            ADD CONSTRAINT `fk_pk_approvalCommentReply_comment` FOREIGN KEY (`pk`) REFERENCES `comment` (`pk`);

        ALTER TABLE `run_step_comment`
            DROP COLUMN `comment`,
            DROP COLUMN `timestamp`,
            DROP FOREIGN KEY `fk_runStepComment_runStepResults`,
            DROP COLUMN `run_step_results_pk`,
            ADD COLUMN `step_def_fk` INT(11) NOT NULL,
            ADD CONSTRAINT `fk_pk_runStepComment_comment` FOREIGN KEY (`pk`) REFERENCES `comment` (`pk`),
            ADD CONSTRAINT `fk_runStepComment_stepDef` FOREIGN KEY (`step_def_fk`) REFERENCES `step_def` (`pk`);


        -- -----------------------------------------------------
        -- Update version numbers
        -- -----------------------------------------------------
        SET majVersion = 33;
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
