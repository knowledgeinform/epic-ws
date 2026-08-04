/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.resource.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.naming.NamingException;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

public class LdapUtils
{

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String LDAP_CONTEXT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
    private static final String LDAP_AUTHENTICATION_TYPE = "simple";
    private static final String LDAP_PREFIX_PRINCIPAL = "jhuapl\\";
    private static Properties properties = null;

    public static void readLdapConfigFile()
    {
        String ldapConfigPath = System.getenv("GSW_CONFIG") + File.separator + "ldap.properties";
        properties = new Properties();
        try
        {
            properties.load(new FileInputStream(ldapConfigPath));
        }
        catch (IOException e)
        {
            e.printStackTrace();
            LOGGER.error("Error reading ldap.properties", e);
        }
    }

    public static String getLdapURLLocation()
    {
        String location = null;

        if (!properties.isEmpty())
        {
            location = properties.getProperty("provider_url");
        }
        else
        {
            LOGGER.error("Error reading ldap.properties  provider_url is empty.");
        }

        return location;
    }

    public static boolean isLdapActivated()
    {
        boolean activateLdap = false;

        if (!properties.isEmpty())
        {
            activateLdap = Boolean.parseBoolean(properties.getProperty("activate_ldap"));
        }
        else
        {

            LOGGER.warn("Error reading ldap.properties activate_ldap is empty or not found setting default to false.");
        }

        return activateLdap;
    }

    public static boolean authenticateLDAP(String username, String password)
    {
        boolean authenticated = false;

        if (isLdapActivated())
        {

            LdapContext ctx = null;
            Control[] connectionControls = null;

            // TODO: Add LDAP Security and certificate
            Hashtable<String, String> env = new Hashtable<>(5);
            env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, LDAP_CONTEXT_FACTORY);
            env.put(javax.naming.Context.PROVIDER_URL, getLdapURLLocation());
            env.put(javax.naming.Context.SECURITY_AUTHENTICATION, LDAP_AUTHENTICATION_TYPE);
            env.put(javax.naming.Context.SECURITY_PRINCIPAL, LDAP_PREFIX_PRINCIPAL + username);
            env.put(javax.naming.Context.SECURITY_CREDENTIALS, password);

            try
            {
                ctx = new InitialLdapContext(env, connectionControls);
                authenticated = true;
                LOGGER.info("User = " + username + " is authenticated");
            }
            catch (NamingException e)
            {
                LOGGER.warn("User = " + username + " is NOT authenticated");
            }
        }
        else
        {
            // Returns true because authenticating wilt LDAP is disabled for testing purposes
            authenticated = true;
        }

        return authenticated;
    }

}
