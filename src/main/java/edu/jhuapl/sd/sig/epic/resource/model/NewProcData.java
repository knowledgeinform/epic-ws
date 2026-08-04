/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.resource.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class NewProcData
{
    private String name;
    private String description;
    private Integer program;
    private Integer subsystem;
    private Boolean esd0;
    private Boolean hazardous;
    private String hazardDescription;

    public NewProcData()
    {

    }

}
