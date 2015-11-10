package edu.ncsu.lubick.launcher;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.logging.*;

public class Main {

    private static class DownloadException extends Exception {
        public DownloadException(String m) {
            super(m);
        }
        public DownloadException(String m, Throwable e) {
            super(m,e);
        }
    }

    private static final String jarURL = "http://10.138.18.45:8080/socaster.jar";

    // how long the user has to be idle before we will update
    private static final int idleMinutes = 10;

    // how often we should check for updates
    private static final int updateFrequencyMinutes = 30;

    private static final Logger logger = Logger.getLogger("socaster.launcher");

    private static Path getDownloadDirectory() {
        return java.nio.file.Paths.get(
                System.getProperty("user.home"),
                ".socaster"
        );
    }

    private static Path getDownloadJarLocation() {
        return getDownloadDirectory().resolve("socaster.jar");
    }

    private static Path getLogFilePath() {
        return  getDownloadDirectory().resolve("launcher.log");
    }

    private static boolean needsUpdate() {
        Path socasterJar = getDownloadJarLocation();
        if (!java.nio.file.Files.exists(socasterJar))
            return true;

        logger.info("Connecting to server to check last modified time");

        try {
            HttpURLConnection conn = (HttpURLConnection)(new URL(jarURL)).openConnection();
            conn.setRequestMethod("HEAD");
            conn.connect();
            long serverLastModified = conn.getLastModified();
            long localLastModified = java.nio.file.Files.getLastModifiedTime(socasterJar).toMillis();
            return serverLastModified > localLastModified;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Exception when checking if jar needs updating", e);
            return false;
        }
    }

    /**
     * Downloads the jar, or raises an exception.
     * @throws DownloadException
     */
    private static void download() throws DownloadException {
        Path socasterJar = getDownloadJarLocation();

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection)(new URL(jarURL)).openConnection();
        } catch (IOException e) {
            throw new DownloadException("Error opening connection: " + e.getMessage(), e);
        }
        if (java.nio.file.Files.exists(socasterJar)) {
            try {
                long lastModifiedJar = java.nio.file.Files.getLastModifiedTime(socasterJar).toMillis();
                conn.setIfModifiedSince(lastModifiedJar);
            } catch (IOException e) {
                // Do nothing. An error getting last modified time of the file on disk shouldn't stop us from attempting
                // to download a new version.
            }
        }

        logger.info("Downloading new jar from server");

        try {
            conn.connect();
        } catch (IOException e) {
            throw new DownloadException("Error connecting to server: " + e.getMessage(), e);
        }

        int responseCode;
        try {
            responseCode = conn.getResponseCode();
        } catch (IOException e) {
            throw new DownloadException("Error getting a response from the server: " + e.getMessage(), e);
        }

        if (responseCode == 304) {
            logger.log(Level.INFO, "Server returned 304 not modified. {0} is not newer than local copy. Not downloading", jarURL);
        } else if (responseCode != 200) {
            throw new DownloadException("Server returned code " + responseCode);
        } else {
            logger.log(Level.INFO, "Downloading from {0}", jarURL);
            try {
                InputStream input = conn.getInputStream();
                java.nio.file.Files.copy(input, socasterJar, StandardCopyOption.REPLACE_EXISTING);
                input.close();
            } catch (IOException e) {
                throw new DownloadException("Error downloading or saving file: " + e.getMessage(), e);
            }
        }
    }

    private static boolean hasExited(Process p) {
        try {
            p.exitValue();
            return true;
        } catch (IllegalThreadStateException e) {
            return false;
        }
    }

    private static Process launch() throws IOException {
        return new ProcessBuilder("java", "-jar", getDownloadJarLocation().toString()).inheritIO().start();
    }

    public static void main(String[] args) {
        try {
            Handler h = new FileHandler(
                            getLogFilePath().toString(),
                            10000000, // max 10 megs per file
                            3, // max 3 files
                            true // append
                            );
            h.setFormatter(new SimpleFormatter());
            Logger.getLogger("").addHandler(h);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error opening log file", e);
            JOptionPane.showMessageDialog(null, "Error opening "+getLogFilePath()+" for logging:\n" + e.getMessage());
            return;
        }
        logger.info("Socaster bootstrap launcher started");

        Path downloadDir = getDownloadDirectory();

        logger.log(Level.INFO, "Checking for data directory existence: {0}", downloadDir);
        if (!java.nio.file.Files.isDirectory(downloadDir)) {
            logger.log(Level.INFO, "Directory doesn't exist. Creating it.");
            try {
                java.nio.file.Files.createDirectory(downloadDir);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error creating directory", e);
                JOptionPane.showMessageDialog(null, "Error creating directory "+downloadDir+":\n" + e.getMessage());
                return;
            }
        }

        Path socasterJar = getDownloadJarLocation();

        if (needsUpdate()) {
            try {
                download();
            } catch (DownloadException e) {
                logger.log(Level.SEVERE, "Error downloading the client", e);
                JOptionPane.showMessageDialog(null, "Error downloading the client:\n" + e.getMessage());
                return;
            }
        } else {
            logger.info("Doesn't need update. Not updating");
        }

        logger.log(Level.INFO, "Launching jar at {0}", socasterJar);

        Process p = null;
        try {
            p = launch();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not execute the client", e);
            JOptionPane.showMessageDialog(null, "Error launching the client:\n"+e.getMessage());
            return;
        }

        Point lastMousePos = MouseInfo.getPointerInfo().getLocation();
        Date lastActivity = new Date();
        Date lastUpdateCheck = new Date();

        // And now we enter the main loop
        while (true) {
            // if the process exited unexpectedly, quit
            if (hasExited(p)) {
                logger.log(Level.INFO, "Process has exited. Quitting.");
                break;
            }

            Date now = new Date();

            // Check how long the user has been idle
            Point newPoint = MouseInfo.getPointerInfo().getLocation();
            if (lastMousePos.x != newPoint.x || lastMousePos.y != newPoint.y) {
                lastActivity = now;
                lastMousePos = newPoint;
            }

            // Is it time to check for updates?
            if (now.getTime() - lastUpdateCheck.getTime() >= 1000*60*updateFrequencyMinutes) {
                // has the user been idle for a while?
                if (now.getTime() - lastActivity.getTime() >= 1000*60*idleMinutes) {
                    // yes? check for an update
                    logger.info("User is idle and update timer expired. Checking for updates...");

                    if (needsUpdate()) {
                        logger.info("A new version is available. Terminating current client and downloading new update");
                        p.destroy();
                        try {
                            p.waitFor();
                        } catch (InterruptedException e) {
                            logger.fine("Interrupted waiting for process to exit");
                        }

                        try {
                            download();
                        } catch (DownloadException e) {
                            logger.log(Level.WARNING, "Could not download new version", e);
                            // this is just a warning. We should still try to launch the old version
                        }

                        try {
                            p = launch();
                        } catch (IOException e) {
                            // If launching the new version fails, we want to alert the user. Reason: while for a lot of
                            // failures we can remain running in the background, if there is an error here there's
                            // nothing we can do but exit. We don't want to silently exit unexpectedly, so we show an
                            // alert to the user.
                            logger.log(Level.SEVERE, "Could not launch new version", e);
                            JOptionPane.showMessageDialog(null, "An error occurred updating the Socaster client. "+
                                    "The screencaster will now exit.\n"+e.getMessage());
                            break;
                        }

                    } else {
                        logger.info("No updates found");
                    }

                    lastUpdateCheck = now;
                } else {
                    logger.log(Level.FINE, "Time for update but user is not idle. User must be idle for {0} more seconds",
                            60*idleMinutes-(now.getTime() - lastActivity.getTime())/1000);
                }
            } else {
                logger.log(Level.FINE, "Not time yet to check for updates. {0} more seconds",
                        60*updateFrequencyMinutes-(now.getTime() - lastUpdateCheck.getTime())/1000);
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                logger.fine("Thread was interrupted");
            }
        }

        logger.info("Launcher is exiting");

    }
}
