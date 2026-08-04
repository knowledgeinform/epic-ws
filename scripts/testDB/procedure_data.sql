INSERT INTO `epicdb`.`program` (`pk`, `name`, `short_name`)
VALUES ('1', 'Program 1', 'P1');

INSERT INTO `epicdb`.`procedure_def` (`pk`, `description`, `name`, `program_pk`)
VALUES ('1', 'The first test procedure ', 'My First Test MyProcedure', '1');
INSERT INTO `epicdb`.`procedure_def_version` (`pk`, `version`, `procedure_def_pk`, `status`)
VALUES ('1', '1', '1', 'DRAFT');
INSERT INTO `epicdb`.`procedure_header` (`pk`, `text`, `procedure_def_version_pk`, `author`)
VALUES ('1', 'header text for My First Test MyProcedure', '1', '2');

INSERT INTO `epicdb`.`procedure_def` (`pk`, `description`, `name`, `program_pk`)
VALUES ('2', 'The second test procedure', 'Main Thrusters Thermal Isolation', '1');
INSERT INTO `epicdb`.`procedure_def_version` (`pk`, `version`, `procedure_def_pk`, `status`)
VALUES ('2', '2', '2', 'WAITING');
INSERT INTO `epicdb`.`procedure_header` (`pk`, `text`, `procedure_def_version_pk`, `author`)
VALUES ('2', 'header text for Main Thrusters Thermal Isolation', '2', '2');

INSERT INTO `epicdb`.`procedure_def` (`pk`, `description`, `name`, `program_pk`)
VALUES ('3', 'The third test procedure', 'Brake Pedal Sensitivity', '1');
INSERT INTO `epicdb`.`procedure_def_version` (`pk`, `version`, `procedure_def_pk`, `status`)
VALUES ('3', '3', '3', 'APPROVED');
INSERT INTO `epicdb`.`procedure_header` (`pk`, `text`, `procedure_def_version_pk`, `author`)
VALUES ('3', 'header text for Brake Pedal Sensitivity', '3', '2');

