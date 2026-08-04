/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.data;

import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javers.core.Javers;
import org.javers.core.diff.Diff;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.diff.changetype.container.ContainerChange;
import org.javers.core.diff.changetype.container.ElementValueChange;

import javax.ws.rs.WebApplicationException;

public class HistoryDAO
{
    private static final Logger LOGGER = LogManager.getLogger();

    public static String generateHistoryDescription(Object oldObject, Object newObject)
    {
        StringBuilder sb = new StringBuilder();
        try
        {
            if (oldObject == null)
            {
                return "Added " + newObject.toString();
            }
            else if (newObject == null)
            {
                return "Removed " + oldObject.toString();
            }

            // use javers to generate a diff
            Javers javers = JPAUtils.getJavers();
            Diff diff = javers.compare(oldObject, newObject);

            diff.getChanges().stream().forEach(change ->
            {
                if (change.getClass().equals(ValueChange.class))
                {
                    if (change.toString().contains("'runValue'") || change.toString().contains("'nonEditableValue'"))
                    {
                        sb.append(newObject.toString());
                    }
                }
                else if (change.getClass().equals(ContainerChange.class))
                {
                    ((ContainerChange) change).getChanges().stream().forEach(containerElementChange ->
                    {
                        if (containerElementChange.getClass().equals(ElementValueChange.class))
                        {
                            sb.append(newObject.toString());
                        }
                    });
                }
            });
        }
        catch (Exception e)
        {
            LOGGER.error("Error in generating history description", e);
            throw new WebApplicationException("Error in generating history description", e);
        }

        return sb.toString();
    }
}
