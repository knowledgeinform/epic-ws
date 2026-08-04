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

    IF majVersion = 37 AND minVersion = 0
    THEN


      DROP TABLE `run_step_table_cell_result`;
      DROP TABLE `run_step_single_result`;
      DROP TABLE `run_step_file_result`;
      DROP TABLE `run_step_result_history`;
      DROP TABLE `run_step_results`;

      CREATE TABLE `run_step_history` (
        `pk` INT NOT NULL AUTO_INCREMENT,
        `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        `description` VARCHAR(1024) NOT NULL,
        `user_id` INT NOT NULL,
        `step_def_id` INT NOT NULL,
        PRIMARY KEY (`pk`),
        INDEX `fk_runStepHistory_users_idx` (`user_id` ASC),
        INDEX `fk_runStepHistory_stepDef_idx` (`step_def_id` ASC),
        CONSTRAINT `fk_runStepHistory_users`
          FOREIGN KEY (`user_id`)
            REFERENCES `users` (`user_id`)
            ON DELETE NO ACTION
            ON UPDATE NO ACTION,
        CONSTRAINT `fk_runStepHistory_stepDef`
          FOREIGN KEY (`step_def_id`)
            REFERENCES `step_def` (`pk`)
            ON DELETE NO ACTION
            ON UPDATE NO ACTION);





      -- -----------------------------------------------------
        -- Update version numbers
        -- -----------------------------------------------------
        SET majVersion = 38;
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
