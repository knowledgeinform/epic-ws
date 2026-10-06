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

/**
 * Lightweight DTO for run name/description edit requests.
 */
public class RunEditDTO
{
    private Integer runPk;
    private String name;
    private String description;

    public RunEditDTO()
    {}

    public Integer getRunPk()
    {
        return runPk;
    }

    public void setRunPk(Integer runPk)
    {
        this.runPk = runPk;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }
}
