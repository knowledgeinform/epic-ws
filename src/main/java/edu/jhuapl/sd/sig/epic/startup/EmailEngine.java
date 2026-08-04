/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.startup;

import edu.jhuapl.sd.sig.epic.data.AppConfigurationDAO;
import edu.jhuapl.sd.sig.epic.data.ApprovalsDAO;
import edu.jhuapl.sd.sig.epic.data.ProcedureApproverDAO;
import edu.jhuapl.sd.sig.epic.data.RunDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.*;
import edu.jhuapl.sd.sig.epic.resource.Export;
import edu.jhuapl.sd.sig.epic.startup.AppConfiguration.AppConfigKey;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Periodically scans database
 */
public class EmailEngine
{

    private final Logger LOGGER = LogManager.getLogger();

    private Integer checkFrequencyMinutes = Integer
            .parseInt(AppConfigurationDAO.getConfigForKey(ConfigKey.EMAIL_REMINDER_CHECK_FREQUENCY_MINUTES));
    private Integer reminderThresholdMinutes = Integer
            .parseInt(AppConfigurationDAO.getConfigForKey(ConfigKey.EMAIL_REMINDER_THRESHOLD_MINUTES));
    private String fromAddr = AppConfigurationDAO.getConfigForKey(ConfigKey.EMAIL_FROM_ADDR);
    private String serverHostname = AppConfigurationDAO.getConfigForKey(ConfigKey.EMAIL_SERVER_HOSTNAME);
    private ScheduledExecutorService executorService;
    private Map<ProcedureApprovalType, ApprovalWords> approvalWordMap = new HashMap<>();
    {
        approvalWordMap.put(ProcedureApprovalType.APPROVER, new ApprovalWords("an <b>APPROVER</b>", "approver", "APPROVE", "Approval Required"));
        approvalWordMap.put(ProcedureApprovalType.REVIEWER, new ApprovalWords("a <b>REVIEWER</b>", "reviewer", "REVIEW", "Review Requested"));
    }

    private Configuration freemarkerConfig;
    public Map<MessageType, EmailTemplate> messageTemplatesMap;

    private static EmailEngine emailEngineInstance = null;

    private EmailEngine()
    {
        freemarkerConfig = new Configuration(Configuration.VERSION_2_3_29);
        freemarkerConfig.setDefaultEncoding("UTF-8");
        freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        freemarkerConfig.setLogTemplateExceptions(false);
        freemarkerConfig.setWrapUncheckedExceptions(true);
        freemarkerConfig.setFallbackOnNullLoopVariable(false);

        this.messageTemplatesMap = new HashMap<>();

        messageTemplatesMap.put(MessageType.REMINDER_PROCEDURE_REVIEW_OR_APPROVE, new EmailTemplate(
                "Procedure Review/Approvals Reminder",
                "<p style=\"color:red\">The following EPIC procedures require your review or approval:</p> <ol style=\"color:red\">" + "<#list approvals as approval>"
                        + "<li><#if approval.approvalType == \"APPROVER\">APPROVE<#else>REVIEW</#if> "
                        + "<a href=\"${approval.procedureDetails.procedureUrl}\">${approval.procedureDetails.id} - ${approval.procedureDetails.procedureDef.name},</a>"
                        + " due ${approval.procedureDetails.procedureApprovalDueDate?date}."
                        + "</li>" + "</#list>" + "</ol>"));

        // Procedure related message templates
        messageTemplatesMap.put(MessageType.NOTIFICATION_PROCEDURE_WAITING, new EmailTemplate(
                "${approvalWords.subjectAction} - ${approval.procedureDetails.id} - ${approval.procedureDetails.procedureDef.name}",
                "You have been added as ${approvalWords.userRoleNoun} to the following procedure: <a href=\"${approval.procedureDetails.procedureUrl}\">${approval.procedureDetails.procedureDef.name}</a> due ${approval.procedureDetails.procedureApprovalDueDate?date}"));

        messageTemplatesMap.put(MessageType.NOTIFICATION_PROCEDURE_DRAFT, new EmailTemplate(
                "Procedure Approval no longer required - ${approval.procedureDetails.id} - ${approval.procedureDetails.procedureDef.name}",
                "A <a href=\"${approval.procedureDetails.procedureUrl}\">procedure</a> you were assigned to ${approvalWords.action} has been returned to draft status. You are no longer required to approve this procedure. Following any changes, you may receive a subsequent email for approval at a later time."));

        messageTemplatesMap.put(MessageType.NOTIFICATION_PROCEDURE_REMOVED, new EmailTemplate(
                "Procedure Approval no longer required - ${approval.procedureDetails.id} - ${approval.procedureDetails.procedureDef.name}",
                "You have been removed as ${approvalWords.userRoleNoun} from a procedure. You are no longer required to approve <a href=\"${approval.procedureDetails.procedureUrl}\">this procedure</a>. Following any changes, you may receive a subsequent email for approval at a later time."));

        // run related message templates
        messageTemplatesMap.put(MessageType.NOTIFICATION_RUN_CLOSEOUT_WAITING, new EmailTemplate(
                "${approvalWords.subjectAction} - ${approval.run.procedureDetails.id} - ${approval.run.name}",
                "You have been added as ${approvalWords.userRoleNoun} to the following run closeout: <a href=\"${approval.run.procedureDetails.runUrl}\">${approval.run.name}</a> due ${approval.dueDate?date}"));

        messageTemplatesMap.put(MessageType.NOTIFICATION_RUN_CLOSEOUT_RETRACTED, new EmailTemplate(
                "Run Closeout Approval no longer required - ${approval.run.procedureDetails.id} - ${approval.run.name}",
                "A <a href=\"${approval.run.procedureDetails.runUrl}\">run closeout</a> you were assigned to ${approvalWords.action} has been transitioned to ${approval.run.status} status. You are no longer required to approve this run at this time. Following any changes, you may receive a subsequent email for approval at a later time."));

        messageTemplatesMap.put(MessageType.NOTIFICATION_RUN_CLOSEOUT_APPROVER_REMOVED, new EmailTemplate(
                "Run Closeout Approval no longer required - ${approval.run.procedureDetails.id} - ${approval.run.name}",
                "You have been removed as ${approvalWords.userRoleNoun} from a run closeout. You are no longer required to approve <a href=\"${approval.run.procedureDetails.runUrl}\">this run closeout</a>. Following any changes, you may receive a subsequent email for approval at a later time."));

        messageTemplatesMap.put(MessageType.REMINDER_RUN_CLOSEOUT_APPROVE, new EmailTemplate(
                "Run Closeout Approvals Reminder",
                "<p style=\"color:red\">The following EPIC run closeouts require your approval:</p> <ol style=\"color:red\">" + "<#list approvals as approval>"
                        + "<li>APPROVE " + "<a href=\"${approval.run.procedureDetails.runUrl}\">${approval.run.procedureDetails.id} - ${approval.run.name},</a>"
                        + " due ${approval.dueDate?date}."
                        + "</li>" + "</#list>" + "</ol>"));

        messageTemplatesMap.put(MessageType.NOTIFICATION_RUN_CLOSEOUT_FULLY_APPROVED, new EmailTemplate(
                "Run Closeout fully approved, move to COMPLETE status - ${approval.run.procedureDetails.id} - ${approval.run.name}",
                "Your run closeout submission has been fully approved. You may now log into EPIC and <a href=\"${approval.run.procedureDetails.runUrl}\">transition the run</a> to COMPLETED status."));

        messageTemplatesMap.put(MessageType.NOTIFICATION_RUN_CLOSEOUT_CORRECTIONS_NEEDED, new EmailTemplate(
                "Run Closeout Needs Correction(s) - ${approval.run.procedureDetails.id} - ${approval.run.name}",
                "Your run closeout submission has been marked NOT APPROVED and needs correction(s). You may <a href=\"${approval.run.procedureDetails.runUrl}\">review and correct the run</a> now."));

        messageTemplatesMap.put(MessageType.NOTIFICATION_PROGRAM_EXPORT_READY, new EmailTemplate(
                "Requested Program Export for ${program.name} is Ready for Download",
                "You requested an export of the full ${program.name} program, and it can be downloaded from <a href=\"${exportUrl}\">this link</a> now. To save space on the server, this export will be deleted in 24 hours."));

        messageTemplatesMap.put(MessageType.NOTIFICATION_PROGRAM_EXPORT_FAILED, new EmailTemplate(
                "Requested Program Export for ${program.name} Failed",
                "You requested an export of the full ${program.name} program. The export unfortunately failed to complete; please contact EPIC Support for assistance."));

        StringTemplateLoader stringLoader = new StringTemplateLoader();
        messageTemplatesMap.forEach((type, template) ->
        {
            // TODO: Consider centralizing the template prefixes (and other template-related
            // functionality) to the EmailTemplate class.
            stringLoader.putTemplate("SUBJECT_" + type.toString(), template.subject);
            stringLoader.putTemplate("BODY_" + type.toString(), template.body);
        });

        freemarkerConfig.setTemplateLoader(stringLoader);
    }

    public static EmailEngine getInstance()
    {
        if (EmailEngine.emailEngineInstance == null)
            EmailEngine.emailEngineInstance = new EmailEngine();
        return EmailEngine.emailEngineInstance;
    }

    /**
     * Begins periodic checking for emails to send.
     */
    public void start()
    {
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> this.checkAndSendEmails(), this.checkFrequencyMinutes,
                this.checkFrequencyMinutes, TimeUnit.MINUTES);
    }

    public void stop()
    {
        List<Runnable> terminatedTasks = this.executorService.shutdownNow();
        LOGGER.info("Email engine stopped, terminating " + terminatedTasks.size() + " tasks.");
    }

    public void checkAndSendEmails()
    {
        Calendar thresholdCal = Calendar.getInstance();
        thresholdCal.add(Calendar.MINUTE, -this.reminderThresholdMinutes);
        Date thresholdDate = thresholdCal.getTime();

        EntityManager em = JPAUtils.getEntityManager();
        try
        {
            this.handleCheckAndSendOfProcedureApprovalEmails(em, thresholdDate);
            this.handleCheckAndSendOfRunApprovalEmails(em, thresholdDate);
        }
        catch (Exception e)
        {
            LOGGER.error("Error sending emails: " + e, e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }

    }

    /**
     * Handles the procedure approval emails
     * 
     * @param em
     */
    private void handleCheckAndSendOfProcedureApprovalEmails(EntityManager em, Date thresholdDate)
    {
        Set<ProcedureApproval> unapprovedEmails = ProcedureApproverDAO.getUnapprovedApprovals(em);

        // Generate of list of procedures to notify about.
        Set<ProcedureApproval> notifications = unapprovedEmails.stream()
                .filter(pa -> pa.getLastReminderDate() == null)
                .collect(Collectors.toSet());

        // Generate lists of procedures to remind about.
        Map<Users, Set<ProcedureApproval>> reminders = unapprovedEmails.stream()
                // Downselect to procedures requiring action by the next calendar day and not recently notified.
                .filter((pa) ->
                {

                    LocalDate dueDate = pa.getProcedureDetails().getProcedureApprovalDueDate().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    boolean dueByTomorrow = !dueDate.isAfter(LocalDate.now().plusDays(1));

                    boolean recentlyNotified = pa.getLastReminderDate() != null && pa.getLastReminderDate().compareTo(thresholdDate) >= 0;

                    boolean isReviewer = pa.getApprovalType().equals(ProcedureApprovalType.REVIEWER);

                    return dueByTomorrow && !recentlyNotified && !isReviewer;
                })
                .collect(Collectors.groupingBy(ProcedureApproval::getUsers, Collectors.toSet()));

        // Send notifications.
        notifications.forEach(approval -> this.sendMessage(MessageType.NOTIFICATION_PROCEDURE_WAITING, approval));

        // Send reminders.
        reminders.forEach((user, approvals) -> this.sendMessage(MessageType.REMINDER_PROCEDURE_REVIEW_OR_APPROVE, user, approvals));
    }

    /**
     * Handles run closeout approval emails
     * 
     * @param em
     * @param thresholdDate
     */
    private void handleCheckAndSendOfRunApprovalEmails(EntityManager em, Date thresholdDate)
    {
        // get list of unapproved run closeouts
        Set<Run> runsSubmittedForCloseout = RunDAO.getRunsWhereCloseoutIsSubmittedAndIncomplete(em);

        // for each run, iterate through approvals
        //    when get to an approval with null last reminder date and !isApproved, send notification email
        // remember that run closeout approvals are hierarchical - only emailing the first approver in the sorted set that
        // hasn't yet approved.
        Set<RunApproval> notifications = new HashSet<>();
        Set<RunApproval> remindersList = new HashSet<>();

        runsSubmittedForCloseout.forEach(run ->
        {
            if (run.getRunApprovals() != null && !run.getRunApprovals().isEmpty())
            {
                Iterator<RunApproval> iterator = run.getRunApprovals().iterator();
                while (iterator.hasNext())
                {
                    RunApproval ra = iterator.next();
                    boolean activeApprover = ra.getApproverDisabled() == null || !ra.getApproverDisabled();
                    if (activeApprover && (ra.getIsApproved() == null || !ra.getIsApproved()))
                    {
                        if (ra.getLastReminderDate() == null)
                        {
                            notifications.add(ra);
                        }
                        else
                        {
                            LocalDate dueDate = ra.getDueDate().toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate();
                            boolean dueByTomorrow = !dueDate.isAfter(LocalDate.now().plusDays(1));
                            boolean recentlyNotified = ra.getLastReminderDate() != null && ra.getLastReminderDate().compareTo(thresholdDate) >= 0;

                            if (dueByTomorrow && !recentlyNotified)
                            {
                                remindersList.add(ra);
                            }
                        }
                        break;
                    }
                }
            }
        });

        Map<Users, Set<RunApproval>> remindersMap = remindersList.stream().collect(Collectors.groupingBy(RunApproval::getUsers, Collectors.toSet()));

        // Send notifications.
        notifications.forEach(approval -> this.sendMessage(MessageType.NOTIFICATION_RUN_CLOSEOUT_WAITING, approval));

        // Send reminders.
        remindersMap.forEach((user, approvals) -> this.sendMessage(MessageType.REMINDER_RUN_CLOSEOUT_APPROVE, user, approvals));
    }

    /**
     * Sends an email to the recipient. Subject is prefixed with "EPIC".
     */
    private void sendEmail(Users recipient, String subject, String body, MessageType messageType) throws EmailException
    {
        this.sendEmail(Arrays.asList(recipient), subject, body, messageType);
    }

    /**
     * Sends an email to the list of recipients. Subject is prefixed with "EPIC".
     */
    private void sendEmail(List<Users> recipients, String subject, String body, MessageType messageType) throws EmailException
    {

        if (AppConfiguration.getConfigValue(AppConfigKey.DISABLE_EMAIL).trim().equalsIgnoreCase("true"))
            return;

        subject = "EPIC - " + subject;

        HtmlEmail email = new HtmlEmail();

        email.setHostName(this.serverHostname);
        email.setFrom(this.fromAddr);
        for (Users recipient : recipients)
            email.addTo(recipient.getEmail());
        email.setSubject(subject);
        email.setHtmlMsg(body);
        email.send();

    }

    /**
     * Sends a personalized message to each Approver/Reviewer on the specified
     * ProcedureDetails.
     */
    public void sendMessage(MessageType messageType, ProcedureDetails pd)
    {
        this.sendMessageFromRunOrProcedure(messageType, new ArrayList<>(pd.getProcedureApprovals()));
    }

    /**
     * Sends message to the next approver on the run closeout who hasn't approved yet.
     * 
     * @param messageType
     * @param run
     */
    public void sendMessage(MessageType messageType, Run run)
    {
        Iterator<RunApproval> iterator = run.getRunApprovals().iterator();
        while (iterator.hasNext())
        {
            RunApproval ra = iterator.next();
            boolean activeApprover = ra.getApproverDisabled() == null || !ra.getApproverDisabled();
            if (activeApprover && (ra.getIsApproved() == null || !ra.getIsApproved()))
            {
                this.sendMessageFromRunOrProcedure(messageType, Arrays.asList(ra));
                break;
            }
        }
    }

    private void sendMessageFromRunOrProcedure(MessageType messageType, List<Approval> approvals)
    {
        approvals.forEach((approval) ->
        {
            this.sendMessage(messageType, approval);
        });
    }

    /**
     * Sends a personalized message to the specified recipient about the approval.
     */
    public void sendMessage(MessageType messageType, Users recipient, Approval approval)
    {
        this.sendMessage(messageType, recipient, new HashSet<>(Collections.singleton(approval)));
    }

    /**
     * Sends a personalized message to the person on the approval.
     */
    public void sendMessage(MessageType messageType, Approval approval)
    {
        if (approval.getClass().equals(ProcedureApproval.class))
        {
            this.sendMessage(messageType, approval.getUsers(), new HashSet<>(Collections.singleton(approval)));
        }
        else
        {
            this.sendMessage(messageType, approval.getUsers(), new HashSet<>(Collections.singleton(approval)));
        }

    }

    /**
     * Sends a personalized message to the specified recipient about the approval.
     */
    public void sendMessage(MessageType messageType, Users recipient, Set<? extends Approval> approvals)
    {
        if (recipient == null)
        {
            LOGGER.warn("Null recipient for message type {} — skipping email", messageType);
            return;
        }

        EntityManager em = JPAUtils.getEntityManager();
        try
        {
            Template subjectTemplate = this.freemarkerConfig.getTemplate("SUBJECT_" + messageType.toString());
            String subject = this.processTemplate(subjectTemplate, recipient, new HashSet<>(approvals));
            Template bodyTemplate = this.freemarkerConfig.getTemplate("BODY_" + messageType);
            String body = this.processTemplate(bodyTemplate, recipient, new HashSet<>(approvals));

            this.sendEmail(recipient, subject, body, messageType);

            // Update approval last reminder date.
            if (messageType.equals(MessageType.REMINDER_PROCEDURE_REVIEW_OR_APPROVE) || messageType.equals(MessageType.NOTIFICATION_PROCEDURE_WAITING))
            {
                approvals.forEach(approval ->
                {
                    approval.setLastReminderDate(new Date());
                    ApprovalsDAO.updateApproval(em, (ProcedureApproval) approval);
                });
            }

            if (messageType.equals(MessageType.REMINDER_RUN_CLOSEOUT_APPROVE) || messageType.equals(MessageType.NOTIFICATION_RUN_CLOSEOUT_WAITING))
            {
                approvals.forEach(approval ->
                {
                    approval.setLastReminderDate(new Date());
                });
                ApprovalsDAO.updateRunApprovals(em, new HashSet<>((Collection<? extends RunApproval>) approvals));
            }

        }
        catch (Exception e)
        {
            throw new WebApplicationException("Failed to send message: ", e);
        }
        finally
        {
            JPAUtils.closeEntityManager(em);
        }
    }

    public void sendMessage(MessageType messageType, Users recipient, Program program, Long programExportId)
    {
        if (recipient == null)
        {
            LOGGER.warn("Null recipient for message type {} — skipping email", messageType);
            return;
        }

        try
        {
            Template subjectTemplate = this.freemarkerConfig.getTemplate("SUBJECT_" + messageType.toString());
            String subject = this.processTemplateForProgramExport(subjectTemplate, recipient, program, programExportId);
            Template bodyTemplate = this.freemarkerConfig.getTemplate("BODY_" + messageType);
            String body = this.processTemplateForProgramExport(bodyTemplate, recipient, program, programExportId);

            this.sendEmail(recipient, subject, body, messageType);
        }
        catch (Exception e)
        {
            throw new WebApplicationException("Failed to send email message about program export. Server reason is: " + e.getMessage());
        }
    }

    private String processTemplate(Template template, Users recipient, Set<Approval> approvals)
            throws TemplateException, IOException
    {

        Approval firstApproval = approvals.iterator().next();

        // Build data model.
        Map<String, Object> templateDataModel = new HashMap<>();
        templateDataModel.put("approvals", approvals);
        templateDataModel.put("approval", firstApproval);
        templateDataModel.put("recipient", recipient);
        templateDataModel.put("approvalWords", this.approvalWordMap.get(firstApproval.getApprovalType()));

        StringWriter stringWriter = new StringWriter();

        template.process(templateDataModel, stringWriter);

        return stringWriter.toString();

    }

    private String processTemplateForProgramExport(Template template, Users recipient, Program program, Long programExportId)
            throws TemplateException, IOException
    {

        // Build data model.
        Map<String, Object> templateDataModel = new HashMap<>();
        templateDataModel.put("program", program);
        templateDataModel.put("exportUrl", Export.getExportDownloadUrl() + programExportId);
        templateDataModel.put("recipient", recipient);

        StringWriter stringWriter = new StringWriter();

        template.process(templateDataModel, stringWriter);

        return stringWriter.toString();
    }
}
