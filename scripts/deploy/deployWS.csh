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
set connection = /project/epic/config/epic_connection.cfg
set attachmentMaxAllowedFileSizeBytes = 10737418240

# set database related variables
if ($host == "epic-dev" || $host == "epic-dev.jhuapl.edu") then
  if ($user != "epicuser") then
    echo "To run this script you need to be logged in as the 'epicuser' user!"
    exit 1
  endif

  set databaseName = "epicdb_dev"
  set databaseUser = "epicdbuser_dev"
  else
    if ($host == "epic-dev2" || $host == "epic-dev2.jhuapl.edu") then
      if ($user != "epicuser") then
        echo "To run this script you need to be logged in as the 'epicuser' user!"
        exit 1
      endif

      set databaseName = "epicdb_dev2"
      set databaseUser = "epicdbuser_dev2"
    else
      if ($host == "epic-test" || $host == "epic-test.jhuapl.edu") then
        if ($user != "epicuser") then
          echo "To run this script you need to be logged in as the 'epicuser' user!"
          exit 1
        endif

        set databaseName = "epicdb_test"
        set databaseUser = "epicdbuser_test"
      else
        if ($host == "epic" || $host == "epic.jhuapl.edu") then
          if ($user != "epicuser") then
            echo "To run this script you need to be logged in as the 'epicuser' user!"
            exit 1
          endif

          set databaseName = "epicdb"
          set databaseUser = "epicdbuser"
        else
          echo "Unknown host $host; cannot determine EPIC database name"
          exit 1
        endif
      endif
    endif
endif


echo "Database name set to " $databaseName
set databaseHost = "sdint-mysql.jhuapl.edu"
set databasePort = "3306"
set databasePassword = `grep -i 'db' $connection  | cut -f2 -d'='`

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
echo "***** Backup current database on remote host"
set dbBackupDir = /project/epic/dbBackups
set dbBackupName = $dbBackupDir/$databaseName'_pre_'$1'.sql'

mysqldump -u $databaseUser -p$databasePassword -h $databaseHost $databaseName > $dbBackupName

# set master.sql with the correct database
cd /project/epic/dbScripts
echo "***** Editing master.sql and schema_045 with the database name"
perl -pi -e 's/{{DATABASE_NAME}}/'$databaseName'/g' master.sql
perl -pi -e 's/{{DATABASE_NAME}}/'$databaseName'/g' schema_045.sql

echo "***** Running database upgrade scripts on remote host"
mysql -u $databaseUser -h $databaseHost -p$databasePassword --table $databaseName < master.sql

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
perl -pi -e 's/{{DATABASE_NAME}}/'$databaseName'/' $epicPropsFile
perl -pi -e 's/{{ATTACHMENT_MAX_ALLOWED_FILE_SIZE_BYTES}}/'$attachmentMaxAllowedFileSizeBytes'/' $epicPropsFile

if ($host == "epic-dev" || $host == "epic-dev.jhuapl.edu" || $host == "epic-dev2" || $host == "epic-dev2.jhuapl.edu" || $host == "epic-vm" || $host == "epic-vm.jhuapl.edu" || $host == "epic-test" || $host == "epic-test.jhuapl.edu") then
  perl -pi -e 's/{{DISABLE_EMAIL}}/true/' $epicPropsFile
else
  perl -pi -e 's/{{DISABLE_EMAIL}}/false/' $epicPropsFile
endif

echo "***** Editing epicdb_override.cfg"

perl -pi -e "s/{{HOST}}/$databaseHost/" $epicDbFile
perl -pi -e "s/{{PORT}}/$databasePort/" $epicDbFile
perl -pi -e "s/{{DBNAME}}/$databaseName/" $epicDbFile
perl -pi -e "s/{{DBUSER}}/$databaseUser/" $epicDbFile
perl -pi -e "s/{{DBPASSWORD}}/$databasePassword/" $epicDbFile

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
