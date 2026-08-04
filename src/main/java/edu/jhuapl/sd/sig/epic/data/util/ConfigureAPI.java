/*
 * COPYRIGHT NOTICE
 * (C) 2010 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.data.util;

public class ConfigureAPI
{
    private static final String PERSISTENCE_UNIT = "epicdb";
    private static boolean INITIALIZED = false;

    @SuppressWarnings("unused")
    public static void init()
    {
        if (INITIALIZED)
        {
            throw new RuntimeException("API is already initialized");
        }
        else
        {
            // This will cause the static initialization block in JPAUtils to run
            // generating the entityManagerFactory and validating the database version
            INITIALIZED = true;
            JPAUtils unused = new JPAUtils();
        }
    }

    public static boolean isInitialized()
    {
        return INITIALIZED;
    }

    public static String getPersistenceUnit()
    {
        return PERSISTENCE_UNIT;
    }

}
