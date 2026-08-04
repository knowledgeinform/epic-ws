/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.resource.auth;

import io.jsonwebtoken.Jwts;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.security.Key;
import java.security.Principal;

@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter
{

    private static final String AUTHENTICATION_SCHEME = "Bearer";

    @Inject
    private KeyGenerator keyGenerator;

    @Override
    public void filter(ContainerRequestContext requestContext)
    {

        // Get the Authorization header from the request
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        // Validate the Authorization header
        if (!isTokenBasedAuthentication(authorizationHeader))
        {
            abortWithUnauthorized(requestContext);
            return;
        }

        // Extract the token from the Authorization header
        String token = authorizationHeader.substring(AUTHENTICATION_SCHEME.length()).trim();
        try
        {

            // Validate the token
            String username = validateToken(token);

            final SecurityContext currentSecurityContext = requestContext.getSecurityContext();
            requestContext.setSecurityContext(new SecurityContext()
            {

                @Override
                public Principal getUserPrincipal()
                {
                    final Principal principal = () -> username;
                    return principal;
                }

                @Override
                public boolean isUserInRole(String role)
                {
                    return true;
                }

                @Override
                public boolean isSecure()
                {
                    return currentSecurityContext.isSecure();
                }

                @Override
                public String getAuthenticationScheme()
                {
                    return AUTHENTICATION_SCHEME;
                }
            });

        }
        catch (Exception e)
        {
            abortWithUnauthorized(requestContext);
        }
    }

    private boolean isTokenBasedAuthentication(String authorizationHeader)
    {

        // Check if the Authorization header is valid
        // It must not be null and must be prefixed with "Bearer" plus a whitespace
        // The authentication scheme comparison must be case-insensitive
        return authorizationHeader != null && authorizationHeader.toLowerCase().startsWith(AUTHENTICATION_SCHEME.toLowerCase() + " ");
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext)
    {

        // Abort the filter chain with a 401 status code response
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
    }

    // Returns username of whom the token was issued. will throw exception if invalid token
    private String validateToken(String token)
    {
        // Check if the token was issued by the server and if it's not expired
        // Throw an Exception if the token is invalid
        Key key = keyGenerator.generateKey();
        return Jwts.parser().setSigningKey(key).build().parseSignedClaims(token).getPayload().getSubject();
    }
}
