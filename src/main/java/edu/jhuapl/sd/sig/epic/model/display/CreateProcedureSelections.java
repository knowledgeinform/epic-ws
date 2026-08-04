/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.model.display;

import edu.jhuapl.sd.sig.epic.model.Program;
import edu.jhuapl.sd.sig.epic.model.Subsystem;
import edu.jhuapl.sd.sig.epic.model.TestingPhase;

import java.util.List;

public class CreateProcedureSelections
{
    private List<Program> programs;
    private List<Subsystem> subsystems;
    private List<TestingPhase> testingPhases;

    public List<Program> getPrograms()
    {
        return programs;
    }

    public void setPrograms(List<Program> programs)
    {
        this.programs = programs;
    }

    public List<Subsystem> getSubsystems()
    {
        return subsystems;
    }

    public void setSubsystems(List<Subsystem> subsystems)
    {
        this.subsystems = subsystems;
    }

    public List<TestingPhase> getTestingPhases()
    {
        return testingPhases;
    }

    public void setTestingPhases(List<TestingPhase> testingPhases)
    {
        this.testingPhases = testingPhases;
    }
}
