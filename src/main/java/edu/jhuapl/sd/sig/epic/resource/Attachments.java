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

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.sd.sig.epic.data.AttachmentDAO;
import edu.jhuapl.sd.sig.epic.data.RunDAO;
import edu.jhuapl.sd.sig.epic.data.UsersDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.*;
import edu.jhuapl.sd.sig.epic.model.util.*;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import edu.jhuapl.sd.sig.epic.resource.model.AttachmentType;
import edu.jhuapl.sd.sig.epic.resource.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

@Secured
@Path("/Attachments")
public class Attachments
{

    @Context
    SecurityContext sc;

    private static final Logger LOGGER = LogManager.getLogger();
    private GenericExceptionMapper gem = new GenericExceptionMapper();

    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/Save")
    public Response saveAttachment(@FormDataParam("file") InputStream fileInputStream,
            @FormDataParam("file") FormDataContentDisposition fileMetaData,
            @FormDataParam("file") FormDataBodyPart fileBody,
            @FormDataParam("parentPk") Integer parentPk,
            @FormDataParam("attachmentType") AttachmentType attachmentType,
            @FormDataParam("editType") EditType editType,
            @FormDataParam("redLineComment") String rlComment)
    {

        EntityManager em = null;
        try
        {
            String newFilename = UUID.randomUUID().toString();
            String destPath = AttachmentHandler.UPLOAD_ROOT_DIR + File.separator + newFilename;

            LOGGER.debug("Saving attachment with newname: {}", fileMetaData.getFileName());

            //save the file
            long attachmentSize = AttachmentHandler.saveAttachment(fileInputStream, destPath);
            LOGGER.info("Saved attachment with newname: {} - has {} bytes", fileMetaData.getFileName(), newFilename);
            if (!AttachmentHandler.isAllowedFileSize(attachmentSize))
            {
                // delete the file if it exceeds the allowed size
                AttachmentHandler.deleteAttachment(destPath);
                throw new BadRequestException("The attachment " + fileMetaData.getFileName() + " file size of " + attachmentSize + " bytes exceeds max allowed size of " +
                        AttachmentHandler.getAttachmentMaxAllowedSizeWithUnits());
            }

            em = JPAUtils.getEntityManager();

            String originalFilename = fileMetaData.getFileName();
            Boolean isImage = fileBody.getMediaType().getType().equalsIgnoreCase("image");

            // the redline comment was passed in as a json string; need to map it back to an object.
            ObjectMapper objectMapper = new ObjectMapper();
            RedLineComment redLineComment = objectMapper.readValue(rlComment, RedLineComment.class);

            Attachment a;
            StepDef sd;
            Users user = UsersDAO.getUserByUsername(em, sc.getUserPrincipal().getName());

            switch (attachmentType)
            {
                case PROCEDURE:
                    ProcedureHeader ph = JPAUtils.getRecordById(em, ProcedureHeader.class, parentPk);

                    //save db record
                    ProcedureAttachment pa = new ProcedureAttachment();
                    pa.setOriginalFilename(originalFilename);
                    pa.setFilename(newFilename);
                    pa.setIsImage(isImage);
                    pa.setProcedureHeader(ph);
                    pa.setEditType(editType);

                    // set the procedure details on the redline comment if not null
                    if (redLineComment != null)
                    {
                        redLineComment.setProcedureDetails(ph.getProcedureDetails());
                    }
                    a = AttachmentDAO.saveAttachment(em, pa, redLineComment, null);
                    break;
                case STEP_DEF:
                    sd = JPAUtils.getRecordById(em, StepDef.class, parentPk);

                    StepDefAttachment sda = new StepDefAttachment();
                    sda.setOriginalFilename(originalFilename);
                    sda.setFilename(newFilename);
                    sda.setIsImage(isImage);
                    sda.setStepDef(sd);
                    sda.setEditType(editType);

                    // set the step on the redline comment if not null
                    if (redLineComment != null)
                    {
                        redLineComment.setStepDef(sd);

                        // redline comments for steps also need to link to the parent procedure details
                        StepGroupDef parent = sd.getStepGroupDef();
                        while (parent != null)
                        {
                            if (parent.getStepGroupDefParent() == null)
                            {
                                // if here, parent is a top level group
                                redLineComment.setProcedureDetails(parent.getProcedureDetails());
                                break;
                            }
                            else
                            {
                                parent = parent.getStepGroupDefParent();
                            }
                        }
                    }

                    a = AttachmentDAO.saveAttachment(em, sda, redLineComment, null);
                    break;
                // Note that run step and run attachments cannot be redlined. Comment for these is null.
                case STEP_RUN:
                    sd = JPAUtils.getRecordById(em, StepDef.class, parentPk);

                    //save database record
                    RunStepAttachment rsa = new RunStepAttachment();
                    rsa.setFilename(newFilename);
                    rsa.setOriginalFilename(originalFilename);
                    rsa.setIsImage(isImage);
                    rsa.setStepDef(sd);
                    rsa.setEditType(editType);
                    a = AttachmentDAO.saveAttachment(em, rsa, null, user);
                    em.refresh(sd);
                    return Response.status(Response.Status.CREATED).entity(sd).build();
                case RUN:
                    Run r = JPAUtils.getRecordById(em, Run.class, parentPk);
                    r = RunDAO.getRunByUniqueCode(em, r.getProcedureDetails().getId());

                    //save database record
                    RunAttachment ra = new RunAttachment();
                    ra.setFilename(newFilename);
                    ra.setOriginalFilename(originalFilename);
                    ra.setIsImage(isImage);
                    ra.setRun(r);
                    ra.setEditType(editType);
                    a = AttachmentDAO.saveAttachment(em, ra, null, user);
                    em.refresh(r);
                    return Response.status(Response.Status.CREATED).entity(r).build();
                default:
                    LOGGER.error("Invalid attachment type for attachment: " + attachmentType.toString());
                    throw new WebApplicationException("Invalid attachment type for attachment, unable to save attachment");
            }

            return Response.status(Response.Status.CREATED).entity(a).build();
        }
        catch (Exception e)
        {
            LOGGER.error("Problem saving attachment", e);
            return gem.toResponse(e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @GET
    @Path("/Download")
    public Response downloadFile(@QueryParam("pk") int pk)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            Attachment a = JPAUtils.getRecordById(em, Attachment.class, pk);
            StreamingOutput fileStream = new StreamingOutput()
            {
                @Override
                public void write(OutputStream outputStream) throws IOException, WebApplicationException
                {
                    try
                    {
                        //Download the file
                        AttachmentHandler.downloadAttachment(outputStream, AttachmentHandler.getFullPathToFile(a));
                        outputStream.flush();
                    }
                    catch (Exception e)
                    {
                        throw new WebApplicationException("Error occurred downloading the attachment: " + e.getMessage());
                    }
                }
            };
            return Response.ok(fileStream, MediaType.APPLICATION_OCTET_STREAM).header("Content-Disposition", "attachment; filename = \"" + a.getOriginalFilename() + "\"")
                    .header("Access-Control-Expose-Headers", "Content-Disposition").build();
        }
        catch (Exception e)
        {
            LOGGER.error("Unable to download file.", e);
            return gem.toResponse(e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    @DELETE
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/Delete")
    public Response deleteAttachmentItem(@QueryParam("pk") int pk)
    {
        EntityManager em = null;
        StepDef step = null;
        Run run = null;
        try
        {
            em = JPAUtils.getEntityManager();
            Attachment a = JPAUtils.getRecordById(em, Attachment.class, pk);
            Users user = UsersDAO.getUserByUsername(em, sc.getUserPrincipal().getName());
            if (a instanceof RunStepAttachment)
            {
                step = JPAUtils.getRecordById(em, StepDef.class, ((RunStepAttachment) a).getStepDef().getPk());
            }
            else if (a instanceof RunAttachment)
            {
                run = JPAUtils.getRecordById(em, Run.class, ((RunAttachment) a).getRun().getPk());
            }

            AttachmentDAO.deleteAttachment(em, a, user);
            AttachmentHandler.deleteAttachment(AttachmentHandler.getFullPathToFile(a));

            if (step != null)
            {
                em.refresh(step);
            }
            else if (run != null)
            {
                em.refresh(run);
                // if here, then return the run as a response; otherwise return step
                return Response.status(Response.Status.OK).entity(run).build();
            }

        }
        catch (Exception e)
        {
            LOGGER.error("Failed to delete attachment with pk: " + pk, e);
            return gem.toResponse(e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
        return Response.status(Response.Status.OK).entity(step).build();
    }

    @PUT
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/RedLineDelete")
    public Response updateAttachmentAsRedLineDelete(@QueryParam("pk") int pk, RedLineComment redLineComment)
    {
        EntityManager em = null;
        try
        {
            em = JPAUtils.getEntityManager();
            Attachment a = JPAUtils.getRecordById(em, Attachment.class, pk);
            AttachmentDAO.redlineDeleteAttachment(em, a, redLineComment);
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to redline delete attachment with pk: " + pk, e);
            return gem.toResponse(e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }

        return Response.status(Response.Status.OK).entity(null).build();
    }

    @GET
    @Produces({MediaType.TEXT_PLAIN})
    @Path("/MaxUploadSize")
    public Response getAttachmentsMaxSizeBytes()
    {
        return Response.ok(String.valueOf(AttachmentHandler.getAttachmentMaxAllowedSize())).build();
    }
}
