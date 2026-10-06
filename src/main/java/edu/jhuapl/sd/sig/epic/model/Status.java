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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class Status
{

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcedureStatusCounts
    {
        @JsonProperty("DRAFT")
        private int DRAFT;

        @JsonProperty("WAITING")
        private int WAITING;

        @JsonProperty("APPROVED")
        private int APPROVED;

        @JsonProperty("READY")
        private int READY;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RunStatusCounts
    {
        @JsonProperty("RUNNING")
        private int RUNNING;

        @JsonProperty("REVIEWING")
        private int REVIEWING;

        @JsonProperty("CORRECTING")
        private int CORRECTING;

        @JsonProperty("COMPLETED")
        private int COMPLETED;

        @JsonProperty("APPROVED")
        private int APPROVED;

        @JsonProperty("ABANDONED")
        private int ABANDONED;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgramStatusDTO
    {
        private Integer programPk;
        private ProcedureStatusCounts procedureStatusCounts;
        private RunStatusCounts runStatusCounts;
    }
}
