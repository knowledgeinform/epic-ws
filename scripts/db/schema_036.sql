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

    IF majVersion = 35 AND minVersion = 0
    THEN



      ALTER TABLE `procedure_def_version`
        DROP FOREIGN KEY `fk_procedureDefVersion_originalProcedureDefVersion`;
      ALTER TABLE `procedure_def_version`
        DROP INDEX `fk_procedureDefVersion_originalProcedureDefVersion` ;


      ALTER TABLE `procedure_def_version`
        RENAME TO  `procedure_details` ;

      ALTER TABLE `procedure_details`
        CHANGE COLUMN `version` `procedure_def_version` INT(11) NULL ,
        CHANGE COLUMN `original_procedure_def_version_fk` `original_procedure_detail` INT(11) NULL DEFAULT NULL ;


      ALTER TABLE `procedure_details`
        ADD INDEX `fk_procedureDetail_originalProcedureDetail_idx` (`original_procedure_detail` ASC);
      ALTER TABLE `procedure_details`
        ADD CONSTRAINT `fk_procedureDetail_originalProcedureDetail`
          FOREIGN KEY (`original_procedure_detail`)
            REFERENCES `procedure_details` (`pk`)
            ON DELETE NO ACTION
            ON UPDATE NO ACTION;

      ALTER TABLE `procedure_approvals`
        DROP FOREIGN KEY `fk_procedureApprovals_procedureDefVersion`;
      ALTER TABLE `procedure_approvals`
        CHANGE COLUMN `procedure_def_version_pk` `procedure_details_pk` INT(11) NOT NULL ;
      ALTER TABLE `procedure_approvals`
        ADD CONSTRAINT `fk_procedureApprovals_procedureDetails`
          FOREIGN KEY (`procedure_details_pk`)
            REFERENCES `procedure_details` (`pk`);


      ALTER TABLE `run`
        DROP FOREIGN KEY `fk_run_procedureDefVersion`;
      ALTER TABLE `run`
        CHANGE COLUMN `procedure_def_version_pk` `procedure_details_pk` INT(11) NOT NULL ,
        DROP INDEX `fk_run_procedureDefVersion` ,
        ADD INDEX `fk_run_procedureDetails` (`procedure_details_pk` ASC);
      ALTER TABLE `run`
        ADD CONSTRAINT `fk_run_procedureDetails`
          FOREIGN KEY (`procedure_details_pk`)
            REFERENCES `procedure_details` (`pk`);

      ALTER TABLE `step_group_def`
        DROP FOREIGN KEY `fk_stepGroupDef_procedureDefVersion`;
      ALTER TABLE `step_group_def`
        CHANGE COLUMN `procedure_def_version_pk` `procedure_details_pk` INT(11) NULL DEFAULT NULL ;
      ALTER TABLE `step_group_def`
        ADD CONSTRAINT `fk_stepGroupDef_procedureDetails`
          FOREIGN KEY (`procedure_details_pk`)
            REFERENCES `procedure_details` (`pk`);


      ALTER TABLE `procedure_instruction`
        DROP FOREIGN KEY `fk_procedureInstruction_procedureDefVersion`;
      ALTER TABLE `procedure_instruction`
        CHANGE COLUMN `procedure_def_version_pk` `procedure_details_pk` INT(11) NOT NULL ;
      ALTER TABLE `procedure_instruction`
        ADD CONSTRAINT `fk_procedureInstruction_procedureDetails`
          FOREIGN KEY (`procedure_details_pk`)
            REFERENCES `procedure_details` (`pk`);

      ALTER TABLE `procedure_header`
        DROP FOREIGN KEY `fk_procedureHeader_procedureDefVersion`;
      ALTER TABLE `procedure_header`
        CHANGE COLUMN `procedure_def_version_pk` `procedure_details_pk` INT(11) NOT NULL ,
        DROP INDEX `fk_procedureHeader_procedureDefVersion` ,
        ADD INDEX `fk_procedureHeader_procedureDetails` (`procedure_details_pk` ASC);
      ALTER TABLE `procedure_header`
        ADD CONSTRAINT `fk_procedureHeader_procedureDetails`
          FOREIGN KEY (`procedure_details_pk`)
            REFERENCES `procedure_details` (`pk`);

      ALTER TABLE `procedure_details`
        DROP FOREIGN KEY `fk_procedureDefVersion_procedureDef`;
      ALTER TABLE `procedure_details`
        DROP INDEX `fk_procedureDefVersion_procedureDef` ,
        ADD INDEX `fk_procedureDetails_procedureDef` (`procedure_def_pk` ASC);
      ALTER TABLE `procedure_details`
        ADD CONSTRAINT `fk_procedureDetails_procedureDef`
          FOREIGN KEY (`procedure_def_pk`)
            REFERENCES `procedure_def` (`pk`);


      ALTER TABLE `black_line_comment`
        DROP FOREIGN KEY `fk_blackLineComment_procedureDefVersion`;
      ALTER TABLE `black_line_comment`
        CHANGE COLUMN `procedure_def_version_fk` `procedure_details_fk` INT(11) NULL DEFAULT NULL ,
        DROP INDEX `fk_blackLineComment_procedureDefVersion` ,
        ADD INDEX `fk_blackLineComment_procedureDetails` (`procedure_details_fk` ASC);
      ALTER TABLE `black_line_comment`
        ADD CONSTRAINT `fk_blackLineComment_procedureDefVersion`
          FOREIGN KEY (`procedure_details_fk`)
            REFERENCES `procedure_details` (`pk`);







      -- -----------------------------------------------------
        -- Update version numbers
        -- -----------------------------------------------------
        SET majVersion = 36;
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
