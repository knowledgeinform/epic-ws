#!/bin/tcsh
#
# Copy SQL file and replace epiddb name with test server and load in db
#

if ($#argv != 1) then
   echo "Usage: dbLoad.csh <dbBackup.sql> \n"
   exit 1
endif

set user = `whoami`
set host = `hostname`

if ($user != "epicuser") then
	echo "To run this script you need to be logged in as the 'epicuser' user!"
	exit 1
endif


# set database related variables
if ($host == "epic-dev" || $host == "epic-dev.jhuapl.edu") then
  set databaseName = "epicdb_dev"
  else
    if ($host == "epic-dev2" || $host == "epic-dev2.jhuapl.edu") then
      set databaseName = "epicdb_dev2"
    else
      if ($host == "epic-test" || $host == "epic-test.jhuapl.edu") then
        set databaseName = "epicdb_test"
      else
        #Do not do anything if it's the epic production system
        if ($host == "epic" || $host == "epic.jhuapl.edu") then
          exit 1
        else
          echo "Unknown host $host; cannot determine EPIC database name"
          exit 1
        endif
      endif
    endif
endif

#connect to mysql with credentials and run the file

set databaseHost = "sdint-mysql.jhuapl.edu"
set databaseUser = "epicdbuser"
set databasePassword = "epicdbpass"

echo "Updating $databaseName database with $1"
mysql -u $databaseUser -p$databasePassword -h $databaseHost $databaseName < $1
