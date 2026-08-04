# noinspection SqlNoDataSourceInspectionForFile


CREATE DATABASE IF NOT EXISTS {{DATABASE_NAME}};

USE `{{DATABASE_NAME}}`;

SOURCE schema_001.sql
SOURCE data_001.sql
SELECT
  "CUR VERSION" AS "",
  v.*
FROM version v;
-- Insert new scripts after this line
SOURCE schema_002.sql
SOURCE schema_003.sql
SOURCE schema_004.sql
SOURCE schema_005.sql
SOURCE schema_006.sql
SOURCE schema_007.sql
SOURCE schema_008.sql
SOURCE schema_009.sql
SOURCE schema_010.sql
SOURCE schema_011.sql
SOURCE schema_012.sql
SOURCE schema_013.sql
SOURCE schema_014.sql
SOURCE schema_015.sql
SOURCE schema_016.sql
SOURCE schema_017.sql
SOURCE schema_018.sql
SOURCE data_002.sql
SOURCE data_003.sql
SOURCE schema_019.sql
SOURCE schema_020.sql
SOURCE data_004.sql
SOURCE schema_021.sql
SOURCE schema_022.sql
SOURCE schema_023.sql
SOURCE schema_024.sql
SOURCE schema_025.sql
SOURCE schema_026.sql
SOURCE schema_027.sql
SOURCE schema_028.sql
SOURCE schema_029.sql
SOURCE schema_030.sql
SOURCE schema_031.sql
SOURCE schema_032.sql
SOURCE schema_033.sql
SOURCE schema_034.sql
SOURCE data_005.sql
SOURCE data_006.sql
SOURCE schema_035.sql
SOURCE schema_036.sql
SOURCE schema_037.sql
SOURCE schema_038.sql
SOURCE schema_039.sql
SOURCE schema_040.sql
SOURCE schema_041.sql
SOURCE schema_042.sql
SOURCE data_007.sql
SOURCE schema_043.sql
SOURCE schema_044.sql
SOURCE schema_045.sql
SOURCE schema_046.sql
SOURCE schema_047.sql
SOURCE schema_048.sql
SOURCE schema_049.sql
SOURCE schema_050.sql
SOURCE schema_051.sql
SOURCE schema_052.sql
SOURCE schema_053.sql
SOURCE schema_054.sql
SOURCE schema_055.sql
SOURCE schema_056.sql
SOURCE schema_057.sql
SOURCE schema_058.sql
SOURCE data_008.sql
SOURCE schema_059.sql
SOURCE schema_060.sql
SOURCE schema_061.sql
SOURCE schema_062.sql
SOURCE schema_063.sql
SOURCE schema_064.sql
SOURCE schema_065.sql
SOURCE schema_066.sql
SOURCE schema_067.sql
SOURCE schema_068.sql
SOURCE schema_069.sql
SOURCE schema_070.sql
SOURCE schema_071.sql
SOURCE data_009.sql
SOURCE schema_072.sql
SOURCE schema_073.sql
SOURCE schema_074.sql
SOURCE data_010.sql
SOURCE schema_075.sql
SOURCE schema_076.sql
SOURCE schema_077.sql
SOURCE schema_078.sql
SOURCE schema_079.sql
SOURCE schema_080.sql
SOURCE schema_081.sql
-- Make sure this line is always last in the file
SELECT
  "NEW VERSION" AS "",
  v.*
FROM version v;
