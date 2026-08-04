DROP PROCEDURE IF EXISTS execute;
delimiter //
CREATE PROCEDURE execute()
BEGIN

  DECLARE runNum INT;
  DECLARE lastInstId INT;


  SET runNum = 1;
  runLoop : WHILE runNum <= 50 DO
  INSERT INTO `epicdb`.`run` (`run_number`, `procedure_def_version_pk`, `status`, `author`) VALUES (runNum, '4', 'ABANDONED', '2');
  -- SET lastInstId = LAST_INSERT_ID();


  SET runNum = runNum + 1;
  END WHILE runLoop;

END//
delimiter ;
CALL execute();
DROP PROCEDURE execute;
