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

import edu.jhuapl.sd.sig.epic.data.AppConfigurationDAO;
import edu.jhuapl.sd.sig.epic.model.Attachment;
import edu.jhuapl.sd.sig.epic.model.ConfigKey;
import edu.jhuapl.sd.sig.epic.startup.AppConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.WebApplicationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class is responsible for handling file operations related to attachments.
 * Attachments are saved to the configured upload directory, which is a NFS mount to a remote server.
 */
public final class AttachmentHandler
{
    private static final Logger LOGGER = LogManager.getLogger();

    public static final String UPLOAD_ROOT_DIR = AppConfigurationDAO.getConfigForKey(ConfigKey.UPLOAD_ROOT_DIR);

    /**
     * Saves an attachment from a source file to a destination file. The source can be either a String representing a file path or an InputStream.
     *
     * @param srcFile The source of the attachment, either a String representing a file path or an InputStream containing the attachment data
     * @param destFile The destination file path where the attachment will be saved
     * @throws IOException if there is an error reading from the source or writing to the destination
     */
    public static void saveAttachment(String srcFile, String destFile) throws IOException
    {
        LOGGER.info("Saving attachment {}", destFile);
        try
        {
            Path destFilePath = Paths.get(destFile);
            Files.copy(Paths.get((String) srcFile), destFilePath);
        }
        catch (IOException e)
        {
            String errMsg = "Error saving attachment: " + destFile;
            LOGGER.error(errMsg, e);
            throw new WebApplicationException("Error saving attachment " + destFile, e);
        }
    }

    public static long saveAttachment(InputStream fileInputStream, String destFile) throws IOException
    {
        if (destFile == null || destFile.isEmpty())
        {
            String errMsg = "Destination file path cannot be null or empty.";
            LOGGER.error(errMsg);
            throw new IOException(errMsg);
        }
        LOGGER.info("Saving attachment {}", destFile);

        if (fileInputStream == null)
        {
            String errMsg = "File input stream cannot be null.";
            LOGGER.error(errMsg);
            throw new IOException(errMsg);
        }

        // Stream directly to disk without loading into memory
        try (OutputStream os = Files.newOutputStream(Paths.get(destFile)))
        {
            return IOUtils.copyLarge(fileInputStream, os);
        }
    }

    /**
     * Downloads an attachment from a source file to a destination. The destination can be either a String representing a file path or an OutputStream.
     *
     * @param destFile The destination of the download, either a String representing a file path or an OutputStream to write the attachment data to
     * @param srcFile The source file path from which the attachment will be downloaded
     * @throws IOException if there is an error reading from the source or writing to the destination
     * @throws WebApplicationException if there is an error downloading attachment
     */
    public static void downloadAttachment(Object destFile, String srcFile) throws IOException, WebApplicationException
    {
        LOGGER.info("Downloading attachment {}", srcFile);

        if (!(destFile instanceof String || destFile instanceof OutputStream))
        {
            String errMsg = "Wrong input file type. Only String or InputStream is accepted: " + destFile;
            LOGGER.error(errMsg);
            throw new IOException(errMsg);
        }

        try
        {
            Path srcFilePath = Paths.get(srcFile);

            if (destFile instanceof OutputStream)
            {
                Files.copy(srcFilePath, (OutputStream) destFile);
            }
            else
            {
                Files.copy(srcFilePath, Paths.get((String) destFile));
            }

        }
        catch (IOException e)
        {
            String errMsg = "Error downloading attachment: " + srcFile;
            LOGGER.error(errMsg, e);
            throw new WebApplicationException("Error downloading attachment " + srcFile, e);
        }
    }

    /**
     * Deletes an attachment file from the filesystem.
     *
     * @param fileName The path of the file to be deleted
     * @throws WebApplicationException when there is something wrong with deleting attachment
     */
    public static void deleteAttachment(String fileName) throws WebApplicationException
    {
        LOGGER.info("Deleting attachment {}", fileName);
        try
        {
            Files.deleteIfExists(Paths.get(fileName));
        }
        catch (IOException e)
        {
            String errMsg = "Error deleting attachment: " + fileName;
            LOGGER.error(errMsg, e);
            throw new WebApplicationException("Error deleting attachment " + fileName, e);
        }

    }

    /**
     * Returns the full path to a file based on its Attachment object.
     *
     * @param attachment The Attachment object for which the full path is required
     * @return The full path to the file
     */
    public static String getFullPathToFile(Attachment attachment)
    {
        return UPLOAD_ROOT_DIR + File.separator + attachment.getFilename();
    }

    public static long getAttachmentMaxAllowedSize()
    {
        return Long.parseLong(
                AppConfiguration.getConfigValue(AppConfiguration.AppConfigKey.ATTACHMENT_MAX_ALLOWED_FILE_SIZE_BYTES));
    }

    /**
     * Checks if the given file size is within the allowed maximum size limit for attachments.
     *
     * @param fileSize the size of the file in bytes to be checked
     * @return {@code true} if the file size is less than or equal to the maximum allowed size,
     *     {@code false} otherwise
     */
    public static boolean isAllowedFileSize(long fileSize)
    {
        return (fileSize <= getAttachmentMaxAllowedSize());
    }

    /**
     * Converts the maximum allowed attachment size from bytes to a human-readable string format.
     * This method retrieves the maximum allowed file size in bytes and converts it to a more
     * readable format (e.g., "10 MB", "2.5 GB") using Apache Commons IO's byte counting utility.
     *
     * @return A string representation of the maximum allowed attachment size in human-readable format
     *     (e.g., "10 MB", "2.5 GB"). The returned string includes appropriate units (B, KB, MB, GB, etc.)
     */
    public static String getAttachmentMaxAllowedSizeWithUnits()
    {
        return FileUtils.byteCountToDisplaySize(getAttachmentMaxAllowedSize());
    }
}
