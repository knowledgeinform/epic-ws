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

import javax.ws.rs.core.Response;
import edu.jhuapl.sd.sig.epic.data.ProcedureDetailsDAO;
import edu.jhuapl.sd.sig.epic.data.util.ConfigureAPI;
import edu.jhuapl.sd.sig.epic.utils.DataGeneratorUtils;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.*;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class TestExport
{
    @Test
    public void testRunExport()
    {
        ProcedureDetails run = DataGeneratorUtils.getTestRun();

        try (MockedStatic<ConfigureAPI> mockApi = Mockito.mockStatic(ConfigureAPI.class))
        {
            mockApi.when(ConfigureAPI::isInitialized).thenReturn(true);

            try (MockedStatic<JPAUtils> mockJpa = Mockito.mockStatic(JPAUtils.class);
                    MockedStatic<edu.jhuapl.sd.sig.epic.startup.AppConfiguration> mockConfig = Mockito.mockStatic(edu.jhuapl.sd.sig.epic.startup.AppConfiguration.class);
                    MockedStatic<ProcedureDetailsDAO> mockPdDao = Mockito.mockStatic(ProcedureDetailsDAO.class))
            {
                // Mock JPAUtils so it does nothing
                mockJpa.when(JPAUtils::getEntityManager).thenReturn(null);
                //mockJpa.when(() -> JPAUtils.closeEntityManager(Mockito.any()));

                // Mock database operations
                mockConfig.when(() -> edu.jhuapl.sd.sig.epic.startup.AppConfiguration.getConfigValue(Mockito.eq(edu.jhuapl.sd.sig.epic.startup.AppConfiguration.AppConfigKey.TEMPLATES_ROOT_DIR)))
                        .thenReturn("test_template");
                mockPdDao.when(() -> ProcedureDetailsDAO.getProcedureDetailsByUniqueCode(Mockito.any(), Mockito.eq(run.getId())))
                        .thenReturn(run);

                Export exportResource = new Export();

                // Export the run.
                Response response = exportResource.exportProcedureResource(run.getId());
            }
        }
    }

}
