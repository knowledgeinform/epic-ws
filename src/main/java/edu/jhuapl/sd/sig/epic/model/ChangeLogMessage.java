/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.model;

import lombok.Getter;
import lombok.Setter;
import org.javers.core.metamodel.annotation.*;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@TypeName("ChangeLogMessage")
@Embeddable
public class ChangeLogMessage
{
    private String javersGloblId;
    private String lineEditTye;
    private Date timeStamp;
    @Transient
    private Users author;

    private String description;

    public ChangeLogMessage()
    {}

    public ChangeLogMessage(String globalId, Date time, Users commitAuthor, String changeMessage)
    {
        javersGloblId = globalId;
        timeStamp = time;
        author = commitAuthor;
        description = changeMessage;
    }

    @Override
    public String toString()
    {
        StringBuilder b = new StringBuilder();
        if (this.lineEditTye != null)
        {
            b.append(this.lineEditTye + ":\n");
        }
        b.append(description + " \n");
        return b.toString();
    }
}
