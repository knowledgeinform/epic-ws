#!/bin/bash
set -e

TOMCAT_CONF="/usr/local/tomcat/conf"
cp "$TOMCAT_CONF/server.xml.template" "$TOMCAT_CONF/server.xml"

#sed -i "s|{{KEYSTORE_PASS}}|${KEYSTORE_PASS:-changeit}|g" "$TOMCAT_CONF/server.xml"
#sed -i "s|{{KEYSTORE_FILE}}|${KEYSTORE_FILE:-/etc/ssl/certs/epic-keystore.p12}|g" "$TOMCAT_CONF/server.xml"

if [[ "$ENV_HOST" == "epic" || "$ENV_HOST" == "epic.jhuapl.edu" ]]; then
  cp "$GSW_CONFIG"/setenv.sh /usr/local/tomcat/bin/setenv.sh
  chmod +x /usr/local/tomcat/bin/setenv.sh
fi

# Remove example webapps
TOMCAT_APPS="/usr/local/tomcat/webapps"
rm -rf $TOMCAT_APPS/examples

# Start Tomcat
exec catalina.sh run