#!/bin/tcsh
#
# Deploy EPIC-WS
#

if ($#argv != 1) then
   echo "Usage: deployWS.csh <version> \n"
   goto done
endif

set user = `whoami`
set host = `hostname`

if ($user != "epicuser") then
	echo "To run this script you need to be logged in as the 'epicuser' user!"
	exit 1
endif

# Check that tomcat is not running.
set tomcatPid = `ps -ef | grep tomcat | grep -v grep | awk '{ print $2 }'`
if ($tomcatPid != "") then
    echo "--Tomcat must be shutdown prior to install--"
    exit 1
endif


# Set up environment
setenv GSW_CONFIG /project/epic/config

#create releases directory if doesn't exist already
set releasesDir = /project/epic/releases
set tomcatDir = /usr/tomcat/webapps
mkdir -p $releasesDir


# Untar the config tar and create a sym link to tomcat web apps directory
# Expects that the WS .war is in the /project/epic directory
echo "***** Expanding the WS Configuration tar.gz"
tar -zxvf EPIC-WS-$1.tar.gz

#backup exising database
echo "***** Backup current database"
set dbBackupDir = /project/epic/dbBackups
mkdir -p $dbBackupDir
mysqldump -u epicdbuser -pepicdbpass epicdb > $dbBackupDir/epicdb_pre_$1.sql

cd /project/epic/dbScripts

echo "***** Editing master.sql and schema_045 with the database name"
perl -pi -e 's/{{DATABASE_NAME}}/epicdb/g' master.sql
perl -pi -e 's/{{DATABASE_NAME}}/epicdb/g' schema_045.sql

echo "***** Running database upgrade scripts"
mysql -u epicdbuser -pepicdbpass --table < master.sql

#go back to original directory
cd -

echo "***** Creating symlink for war"
mv /project/epic/EPIC-WS-$1.war $releasesDir
rm -f $tomcatDir/EPIC-WS.war
ln -s $releasesDir/EPIC-WS-$1.war $tomcatDir/EPIC-WS.war

# Customize configuration files
echo "***** Editing the configuration files"
set configDir = /project/epic/config
set epicPropsFile = $configDir/epic.properties
set epicDbFile = $configDir/epicdb_override.cfg
set ldapPropsFile = $configDir/ldap.properties
set exportRootDirString = '\/project\/epic\/export'
set exportRootDir = /project/epic/export
set templatesRootDirString = '\/project\/epic\/templates'
set templatesRootDir = /project/epic/templates
set clientLogsDirString = '\/project\/epic\/client_logs'
set clientLogsDir = /project/epic/client_logs

echo "***** Editing epic.properties"
perl -pi -e 's/{{ORIGIN_URL}}/https:\/\/'$host'\:8443/' $epicPropsFile
perl -pi -e 's/{{EXPORT_ROOT_DIR}}/'$exportRootDirString'/' $epicPropsFile
perl -pi -e 's/{{TEMPLATES_ROOT_DIR}}/'$templatesRootDirString'/' $epicPropsFile
perl -pi -e 's/{{CLIENT_LOGS_DIR}}/'$clientLogsDirString'/' $epicPropsFile
perl -pi -e 's/{{DATABASE_NAME}}/epicdb/' $epicPropsFile

if ($host == "epic-dev" || $host == "epic-dev.jhuapl.edu" || $host == "epic-dev2" || $host == "epic-dev2.jhuapl.edu" || $host == "epic-vm" || $host == "epic-vm.jhuapl.edu" || $host == "epic-test" || $host == "epic-test.jhuapl.edu") then
  perl -pi -e 's/{{DISABLE_EMAIL}}/true/' $epicPropsFile
else
  perl -pi -e 's/{{DISABLE_EMAIL}}/false/' $epicPropsFile
endif

echo "***** Editing epicdb_override.cfg"

perl -pi -e 's/{{HOST}}/localhost/' $epicDbFile
perl -pi -e 's/{{PORT}}/3306/' $epicDbFile
perl -pi -e 's/{{DBNAME}}/epicdb/' $epicDbFile
perl -pi -e 's/{{DBUSER}}/epicdbuser/' $epicDbFile
perl -pi -e 's/{{DBPASSWORD}}/epicdbpass/' $epicDbFile


echo "***** Creating export-related directories"
mkdir -p $exportRootDir
mkdir -p $templatesRootDir

echo "***** Creating client-side logging directory"
mkdir -p $clientLogsDir

echo "*************************************"
echo "**** EPIC WS Deployment Complete ****"
echo "****  Remember To Start Tomcat   ****"
echo "*************************************"

done:
	exit 0
