/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.javers;

import edu.jhuapl.sd.sig.epic.data.*;
import edu.jhuapl.sd.sig.epic.data.util.*;
import edu.jhuapl.sd.sig.epic.model.*;
import edu.jhuapl.sd.sig.epic.model.util.JaversUtils;
import edu.jhuapl.sd.sig.epic.startup.AppConfiguration;
import edu.jhuapl.sd.sig.epic.utils.*;
import org.apache.logging.log4j.*;
import org.apache.logging.log4j.core.config.*;
import org.javers.core.ChangesByCommit;
import org.javers.repository.jql.*;
import org.junit.jupiter.api.*;
import org.javers.common.string.*;
import org.javers.core.*;
import org.javers.core.diff.*;
import org.javers.core.diff.changetype.*;
import org.javers.core.metamodel.object.CdoSnapshot;

import javax.persistence.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.Field;

public class TestCommit
{

    Javers javers = JPAUtils.getJavers();

    /**
     * Set environment variable by key/value pair for System.getenv
     * This is pseically done for GSW_CONFIG to avoid getting null from System.getenv("GSW_CONFIG")
     * in JPAUtils when creating entity manager factor, Persistence.createEntityManagerFactory
     * 
     * @param newenv
     * @throws Exception
     */
    public static void setEnv(Map<String, String> newenv) throws Exception
    {
        try
        {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        }
        catch (NoSuchFieldException e)
        {
            Class[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for (Class cl : classes)
            {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName()))
                {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(env);
                    Map<String, String> map = (Map<String, String>) obj;
                    map.clear();
                    map.putAll(newenv);
                }
            }
        }
    }

    @BeforeAll
    public static void beforeClass() throws Exception
    {
        Map<String, String> config_map = new HashMap<>();
        config_map.put("GSW_CONFIG", System.getProperty("user.dir") + File.separator + "docker" + File.separator + "config");
        setEnv(config_map);

        Configurator.setRootLevel(Level.ERROR);
        AppConfiguration.loadAppConfiguration();
        TestUtils.init();
    }

    private void printChanges(Object obj)
    {
        // get changes
        Changes changes = javers.findChanges(QueryBuilder.byClass(obj.getClass()).build());
        //then
        System.out.println("Printing the flat list of Changes :");
        changes.forEach(change -> System.out.println("- " + change));

        System.out.println("Changes prettyPrint :");
        System.out.println(changes.prettyPrint());
        for (ChangesByObject co : changes.groupByObject())
        {
            System.out.println(co.getGlobalId());

            if (obj instanceof Employee)
            {
                Employee e = (Employee) obj;

                String objClassNameFull = obj.getClass().getName();
                String objClassName = objClassNameFull.substring(objClassNameFull.lastIndexOf(".") + 1);
                System.out.println(objClassName + "/" + e.getName());

                if (co.getGlobalId().value().equals(objClassName + "/" + e.getName()))
                {
                    System.out.println("Matched!");
                    System.out.println(co);

                    for (Change eachChange : co.get())
                    {
                        System.out.println("Javers global id: " + co.getGlobalId());
                        System.out.println("Author: " + eachChange.getCommitMetadata().get().getAuthor());
                        System.out.println("Timestamp, " + eachChange.getCommitMetadata().get().getCommitDate().toString());
                        System.out.println("CommitID: " + eachChange.getCommitMetadata().get().getId());
                        System.out.println("Message: " + eachChange.prettyPrint(PrettyValuePrinter.getDefault()));

                        ChangeLogMessage log = new ChangeLogMessage();

                    }
                }
            }
        }
        for (ChangesByCommit c : changes.groupByCommit())
        {

            String temp1 = c.toString();
            String temp2 = c.prettyPrint();
            System.out.println(c.prettyPrint());
            System.out.println(c.getCommit().getAuthor());
            System.out.println(c.getCommit().getCommitDate());
            System.out.println(c.getCommit().getId());

            Map<String, String> mapStrings = c.getCommit().getProperties();

            List<ChangesByObject> changesByObject = c.groupByObject();
            for (ChangesByObject eachObj : changesByObject)
            {
                System.out.println(eachObj.getGlobalId());
                System.out.println("* changes on " + eachObj.getGlobalId().value() + " : ");
                System.out.println(eachObj.toString());

                List<PropertyChange> changeProperties = eachObj.getPropertyChanges();

                for (PropertyChange eachChange : changeProperties)
                {
                    System.out.println(eachChange.toString());
                }
            }

        }
        System.out.println("Printing Changes grouped by commits and by objects :");

        changes.groupByCommit().forEach(byCommit ->
        {
            System.out.println("commit " + byCommit.getCommit().getId());
            byCommit.groupByObject().forEach(byObject ->
            {
                System.out.println("* changes on " + byObject.getGlobalId().value() + " : ");
                byObject.get().forEach(change -> System.out.println("  - " + change));
            });
        });
    }

    private void printSnapshots(Object obj)
    {
        // Print out changes by snapshots
        List<CdoSnapshot> snapshots = javers.findSnapshots(QueryBuilder.byInstance(obj).build());
        for (CdoSnapshot eachSnapshot : snapshots)
        {
            System.out.println(eachSnapshot.getCommitId().toString() + ", " + eachSnapshot.getCommitMetadata().getAuthor());
            eachSnapshot.getCommitMetadata().getAuthor();
            for (String change : eachSnapshot.getChanged())
            {
                System.out.println(change + ": " + eachSnapshot.getPropertyValue(change));
            }
        }
    }

    @Test
    public void testChangeLogsForStepDef()
    {

        EntityManager em = JPAUtils.getEntityManager();

        StepDef stepDef = new StepSingleValue();
        stepDef.setPk(100);
        stepDef.setStepName("Test change logs Step 1");
        stepDef.setEditType(EditType.REDLINE_EDIT);

        Users user = UsersDAO.getUserByUsername(em, "user1");

        javers.commit(user.getUsername(), stepDef);

        stepDef.setDisplayOrder(2);
        stepDef.setEditType(EditType.REDLINE_EDIT);
        javers.commit(user.getUsername(), stepDef);
        JaversUtils.updateChangeLogs(stepDef);
        System.out.println(stepDef.printLogs());
    }

    @Test
    public void testChangeLogsForEmployee()
    {
        // Print change logs Employee/Sam
        Employee sam = new Employee("Sam", 1_000);

        JaversUtils.updateChangeLogs(sam);
        System.out.println(sam.printLogs());
    }

    @Test
    public void testEmployee() throws Exception
    {
        EntityManager em = null;

        em = JPAUtils.getEntityManager();
        Employee sam = new Employee("Sam", 1_000);
        sam.setPk(1);
        Employee frodo = new Employee("Frodo", 10_000);
        frodo.setPk(1001);
        Users user = UsersDAO.getUserByUsername(em, "user1");
        System.out.println("User, " + user.getUserId());
        Users user2 = UsersDAO.getUserByUsername(em, user.getUsername());
        System.out.println("User, " + user2.getUserId());
        javers.commit("user", user.getUsername());

        frodo.addSubordinate(sam);
        frodo.setSalary(11_000);
        sam.setSalary(2_000);
        sam.setPk(2);
        javers.commit("author", sam);

        sam.setPk(200);
        sam.setSalary(6000);
        javers.commit("author", sam);
        // print out the changes
        this.printChanges(sam);
        // Print out changes by snap shots
        this.printSnapshots(sam);
        // Print the last snapshot
        Optional<CdoSnapshot> latestSnapshotOpt = javers.getLatestSnapshot(sam.getName(), sam.getClass());
        CdoSnapshot latestSnapshot = latestSnapshotOpt.get();
        System.out.println(latestSnapshot.toString());
        for (String change : latestSnapshot.getChanged())
        {
            System.out.println(change + ": " + latestSnapshot.getPropertyValue(change));
        }
    }

    @Test
    public void testStepDef() throws Exception
    {

        // Create a stepdef
        StepDef stepDef = new StepSingleValue();
        stepDef.setPk(100);
        stepDef.setDisplayOrder(1);
        stepDef.setEsd0(false);
        stepDef.setHazardous(false);
        stepDef.setInstructions("These are instructions for step 1 of test data set 1");
        stepDef.setMandatoryInspection(false);
        stepDef.setRequireWitness(true);
        stepDef.setStepName("Test Step 1");
        stepDef.setAllowEquipmentEntry(false);
        stepDef.setType(StepType.SINGLE_VALUE);
        // Initial commit
        javers.commit("EPIC", stepDef);

        // Now change some values
        stepDef.setStepName("Test Step 1 for testing javers");
        stepDef.setInstructions("These are instructions for testing javers with step 1 of test data set 1");
        javers.commit("EPIC", stepDef);
        // Print out the changes
        this.printChanges(stepDef);
        // Print out changes by snap shots
        this.printSnapshots(stepDef);
        // Print the last snapshot
        Optional<CdoSnapshot> latestSnapshot = javers.getLatestSnapshot(stepDef.getPk(), stepDef.getClass());
        System.out.println(latestSnapshot.toString());
    }
}
