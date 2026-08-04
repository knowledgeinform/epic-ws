FROM docker-remote.artifactory.jhuapl.edu/library/mariadb:10.4.4-bionic

# Initialize DB 
COPY ./scripts/db .
COPY ./docker/db-updater.sh .
ENTRYPOINT ./db-updater.sh
