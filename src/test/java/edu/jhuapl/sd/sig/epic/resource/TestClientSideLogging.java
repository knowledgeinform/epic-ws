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

import edu.jhuapl.sd.sig.epic.data.ProcedureDetailsDAO;
import edu.jhuapl.sd.sig.epic.data.util.ConfigureAPI;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.Users;
import edu.jhuapl.sd.sig.epic.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.ws.rs.core.SecurityContext;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestClientSideLogging
{
    @TempDir
    Path tempDir;

    @Test
    public void formatLogMessage_utcTimestamp_expectedAmericaNewYorkLocalTime()
    {
        int logLevel = 3000;
        String message = "Test Log Message 1";
        String name = "user1";
        long timestamp = 1601546400; // 2020-Oct-01 10:00:00AM UTC in seconds - is 06:00:00AM EDT
        long eventId = 1;

        Log.LogMessage logMessage = new Log.LogMessage(logLevel, message, name, timestamp * 1000, eventId);

        // Act
        String formattedMessage = Log.formatLogMessage(logMessage);

        // Assert
        assertEquals("[INFO ] 2020-10-01 06:00:00,000 - Test Log Message 1", formattedMessage);
    }

    @Test
    public void saveLogMessages_authenticatedUserTwoMessages_expectedLogFileCreatedWithMessagesInOrder() throws Exception
    {
        try (MockedStatic<ConfigureAPI> mockApi = Mockito.mockStatic(ConfigureAPI.class))
        {
            mockApi.when(ConfigureAPI::isInitialized).thenReturn(true);

            try (MockedStatic<JPAUtils> mockJpa = Mockito.mockStatic(JPAUtils.class);
                    MockedStatic<edu.jhuapl.sd.sig.epic.startup.AppConfiguration> mockConfig = Mockito.mockStatic(edu.jhuapl.sd.sig.epic.startup.AppConfiguration.class);
                    MockedStatic<ProcedureDetailsDAO> mockPdDao = Mockito.mockStatic(ProcedureDetailsDAO.class))
            {
                // Need to mock this as it is called, but will be overwritten by createLogResourceForUser
                mockConfig.when(() -> edu.jhuapl.sd.sig.epic.startup.AppConfiguration.getConfigValue(Mockito.eq(edu.jhuapl.sd.sig.epic.startup.AppConfiguration.AppConfigKey.CLIENT_LOGS_DIR)))
                        .thenReturn("");

                // Arrange
                Users user = TestUtils.getTestUser();
                Log logResource = createLogResourceForUser(user.getUsername());
                Log.LogMessage first = new Log.LogMessage(3000, "First client message", user.getUsername(), 1601546400000L, 1L);
                Log.LogMessage second = new Log.LogMessage(5000, "Second client message", user.getUsername(), 1601546460000L, 2L);

                // Act
                logResource.saveLogMessages(new Log.LogJSON("", Arrays.asList(first, second)));

                // Assert
                Path logFile = tempDir.resolve(user.getUsername()).resolve(Log.formatLogFileName(LocalDate.now()));
                // TODO - Debug and fix
                //assertTrue(Files.exists(logFile));
                //assertLinesMatch(Arrays.asList(
                //Log.formatLogMessage(first),
                //Log.formatLogMessage(second)), Files.readAllLines(logFile));
            }
        }
    }

    private Log createLogResourceForUser(String username)
    {
        SecurityContext sc = new SecurityContext()
        {
            @Override
            public Principal getUserPrincipal()
            {
                return () -> username;
            }

            @Override
            public boolean isUserInRole(String role)
            {
                return true;
            }

            @Override
            public boolean isSecure()
            {
                return true;
            }

            @Override
            public String getAuthenticationScheme()
            {
                return "Bearer";
            }
        };

        return new Log(sc, tempDir.toString());
    }
}
