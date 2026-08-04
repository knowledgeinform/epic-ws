/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.model.util;

import edu.jhuapl.sd.sig.epic.data.*;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.ChangeLogMessage;
import edu.jhuapl.sd.sig.epic.model.ChangeLogsHolder;
import org.javers.common.string.PrettyValuePrinter;
import org.javers.core.*;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.*;
import org.javers.repository.jql.QueryBuilder;

import javax.persistence.*;
import java.util.Date;
import java.time.ZoneId;

public class JaversUtils
{
    public static String convertEditTypeToString(String type)
    {

        if (type == null)
            return type;
        String convertedType = type.replace("_", "-");
        convertedType = Character.toUpperCase(convertedType.charAt(0)) + convertedType.substring(1).toLowerCase();
        return convertedType;
    }

    public static void updateChangeLogs(ChangeLogsHolder obj)
    {
        // First clear out old logs
        obj.clearChangeLogs();
        // Now go through javers changes and add all in
        EntityManager em = JPAUtils.getEntityManager();
        Javers javers = JPAUtils.getJavers();
        // Get changes
        Changes changes = javers.findChanges(QueryBuilder.byClass(obj.getClass()).build());

        String objClassName = obj.getClass().getName().substring(obj.getClass().getName().lastIndexOf(".") + 1);
        String objId = objClassName + "/" + obj.getId();

        for (ChangesByCommit cc : changes.groupByCommit())
        {
            for (ChangesByObject co : cc.groupByObject())
            {
                if (co.getGlobalId().value().equals(objId))
                {
                    ChangeLogMessage changeLog = new ChangeLogMessage();
                    String lineEditType = null;
                    // Call UsersDAO.getUserByUsername to get users, this has to match with javers call to commit a change
                    // it needs to use users.getUsername for "author" commit
                    changeLog.setAuthor(UsersDAO.getUserByUsername(em, cc.getCommit().getAuthor()));
                    changeLog.setJaversGloblId(co.getGlobalId().toString());

                    Date date = Date.from(cc.getCommit().getCommitDate().atZone(ZoneId.systemDefault()).toInstant());
                    changeLog.setTimeStamp(date);
                    StringBuilder b = new StringBuilder();

                    for (Change eachChange : co.get())
                    {

                        String str = eachChange.prettyPrint(PrettyValuePrinter.getDefault());

                        if (eachChange instanceof NewObject)
                        { // special log message for new object
                            str = "A new " + eachChange.getAffectedGlobalId().toString().split("/")[0] + ", is created";
                        }
                        else if (eachChange instanceof ValueChange)
                        {
                            ValueChange valueChange = (ValueChange) eachChange;
                            if (valueChange.getPropertyName().contains("editType"))
                            {
                                if (lineEditType == null) // lineEditType has not been set yet
                                    lineEditType = valueChange.getRight() != null ? valueChange.getRight().toString() : null;
                                else
                                    System.out.println("Something went wrong: " + lineEditType + " was set already for this commit: " + cc.getCommit().getId() +
                                            " for this object: " + co);
                                continue; // skip javers change for line edit type
                            }
                        }
                        b.append(str + " \n");
                    }
                    changeLog.setLineEditTye(convertEditTypeToString(lineEditType));
                    changeLog.setDescription(b.toString());
                    obj.addChangeLog(changeLog);
                }
            }
        }
    }
}
