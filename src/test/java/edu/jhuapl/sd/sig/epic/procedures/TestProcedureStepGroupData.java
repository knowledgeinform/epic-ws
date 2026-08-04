/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.procedures;

import edu.jhuapl.sd.sig.epic.model.StepGroupDef;

import java.util.SortedSet;
import java.util.TreeSet;

public enum TestProcedureStepGroupData
{

    STEP_GROUP_SET_12
    {
        @Override
        public SortedSet<StepGroupDef> getTestData()
        {
            SortedSet<StepGroupDef> stepGroups = new TreeSet<>();
            StepGroupDef stepGroupDef = new StepGroupDef();
            stepGroupDef.setDisplayOrder(1);
            stepGroupDef.setStepGroupName("Test Procedure Step Group 1");
            stepGroupDef.setDescription("Step Group 1 description");
            stepGroups.add(stepGroupDef);

            stepGroupDef = new StepGroupDef();
            stepGroupDef.setDisplayOrder(2);
            stepGroupDef.setStepGroupName("Test Procedure Step Group 2");
            stepGroupDef.setDescription("Step group 2 desc.");
            stepGroups.add(stepGroupDef);

            return stepGroups;
        }
    },

    STEP_GROUP_SET_ABC
    {
        @Override
        public SortedSet<StepGroupDef> getTestData()
        {
            SortedSet<StepGroupDef> stepGroups = new TreeSet<>();
            StepGroupDef stepGroupDef = new StepGroupDef();
            stepGroupDef.setDisplayOrder(1);
            stepGroupDef.setStepGroupName("Test Procedure Step Group A");
            stepGroupDef.setDescription("Description Group A");
            stepGroups.add(stepGroupDef);

            stepGroupDef = new StepGroupDef();
            stepGroupDef.setDisplayOrder(2);
            stepGroupDef.setStepGroupName("Test Procedure Step Group B");
            stepGroupDef.setDescription("description group b");
            stepGroups.add(stepGroupDef);

            stepGroupDef = new StepGroupDef();
            stepGroupDef.setDisplayOrder(3);
            stepGroupDef.setStepGroupName("Test Procedure Step Group C");
            stepGroupDef.setDescription("desc group c");
            stepGroups.add(stepGroupDef);

            return stepGroups;
        }
    },

    STEP_GROUP_SET_XYZ
    {
        @Override
        public SortedSet<StepGroupDef> getTestData()
        {
            SortedSet<StepGroupDef> stepGroups = new TreeSet<>();
            StepGroupDef stepGroupDef = new StepGroupDef();
            stepGroupDef.setDisplayOrder(1);
            stepGroupDef.setStepGroupName("Test Procedure Step Group X");
            stepGroupDef.setDescription("group x");
            stepGroups.add(stepGroupDef);

            stepGroupDef = new StepGroupDef();
            stepGroupDef.setDisplayOrder(2);
            stepGroupDef.setStepGroupName("Test Procedure Step Group Y");
            stepGroupDef.setDescription("group y");
            stepGroups.add(stepGroupDef);

            stepGroupDef = new StepGroupDef();
            stepGroupDef.setDisplayOrder(3);
            stepGroupDef.setStepGroupName("Test Procedure Step Group Z");
            //no description
            stepGroups.add(stepGroupDef);

            return stepGroups;
        }
    };

    public abstract SortedSet<StepGroupDef> getTestData();
}
