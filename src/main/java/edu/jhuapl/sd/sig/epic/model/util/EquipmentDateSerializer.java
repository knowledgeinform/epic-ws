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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Serializes date-only {@link Date} values as ISO calendar dates ("yyyy-MM-dd") in UTC.
 *
 * Equipment calibration dates are calendar dates, not instants. Serializing them as epoch
 * millis forces clients to interpret an instant, and the displayed day then depends on the
 * viewer's local timezone (a UTC-midnight instant renders as the previous day for viewers
 * west of UTC). Writing the calendar date explicitly preserves day semantics on the wire.
 *
 * Serialization-only: default {@code Date} deserialization (epoch millis / ISO-8601) is
 * unchanged, so existing clients that send epoch millis on update requests (e.g.
 * {@code POST /Equipment/UpdateItem}) continue to work.
 *
 * The formatter is fixed to UTC, so the output does not depend on the JVM default timezone.
 *
 * Background: session notes equipment-date-shift-diagnosis-2026-08-21.md.
 */
public class EquipmentDateSerializer extends JsonSerializer<Date>
{
    private static final DateTimeFormatter UTC_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    @Override
    public void serialize(Date value, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        if (value == null)
        {
            gen.writeNull();
            return;
        }
        gen.writeString(UTC_DATE.format(value.toInstant()));
    }
}
