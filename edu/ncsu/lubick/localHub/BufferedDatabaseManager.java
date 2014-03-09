package edu.ncsu.lubick.localHub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.database.DBAbstractionException;
import edu.ncsu.lubick.localHub.database.DBAbstractionFactory;
import edu.ncsu.lubick.localHub.database.LocalDBAbstraction;
import edu.ncsu.lubick.util.ToolCountStruct;

/**
 * An implementation of a database that prioritizes quick writes at the expenses of blocking on data pulls.
 * 
 * However, this implementation is NOT completely thread safe. Do not try to send stuff to the database while a request from the database is blocking. There is
 * a good chance things will fail
 * 
 * TODO set this up to work with Futures.  I.E. make this thread safe.
 * 
 * @author Kevin Lubick
 * 
 */
public class BufferedDatabaseManager
{

	private LocalDBAbstraction localDB = null;
	
	private ExecutorService localThreadPool;

	private static BufferedDatabaseManager singletonBufferedDatabaseManager = null;

	private static Logger logger = Logger.getLogger(BufferedDatabaseManager.class.getName());

	private BufferedDatabaseManager(String databaseLocation)
	{
		this.localDB = DBAbstractionFactory.createAndInitializeDatabase(databaseLocation, DBAbstractionFactory.SQL_IMPLEMENTATION);
		
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

	public void reportMediaMadeForToolUsage(final String clipID, final ToolUsage toolUsage)
	{
		logger.debug("Reporting media exists for "+clipID);
		localThreadPool.execute(new Runnable() {
	
			@Override
			public void run()
			{
				
				localDB.createClipForToolUsage(clipID, toolUsage);
			}
		});
	}

	public void reportMediaDeletedForToolUsage(final String clipID)
	{
		logger.debug("Reporting media was deleted for "+clipID);
		localThreadPool.execute(new Runnable() {
	
			@Override
			public void run()
			{
				localDB.deleteClipForToolUsage(clipID);
			}
		});
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
		}

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
	}

	public void shutDown()
	{
		localThreadPool.shutdown();
		try
		{
			localThreadPool.awaitTermination(30, TimeUnit.SECONDS);
		}
		catch (InterruptedException e)
		{
			logger.error("was interrupted trying to wait for the threadpools to close");
		}

		localDB.close();
		reset();
	}

	private static void reset()
	{
		singletonBufferedDatabaseManager = null; // it is shut down, recreate
													// next time
	}

	public List<ToolUsage> getAllToolUsageHistoriesForPlugin(String currentPluginName)
	{
		List<ToolUsage> retval;
		waitForLocalThreadPool();
		synchronized (localDB)
		{
			retval = localDB.getAllToolUsageHistoriesForPlugin(currentPluginName);
		}

		

		resetThreadPools();

		return retval;
	}


	public List<ToolUsage> getBestNInstancesOfToolUsage(int n, String pluginName, String toolName)
	{
		waitForLocalThreadPool();
		List<ToolUsage> retVal = null;
		try
		{
			retVal = localDB.getBestNInstancesOfToolUsage(n, pluginName, toolName);
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
		List<String> retVal = Collections.emptyList(); // avoids any templates from crashing if the
														// query runs into trouble
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

	public List<ToolCountStruct> getAllToolAggregateForPlugin(String pluginName)
	{
		waitForLocalThreadPool();
		List<ToolUsage> toolUsages = getAllToolUsageHistoriesForPlugin(pluginName);
		Map<String, Integer> toolCountsMap = new HashMap<>();
		// add the toolusages to the map
		for (ToolUsage tu : toolUsages)
		{
			Integer previousCount = toolCountsMap.get(tu.getToolName());
			if (previousCount == null)
			{
				previousCount = 0;
			}
			toolCountsMap.put(tu.getToolName(), previousCount + 1);
		}
		// convert the map back to a list
		List<ToolCountStruct> retVal = new ArrayList<>();
		for (String toolName : toolCountsMap.keySet())
		{
			retVal.add(new ToolCountStruct(toolName, toolCountsMap.get(toolName)));
		}
		// sort, using the internal comparator
		Collections.sort(retVal);
		
		resetThreadPools();
		
		return retVal;
	}

	public List<Integer> getTopScoresForToolUsage(ToolUsage tu)
	{
		List<Integer> retVal = null;
		waitForLocalThreadPool();
		try
		{
			retVal = localDB.getTopScoresForToolUsage(LocalHub.MAX_TOOL_USAGES, tu.getPluginName(), tu.getToolName());
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

	public List<String> getExcesiveTools(int maxToolUsages)
	{
		waitForLocalThreadPool();
		
		List<String> retVal = localDB.getExcesiveTools();
		
		resetThreadPools();
		
		return retVal;
	}

}
