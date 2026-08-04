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

import edu.jhuapl.sd.sig.epic.model.ProcedureInstruction;

import java.util.*;

public enum TestProcedureInstructionData
{

    PROCEDURE_INSTRUCTION_SET_1
    {
        @Override
        public SortedSet<ProcedureInstruction> getTestData()
        {
            SortedSet<ProcedureInstruction> instructions = new TreeSet<>();
            ProcedureInstruction instruction = new ProcedureInstruction();
            instruction.setDisplayOrder(1);
            instruction.setSectionName("Section 1");
            instruction.setText("This is text for section 1");
            instructions.add(instruction);

            instruction = new ProcedureInstruction();
            instruction.setDisplayOrder(2);
            instruction.setSectionName("Section 2");
            instruction.setText("This is text for section 2");
            instructions.add(instruction);

            instruction = new ProcedureInstruction();
            instruction.setDisplayOrder(3);
            instruction.setSectionName("Section 3");
            instruction.setText("This is text for section 3");
            instructions.add(instruction);

            return instructions;
        }
    };

    public abstract SortedSet<ProcedureInstruction> getTestData();
}
