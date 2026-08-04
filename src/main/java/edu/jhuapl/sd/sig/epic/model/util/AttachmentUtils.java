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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This class is responsible for handling file operations related to attachments.
 * Attachments are saved to the configured upload directory, which is a NFS mount to a remote server.
 * This NFS mount is a hard mount, which can cause blocking issues if the NFS server is not responsive.
 * Therefore, this class utilizes a thread pool to manage file operations and has handling to try to recover if the NFS mount
 * becomes unresponsive. (Obviously, if the NFS mount is totally unresponsive, then not much can be done, but try to handle gracefully
 * if we have intermittent connection issues).
 */
@Deprecated
public class AttachmentUtils
{
    /*
        Maximum number of threads will be active to process tasks.
        If more than this number of attachment requests are submitted then they are held in a queue until threads become available.
        A new thread is created to take its place if a thread terminates due to failure during execution shutdown on executor is not yet called.
        Any thread exists till the pool is shutdown.
    */
    private static final int ATTACHMENT_CONCURRENCY = 5;
    private static ExecutorService attachmentThreadPool = Executors.newFixedThreadPool(ATTACHMENT_CONCURRENCY);

    private static final Object lock = new Object();
    private static Queue<Pair<Object, String>> attachmentsQueue = new ConcurrentLinkedDeque<>();
    private static Queue<Pair<Object, String>> downloadAttachmentsQueue = new ConcurrentLinkedDeque<>();
    private static Queue<String> deleteAttachmentsQueue = new ConcurrentLinkedDeque<>();

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String UPLOAD_ROOT_DIR = AppConfigurationDAO.getConfigForKey(ConfigKey.UPLOAD_ROOT_DIR);

    private static final int NFS_MOUNT_FILE_OPERATION_TIMEOUT_SECONDS = 60 * 10; // setting to 10 minutes - unlikely to be used; only if NFS mount becomes unresponsive
    // timeout grace period to give some time for the file transfer to complete before foreably shutting down thread pool
    private static final int TIMEOUT_GRACE_PERIOD = 30;

    public static void saveAttachment(Object srcfile, String destfile) throws Exception
    {
        LOGGER.debug("Saving attachment source: {} destination {}", srcfile, destfile);

        if (!(srcfile instanceof String || srcfile instanceof InputStream))
        {
            String errMsg = "Wrong input file type. Only String or InputStream is accepted: " + srcfile;
            LOGGER.error(errMsg);
            throw new IOException(errMsg);
        }

        attachmentsQueue.add(new Pair(srcfile, destfile));
        // Launch a thread to copy file
        SaveAttachmentHandler saveHandler = new AttachmentUtils().new SaveAttachmentHandler();
        LOGGER.debug("Submitting save attachment handler to attachment thread pool");
        Future future = attachmentThreadPool.submit(new Thread(saveHandler));
        try
        {
            future.get(NFS_MOUNT_FILE_OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        catch (TimeoutException e)
        {
            LOGGER.error("TimeoutException occurred for copying attachment: {} ", destfile, e);
            HandleAttachmentTimeoutException(future);
            throw new WebApplicationException(e);
        }
        catch (ExecutionException ex)
        {
            LOGGER.error("ExecutionException occurred for copying attachment: {} ", destfile, ex);
        }
        finally
        {
            // throw any connection or file transfer exceptions
            if (saveHandler.getException() != null)
            {
                LOGGER.error("Save attachment: File attachment error with connection and or file transfer exception:", saveHandler.getException());
                throw saveHandler.getException();
            }
        }
    }

    public static void downloadAttachment(Object destfile, String srcfile) throws Exception
    {
        if (!(destfile instanceof String || destfile instanceof OutputStream))
        {
            String errMsg = "Wrong input file type. Only String or InputStream is accepted: " + destfile;
            LOGGER.error(errMsg);
            throw new IOException(errMsg);
        }

        downloadAttachmentsQueue.add(new Pair(destfile, srcfile));
        DownloadAttachmentHandler downloadAttachmentHandler = new AttachmentUtils().new DownloadAttachmentHandler();

        LOGGER.debug("Submitting download attachment handler to attachment thread pool");
        Future future = attachmentThreadPool.submit(new Thread(downloadAttachmentHandler));

        try
        {
            future.get(NFS_MOUNT_FILE_OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        catch (TimeoutException e)
        {
            LOGGER.error("TimeoutException occurred for downloading attachment: {} ", srcfile, e);
            HandleAttachmentTimeoutException(future);
            throw new WebApplicationException(e);
        }
        catch (ExecutionException e)
        {
            LOGGER.error("ExecutionException occurred for downloading attachment: {}", srcfile, e);
            throw new WebApplicationException(e);
        }
        finally
        {
            // throw any connection or file transfer exceptions
            if (downloadAttachmentHandler.getException() != null)
            {
                LOGGER.error("Download attachment: File attachment error with connection and or file transfer exception:", downloadAttachmentHandler.getException());
                throw downloadAttachmentHandler.getException();
            }
        }
    }

    public static void shutdownAndAwaitTermination()
    { // shutdown thread pool and wait a while for all the tasks to finish
        LOGGER.debug("Shutting down thread pool and awaiting all attachment task to finish.");
        attachmentThreadPool.shutdown(); // Disable new tasks from being submitted
        try
        {
            // Wait a while for existing tasks to terminate
            if (!attachmentThreadPool.awaitTermination(60, TimeUnit.SECONDS))
            {
                attachmentThreadPool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!attachmentThreadPool.awaitTermination(60, TimeUnit.SECONDS))
                    LOGGER.error("attachmentThreadPool did not terminate!");
            }
        }
        catch (InterruptedException ie)
        {
            LOGGER.error("Attachment thread pool interrupted exception - shutting down thread pool", ie);
            // (Re-)Cancel if current thread also interrupted
            attachmentThreadPool.shutdownNow();
            // Preserve interrupt status
            LOGGER.error("Preserving interrupt status");
            Thread.currentThread().interrupt();
        }
    }

    public static void deleteAttachment(String filename) throws Exception
    {
        LOGGER.debug("Deleting attachment {}", filename);
        deleteAttachmentsQueue.add(filename);
        // Launch a thread to copy file
        DeleteAttachmentHandler deleteAttachmentHandler = new AttachmentUtils().new DeleteAttachmentHandler();
        Future future = attachmentThreadPool.submit(new Thread(deleteAttachmentHandler));
        try
        {
            future.get(NFS_MOUNT_FILE_OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        catch (TimeoutException e)
        {
            LOGGER.error("TimeoutException occurred for deleting attachment: {} ", filename, e);
            HandleAttachmentTimeoutException(future);
            throw new WebApplicationException(e);
        }
        catch (ExecutionException e)
        {
            LOGGER.error("ExecutionException occurred for deleting attachment: " + filename, e);
            throw new WebApplicationException(e);
        }
        finally
        {
            if (deleteAttachmentHandler.getException() != null)
            {
                LOGGER.error("Failed to delete attachment {}", filename, deleteAttachmentHandler.getException());
                throw deleteAttachmentHandler.getException();
            }
        }
    }

    public static String getFullPathToFile(Attachment a)
    {
        return UPLOAD_ROOT_DIR + File.separator + a.getFilename();
    }

    /**
     * Save attachment handler
     */
    private class SaveAttachmentHandler implements Runnable
    {

        private Exception exception = null;

        public Exception getException()
        {
            return exception;
        }

        @Override
        public void run()
        {
            synchronized (lock)
            {
                LOGGER.debug("Save attachment local handler synchronized");
                if (!attachmentsQueue.isEmpty())
                {
                    Pair<Object, String> attachmentPair = attachmentsQueue.remove();
                    String destFile = attachmentPair.u;

                    try
                    {
                        Path destFilePath = Paths.get(destFile);

                        if (attachmentPair.t instanceof InputStream)
                        {
                            Files.copy((InputStream) attachmentPair.t, destFilePath);
                        }
                        else
                        {
                            // assuming it is a string path
                            Files.copy(Paths.get((String) attachmentPair.t), destFilePath);
                        }
                    }
                    catch (IOException e)
                    {
                        exception = e;
                    }
                }
            }
        }
    }

    /**
     * Download attachment handler
     */
    private class DownloadAttachmentHandler implements Runnable
    {
        private Exception exception = null;

        public Exception getException()
        {
            return exception;
        }

        @Override
        public void run()
        {
            LOGGER.debug("Download attachment local handler synchronized");
            if (!downloadAttachmentsQueue.isEmpty())
            {
                // transfer file from local to remote server
                while (!downloadAttachmentsQueue.isEmpty())
                {
                    Pair<Object, String> attachmentPair = downloadAttachmentsQueue.remove();
                    String srcFile = attachmentPair.u;
                    try
                    {
                        Path srcFilePath = Paths.get(srcFile);

                        if (attachmentPair.t instanceof OutputStream)
                        {
                            Files.copy(srcFilePath, (OutputStream) attachmentPair.t);
                        }
                        else
                        {
                            Files.copy(srcFilePath, Paths.get((String) attachmentPair.t));
                        }

                    }
                    catch (IOException e)
                    {
                        // fail to download this file, remove it from the queue and move on to the next one
                        exception = e;
                    }
                }
            }
        }
    }

    /**
     * Delete attachment handler
     */
    private class DeleteAttachmentHandler implements Runnable
    {
        private Exception exception = null;

        public Exception getException()
        {
            return exception;
        }

        @Override
        public void run() throws WebApplicationException
        {
            LOGGER.debug("Delete attachment local handler");
            synchronized (lock)
            {
                if (!deleteAttachmentsQueue.isEmpty())
                {
                    while (!deleteAttachmentsQueue.isEmpty())
                    {
                        String deleteFile = deleteAttachmentsQueue.remove();
                        try
                        {
                            Files.deleteIfExists(Paths.get(deleteFile));
                        }
                        catch (IOException e)
                        {
                            exception = e;
                            String errMsg = "Error deleting attachment: " + deleteFile;
                            LOGGER.error(errMsg, e);
                            throw new WebApplicationException("Error in deleting attachment " + deleteFile, e);
                        }
                    }
                }
            }
        }
    }

    private static void HandleAttachmentTimeoutException(Future future)
    {
        // try to cancel the stuck thread
        future.cancel(true);

        LOGGER.error("Attempting to recreate thread pool.");
        replaceExistingThreadPool();
    }

    /**
     * Method responsible for creating a new thread pool to immediately service new requests and
     * clear up the existing attachment thread pool to make sure no threads are blocked forever
     */
    private static synchronized void replaceExistingThreadPool()
    {
        // Create the new executor immediately
        ExecutorService newExecutor = Executors.newFixedThreadPool(ATTACHMENT_CONCURRENCY);

        // Log hash codes for verification
        LOGGER.debug("Old ExecutorService hashCode: {}", System.identityHashCode(attachmentThreadPool));
        LOGGER.debug("New ExecutorService hashCode: {}", System.identityHashCode(newExecutor));

        // Log thread counts
        logDebugThreadPool(attachmentThreadPool, "old-attachmentThreadPool");
        logDebugThreadPool(newExecutor, "new-attachmentThreadPool");

        // Swap the reference so all new requests use the new pool instantly
        ExecutorService oldAttachmentThreadPool = attachmentThreadPool;
        attachmentThreadPool = newExecutor;

        LOGGER.info("New attachments thread pool established and active for new requests.");
        shutdownThreadPool(oldAttachmentThreadPool);

        // Log thread counts again, after replacement
        logDebugThreadPool(attachmentThreadPool, "old-attachmentThreadPool");
        logDebugThreadPool(newExecutor, "new-attachmentThreadPool");
    }

    private static void logDebugThreadPool(ExecutorService attachmentThreadPool, String prefix)
    {
        if (attachmentThreadPool instanceof ThreadPoolExecutor)
        {
            ThreadPoolExecutor threadPool = (ThreadPoolExecutor) attachmentThreadPool;
            LOGGER.debug("{} - Pool Size: {}, Active Count: {}, Queue Size: {}",
                    prefix, threadPool.getPoolSize(), threadPool.getActiveCount(), threadPool.getQueue().size());
        }
        else
        {
            LOGGER.warn("{} is not a ThreadPoolExecutor, cannot retrieve detailed metrics.", prefix);
        }
    }

    private static void shutdownThreadPool(ExecutorService executor)
    {

        Runnable myTask = () ->
        {
            try
            {
                executor.shutdown();
                LOGGER.info("Attempting to shut down thread pool gracefully.");

                if (!executor.awaitTermination(TIMEOUT_GRACE_PERIOD, TimeUnit.SECONDS))
                {
                    LOGGER.info("Old thread pool is stuck trying to shutdown gracefully. Forcing shutdownNow().");
                    executor.shutdownNow();
                }
            }
            catch (InterruptedException ex)
            {
                LOGGER.error("Interrupted while waiting for thread pool to shut down.", ex);
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        };

        // kick off the shutting down of the executor thread pool
        Thread shutdownExecutorThread = new Thread(myTask);
        shutdownExecutorThread.start();
    }

    public static long getAttachmentMaxAllowedSize()
    {
        return Long.parseLong(
                AppConfiguration.getConfigValue(AppConfiguration.AppConfigKey.ATTACHMENT_MAX_ALLOWED_FILE_SIZE_BYTES));
    }

    public static String getAttachmentMaxAllowedSizeWithUnits()
    {
        return FileUtils.byteCountToDisplaySize(getAttachmentMaxAllowedSize());
    }

    public static boolean isAllowedFileSize(long fileSize)
    {
        return (fileSize <= getAttachmentMaxAllowedSize());
    }

}
