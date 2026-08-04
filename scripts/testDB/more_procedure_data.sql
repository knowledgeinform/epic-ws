INSERT INTO `epicdb`.`procedure_def` (`pk`, `description`, `name`, `program_pk`)
VALUES ('4', 'The fourth test procedure', 'Ready MyProcedure', '1');
INSERT INTO `epicdb`.`procedure_def_version` (`pk`, `version`, `procedure_def_pk`, `status`)
VALUES ('4', '1', '4', 'READY');
INSERT INTO `epicdb`.`procedure_header` (`pk`, `text`, `procedure_def_version_pk`, `author`)
VALUES ('4', 'header text for Ready MyProcedure', '4', '2');

INSERT INTO `epicdb`.`procedure_def` (`pk`, `description`, `name`, `program_pk`)
VALUES ('5', 'The fifth test procedure', 'Rejected MyProcedure', '1');
INSERT INTO `epicdb`.`procedure_def_version` (`pk`, `version`, `procedure_def_pk`, `status`)
VALUES ('5', '1', '5', 'REJECTED');
INSERT INTO `epicdb`.`procedure_header` (`pk`, `text`, `procedure_def_version_pk`, `author`)
VALUES ('5', 'header text for Rejected MyProcedure', '5', '2');
