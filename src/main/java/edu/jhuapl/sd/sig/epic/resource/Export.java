/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.resource;

import edu.jhuapl.sd.sig.epic.data.AppConfigurationDAO;
import edu.jhuapl.sd.sig.epic.data.ProcedureDetailsDAO;
import edu.jhuapl.sd.sig.epic.data.ProgramDAO;
import edu.jhuapl.sd.sig.epic.data.UsersDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.*;
import edu.jhuapl.sd.sig.epic.model.util.AttachmentHandler;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import edu.jhuapl.sd.sig.epic.startup.AppConfiguration;
import edu.jhuapl.sd.sig.epic.startup.AppConfiguration.AppConfigKey;
import edu.jhuapl.sd.sig.epic.startup.EmailEngine;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

@Path("/Export")
public class Export
{
    @Context
    SecurityContext sc;

    private static final Logger LOGGER = LogManager.getLogger(Runs.class.getName());
    private Configuration freemarkerConfig;

    public Export()
    {
        try
        {
            freemarkerConfig = new Configuration(Configuration.VERSION_2_3_29);
            freemarkerConfig.setDirectoryForTemplateLoading(
                    new File(AppConfiguration.getConfigValue(AppConfigKey.TEMPLATES_ROOT_DIR)));
            freemarkerConfig.setDefaultEncoding("UTF-8");
            freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            freemarkerConfig.setLogTemplateExceptions(false);
            freemarkerConfig.setWrapUncheckedExceptions(true);
            freemarkerConfig.setFallbackOnNullLoopVariable(false);
            freemarkerConfig.setBooleanFormat("yes,no");
        }
        catch (IOException e)
        {
            LOGGER.error("Error configuring Freemarker: " + e, e);
        }
    }

    /**
     * Returns the base link for downloading a program export
     * 
     * @return
     */
    public static String getExportDownloadUrl()
    {
        String baseUrl = AppConfiguration.getConfigValue(AppConfigKey.ALLOWED_ORIGIN);
        String contextRoot = AppConfigurationDAO.getConfigForKey(ConfigKey.APP_CONTEXT_ROOT);
        return baseUrl + contextRoot + "/download/";
    }

    /**
     * Determines if a program export file exists for a given ID
     * 
     * @param programExportId
     * @return
     */
    @GET
    @Secured
    @Path("programExportExists/{programExportId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response checkIfProgramExportExists(@PathParam("programExportId") Long programExportId)
    {
        try
        {
            return Response.status(Response.Status.OK).entity(checkForProgramExportExistence(programExportId)).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Error while checking for existence of program export archive with id of " + programExportId, e);
            return Response.status(Response.Status.OK).entity(false).build();
        }
    }

    /**
     * Given a program export id (the name of the export directory - this is the time in milliseconds that the export was started) -
     * returns the zip file for that program export. Note that this endpoint is not secured, unlike other endpoints - this is to
     * accommodate how the front end handles the program download request when someone clicks to the link.
     * 
     * @param programExportId
     * @return
     */
    @GET
    @Path("download/{programExportId}")
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    public Response downloadProgramExport(@PathParam("programExportId") long programExportId)
    {
        File exportDirectory = new File(AppConfiguration.getConfigValue(AppConfigKey.EXPORT_ROOT_DIR) + "/" + programExportId);
        try
        {
            boolean fileExists = checkForProgramExportExistence(programExportId);

            if (fileExists)
            {
                File exportFile = exportDirectory.listFiles()[0]
                        .listFiles((dir, name) -> name.startsWith("export_") && name.endsWith(".zip"))[0];

                StreamingOutput archiveFileStream = getFileStream(exportFile);
                return Response.ok(archiveFileStream, MediaType.APPLICATION_OCTET_STREAM)
                        .header("Content-Disposition", "attachment; filename = \"" + exportFile.getName() + "\"")
                        .header("Access-Control-Expose-Headers", "Content-Disposition")
                        .header("Content-Length", exportFile.length())
                        .build();
            }
            else
            {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Program export archive could not be found.").build();
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to download program export archive. Archive with id " + programExportId + " could not be downloaded.", e);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Program export archive could not be downloaded.").build();
    }

    /**
     * Initiates a program export. This runs in a separate thread so that the user can continue doing other things while the
     * export zip file builds.
     * 
     * @param programPk
     * @return
     */
    @GET
    @Secured
    @Path("program/{programPk}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response exportProgramResource(@PathParam("programPk") int programPk)
    {
        try
        {
            Runnable runnable = () ->
            {
                EntityManager em = JPAUtils.getEntityManager();
                Users requestingUser = UsersDAO.getUserByUsername(em, sc.getUserPrincipal().getName());
                Long programExportId = System.currentTimeMillis();
                Program program = ProgramDAO.getProgram(em, programPk);
                try
                {
                    exportProgram(em, program, programExportId);
                    // after the export is created, the requesting user receives an email with the link.
                    EmailEngine.getInstance().sendMessage(MessageType.NOTIFICATION_PROGRAM_EXPORT_READY, requestingUser,
                            program, programExportId);
                }
                catch (Exception e)
                {
                    String message = "Program export failed during zip file creation. Reason given by server: " + e.getMessage();
                    LOGGER.error(message, e);
                    EmailEngine.getInstance().sendMessage(MessageType.NOTIFICATION_PROGRAM_EXPORT_FAILED, requestingUser,
                            program, programExportId);
                    throw new WebApplicationException(e);
                }
                finally
                {
                    JPAUtils.closeEntityManager(em);
                }
            };
            runnable.run();
            return Response.status(Response.Status.CREATED).entity(true).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Could not export program:", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Could not export program. Reason given by server: " + e.getMessage()).build();
        }
    }

    /**
     * Exports all runs for a given procedure, or a single run.
     * 
     * @param procedureId
     * @return
     */
    @GET
    @Secured
    @Path("procedure/{procedureId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response exportProcedureResource(@PathParam("procedureId") String procedureId)
    {

        EntityManager em = null;

        try
        {
            em = JPAUtils.getEntityManager();
            ProcedureDetails pd = ProcedureDetailsDAO.getProcedureDetailsByUniqueCode(em, procedureId);

            if (pd.getEditType().equals(EditType.ORIGINAL) && pd.getProcedureDetailRuns().isEmpty())
            {
                LOGGER.error("exportProcedureResource() failed to create program export archive for procedure {}. Procedure has no runs to export.", procedureId);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Procedure has no runs. Cannot export.").build();
            }

            File archive = exportProcedure(em, pd);

            if (!archive.exists())
            {
                LOGGER.error("exportProcedureResource() failed to create program export archive for procedure {}. Archive does not exist.", procedureId);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Procedure export archive does not exist. " +
                        "Possibly there are no runs for this program.").build();
            }

            StreamingOutput archiveFileStream = getFileStream(archive);
            return Response.ok(archiveFileStream, MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename = \"" + archive.getName() + "\"")
                    .header("Access-Control-Expose-Headers", "Content-Disposition")
                    .build();

        }
        catch (Exception e)
        {
            LOGGER.error("Could not export procedure:", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Could not export procedure.").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    /**
     * Exports the attachments for a given run/procedure
     * 
     * @param procedureId
     * @return
     */
    @GET
    @Secured
    @Path("procedure/{procedureId}/attachments")
    @Produces({MediaType.APPLICATION_JSON})
    public Response exportProcedureAttachmentsResource(@PathParam("procedureId") String procedureId)
    {

        EntityManager em = null;

        try
        {
            em = JPAUtils.getEntityManager();
            ProcedureDetails pd = ProcedureDetailsDAO.getProcedureDetailsByUniqueCode(em, procedureId);

            File archive = getProcedureAttachmentArchive(em, pd);

            if (!archive.exists())
            {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Procedure attachment export archive does not exist.").build();
            }

            StreamingOutput archiveFileStream = getFileStream(archive);
            return Response.ok(archiveFileStream, MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename = \"" + archive.getName() + "\"")
                    .header("Access-Control-Expose-Headers", "Content-Disposition")
                    .header("Content-Length", archive.length())
                    .build();
        }
        catch (Exception e)
        {
            LOGGER.error("Could not export procedure attachments:", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Could not export procedure attachments.").build();
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    /**
     * Helper function to look for the existence of a program export file given an ID.
     * 
     * @param programExportId
     * @return
     */
    private boolean checkForProgramExportExistence(Long programExportId) throws Exception
    {
        File exportDirectory = new File(AppConfiguration.getConfigValue(AppConfigKey.EXPORT_ROOT_DIR) + "/" + programExportId);
        if (exportDirectory.listFiles().length == 1 && exportDirectory.listFiles()[0].isDirectory())
        {
            // we expect to only find one file in the directory. Get the export file from that directory.
            File exportFile = exportDirectory.listFiles()[0]
                    .listFiles((dir, name) -> name.startsWith("export_") && name.endsWith(".zip"))[0];

            if (!exportFile.exists())
            {
                LOGGER.error("Program export archive with id " + programExportId + " does not exist.");
                return false;
            }
            else
                return true;
        }
        else
            return false;
    }

    /**
     * Creates an HTML export of a run under the directory specified. The run is named
     * based on its ID.
     * 
     * @param run The run to export.
     * @param runOutputPath The path to the directory to write the file to.
     *     Should *not* end in a path separator.
     * @param zipStream The zip file output stream
     */
    public void exportRun(EntityManager em, ProcedureDetails run, java.nio.file.Path rootDir, java.nio.file.Path runOutputPath, ZipOutputStream zipStream) throws TemplateException, IOException
    {
        java.nio.file.Path pathForRunFile = rootDir.resolve(runOutputPath);
        pathForRunFile.toFile().mkdirs();
        File file = new File(pathForRunFile + "/" + run.getId() + ".html");
        Writer writer = new FileWriter(file);

        List<RunStepComment> nonconformanceCommentsForThisRun = run.getAllSteps()
                .stream()
                .flatMap(step -> Optional.ofNullable(step.getRunStepComments())
                        .orElseGet(Collections::emptySortedSet)
                        .stream()
                        .filter(comment -> comment.getIsNonconformance() != null && comment.getIsNonconformance()))
                .collect(Collectors.toList());

        // Get Blackline Comments.
        List<BlackLineComment> blackLineComments = em.createQuery("SELECT b from BlackLineComment b WHERE procedureDetails.pk = :procedureDetailsPk", BlackLineComment.class)
                .setParameter("procedureDetailsPk", run.getPk())
                .getResultList();

        try
        {
            Template runExportTemplate = freemarkerConfig.getTemplate("export/run.html");

            // Build data model.
            Map<String, Object> templateDataModel = new HashMap<>();
            templateDataModel.put("procedureDetails", run);
            templateDataModel.put("nonconformanceComments", nonconformanceCommentsForThisRun);
            templateDataModel.put("blackLineComments", blackLineComments);

            runExportTemplate.process(templateDataModel, writer);
            writer.flush();

            try
            {
                writeFileToZipStream(runOutputPath.toString(), file, zipStream);
            }
            catch (Exception e)
            {
                String message = "Could not add the run export file for run ID " + run.getId() + " to the zip file.";
                LOGGER.error(message, e);
            }
        }
        finally
        {
            writer.close();
        }

    }

    /**
     * Copies all attachments' files to the specified `outputDir` directory.
     */
    private static void bulkAttachmentCopy(EntityManager em, Set<Attachment> attachments, java.nio.file.Path rootPath, java.nio.file.Path outputDir, ZipOutputStream zipStream)
    {
        java.nio.file.Path pathToAttachments = rootPath.resolve(outputDir);
        pathToAttachments.toFile().mkdirs();
        for (Attachment attachment : attachments)
        {
            if (attachment.getEditType() == EditType.REDLINE_DELETE)
                continue;
            try
            {
                // Copy the file
                String destFileName = pathToAttachments /*outputDir*/ + "/" + attachment.getOriginalFilename();
                File destinationFile = new File(destFileName);
                AttachmentHandler.downloadAttachment(new FileOutputStream(destinationFile), AttachmentHandler.getFullPathToFile(attachment));
                // Create a zip file
                writeFileToZipStream(outputDir.toString(), destinationFile, zipStream);
            }
            catch (Exception e)
            {
                if (e instanceof ZipException)
                {
                    // we do not want to log the exceptions for duplicate entries - this fills up the log.
                    if (!e.getMessage().contains("duplicate entry"))
                    {
                        // Allow the process to continue if an attachment is missing.
                        String message = "Attachment with name " + attachment.getOriginalFilename() + " is listed in the EPIC " +
                                "database, but could not be copied to the export directory. It will not be included in the export file.";
                        LOGGER.error(message, e);
                    }
                }
                else
                {
                    LOGGER.error("bulkAttachmentCopy.exception", e);
                }
            }
        }
    }

    /**
     * Creates a procedure export directory and zip file, and calls the method to actually create a procedure export.
     * 
     * @param em
     * @param procedure
     * @return
     * @throws Exception
     */
    private File exportProcedure(EntityManager em, ProcedureDetails procedure) throws Exception
    {
        // Save as {timestamp}/{procedureDefId}/export file

        final java.nio.file.Path rootDir = Paths.get(AppConfiguration.getConfigValue(AppConfigKey.EXPORT_ROOT_DIR))
                .resolve(Long.toString(System.currentTimeMillis()))
                .resolve(procedure.getId());
        rootDir.toFile().mkdirs();
        File zipFile = new File(rootDir + "/" + "export_" + procedure.getId() + ".zip");

        ZipOutputStream zipOutputStream = initializeZipStream(zipFile);
        exportProcedure(em, procedure, rootDir, Paths.get(procedure.getId()), zipOutputStream);
        closeZipStream(zipOutputStream);
        return zipFile;
    }

    /**
     * Creates an export file for all the attachments for a given procedure/run
     * 
     * @param em
     * @param run
     * @return
     * @throws Exception
     */
    private File getProcedureAttachmentArchive(EntityManager em, ProcedureDetails run) throws Exception
    {

        // Save as {timestamp}/{procedureDefId}/export file
        final java.nio.file.Path baseOutputPath = Paths.get(AppConfiguration.getConfigValue(AppConfigKey.EXPORT_ROOT_DIR))
                .resolve(Long.toString(System.currentTimeMillis()))
                .resolve(run.getId());
        baseOutputPath.toFile().mkdirs();
        File attachmentsZipFile = new File(baseOutputPath + "/" + "export_" + run.getId() + ".zip");

        ZipOutputStream zipOutputStream = initializeZipStream(attachmentsZipFile);

        copyAllProcedureDefAttachments(em, run, baseOutputPath, Paths.get("attachments"), zipOutputStream);
        if (run.getRun() != null)
            copyAllRunAttachments(em, run, baseOutputPath, Paths.get("attachments"), zipOutputStream);

        closeZipStream(zipOutputStream);
        return attachmentsZipFile;
    }

    /**
     * Sets up the copying of all the procedure and step_def attachments for a procedure/run
     */
    private void copyAllProcedureDefAttachments(EntityManager em, ProcedureDetails procedure, java.nio.file.Path rootPath, java.nio.file.Path targetPath, ZipOutputStream zipOutputStream)
            throws WebApplicationException
    {
        Set<Attachment> procedureAttachments = (Set<Attachment>) (Object) procedure.getProcedureHeader().getAttachments();
        bulkAttachmentCopy(em, procedureAttachments, rootPath, targetPath.resolve("procedure_attachments"), zipOutputStream);

        Set<Attachment> stepAttachments = procedure.getAllSteps().stream().flatMap(step -> step.getStepDefAttachments().stream()).collect(Collectors.toSet());
        bulkAttachmentCopy(em, stepAttachments, rootPath, targetPath.resolve("step_def_attachments"), zipOutputStream);
    }

    /**
     * Sets up the copying of all run and run step attachments for a run
     * 
     * @param em
     * @param run
     * @param targetPath
     * @param zipOutputStream
     * @throws WebApplicationException
     */
    private void copyAllRunAttachments(EntityManager em, ProcedureDetails run, java.nio.file.Path rootPath, java.nio.file.Path targetPath, ZipOutputStream zipOutputStream)
            throws WebApplicationException
    {

        if (run.getRun() == null)
        {
            String msg = "Cannot get run attachments for procedure without run.";
            LOGGER.error(msg);
            throw new WebApplicationException(msg);
        }

        bulkAttachmentCopy(em, (Set<Attachment>) (Object) run.getRun().getAttachments(), rootPath, targetPath.resolve("run_attachments"), zipOutputStream);

        bulkAttachmentCopy(em, run.getAllSteps().stream().flatMap(step -> step.getRunStepAttachments().stream()).collect(Collectors.toSet()), rootPath,
                targetPath.resolve("run_step_attachments"), zipOutputStream);

    }

    /**
     * Sets up the export for the run/procedure. First attachments are copied to the zip stream, then the run html file is created.
     */
    private void exportProcedure(EntityManager em, ProcedureDetails procedure, java.nio.file.Path rootPath, java.nio.file.Path procedureDefOutputPath, ZipOutputStream zipStream) throws Exception
    {
        final java.nio.file.Path exportAttachmentsPath = procedureDefOutputPath.resolve("attachments");
        final java.nio.file.Path exportRunsPath = procedureDefOutputPath.resolve("runs");

        packageProcedureAndStepDefAttachments(em, rootPath, exportAttachmentsPath, procedure, zipStream);

        // Export each run & its attachments.
        if (procedure.getEditType().equals(EditType.ORIGINAL))
        {
            List<ProcedureDetails> runsForThisProcedure = em.createQuery("SELECT p from ProcedureDetails p WHERE p.originalProcedureDetails.pk = :procedureDetailsPk ", ProcedureDetails.class)
                    .setParameter("procedureDetailsPk", procedure.getPk())
                    .getResultList();

            for (ProcedureDetails run : runsForThisProcedure)
            {
                doRunAndAttachmentsExport(em, rootPath, exportRunsPath, run, zipStream);
            }
            ;
        }
        else
        {
            doRunAndAttachmentsExport(em, rootPath, exportRunsPath, procedure, zipStream);
        }
    }

    /**
     * Sets up the export of the run and its attachments
     * 
     * @param em
     * @param parentPath
     * @param run
     * @param zipStream
     */
    private void doRunAndAttachmentsExport(EntityManager em, java.nio.file.Path rootPath, java.nio.file.Path parentPath, ProcedureDetails run, ZipOutputStream zipStream)
    {
        try
        {
            final java.nio.file.Path exportRunPath = parentPath.resolve(run.getId());
            exportRun(em, run, rootPath, exportRunPath, zipStream);
            packageRunAndRunStepAttachments(em, rootPath, exportRunPath, run, zipStream);
        }
        catch (TemplateException e)
        {
            LOGGER.error("Error processing run export template:", e);
        }
        catch (IOException e)
        {
            LOGGER.error("Error writing run to file:", e);
        }
    }

    /**
     * Sets up the export of the procedure/step def attachments
     * 
     * @param em
     * @param exportAttachmentsPath
     * @param procedureDetails
     * @param zipStream
     * @throws WebApplicationException
     */
    private void packageProcedureAndStepDefAttachments(EntityManager em, java.nio.file.Path rootPath, java.nio.file.Path exportAttachmentsPath,
            ProcedureDetails procedureDetails, ZipOutputStream zipStream) throws WebApplicationException
    {
        // Copy attachments to `attachments` dir in the archive directory.
        Set<Attachment> procedureAttachments = (Set<Attachment>) (Object) procedureDetails.getProcedureHeader().getAttachments();
        if (procedureAttachments != null && !procedureAttachments.isEmpty())
        {
            bulkAttachmentCopy(em, procedureAttachments, rootPath, exportAttachmentsPath.resolve("procedure_attachments"), zipStream);
        }
        Set<Attachment> stepDefAttachments = procedureDetails.getAllSteps().stream()
                .flatMap(step -> step.getStepDefAttachments().stream())
                .collect(Collectors.toSet());
        if (stepDefAttachments != null && !stepDefAttachments.isEmpty())
        {
            bulkAttachmentCopy(em, stepDefAttachments, rootPath, exportAttachmentsPath.resolve("step_def_attachments"), zipStream);
        }
    }

    /**
     * Sets up the export of run attachments
     * 
     * @param em
     * @param exportRunPath
     * @param run
     * @param zipOutputStream
     * @throws WebApplicationException
     */
    private void packageRunAndRunStepAttachments(EntityManager em, java.nio.file.Path rootPath, java.nio.file.Path exportRunPath, ProcedureDetails run, ZipOutputStream zipOutputStream)
            throws WebApplicationException
    {
        Set<Attachment> runAttachments = (Set<Attachment>) (Object) run.getRun().getAttachments();
        if (runAttachments != null && !runAttachments.isEmpty())
        {
            bulkAttachmentCopy(em, runAttachments, rootPath, exportRunPath.resolve("run_attachments"), zipOutputStream);
        }
        Set<Attachment> runStepAttachments = run.getAllSteps().stream()
                .flatMap(step -> step.getRunStepAttachments().stream())
                .collect(Collectors.toSet());
        if (runStepAttachments != null && !runStepAttachments.isEmpty())
        {
            bulkAttachmentCopy(em, runStepAttachments, rootPath, exportRunPath.resolve("run_step_attachments"), zipOutputStream);
        }
    }

    /**
     * Given a file, writes the file to an output stream.
     * 
     * @param file
     * @return
     */
    private StreamingOutput getFileStream(File file)
    {
        return new StreamingOutput()
        {
            @Override
            public void write(OutputStream outputStream) throws IOException, WebApplicationException
            {
                try
                {
                    byte[] buffer = new byte[1024];
                    int length;
                    FileInputStream source = new FileInputStream(file);
                    while ((length = source.read(buffer)) > 0)
                    {
                        outputStream.write(buffer, 0, length);
                    }
                    source.close();
                    outputStream.flush();
                }
                catch (Exception e)
                {
                    throw new WebApplicationException("File Not Found!!");
                }
                finally
                {
                    outputStream.close();
                }
            }
        };
    }

    /**
     * Creates a ZIP file of the exported procedures for the given program in the directory specified.
     * Note that this does not return the zip file to the endpoint; because these files get so large,
     * they are generated in a separate thread and then when done, the user is emailed a download link.
     */
    private void exportProgram(EntityManager em, Program program, Long exportId) throws Exception
    {
        List<Integer> procedurePks = em
                .createQuery("SELECT DISTINCT v.originalProcedureDetails.pk FROM ProcedureDetails v WHERE v.procedureDef.program.pk = :programPk AND v.originalProcedureDetails != null ",
                        Integer.class)
                .setParameter("programPk", program.getPk())
                .getResultList();

        java.nio.file.Path baseOutPath = Paths.get(AppConfiguration.getConfigValue(AppConfigKey.EXPORT_ROOT_DIR))
                .resolve(Long.toString(exportId))
                .resolve(program.getName());
        baseOutPath.toFile().mkdirs();
        File zipFile = new File(baseOutPath + "/" + "export_" + program.getName() + ".zip");

        ZipOutputStream zipOutputStream = initializeZipStream(zipFile);

        for (Integer procedurePk : procedurePks)
        {
            ProcedureDetails procedure = JPAUtils.getRecordById(em, ProcedureDetails.class, procedurePk);
            exportProcedure(em, procedure, baseOutPath, Paths.get(procedure.getId()), zipOutputStream);
            JPAUtils.closeEntityManager(em);
            em = JPAUtils.getEntityManager();
            FileUtils.deleteDirectory(new File(baseOutPath + "/" + procedure.getId()));
        }
        closeZipStream(zipOutputStream);
    }

    private void closeZipStream(ZipOutputStream zipOutputStream) throws IOException
    {
        zipOutputStream.finish();
        zipOutputStream.close();
    }

    /**
     * Helper method for initializing a zip stream
     * 
     * @param zipFile
     * @return
     * @throws FileNotFoundException
     */
    private ZipOutputStream initializeZipStream(File zipFile) throws FileNotFoundException
    {
        return new ZipOutputStream(new FileOutputStream(zipFile));
    }

    /**
     * Helper method for writing a file to the given zip stream
     * 
     * @param path
     * @param sourceFile
     * @param zipStream
     * @throws Exception
     */
    private static void writeFileToZipStream(String path, File sourceFile, ZipOutputStream zipStream) throws Exception
    {
        byte[] buffer = new byte[1024];
        int length;
        FileInputStream source = new FileInputStream(sourceFile.getPath());
        zipStream.putNextEntry(new ZipEntry(path + "/" + (sourceFile.getName())));
        while ((length = source.read(buffer)) > 0)
        {
            zipStream.write(buffer, 0, length);
        }
        source.close();
    }
}
