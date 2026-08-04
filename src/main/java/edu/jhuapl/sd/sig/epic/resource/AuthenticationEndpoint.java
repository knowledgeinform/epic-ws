/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.resource;

import edu.jhuapl.sd.sig.epic.data.UsersDAO;
import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.Users;
import edu.jhuapl.sd.sig.epic.resource.auth.AuthResponse;
import edu.jhuapl.sd.sig.epic.resource.auth.AuthUserInfo;
import edu.jhuapl.sd.sig.epic.resource.auth.KeyGenerator;
import edu.jhuapl.sd.sig.epic.resource.util.LdapUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.security.sasl.AuthenticationException;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Path("/authentication")
@Transactional
public class AuthenticationEndpoint
{

    private static final Logger LOGGER = LogManager.getLogger();

    @Context
    private UriInfo uriInfo;

    @Inject
    private KeyGenerator keyGenerator;

    @PostConstruct
    public void init()
    {
        LOGGER.info("Reading Ldap Properties File.");
        LdapUtils.readLdapConfigFile();
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response authenticateUser(AuthUserInfo userInfo)
    {

        try
        {
            // Authenticate the user using the credentials provided
            Users user = authenticate(userInfo.getUsername(), userInfo.getPassword());

            // Issue a token for the user
            String token = issueToken(userInfo.getUsername());

            AuthResponse ar = new AuthResponse(token, user.getUsername(), user.getDisplayName(), user.getPin(), user.getIsAdmin());

            // Return the token on the response
            return Response.ok(ar).build();

        }
        catch (Exception e)
        {
            LOGGER.warn("Unable to Authenticate User: " + userInfo.getUsername());
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    private Users authenticate(String username, String password) throws Exception
    {

        EntityManager em = null;
        Users user = null;

        boolean authenticated = LdapUtils.authenticateLDAP(username, password);

        if (authenticated)
        {

            try
            {
                em = JPAUtils.getEntityManager();
                user = UsersDAO.getUserByUsername(em, username);

                if (user == null)
                {
                    LOGGER.error("User not found: " + username);
                    throw new AuthenticationException("User not found: " + username);
                }
            }
            finally
            {
                JPAUtils.closeEntityManager(em);
            }
        }
        return user;

    }

    private String issueToken(String username)
    {
        // Issue a token (can be a random String persisted to a database or a JWT token)
        // The issued token must be associated to a user
        // Return the issued token

        Key key = keyGenerator.generateKey();
        String jwtToken = Jwts.builder()
                .setSubject(username)
                .setIssuer(uriInfo.getAbsolutePath().toString())
                .setIssuedAt(new Date())
                .setExpiration(toDate(LocalDateTime.now().plusDays(1L)))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
        return jwtToken;
    }

    private Date toDate(LocalDateTime localDateTime)
    {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
