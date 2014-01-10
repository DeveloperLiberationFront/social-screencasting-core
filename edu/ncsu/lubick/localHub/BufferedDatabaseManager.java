package edu.ncsu.lubick.localHub;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.database.DBAbstractionException;
import edu.ncsu.lubick.localHub.database.DBAbstractionFactory;
import edu.ncsu.lubick.localHub.database.LocalDBAbstraction;
import edu.ncsu.lubick.localHub.database.LocalDBAbstraction.FileDateStructs;
import edu.ncsu.lubick.localHub.database.RemoteDBAbstraction;
import edu.ncsu.lubick.localHub.database.RemoteSQLDatabaseFactory;

/**
 * An implementation of a database that prioritizes quick writes at the expenses of blocking on data pulls.
 * 
 * However, this implementation is NOT completely thread safe. Do not try to send stuff to the database while a request from the database is blocking. There is
 * a good chance things will fail
 * 
 * @author Kevin Lubick
 * 
 */
public class BufferedDatabaseManager
{

	private LocalDBAbstraction localDB = null;
	private RemoteDBAbstraction remoteDB = null;
	
	private ExecutorService localThreadPool;
	private ExecutorService remoteThreadPool;
	private static BufferedDatabaseManager singletonBufferedDatabaseManager = null;

	private static Logger logger = Logger.getLogger(BufferedDatabaseManager.class.getName());

	private BufferedDatabaseManager(String databaseLocation)
	{
		this.localDB = DBAbstractionFactory.createAndInitializeDatabase(databaseLocation, DBAbstractionFactory.SQL_IMPLEMENTATION);
		this.remoteDB = RemoteSQLDatabaseFactory.createMySQLDatabaseUsingUserFile();
		
		resetThreadPools();
	}

	// This is synchronized to appease FindBugs. I doubt this will ever be
	// called from a multi thread environment, but
	// this is a bit more bullet proof. It's not time critical, so we should be
	// all right.
	public static synchronized BufferedDatabaseManager createBufferedDatabasemanager(String localDatabaseLocation)
	{
		if (singletonBufferedDatabaseManager != null)
		{
			return singletonBufferedDatabaseManager;
		}

		singletonBufferedDatabaseManager = new BufferedDatabaseManager(localDatabaseLocation);

		return singletonBufferedDatabaseManager;
	}

	public void writeToolStreamToDatabase(final ToolStream ts)
	{
		for (final ToolUsage tu : ts.getAsList())
		{
			logger.debug("Queueing up tool usage store");
			localThreadPool.execute(new Runnable() {

				@Override
				public void run()
				{
					localDB.storeToolUsage(tu, ts.getAssociatedPlugin());
				}
			});

			remoteThreadPool.execute(new Runnable() {

				@Override
				public void run()
				{
					remoteDB.storeToolUsage(tu, ts.getAssociatedPlugin());
				}
			});
		}

	}

	public void addVideoFile(final File newVideoFile, final Date videoStartTime, final int durationOfClip)
	{
		logger.debug("Adding new video file that starts on " + videoStartTime + "and goes " + durationOfClip + " seconds");
		localThreadPool.execute(new Runnable() {

			@Override
			public void run()
			{
				localDB.storeVideoFile(newVideoFile, videoStartTime, durationOfClip);
			}
		});
	}

	private void waitForLocalThreadPool()
	{
		localThreadPool.shutdown();
		logger.debug("Waiting for the threadpool to finish tabulating");
		try
		{
			localThreadPool.awaitTermination(30, TimeUnit.SECONDS);
		}
		catch (InterruptedException e)
		{
			logger.error("was interrupted trying to wait for the threadpool to complete all transactions");
		}
	}

	private void resetThreadPools()
	{
		this.localThreadPool = Executors.newSingleThreadExecutor();
		this.remoteThreadPool = Executors.newSingleThreadExecutor();
	}

	public void shutDown()
	{
		localThreadPool.shutdown();
		remoteThreadPool.shutdown();
		try
		{
			localThreadPool.awaitTermination(30, TimeUnit.SECONDS);
			remoteThreadPool.awaitTermination(30, TimeUnit.SECONDS);
		}
		catch (InterruptedException e)
		{
			logger.error("was interrupted trying to wait for the threadpools to close");
		}

		localDB.close();
		remoteDB.close();
		reset();
	}

	private static void reset()
	{
		singletonBufferedDatabaseManager = null; // it is shut down, recreate
													// next time
	}

	public List<ToolUsage> getAllToolUsageHistoriesForPlugin(String currentPluginName)
	{
		waitForLocalThreadPool();

		List<ToolUsage> retval = localDB.getAllToolUsageHistoriesForPlugin(currentPluginName);

		resetThreadPools();

		return retval;
	}

	private List<FileDateStructs> getVideoFilesLinkedToTimePeriod(Date timeStamp, int durationInSeconds)
	{
		if (durationInSeconds > 120)
		{
			logger.info("WARNING: Duration of Screencast longer than 2 minutes.  Are you sure that you converted milliseconds to seconds?");
		}
		waitForLocalThreadPool();
		List<FileDateStructs> retVal = null;
		try
		{
			logger.debug("Searching for a time frame starting at " + timeStamp + "and going " + durationInSeconds + " seconds");

			retVal = localDB.getVideoFilesLinkedToTimePeriod(timeStamp, durationInSeconds);
		}
		catch (DBAbstractionException e)
		{
			logger.error("There was a problem in the database query", e);
		}
		finally
		{
			resetThreadPools();
		}

		return retVal;
	}

	public List<FileDateStructs> getVideoFilesLinkedToTimePeriod(ToolUsage tu)
	{
		// convert milliseconds to seconds, as that is what the database has
		// durations for videos stored in
		int durationInSecondsRoundedUp = (int) Math.ceil(tu.getDuration() / 1000.0);
		return getVideoFilesLinkedToTimePeriod(tu.getTimeStamp(), durationInSecondsRoundedUp);
	}

	public List<ToolUsage> getLastNInstancesOfToolUsage(int n, String pluginName, String toolName)
	{
		waitForLocalThreadPool();
		List<ToolUsage> retVal = null;
		try
		{
			retVal = localDB.getLastNInstancesOfToolUsage(n, pluginName, toolName);
		}
		catch (DBAbstractionException e)
		{
			logger.error("There was a problem in the database query", e);
		}
		finally
		{
			resetThreadPools();
		}

		return retVal;
	}

	public List<String> getNamesOfAllPlugins()
	{
		waitForLocalThreadPool();
		List<String> retVal = Collections.emptyList(); // avoids the template
														// from crashing if the
														// query runs into
														// trouble
		try
		{
			retVal = localDB.getNamesOfAllPlugins();
		}
		catch (DBAbstractionException e)
		{
			logger.error("There was a problem in the database query", e);
		}
		finally
		{
			resetThreadPools();
		}
		return retVal;
	}

}
