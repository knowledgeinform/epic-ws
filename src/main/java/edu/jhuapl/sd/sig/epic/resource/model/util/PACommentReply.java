/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.resource.model.util;

public class PACommentReply
{
    private boolean isReply;
    private int parentPk;
    private String comment;

    public boolean isReply()
    {
        return isReply;
    }

    public void setIsReply(boolean reply)
    {
        isReply = reply;
    }

    public int getParentPk()
    {
        return parentPk;
    }

    public void setParentPk(int parentPk)
    {
        this.parentPk = parentPk;
    }

    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }
}
