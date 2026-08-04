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

    IF majVersion = 44 AND minVersion = 0
    THEN

      ALTER DATABASE `{{DATABASE_NAME}}` CHARACTER SET = 'utf8' COLLATE = 'utf8_general_ci';

      ALTER TABLE `app_configuration` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `approval_comment` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `approval_comment_reply` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `black_line_comment` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `comment` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `equipment_list` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `mandatory_inspection_second_signature` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `procedure_approvals` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `procedure_def` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `procedure_details` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `procedure_header` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `procedure_instruction` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `program` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `run` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `run_step_comment` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `run_step_history` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `second_signature` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `step_checkbox` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `step_def` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `step_file_upload` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `step_group_def` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `step_single_value` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `step_table` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `step_table_cell` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `step_table_row` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `subsystem` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `testing_phase` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `users` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `version` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      ALTER TABLE `witness_second_signature` CONVERT TO CHARACTER SET 'utf8' COLLATE 'utf8_general_ci';
      

      -- -----------------------------------------------------
        -- Update version numbers
        -- -----------------------------------------------------
        SET majVersion = 45;
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
