/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.model.display.dto;

import edu.jhuapl.sd.sig.epic.model.Run;

/**
 * Result object returned after a run metadata edit attempt.
 */
public class RunEditResult
{
    private boolean success;
    private String message;
    private String previousName;
    private String previousDescription;
    private Run run;

    public RunEditResult()
    {}

    public RunEditResult(boolean success, String message, String previousName,
            String previousDescription, Run run)
    {
        this.success = success;
        this.message = message;
        this.previousName = previousName;
        this.previousDescription = previousDescription;
        this.run = run;
    }

    public boolean isSuccess()
    {
        return success;
    }

    public void setSuccess(boolean success)
    {
        this.success = success;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public String getPreviousName()
    {
        return previousName;
    }

    public void setPreviousName(String previousName)
    {
        this.previousName = previousName;
    }

    public String getPreviousDescription()
    {
        return previousDescription;
    }

    public void setPreviousDescription(String previousDescription)
    {
        this.previousDescription = previousDescription;
    }

    public Run getRun()
    {
        return run;
    }

    public void setRun(Run run)
    {
        this.run = run;
    }
}
