
# Docker Readme


## Overview

The WS and DB can be run using Docker Compose. In general, they should be a drop-in replacement for local databases. 

Docker Compose will retain each container's database data unless the container is destroyed (most usually by the `docker-compose down` command).


## Development Workflow

You can attach the UI to the WS as per normal. 

When you make a change to the WS, you'll want to rebuild the container. (See the Commands and Arguments section.)


## Configuration

Database connection arguments can be modified in each compose file. (See the Dockerfiles for specifics, such as accepted arguments.) 

If you would like to connect the WS to your own local database, set the compose file's `DB_HOST` arg to `host.docker.internalhost`.


## Useful Commands & Arguments

`docker-compose up` Launches the containers in your composition, replacing any already running instances. It will not rebuild containers if a built version already exists. Append the `--build` argument to force a rebuild. Append the name of a service (e.g. `epic-db` or `epic-ws`) to limit the command to a specific service.

`docker-compose stop` Stops the containers in your composition.

`docker-compose down` Stops and destroys the containers in your composition. All data contained in the containers will be lost. 

`docker-compose down --rmi all` Stops and destroys all the containers and images.