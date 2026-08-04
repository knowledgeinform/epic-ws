# EPIC

## Dev Tips

* You can run the PMD Copy/Paste Detector using `mvn pmd:cpd-check`.

## Using Containers

The preferred container engine to use is Podman. For installing and setting up Podman, please see and follow instruction on the [wiki](https://aplwiki.jhuapl.edu/confluence/spaces/SESSIG/pages/910532943/Podman+Setup).
However, `podman-compose` and `docker-compose` can be replaced and for basic functionality use the same inputs.

`podman-compose build` - builds the containers. To pass a build argument, `podman-compose build --build-arg ARG_NAME=value`. Alternatively, environment variables can be defined within the `.env` file.

`podman-compose up` - launches the containers. To build before launching, add the `--build` argument. To run `docker-compose` in compatibility mode, add the `--compatibility` flag before the up.

`podman-compose stop` - stops all the containers in the composition

`podman-compose down` - stops and destroys all the containers in the composition

Note, for dev environments, the docker-compose.dev.yml file should be used. You can develop with the following command:
```bash
podman compose -f docker-compose.dev.yml build
podman compose -f docker-compose.dev.yml up
podman compose -f docker-compose.dev.yml down
```

### Dev Container

A Dockerfile that defines a dev environment has been included within the .devcontainer directory. 
You should be able to run this in VSCode or IntelliJ.

For VSCode, being up the command palette and run `Dev Containers: Rebuild and Rerun in Container`.

For IntelliJ, see [here for more information](https://www.jetbrains.com/help/idea/start-dev-container-inside-ide.html)

#### Tabnine Usage

Within the devcontainer.json there is a field where we are able to define extension to load into the devcontainer on initialization. 
For tabnine, do NOT include this extension. There is a weird interaction where it gets stuck in an initialization loop. 
Instead, you can manually install in dev container, reload window, sign in to tabnine through web page, and it should be able to use.

### Persistent Database Data
The database data is mounted in a volume to allow it to persist locally. Be aware that if you load a large dataset, 
it will be stored and hosted locally on your machine. Also note that if there have been schema changes, you 
will need to destroy the containers, delete the `mysql_data` directory, and then rebuild and run the containers.

## Running Tests
Until the resolution of [EPIC-1301](https://sd-jira.jhuapl.edu/browse/EPIC-1301), tests must be run using a combination of docker and the local machine.

## Testing/debugging tests locally  
- Follow `Running the test container` section then
- Follow the `Running the tests locally` (run/debug tests in your IDE)


### Running the test container
To run the test container, perform the following steps:
```
cd docker
podman-compose stop
podman-compose -p epic-test down
podman-compose -p epic-test build --build-arg SKIP_TESTS=true
podman-compose -p epic-test up
```
If the container initializes with errors, try bringing it up a second time without rebuilding.

### Running the tests locally

- Create an equivalent to the operational /project/epic directory on your local system
- Copy the `config` directory (NOT `docker/config`), and its contents to the operational directory on your local system
- Create the following additional sub-directories in the local operational directory
  - `export`
  - `client-logs`
  - `templates`
- Within the `config` directory, edit `epic.properties` with values that match your local environment. It should look something like this:
```
ALLOWED_ORIGIN=http://localhost:4200
DISABLE_EMAIL=true
EXPORT_ROOT_DIR=path/to/epic-export/on/local/machine
TEMPLATES_ROOT_DIR=path/to/templates/on/local/machine
CLIENT_LOGS_DIR=path/to/epic-client-logs/on/local/machine
DATABASE_NAME=epiclocaldb
ATTTACHMENT_MAX_ALLOWED_FILE_SIZE_BYTES=10737418240
```
- Within the `config` directory, edit `epicdb_override.cfg` with values that match your local machine. It should look 
something like this:
```
hibernate.connection.url,jdbc:mariadb://localhost:3306/epiclocaldb?sessionVariables=optimizer_search_depth=0
hibernate.connection.username,epicdb_user
hibernate.connection.password,epicdb_pass
```

- Additionally, update `src/META-INF/persistence.xml` `<property name="hibernate.search.backend.directory.root" value="/project/epic/data/index"/>`    
value to path on your local machine.
    - NOTE: DO NOT CHECK THIS CHANGE INTO THE REMOTE REPO!

- Recycle the test container and once it is up and running, you can run the top level EpicTestSuite or any of the individual test within the `test.edu.jhuapl.sd.sig.epic` folder
  - Within your run configuration, you must set the environment variable $GSW_CONFIG to point to your local `config` directory

### Running Tests with JMeter

To run performance tests using JMeter, follow these steps:
1. Install JMeter:
- Download Apache JMeter from the official website. 
- Extract the archive to a directory on your local machine.
- Ensure that Java is installed and configured in your environment variables.
- Optionally though IntelliJ Plugin.

2. Open the Test Plan:
- Launch JMeter.
- Navigate to File > Open and select the .jmx file relevant to the test you wish to run (located in jmeter directory of this repo).

3. Edit Variables:

Edit variables based on test target environment:

| Variable  | Value                | Page                        | Notes                                              |
|-----------|----------------------|-----------------------------|----------------------------------------------------|
| host      | hostname of epic-ws  | EPIC                        |                                                    |
| port      | port of epic-ws      | EPIC                        |                                                    |
| user pw   | user/pass            | Login - Request Body        | TODO - add service account as opposed to user info |
| file path | attachment file path | Save Attachment - File Path | Path on local machine                              |


4. Run the Test:
- Click Run > Start (or press Ctrl+R) to execute the test plan.
- See View Results tab for results.

## Code Quality

### Checkstyle 

The checkstyle is used to enforce a style to coding that including naming and whitespace conventions.
As developers work on code, they should do the best that they can to update any style violations. 
Running a `mvn clean package` will generate a file `target/checkstyle-result.xml`,
but it would be easier from a developer perspective to use the built-in IDE tools to make updates and focus one file at a time.
See [wiki](https://aplwiki.jhuapl.edu/confluence/spaces/SESSIG/pages/864752487/Code+Quality+Tools) for setting up IDE.

### Spotless

Spotless is tool that will automatically format code to comply with the checkstyle. 
Rules that are part of the spotless rules will cause the build to fail, 
but it is simple enough to `mvn dependency:unpack spotless:apply` to fix any issues.

Note, on Windows we have seen issues with the formatter file not being found when doing a ```docker-compose build```:
```Execution default-cli of goal com.diffplug.spotless:spotless-maven-plugin:2.30.0:apply failed: Unable to locate file with path: C:\Users\niepovm1\IdeaProjects\SESW\epic\epic-ws/target/spotless/eclipse-java-formatter.xml: Could not find resource ```

To resolve this, perform a ```mvn package '-Dmaven.test.skip=true'``` and then try the build again. 
Additionally, make sure project settings are using /n for eol and not /r/n.
In IntelliJ, this setting can be set under File | Settings | Code Style | Line Separator

### PMD

PMD is a static analysis tool that is ran within the mavn pipeline.
A report will be generated at `target\site\pmd.html`.

### Spotbugs

Spotbugs is another analysis tool that can be utilize to find bugs.
A report will be generated at `target\spotbugsXml.xml`.