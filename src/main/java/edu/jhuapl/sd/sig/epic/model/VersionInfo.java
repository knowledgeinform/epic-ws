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

import javax.persistence.*;

@Entity
@Table(name = "version")
public class VersionInfo
{

    @Id
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private Type type;

    @Column(name = "major")
    private Integer major;

    @Column(name = "minor")
    private Integer minor;

    public Integer getMajor()
    {
        return major;
    }

    public void setMajor(Integer major)
    {
        this.major = major;
    }

    public Integer getMinor()
    {
        return minor;
    }

    public void setMinor(Integer minor)
    {
        this.minor = minor;
    }

    public enum Type
    {
        DATABASE,
        DATA
    }
}
