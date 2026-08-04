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

        IF majVersion = 25 AND minVersion = 0
        THEN

          ALTER TABLE `users`
            ADD COLUMN `email` VARCHAR(255) NOT NULL AFTER `last_login`;

          ALTER TABLE `procedure_approvals`
            DROP COLUMN `comments`;


          CREATE TABLE `approval_comment` (
                                            `pk` INT NOT NULL AUTO_INCREMENT,
                                            `procedure_approval_id` INT NOT NULL,
                                            `comment_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                            `comment` VARCHAR(2048) NOT NULL,
                                            PRIMARY KEY (`pk`),
                                            INDEX `fk_procedureApprovalComment_procedureApproval_idx` (`procedure_approval_id` ASC),
                                            CONSTRAINT `fk_procedureApprovalComment_procedureApproval`
                                              FOREIGN KEY (`procedure_approval_id`)
                                                REFERENCES `procedure_approvals` (`pk`)
                                                ON DELETE NO ACTION
                                                ON UPDATE NO ACTION);


          CREATE TABLE `approval_comment_reply` (
                                                  `pk` INT NOT NULL,
                                                  `approval_comment_id` INT NOT NULL,
                                                  `replyDate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                  `reply` VARCHAR(2048) NOT NULL,
                                                  PRIMARY KEY (`pk`),
                                                  INDEX `fk_approvalCommentReply_approvalComment_idx` (`approval_comment_id` ASC),
                                                  CONSTRAINT `fk_approvalCommentReply_approvalComment`
                                                    FOREIGN KEY (`approval_comment_id`)
                                                      REFERENCES `approval_comment` (`pk`)
                                                      ON DELETE NO ACTION
                                                      ON UPDATE NO ACTION);




          -- -----------------------------------------------------
          -- Update version numbers
          -- -----------------------------------------------------
          SET majVersion = 26;
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

