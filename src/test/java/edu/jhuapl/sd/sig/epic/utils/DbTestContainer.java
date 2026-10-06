/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.utils;

import edu.jhuapl.sd.sig.epic.data.util.ConfigureAPI;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.startup.AppConfiguration;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class DbTestContainer
{
    private static final String TEST_DB = "epic_test";
    private static final int EXPECTED_MAJOR_VER = 81;
    private static final String SQL_DIR = "scripts/db";
    private static final String SCRIPT_NAME = "master.sql";

    private MariaDBContainer<?> container;
    private EntityManagerFactory testEmf;
    private Path searchIndexRoot;

    public void start() throws IOException, InterruptedException
    {
        AppConfiguration.loadAppConfiguration();
        ConfigureAPI.initForTest();
        this.startContainer();
        JPAUtils.initialize(this.getTestEntityManagerFactory());
    }

    private void startContainer() throws IOException, InterruptedException
    {
        String scriptDir = System.getProperty("user.dir") + "/" + SQL_DIR;

        container = new MariaDBContainer<>("mariadb:10.5")
                .withDatabaseName(TEST_DB)
                .withUsername("test")
                .withPassword("test")
                .waitingFor(Wait.defaultWaitStrategy());
        // Mount the entire scripts/db directory into the container
        container.withCopyFileToContainer(
                MountableFile.forHostPath(scriptDir),
                "/tmp/db-scripts");
        container.start();

        // Execute database script to initialize schema
        // Replace {{DATABASE_NAME}} and run the script via the mysql client
        Container.ExecResult initResult = container.execInContainer(
                "bash", "-c",
                "cd /tmp/db-scripts && " +
                        "find . -name '*.sql' -exec sed -i 's/{{DATABASE_NAME}}/" + TEST_DB + "/g' {} + && " +
                        "sed 's/{{DATABASE_NAME}}/" + TEST_DB + "/g' " + SCRIPT_NAME + " | " +
                        "mysql -u" + "test" + " -p" + "test" + " " + TEST_DB);

        if (initResult.getExitCode() != 0)
        {
            throw new RuntimeException(
                    "Schema initialization failed: " + initResult.getExitCode() + "\n" +
                            "STDOUT: " + initResult.getStdout() + "\n" +
                            "STDERR: " + initResult.getStderr());
        }

        String url = container.getJdbcUrl();

        // Keep Hibernate Search active (production code paths use it), but write
        // its Lucene index into a temp dir instead of the test JVM's working
        // directory. Note: 'dummy' backend type does not exist in Hibernate
        // Search 6.x, which is why that approach fails EMF creation.
        searchIndexRoot = Files.createTempDirectory("epic-test-index");

        // Create EntityManagerFactory pointing at the testcontainer
        Map<String, String> props = new HashMap<>();
        props.put("hibernate.connection.url", url);
        props.put("hibernate.connection.username", "test");
        props.put("hibernate.connection.password", "test");
        props.put("hibernate.search.backend.directory.root", searchIndexRoot.toString());

        testEmf = Persistence.createEntityManagerFactory("epicdb", props);

        // Seed VersionInfo so the version check doesn't fail
        /*
        new TestEmfManager(testEmf).doInTransaction(em -> {
            em.createNativeQuery(
                            "INSERT INTO version (type, major, minor) VALUES ('DATABASE', " + EXPECTED_MAJOR_VER + ", 0)")
                    .executeUpdate();
        });
        */
    }

    public EntityManagerFactory getTestEntityManagerFactory()
    {
        return testEmf;
    }

    public void stop()
    {
        JPAUtils.closeEntityManagerFactory();

        if (container != null)
        {
            container.stop();
        }

        // Remove the temp search index so tests leave no artifacts behind.
        FileUtils.deleteAllRecursively(searchIndexRoot);
        searchIndexRoot = null;
    }

    // Simple transaction helper
    private static class TestEmfManager
    {
        private final EntityManagerFactory emf;

        TestEmfManager(EntityManagerFactory emf)
        {
            this.emf = emf;
        }

        void doInTransaction(java.util.function.Consumer<javax.persistence.EntityManager> op)
        {
            javax.persistence.EntityManager em = emf.createEntityManager();
            try
            {
                em.getTransaction().begin();
                op.accept(em);
                em.getTransaction().commit();
            }
            catch (Exception e)
            {
                em.getTransaction().rollback();
                throw e;
            }
            finally
            {
                em.close();
            }
        }
    }
}
