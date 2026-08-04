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

import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;
import edu.jhuapl.sd.sig.epic.data.TestProcedureDAO;
import edu.jhuapl.sd.sig.epic.data.util.ConfigureAPI;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.*;
import edu.jhuapl.sd.sig.epic.startup.AppConfiguration;

import javax.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUtils
{

    public static final String SDF_FULL_DATE_FORMAT = "yyyy-dd-MM HH:mm:ss";
    public static final String SDF_SHORT_DATE_FORMAT = "yyyy-dd-MM";
    public static final String TEST_START_DATE = "2035-01-01";
    public static final String TEST_END_DATE = "2035-04-01";

    static Random rand = new Random();

    static Lorem lorem = LoremIpsum.getInstance();

    public static void init()
    {
        if (!ConfigureAPI.isInitialized())
        {
            AppConfiguration.loadAppConfiguration();
            ConfigureAPI.init();
        }
    }

    public static Date getFullDateFromString(String fullDateString)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(SDF_FULL_DATE_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return returnDate(sdf, fullDateString);
    }

    public static Date getShortDateFromString(String shortDateString)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(SDF_SHORT_DATE_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return returnDate(sdf, shortDateString);
    }

    private static Date returnDate(SimpleDateFormat simpleDateFormat, String dateString)
    {
        Date d = null;
        try
        {
            d = simpleDateFormat.parse(dateString);
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }
        return d;
    }

    public static void purgeTestProcedures()
    {
        try
        {
            TestProcedureDAO.deleteProcedureDefAfterDate(getShortDateFromString(TEST_START_DATE));

            // check that all have been deleted
            Set<ProcedureDef> procedureDefs = TestProcedureDAO.getProcedureDefAfterDate(getShortDateFromString(TEST_START_DATE));
            assertEquals(0, procedureDefs.size());
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public static <T> InputStream createRemoveAndUpdateDisplayOrderPatchForArrayAsInputStream(int indexToRemove, Collection<T> arrayToUpdate)
    {
        StringBuilder stringBuilder = new StringBuilder("[");

        stringBuilder.append("{\"op\": \"remove\", \"path\": \"/" + indexToRemove + "\"}");
        for (int i = indexToRemove; i < arrayToUpdate.size() - 1; i++)
        {
            stringBuilder.append(", {\"op\": \"replace\", \"path\": \"/" + i + "/displayOrder\", \"value\": \"" + (i + 1) + "\"}");
        }
        stringBuilder.append("]");
        return new ByteArrayInputStream(stringBuilder.toString().getBytes());
    }

    public static ProcedureDetails createCleanRun() throws Exception
    {
        ProcedureDetails original = DataGeneratorUtils.generateProcedureDef(2, 0, 3, 3).getProcedureDetails()
                .iterator().next();
        TestProcedureDAO.transitionToReadyWrapper(original);
        return DataGeneratorUtils.generateRuns(1, 1, original).get(0);
    }

    public static ProcedureDetails createDraftProcedureDetails(EntityManager em)
    {
        System.out.println("Generating Procedure Def.");

        String procedureName = lorem.getTitle(2, 8);
        String procedureDescription = lorem.getWords(5, 20);
        Program program = DataGeneratorUtils.getRandomProgram();
        Subsystem subsystem = DataGeneratorUtils.getRandomSubsystem();
        Boolean esd0 = rand.nextBoolean();
        Boolean hazardous = rand.nextBoolean();
        String hazardDescription = hazardous ? lorem.getParagraphs(1, 3) : "";
        Users user = DataGeneratorUtils.getRandomUser();
        System.out.println("Generating for user: " + user.getDisplayName());
        ProcedureDef procedureDef = TestProcedureDAO.createProcedureDefWrapper(procedureName, procedureDescription,
                program.getPk(), subsystem.getPk(), esd0, hazardous, hazardDescription, user.getUserId());

        // Set some properties that aren't already set.
        ProcedureDetails detail = procedureDef.getProcedureDetails().iterator().next();
        System.out.println("Procedure Def saved with ID " + detail.getId());
        detail.setProcedureInstructions(DataGeneratorUtils.generateProcedureInstructions(1, 5, detail));
        detail.setStepGroupDefs(DataGeneratorUtils.generateStepGroupDefs(1, 1, 0, 3, detail, null, detail, 0));

        return JPAUtils.getRecordById(em, ProcedureDetails.class, detail.getPk());
    }
}
