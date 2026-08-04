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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtils
{
    public static final String COMMUNICATION_BANNER_EXPIRY_DATE_PATTERN = "yyyy-MM-dd";
    private static final DateTimeFormatter COMMUNICATION_BANNER_EXPIRY_FORMATTER = DateTimeFormatter.ofPattern(COMMUNICATION_BANNER_EXPIRY_DATE_PATTERN);

    public static boolean isCommunicationBannerExpired(String expiry)
    {
        if (expiry != null && !expiry.trim().isEmpty())
        {
            return LocalDate.now().compareTo(LocalDate.parse(expiry, COMMUNICATION_BANNER_EXPIRY_FORMATTER)) >= 1;
        }

        return false;
    }
}
