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

    IF majVersion = 30 AND minVersion = 0
    THEN

      ALTER TABLE `approval_comment`
        DROP FOREIGN KEY `fk_procedureApprovalComment_procedureApproval`;
      ALTER TABLE `approval_comment`
        ADD CONSTRAINT `fk_procedureApprovalComment_procedureApproval`
          FOREIGN KEY (`procedure_approval_id`)
            REFERENCES `procedure_approvals` (`pk`)
            ON DELETE CASCADE
            ON UPDATE NO ACTION;


      ALTER TABLE `approval_comment_reply`
        DROP FOREIGN KEY `fk_approvalCommentReply_approvalComment`;
      ALTER TABLE `approval_comment_reply`
        ADD CONSTRAINT `fk_approvalCommentReply_approvalComment`
          FOREIGN KEY (`approval_comment_id`)
            REFERENCES `approval_comment` (`pk`)
            ON DELETE CASCADE
            ON UPDATE NO ACTION;


        -- -----------------------------------------------------
        -- Update version numbers
        -- -----------------------------------------------------
        SET majVersion = 31;
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
