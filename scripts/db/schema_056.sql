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

    IF majVersion = 55 AND minVersion = 0
    THEN

				-- Create a join table for equipment and run steps
				CREATE TABLE step_equipment_list (
					step_pk INT NOT NULL,
					equipment_pk INT NOT NULL,
					PRIMARY KEY (step_pk, equipment_pk),
					CONSTRAINT step_pk_foreign FOREIGN KEY (step_pk) REFERENCES step_def (pk),
					CONSTRAINT equipment_pk_foreign FOREIGN KEY (equipment_pk) REFERENCES equipment_list (pk)
				);

				-- Migrate values into new table
				INSERT INTO step_equipment_list
				SELECT step_pk, pk
				FROM equipment_list legacy
				WHERE step_pk IS NOT null;

				-- Update equipment_list with new cols, remove run step FK.
				ALTER TABLE `equipment_list`
					CHANGE COLUMN `calibration_date` `calibration_due_date` varchar(255) DEFAULT NULL,
					ADD COLUMN `calibration_date` DATE NULL DEFAULT NULL,
					ADD COLUMN `property_number` varchar(255) DEFAULT NULL,
					DROP FOREIGN KEY fk_equipmentList_step,
					DROP COLUMN `step_pk`;

        -- -----------------------------------------------------
        -- Update version numbers
        -- -----------------------------------------------------
        SET majVersion = 56;
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
