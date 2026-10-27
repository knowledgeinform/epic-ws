/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.model.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.sd.sig.epic.model.EquipmentList;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wire-format tests for the equipment calibration dates.
 *
 * Regression background: the run response used to ship these dates as epoch millis, so the
 * day rendered in the UI depended on the viewer's local timezone. They now ship as UTC
 * calendar date strings, and update requests must still accept epoch millis.
 * See session notes equipment-date-shift-diagnosis-2026-08-21.md.
 */
public class TestEquipmentDateSerializer
{

    /** 2026-02-05T00:00:00Z, as produced by a GMT-pinned JVM reading DB date 2026-02-05. */
    private static final long FEB_5_2026_UTC_MIDNIGHT = 1770249600000L;

    /** 2027-02-28T00:00:00Z, as produced by a GMT-pinned JVM reading DB date 2027-02-28. */
    private static final long FEB_28_2027_UTC_MIDNIGHT = 1803772800000L;

    @Test
    public void serializesCalibrationDatesAsUtcCalendarDates() throws Exception
    {
        // Arrange
        EquipmentList equipment = new EquipmentList();
        equipment.setName("Amprobe");
        equipment.setCalibrationDate(new Date(FEB_5_2026_UTC_MIDNIGHT));
        equipment.setCalibrationDueDate(new Date(FEB_28_2027_UTC_MIDNIGHT));

        // Act
        String json = new ObjectMapper().writeValueAsString(equipment);

        // Assert
        assertTrue(json.contains("\"calibrationDate\":\"2026-02-05\""), json);
        assertTrue(json.contains("\"calibrationDueDate\":\"2027-02-28\""), json);
    }

    @Test
    public void serializesNullCalibrationDatesAsJsonNull() throws Exception
    {
        // Arrange
        EquipmentList equipment = new EquipmentList();
        equipment.setName("Amprobe");

        // Act
        String json = new ObjectMapper().writeValueAsString(equipment);

        // Assert
        assertTrue(json.contains("\"calibrationDate\":null"), json);
        assertTrue(json.contains("\"calibrationDueDate\":null"), json);
    }

    @Test
    public void stillDeserializesEpochMillisCalibrationDates() throws Exception
    {
        // Arrange: the payload shape the UI's asDTO() sends today on update requests
        String json = "{\"name\":\"Amprobe\"," +
                "\"calibrationDate\":" + FEB_5_2026_UTC_MIDNIGHT + "," +
                "\"calibrationDueDate\":" + FEB_28_2027_UTC_MIDNIGHT + "}";

        // Act
        EquipmentList equipment = new ObjectMapper().readValue(json, EquipmentList.class);

        // Assert
        assertEquals(FEB_5_2026_UTC_MIDNIGHT, equipment.getCalibrationDate().getTime());
        assertEquals(FEB_28_2027_UTC_MIDNIGHT, equipment.getCalibrationDueDate().getTime());
    }
}
