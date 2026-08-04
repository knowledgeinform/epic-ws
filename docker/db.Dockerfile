FROM docker-remote.artifactory.jhuapl.edu/library/mariadb:10.4.4-bionic

# TODO: Set environment variables as arguments from .env file
ARG DB_DATABASE=epiclocaldb

# Configure root user's PW (required) and create a new database.
ENV MYSQL_ROOT_PASSWORD=rootpw
ENV MYSQL_DATABASE=$DB_DATABASE

# Add user and grant DB access
COPY ./docker/create-user.sql /docker-entrypoint-initdb.d

# Initialize DB 
# NB: Files have to be moved to another folder (and `master.sql` updated) because they would all run automatically if placed in the entrypoint directory.
COPY ./scripts/db /db_scripts

RUN sed -i -e 's/{{DATABASE_NAME}}/epiclocaldb/g' /db_scripts/master.sql
RUN sed -i -e 's/{{DATABASE_NAME}}/epiclocaldb/g' /db_scripts/schema_045.sql
RUN sed -i -e 's/SOURCE /SOURCE \/db_scripts\//g' /db_scripts/master.sql
RUN mv /db_scripts/master.sql /docker-entrypoint-initdb.d


