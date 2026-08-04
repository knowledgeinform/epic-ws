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

import edu.jhuapl.sd.sig.epic.data.UsersDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.Users;
import edu.jhuapl.sd.sig.epic.resource.auth.Secured;
import edu.jhuapl.sd.sig.epic.startup.AppConfiguration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Secured
@Path("/Log")
public class Log
{
    private static final String CLIENT_LOG_FILE_EXTENSION = ".log";

    // EPIC is only used at JHU APL, so we should be safe hard coding to this timezone. Not necessarily the best practice, alternative would be to pass that in the endpoint
    private static final ZoneId CLIENT_LOG_TIME_ZONE = ZoneId.of("America/New_York");
    @Context
    SecurityContext sc;

    private static final Logger LOGGER = LogManager.getLogger();
    private static final DateTimeFormatter LOG_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private String clientLogDirPath = AppConfiguration.getConfigValue(AppConfiguration.AppConfigKey.CLIENT_LOGS_DIR);

    public Log()
    {}

    public Log(SecurityContext sc, String clientPath)
    {
        this.sc = sc;
        this.clientLogDirPath = clientPath;
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public void saveLogMessages(LogJSON logJSON)
    {
        List<LogMessage> logMessages = logJSON.getLg();
        String userId = logMessages.get(0).getN();
        PrintWriter printWriter = null;
        EntityManager em = JPAUtils.getEntityManager();

        try
        {
            // check that the userid matches what the server thinks this id is
            Users user = UsersDAO.getUserByUsername(em, sc.getUserPrincipal().getName());

            if (!user.getUsername().equalsIgnoreCase(userId))
            {
                // if here, there is a mismatch. Log the mismatch to the server as an error and save the client log
                // messages to the server's name
                LOGGER.error("Mismatch between client logger user name and server user name. Client username: " + userId +
                        ", server username: " + user.getUsername());
                userId = user.getUsername();
            }

            // find the user's log directory. If doesn't exist, create one
            String directoryPath = clientLogDirPath + File.separator + userId;
            File userClientLogDir = new File(directoryPath);
            if (!userClientLogDir.exists())
            {
                userClientLogDir.mkdir();
            }

            // find the current log file for today; if doesn't exist, create one
            File todayLogFile = new File(directoryPath + File.separator + formatLogFileName(LocalDate.now(CLIENT_LOG_TIME_ZONE)));
            todayLogFile.createNewFile();
            printWriter = new PrintWriter(new BufferedWriter(new FileWriter(todayLogFile, true)));

            for (LogMessage message : logMessages)
            {
                // format each log message into human readable format
                String formattedMessage = formatLogMessage(message);
                // append each log message to user's log file for today
                printWriter.println(formattedMessage);
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Could not save log messages from client side for user " + userId, e);
        }
        finally
        {
            if (printWriter != null)
            {
                printWriter.flush();
                printWriter.close();
            }
        }
    }

    /**
     * Helper method for formatting log messages coming from the client into human-readable format.
     * 
     * @param logMessage message to be formatted into a log entry
     * @return String log entry
     */
    public static String formatLogMessage(LogMessage logMessage)
    {
        String eventTimestamp = LOG_TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(logMessage.getT()).atZone(CLIENT_LOG_TIME_ZONE));
        String logLevelString = LogLevel.getLogLevelForNumber(logMessage.getL());
        return String.format("[%1$-5s] %2$s - %3$s",
                logLevelString,
                eventTimestamp,
                logMessage.getM());
    }

    public static String formatLogFileName(LocalDate date)
    {
        return FILE_DATE_FORMATTER.format(date) + CLIENT_LOG_FILE_EXTENSION;
    }

    /**
     * this is the format of the raw data coming from the client: http://js.jsnlog.com/Documentation/HowTo/LogMessageFormat
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LogJSON
    {
        private String r = ""; // request id, deprecated, don't use
        private List<LogMessage> lg; // log messages
    }

    /**
     * This is the format of the raw log message
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LogMessage
    {
        private int l; // the log level (the severity)
        private String m; // the log message
        private String n; // the logger name (will be the userId)
        private long t; // timestamp in ms
        private long u; // event id
    }

    /**
     * Enum for the log levels
     */
    @Getter
    public enum LogLevel
    {
        TRACE(1000, "TRACE"),
        DEBUG(2000, "DEBUG"),
        INFO(3000, "INFO"),
        WARN(4000, "WARN"),
        ERROR(5000, "ERROR"),
        FATAL(6000, "FATAL");

        private final Integer LOG_NUMERIC_LEVEL;
        private final String LOG_LEVEL;
        private static final Map<Integer, String> MAP_NUMBER_TO_LOG_LEVEL = Arrays.stream(values())
                .collect(Collectors.toMap(logLevel -> logLevel.getLOG_NUMERIC_LEVEL(), logLevel -> logLevel.getLOG_LEVEL()));

        LogLevel(Integer logNumericLevel, String logLevel)
        {
            LOG_NUMERIC_LEVEL = logNumericLevel;
            LOG_LEVEL = logLevel;
        }

        public static String getLogLevelForNumber(Integer logNumber)
        {
            return MAP_NUMBER_TO_LOG_LEVEL.get(logNumber);
        }
    }
}
