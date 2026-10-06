/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class FileUtils
{

    /**
     * Best-effort recursive delete. The index is closed at this point, so no
     * handles should remain open. Failures are logged, not thrown: cleanup
     * must never fail an otherwise-passing test suite.
     */
    public static void deleteAllRecursively(Path root)
    {
        if (root == null)
        {
            return;
        }
        try (java.util.stream.Stream<Path> paths = Files.walk(root))
        {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path ->
                    {
                        try
                        {
                            Files.deleteIfExists(path);
                        }
                        catch (IOException e)
                        {
                            System.err.println("Could not delete search index file: " + path + " (" + e.getMessage() + ")");
                        }
                    });
        }
        catch (IOException e)
        {
            System.err.println("Could not delete search index root: " + root + " (" + e.getMessage() + ")");
        }
    }
}
