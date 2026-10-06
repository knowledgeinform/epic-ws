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

/**
 * Represents the result of an editability validation check.
 */
public class ValidationResult
{
    private boolean allowed;
    private String denialReason;
    private boolean isUnauthorized;

    public ValidationResult()
    {}

    public ValidationResult(boolean allowed, String denialReason, boolean isUnauthorized)
    {
        this.allowed = allowed;
        this.denialReason = denialReason;
        this.isUnauthorized = isUnauthorized;
    }

    public boolean isAllowed()
    {
        return allowed;
    }

    public void setAllowed(boolean allowed)
    {
        this.allowed = allowed;
    }

    public String getDenialReason()
    {
        return denialReason;
    }

    public void setDenialReason(String denialReason)
    {
        this.denialReason = denialReason;
    }

    public boolean isUnauthorized()
    {
        return isUnauthorized;
    }

    public void setUnauthorized(boolean unauthorized)
    {
        isUnauthorized = unauthorized;
    }
}
