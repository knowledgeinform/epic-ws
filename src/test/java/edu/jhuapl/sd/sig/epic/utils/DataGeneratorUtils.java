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

import edu.jhuapl.sd.sig.epic.data.ProgramRolesDAO;
import edu.jhuapl.sd.sig.epic.data.TestProcedureDAO;
import edu.jhuapl.sd.sig.epic.data.util.ConfigureAPI;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.*;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class DataGeneratorUtils
{

    static Random rand = new Random();

    static Lorem lorem = LoremIpsum.getInstance();

    // Global configuration options.
    final static int numUsers = 1;

    final static int numProcedures = 5;

    final static int maxTableRows = 30;

    final static int maxTableCols = 10;

    final static int maxDepth = 5;

    final static boolean setChildGroups = true;

    final static boolean logHibernate = true;

    static Instant startInstant = null;

    public static void main(String[] args) throws Exception
    {

        // Configure logger
        if (logHibernate)
            Configurator.initialize(null, System.getenv("GSW_CONFIG") + File.separator + "loggingEPIC.xml");
        else
            Configurator.setRootLevel(Level.OFF);

        // Initialize DB connection
        if (!ConfigureAPI.isInitialized())
            ConfigureAPI.init();

        // Add users if necessary
        if (TestProcedureDAO.getAllUsers().size() < numUsers)
            generateRandomUsers(numUsers);

        // Log start time.
        System.out.println("Start time: " + getTimeStr());
        startInstant = Instant.now();

        // Generate procedures
        for (int i = 0; i < numProcedures; i++)
            generateProcedureDef(10, 10, 1, 10);

        // Log end time.
        System.out.println("End time: " + getTimeStr());
        printElapsedTime();

        // Close DB connection.
        JPAUtils.closeEntityManagerFactory();

    }

    public static void generateRandomUsers(int count)
    {
        for (int i = 0; i < count; i++)
        {
            Users user = new Users();
            user.setDisplayName(lorem.getName());
            user.setUsername(lorem.getName().replace(" ", ""));
            user.setEmail(user.getUsername() + "@fake.com");

            TestProcedureDAO.addUserWrapper(user);
        }
    }

    // TODO: Add generators for: Program, Subsystem?

    /**
     * Generates a new Procedure with metadata based on the provided genId. Assumes
     * programs, subsystems, and users exist already.
     *
     * @param numStepGroups The number of top-level step groups to create.
     * @param numRuns The number of runs to make.
     * @param minSteps The min number of steps for each group. (The precise
     *     number will be randomly chosen.)
     * @param maxSteps The max number of steps for each group. (The precise
     *     number will be randomly chosen.)
     * @throws Exception
     */
    public static ProcedureDef generateProcedureDef(int numStepGroups, int numRuns, int minSteps, int maxSteps)
            throws Exception
    {

        System.out.println("Generating Procedure Def.");

        String procedureName = lorem.getTitle(2, 8);
        String procedureDescription = lorem.getWords(5, 20);
        Program program = getRandomProgram();
        Subsystem subsystem = getRandomSubsystem();
        Boolean esd0 = rand.nextBoolean();
        Boolean hazardous = rand.nextBoolean();
        String hazardDescription = hazardous ? lorem.getParagraphs(1, 3) : "";
        Users user = getRandomUser();
        System.out.println("Generating for user: " + user.getDisplayName());
        ProcedureDef procedureDef = TestProcedureDAO.createProcedureDefWrapper(procedureName, procedureDescription,
                program.getPk(), subsystem.getPk(), esd0, hazardous, hazardDescription, user.getUserId());

        // Set some properties that aren't already set.
        ProcedureDetails detail = procedureDef.getProcedureDetails().iterator().next();
        if (detail == null)
        {
            throw new Exception("No procedure details found while generating procedure def.");
        }
        System.out.println("Procedure Def saved with ID " + detail.getId());
        detail.setProcedureInstructions(generateProcedureInstructions(1, 5, detail));
        detail.setStepGroupDefs(generateStepGroupDefs(numStepGroups, numStepGroups, minSteps, maxSteps, detail, null, detail, 1));
        Boolean fullyApproved = rand.nextBoolean();
        detail.setProcedureApprovals(generateProcedureApprovals(1, Math.min(numUsers, 3), detail, fullyApproved));
        detail.setEditType(EditType.RUN);
        if (fullyApproved)
        {
            TestProcedureDAO.transitionToReadyWrapper(detail);
            detail.setProcedureDetailRuns(generateRuns(0, numRuns, detail)); // TODO: Review if necessary.
        }

        System.out.println("Done generating procedure def " + detail.getId());
        return procedureDef;

    }

    private static Integer randBetween(int min, int max)
    {
        if (max < 1)
        {
            return 0;
        }
        else if (max == min)
        {
            return min;
        }
        return rand.nextInt(max - min) + min;
    }

    public static List<ProcedureDetails> generateRuns(int min, int max, ProcedureDetails original)
    {

        List<ProcedureDetails> details = new ArrayList<ProcedureDetails>();
        int count = randBetween(min, max);
        for (int i = 0; i < count; i++)
        {

            System.out.println("Generating Run " + (i + 1) + " of " + count + " for PD " + original.getPk());

            Users user = getRandomUser();
            System.out.println("Generating for user: " + user.getDisplayName());

            Run run = new Run();
            run.setDescription(lorem.getWords(0, 30)); // Database limit: 255 VARCHAR.
            if (run.getDescription().length() > 255)
            {
                run.setDescription(run.getDescription().substring(0, 255));
            }
            run.setEquipmentList(generateEquipment(0, 5));
            run.setName(lorem.getTitle(2, 8));
            run.setStatus(RunStatus.RUNNING); // We don't seem to use this, so it shouldn't matter what we set this as.
            run.setTestingPhase(getRandomTestingPhase());
            run.setUser(getRandomUser());
            ProcedureDetails detail = TestProcedureDAO.createRunWrapper(original, run, i, user);
            detail.setBlackLineComments(generateBlackLineComments(0, 3, detail));

            details.add(detail);
        }
        return details;
    }

    public static SortedSet<StepGroupDef> generateStepGroupDefs(int min, int max, int minSteps, int maxSteps,
            ProcedureDetails parentProcedure, StepGroupDef parentGroup, ProcedureDetails rootDetails, int depth)
    {

        if (depth > maxDepth)
            return null;

        int count = randBetween(min, max);
        SortedSet<StepGroupDef> groups = new TreeSet<StepGroupDef>();
        for (int i = 1; i <= count; i++)
        {

            // TODO Add depth indent here.
            System.out.println("Generating Step Group Def " + i + " of " + count + " for procedure " + parentProcedure.getPk()
                    + " and group " + (parentGroup != null ? parentGroup.getPk() : parentGroup));

            StepGroupDef group = new StepGroupDef();
            group.setStepGroupName(lorem.getTitle(1, 10));
            group.setDescription(lorem.getTitle(1, 500));
            group.setDisplayOrder(i);
            group.setStepGroupDefParent(parentGroup);
            group.setProcedureDetails(parentProcedure);
            group = TestProcedureDAO.saveStepGroupDefWrapper(group);

            if (DataGeneratorUtils.setChildGroups)
                group.setStepGroupDefsChildren(generateStepGroupDefs(0, randBetween(0, Math.min(5, max - 1)), minSteps, maxSteps,
                        parentProcedure, group, rootDetails, depth + 1));

            group.setStepDefs(generateStepDefs(minSteps, maxSteps, group, rootDetails));

            if (parentProcedure.getEditType() == EditType.RUN)
                group.setBlackLineComments(generateBlackLineComments(0, 3, group));

            groups.add(group);
        }
        return groups;
    }

    public static StepTable generateStepTable(int minRows, int maxRows, int minCols, int maxCols)
    {
        StepTable st = new StepTable();

        SortedSet<StepTableRow> rows = new TreeSet<StepTableRow>();
        int rowCount = randBetween(minRows, maxRows);
        int colCount = randBetween(minCols, maxCols);

        for (int i = 0; i < rowCount; i++)
        {
            StepTableRow row = new StepTableRow();
            row.setRowNumber(i);
            row.setStepTable(st);

            SortedSet<StepTableCell> cells = new TreeSet<StepTableCell>();
            for (int j = 0; j < colCount; j++)
            {
                StepTableCell cell = new StepTableCell();
                cell.setCellIndex(j);
                cell.setEditable(rand.nextBoolean());
                cell.setNonEditableValue(lorem.getWords(0, 10));
                cell.setStepTableRow(row);
                cells.add(cell);
            }
            row.setStepTableCells(cells);
            rows.add(row);
        }
        st.setStepTableRows(rows);
        return st;
    }

    public static SortedSet<StepDef> generateStepDefs(int min, int max, StepGroupDef parentGroup,
            ProcedureDetails rootDetails)
    {

        SortedSet<StepDef> steps = new TreeSet<StepDef>();
        final int count = randBetween(min, max);
        for (int i = 1; i <= count; i++)
        {

            System.out.println("Generating Step Def " + i + " of " + count + " for group " + parentGroup.getPk());

            Boolean isRun = rootDetails.getEditType() == EditType.RUN;

            StepDef step = null;
            StepType type = getRandFrom(StepType.values());
            switch (type)
            {
                case SINGLE_VALUE:
                    step = new StepSingleValue();
                    if (isRun)
                        ((StepSingleValue) step).setRunValue(lorem.getWords(0, 10));
                    break;
                case TABLE:
                    step = generateStepTable(1, DataGeneratorUtils.maxTableRows, 1, DataGeneratorUtils.maxTableCols);
                    break;
                case CHECKBOX:
                    step = new StepCheckbox();
                    if (isRun)
                        ((StepCheckbox) step).setRunValue(rand.nextBoolean());
                    break;
                default:
                    System.out.println("INVALID STEP INPUT TYPE!!!! " + type);
                    continue;
            }

            if (isRun)
            {
                step.setBlackLineComments(generateBlackLineComments(0, 3, step));
                step.setEquipment(generateEquipment(0, 10));
                step.setIsManualValidation(rand.nextBoolean());
                if (step.getMandatoryInspection())
                    step.setMandatoryInspectionSecondSignature(generateMandatoryInspectionSecondSignature());
                step.setRunStepComments(generateRunStepComments(0, 3, step));
                if (step.getRequireWitness())
                    step.setWitnessSecondSignature(generateWitnessSecondSignature());

                generateRunStepValue(0, 3, step); // Generate values AND histories.

            }
            else
            {
                step.setStepGroupDef(parentGroup);
                step.setAllowEquipmentEntry(rand.nextBoolean());
                step.setDisplayOrder(i);
                step.setEsd0(rand.nextBoolean());
                step.setHazardous(rand.nextBoolean());
                step.setInstructions(lorem.getParagraphs(1, 4));
                step.setMandatoryInspection(rand.nextBoolean());
                step.setRequireWitness(rand.nextBoolean());
                step.setStepName(lorem.getTitle(2, 8));
                step.setType(type);
            }

            step = TestProcedureDAO.saveStepDefWrapper(step);

            steps.add(step);

            System.out.println("Set stepGroupDef: " + parentGroup.getPk() + " for step: " + step.getPk());
        }
        return steps;
    }

    /**
     * Sets the value of the step between `min` and `max` times. While each step may
     * only have 1 value, whenever it is updated the history of the field changes,
     * so this method creates history.
     */
    public static void generateRunStepValue(int min, int max, StepDef abstractStep)
    {
        final int count = randBetween(min, max);
        for (int i = 0; i < count; i++)
        {

            System.out.println("Generating Run Step Value " + i + " of " + count + " for step " + abstractStep.getPk());

            switch (abstractStep.getType())
            {
                case CHECKBOX:
                    StepCheckbox checkbox = (StepCheckbox) abstractStep;
                    checkbox.setRunValue(rand.nextBoolean());
                    TestProcedureDAO.saveRunValueForCheckboxStepWrapper(checkbox, getRandomUser());
                    break;
                case SINGLE_VALUE:
                    StepSingleValue single = (StepSingleValue) abstractStep;
                    single.setRunValue(lorem.getWords(1, 5));
                    TestProcedureDAO.saveRunValueForSingleValueStepWrapper(single, getRandomUser());
                    break;
                case TABLE:
                    StepTable table = (StepTable) abstractStep;
                    table.getStepTableRows().forEach(row ->
                    {
                        row.getStepTableCells().forEach(cell ->
                        {
                            String newVal = rand.nextBoolean() ? lorem.getWords(0, 10) : randBetween(0, 10000).toString();
                            cell.setNonEditableValue(newVal);
                        });
                    });
                    StepTableCell cell = table.getStepTableRows().stream()
                            .filter(row ->
                            {
                                StepTableCell c = null;
                                c = row.getStepTableCells().stream()
                                        .filter(aCell -> aCell.getEditable()).findFirst().get();
                                return c != null;
                            }).findFirst()
                            .get()
                            .getStepTableCells().stream()
                            .filter(cl -> cl.getEditable())
                            .findFirst()
                            .get();

                    cell.setNonEditableValue(lorem.getWords(0, 10));
                    TestProcedureDAO.saveRunValueForTableStepWrapper(table, cell, getRandomUser());
                    break;
                default:
                    break;
            }

        }
    }

    public static SortedSet<RunStepComment> generateRunStepComments(int min, int max, StepDef parentStep)
    {
        SortedSet<RunStepComment> comments = new TreeSet<RunStepComment>();
        final int count = randBetween(min, max);
        for (int i = 0; i < count; i++)
        {
            RunStepComment comment = new RunStepComment();

            // Set automatically on save: Timestamp, CommentType.
            comment.setCommentText(lorem.getParagraphs(1, 3));
            comment.setIsNonconformance(rand.nextBoolean());
            comment.setStepDef(parentStep);
            comment.setUsers(getRandomUser());

            comments.add(comment);
        }
        return comments;
    }

    public static Set<EquipmentList> generateEquipment(int min, int max)
    {
        Set<EquipmentList> list = new HashSet<EquipmentList>();
        final int count = randBetween(min, max);
        for (int i = 0; i < count; i++)
        {
            EquipmentList elm = new EquipmentList();
            elm.setCalibrationDate(getRandomTimestampDaysBack(10));
            elm.setName(lorem.getTitle(1, 5));
            elm.setSerialNumber(String.valueOf(rand.nextInt(100000000)));
            list.add(elm);
        }
        return list;
    }

    public static WitnessSecondSignature generateWitnessSecondSignature()
    {
        WitnessSecondSignature signature = new WitnessSecondSignature()
        {};
        signature.setTimestamp(getRandomTimestampDaysBack(10));
        signature.setType(SecondSignatureType.MANDATORY_INSPECTION);
        signature.setUser(getRandomUser());

        TestProcedureDAO.RunStepSecondSignatureWrapper(signature);

        return signature;
    }

    public static MandatoryInspectionSecondSignature generateMandatoryInspectionSecondSignature()
    {
        MandatoryInspectionSecondSignature signature = new MandatoryInspectionSecondSignature();
        signature.setTimestamp(getRandomTimestampDaysBack(10));
        signature.setType(SecondSignatureType.MANDATORY_INSPECTION);
        signature.setUser(getRandomUser());

        TestProcedureDAO.RunStepSecondSignatureWrapper(signature);

        return signature;
    }

    public static SortedSet<ProcedureInstruction> generateProcedureInstructions(int min, int max,
            ProcedureDetails parentProcedure)
    {
        SortedSet<ProcedureInstruction> instructions = new TreeSet<ProcedureInstruction>();
        final int count = randBetween(min, max);
        for (int i = 1; i <= count; i++)
        {
            ProcedureInstruction instruction = new ProcedureInstruction();
            if (parentProcedure.getEditType() != EditType.ORIGINAL)
                instruction.setBlackLineComments(generateBlackLineComments(0, 3, instruction));
            instruction.setDisplayOrder(i);
            instruction.setSectionName(lorem.getTitle(1, 8));
            instruction.setText(lorem.getParagraphs(1, 4));
            instruction.setProcedureDetails(parentProcedure);

            instruction = TestProcedureDAO.saveProcedureInstructionSectionWrapper(instruction);
            // instruction = TestProcedureDAO.updateInstructionSectionWrapper(instruction, instruction);

            instructions.add(instruction);
        }
        return instructions;
    }

    public static ProcedureHeader generateProcedureHeader()
    {
        ProcedureHeader header = new ProcedureHeader();
        header.setCreationDate(getRandomTimestampDaysBack(30));
        header.setText(lorem.getParagraphs(1, 8));
        header.setUser(getRandomUser());
        return header;
    }

    public static List<BlackLineComment> generateBlackLineComments(int min, int max, Object parent)
    {
        List<BlackLineComment> comments = new ArrayList<>();
        final int count = randBetween(min, max);
        for (int i = 0; i < count; i++)
        {
            BlackLineComment comment = new BlackLineComment();
            comment.setCommentText(lorem.getParagraphs(1, 3));
            comment.setCommentTimestamp(getRandomTimestampDaysBack(30));
            comment.setUsers(getRandomUser());
            comment.setCommentType(CommentType.BLACK_LINE_COMMENT);
            comment.setProcedureChangeType(generateProcedureChangeType());

            switch (parent.getClass().toString())
            {
                case "ProcedureDetails":
                    comment.setProcedureDetails((ProcedureDetails) parent);
                    break;
                case "StepGroupDef":
                    comment.setStepGroupDef((StepGroupDef) parent);
                    break;
                case "StepDef":
                    comment.setStepDef((StepDef) parent);
                    break;
                case "ProcedureInstruction":
                    comment.setProcedureInstruction((ProcedureInstruction) parent);
                    break;
            }

            TestProcedureDAO.blacklineCommentWrapper(comment, rand.nextBoolean());

            comments.add(comment);
        }
        return comments;
    }

    public static Program getRandomProgram()
    {
        return getRandFrom(TestProcedureDAO.getAllPrograms());
    }

    public static Subsystem getRandomSubsystem()
    {
        return getRandFrom(TestProcedureDAO.getAllSubsystems());
    }

    public static Users getRandomUser()
    {
        return getRandFrom(TestProcedureDAO.getAllUsers());
    }

    public static TestingPhase getRandomTestingPhase()
    {
        return getRandFrom(TestProcedureDAO.getAllTestingPhases());
    }

    private static <T> T getRandFrom(Collection<T> collection)
    {
        // TODO: Consider throwing an error if the collection is empty.
        T[] array = (T[]) collection.toArray();
        return array[rand.nextInt(array.length)];
    }

    private static <T> T getRandFrom(T[] array)
    {
        return array[rand.nextInt(array.length)];
    }

    private static Date getRandomTimestampDaysBack(int numDaysBack)
    {
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.DAY_OF_MONTH, -numDaysBack);
        Date start = cal.getTime();
        return getRandomTimestamp(start, now);
    }

    private static Date getRandomTimestamp(Date min, Date max)
    {
        Date randomDate = new Date(ThreadLocalRandom.current().nextLong(min.getTime(), max.getTime()));
        return randomDate;
    }

    /**
     *
     * @param approved If true, all will be set to true. Else some may be set to
     *     true, or not.
     */
    public static Set<ProcedureApproval> generateProcedureApprovals(int min, int max, ProcedureDetails procedureDetails,
            Boolean approved)
    {
        Set<ProcedureApproval> approvals = new HashSet<ProcedureApproval>();
        final int count = randBetween(min, max);
        for (int i = 0; i < count; i++)
        {

            ProcedureApproval approval = new ProcedureApproval();
            approval.setApprovalType(getRandFrom(ProcedureApprovalType.values()));
            approval.setUsers(getRandomUser());
            approval.setProcedureDetails(procedureDetails);
            approval = TestProcedureDAO.saveProcedureApprovalWrapper(approval);

            approval.setIsApproved(approved || rand.nextBoolean());
            approval.setComments(generateRandomApprovalComments(0, 3));

            TestProcedureDAO.setApprovalFlagWrapper(approval, approval.getIsApproved());

            approvals.add(approval);
        }
        return approvals;
    }

    public static SortedSet<ApprovalComment> generateRandomApprovalComments(int min, int max)
    {
        SortedSet<ApprovalComment> comments = new TreeSet<ApprovalComment>();
        final int count = randBetween(min, max);
        for (int i = 0; i < count; i++)
        {
            ApprovalComment comment = new ApprovalComment();
            comment.setCommentText(lorem.getParagraphs(1, 2));

            // TODO: Consider whether days back should be limited based on parent
            // ProcedureApproval.
            comment.setCommentTimestamp(getRandomTimestampDaysBack(20));
            comment.setCommentType(CommentType.APPROVAL_COMMENT);
            comment.setUsers(getRandomUser());
            comment.setReplies(generateRandomReplies(0, 3));
            comments.add(comment);
        }
        return comments;
    }

    public static SortedSet<ApprovalCommentReply> generateRandomReplies(int min, int max)
    {
        SortedSet<ApprovalCommentReply> replies = new TreeSet<ApprovalCommentReply>();
        final int count = randBetween(min, max);
        for (int i = 0; i < count; i++)
        {
            ApprovalCommentReply reply = new ApprovalCommentReply();
            reply.setCommentText(lorem.getParagraphs(1, 3));
            // TODO: Consider whether days back should be limited based on parent
            // ProcedureApproval.
            reply.setCommentTimestamp(getRandomTimestampDaysBack(10));
            reply.setCommentType(CommentType.APPROVAL_COMMENT_REPLY);
            reply.setUsers(getRandomUser());
            replies.add(reply);
        }
        return replies;
    }

    public static ProgramRole generateProgramRole()
    {

        ProgramRole newProgramRole = ProgramRolesDAO.addProgramRole(JPAUtils.getEntityManager(), lorem.getWords(1, 2));

        List<ProgramRole> prs = new ArrayList<>();
        JPAUtils.basicTransaction(em ->
        {
            prs.add(newProgramRole);
        }, "Could not add generated ProgramRole.");
        return prs.get(0);
    }

    public static ProcedureChangeType generateProcedureChangeType()
    {
        ProcedureChangeType pct = ProgramRolesDAO.addChangeType(JPAUtils.getEntityManager(), lorem.getWords(1, 3));

        List<ProcedureChangeType> pcts = new ArrayList<>();
        ProcedureChangeType finalPct = pct;
        JPAUtils.basicTransaction(em ->
        {
            pcts.add(finalPct);
        }, "Could not add generated ProcedureChangeType.");
        return pcts.get(0);
    }

    private static String getTimeStr()
    {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    private static void printElapsedTime()
    {
        Instant finish = Instant.now();
        Duration duration = Duration.between(startInstant, finish);
        System.out.println("Duration: " + duration);
        System.out.println("Duration in minutes: " + duration.toMinutes());
    }

    public static ProcedureDetails getTestRun()
    {
        ProcedureDetails pd = new ProcedureDetails();
        pd.setPk(1);
        pd.setId("TestProcDetails");
        pd.setEditType(EditType.ORIGINAL);
        pd.setEsd0(false);
        pd.setHazardous(false);
        pd.setStatus(ProcedureStatus.READY);
        return pd;
    }
}
