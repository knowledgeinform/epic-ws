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
FROM docker-remote.artifactory.jhuapl.edu/library/tomcat:9.0.109-jdk8-temurin-noble

RUN apt-get update && apt-get install iputils-ping -y

COPY ./docker/apache-context.xml /usr/local/tomcat/webapps/manager/META-INF/context.xml
COPY ./docker/apache-users.xml /usr/local/tomcat/conf/tomcat-users.xml

# Set config dir.
ENV GSW_CONFIG '/GSW_CONFIG'

RUN mkdir -p /epic-uploads/

COPY ./docker/config/ /GSW_CONFIG

COPY ./docker/assets/JHUAPL-MS-Root-CA-05-21-2038-B64-text.cer .

RUN keytool -importcert -noprompt -alias JHUAPL_ROOT -keystore /opt/java/openjdk/jre/lib/security/cacerts -storepass changeit -file JHUAPL-MS-Root-CA-05-21-2038-B64-text.cer

# Copy the war to the tomcat directory
COPY --from=build /project/target/*.war /usr/local/tomcat/webapps/EPIC-WS.war

# Check if the db updater is complete
COPY ./docker/is-db-updater-done.sh .

CMD sh is-db-updater-done.sh epic-db-updater && /usr/local/tomcat/bin/catalina.sh jpda run

