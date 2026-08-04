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

    IF majVersion = 52 AND minVersion = 0
    THEN

				-- Create procedure_approval_due_date
				ALTER TABLE `procedure_details` 
						ADD COLUMN `procedure_approval_due_date` DATE;

				-- Move data to new col
				UPDATE `procedure_details` 
					INNER JOIN 
						(SELECT DISTINCT procedure_details_pk, due_date FROM `procedure_approvals`
							WHERE due_date IS NOT null) as legacy
						ON (procedure_details.pk = procedure_details_pk) 
					SET procedure_details.procedure_approval_due_date = legacy.due_date;

				-- Drop approvers due_date
				ALTER TABLE `procedure_approvals`
						DROP COLUMN `due_date`;

        -- -----------------------------------------------------
        -- Update version numbers
        -- -----------------------------------------------------
        SET majVersion = 53;
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
