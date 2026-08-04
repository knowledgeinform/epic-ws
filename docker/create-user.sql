-- NB: The credentials here should match those in `/docker/.env`.
-- TODO: Have this use users from environment variable. 
CREATE USER 'epicdb_user'@'%' IDENTIFIED BY 'epicdb_pass';
GRANT CREATE ON *.* TO 'epicdb_user'@'%';
GRANT ALL PRIVILEGES ON epiclocaldb.* TO 'epicdb_user'@'%';