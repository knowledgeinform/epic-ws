###################
# Stage 1: Build WS
###################

FROM docker-remote.artifactory.jhuapl.edu/library/maven:3.5.4-alpine AS build
WORKDIR /project/

# By default unit tests will not run; to run, pass '--build-arg SKIP_TESTS=false' in the Docker build command.
ARG SKIP_TESTS=true

# Configure Maven to use SD Artifactory:
COPY ./docker/settings.xml /usr/share/maven/conf/settings.xml
COPY ./docker/assets/JHUAPL-MS-Root-CA-05-21-2038-B64-text.cer .

# Trust the internal CA before Maven reaches out to Artifactory during the build.
RUN if [ -f "$JAVA_HOME/jre/lib/security/cacerts" ]; then \
  CACERTS="$JAVA_HOME/jre/lib/security/cacerts"; \
else \
  CACERTS="$JAVA_HOME/lib/security/cacerts"; \
fi && \
echo "Using cacerts: $CACERTS" && \
keytool -importcert -noprompt -alias JHUAPL_ROOT -keystore "$CACERTS" -storepass changeit -file JHUAPL-MS-Root-CA-05-21-2038-B64-text.cer

# Cache dependencies.
ADD pom.xml .
RUN mvn verify clean --fail-never

# Copy source files to build:
COPY . .

# Build EPIC:
RUN mvn package -Dmaven.test.skip=${SKIP_TESTS}


#################
# Stage 2: Run WS
#################
FROM docker-remote.artifactory.jhuapl.edu/library/tomcat:9.0-jdk8-slim AS tomcat

# Copy tomcat configuration
COPY ./docker/apache-context.xml /usr/local/tomcat/webapps/manager/META-INF/context.xml
COPY ./docker/apache-users.xml /usr/local/tomcat/conf/tomcat-users.xml

# Copy setenv.sh into Tomcat's bin directory
COPY ./docker/config/tomcat_setenv.sh /usr/local/tomcat/bin/setenv.sh
COPY ./docker/config/tomcat_setenv.sh /usr/local/tomcat/bin/setenv.sh
RUN chmod +x /usr/local/tomcat/bin/setenv.sh

# Set config dir.
ENV GSW_CONFIG '/project/epic/config'
RUN mkdir -p $GSW_CONFIG

# Copy setenv.sh into config directory
# Will copy into Tomcat's bin directory in host is prod, see entrypoint.sh
COPY ./docker/config/tomcat_setenv.sh $GSW_CONFIG/setenv.sh

#Import APL Certification
COPY ./docker/assets/JHUAPL-MS-Root-CA-05-21-2038-B64-text.cer .
RUN keytool -importcert -noprompt -alias JHUAPL_ROOT -keystore /usr/local/openjdk-8/jre/lib/security/cacerts -storepass changeit -file JHUAPL-MS-Root-CA-05-21-2038-B64-text.cer

# Copy the war to the tomcat directory
COPY --from=build /project/target/*.war /usr/local/tomcat/webapps/EPIC-WS.war

# Setup for entrypoint.sh.
# This script will run on container startup and handle environment specific configuration for Tomcat
COPY docker/config/server.xml.template /usr/local/tomcat/conf/server.xml.template
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
