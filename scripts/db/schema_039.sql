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

    IF majVersion = 38 AND minVersion = 0
    THEN

      ALTER TABLE `comment`
                ADD COLUMN `user_id` INT(11) NULL DEFAULT NULL AFTER `comment_type`,
                ADD CONSTRAINT `fk_comment_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

      UPDATE `comment`, `approval_comment_reply`
                SET `comment`.`user_id` = `approval_comment_reply`.`user_id`
                WHERE `comment`.`pk` = `approval_comment_reply`.`pk`;

      ALTER TABLE `approval_comment_reply`
                DROP FOREIGN KEY `fk_approvalCommentReply_users`,
                DROP COLUMN `user_id`;

      UPDATE `comment`
                INNER JOIN `approval_comment` ON `comment`.`pk` = `approval_comment`.`pk`
                INNER JOIN `procedure_approvals` ON `approval_comment`.`procedure_approval_id` = `procedure_approvals`.`pk`
                SET `comment`.`user_id` = `procedure_approvals`.`user_id`
                WHERE `comment`.`pk` = `approval_comment`.`pk`;

      ALTER TABLE `comment`
        DROP FOREIGN KEY `fk_comment_users`;
      ALTER TABLE `comment`
        CHANGE COLUMN `user_id` `user_id` INT(11) NOT NULL ;
      ALTER TABLE `comment`
        ADD CONSTRAINT `fk_comment_users`
          FOREIGN KEY (`user_id`)
            REFERENCES `users` (`user_id`);



      -- -----------------------------------------------------
        -- Update version numbers
        -- -----------------------------------------------------
        SET majVersion = 39;
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
