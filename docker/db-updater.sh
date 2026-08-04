#!/bin/bash
set -e

now=$(date)
echo "$now Updating database..."

while ! mysqladmin ping -h"epic-db" --silent; do
    sleep 1
done

# Run update script
perl -pi -e 's/\{\{DATABASE_NAME\}\}/epiclocaldb/g' master.sql
perl -pi -e 's/\{\{DATABASE_NAME\}\}/epiclocaldb/g' schema_045.sql
mysql -u epicdb_user -pepicdb_pass -h epic-db -P 3306 --table < master.sql

# Update default upload directory.
mysql -u epicdb_user -pepicdb_pass -h epic-db -P 3306 -e "USE epiclocaldb; UPDATE app_configuration SET config_value = '/epic-uploads' WHERE config_key = 'UPLOAD_ROOT_DIR';"

now=$(date)
echo "$now Database updated."
