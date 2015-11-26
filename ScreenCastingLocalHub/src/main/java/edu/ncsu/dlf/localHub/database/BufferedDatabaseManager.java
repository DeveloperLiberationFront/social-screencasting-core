package edu.ncsu.dlf.localHub.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import edu.ncsu.dlf.localHub.ClipOptions;
import edu.ncsu.dlf.localHub.LocalHub;
import edu.ncsu.dlf.localHub.ToolUsage;
import edu.ncsu.dlf.localHub.UserManager;
import edu.ncsu.dlf.localHub.forTesting.UnitTestUserManager;
import edu.ncsu.dlf.util.ToolCountStruct;

/**
 * An implementation of a database that prioritizes quick writes at the expenses of blocking on data pulls.
 * 
 * This implementation should be thread safe as the database is controlled by a single thread.
 * 
 * @author Kevin Lubick
 * 
 */
public final class BufferedDatabaseManager
{

	
	private LocalDBAbstraction localDB = null;
	
	private ExecutorService localThreadPool;

	private static BufferedDatabaseManager singletonBufferedDatabaseManager = null;

	private static Logger logger = Logger.getLogger(BufferedDatabaseManager.class.getName());

	private BufferedDatabaseManager(String databaseLocation, UserManager um)
	{
		this.localDB = DBAbstractionFactory.createAndInitializeLocalDatabase(databaseLocation, DBAbstractionFactory.SQL_IMPLEMENTATION, um);
		
		startThreadPools();
	}

	// This is synchronized to appease FindBugs. I doubt this will ever be
	// called from a multi thread environment, but
	// this is a bit more bullet proof. It's not time critical, so we should be
	// all right.
	public static synchronized BufferedDatabaseManager createBufferedDatabasemanager(String localDatabaseLocation, UserManager um, boolean isDebug)
	{
		if (singletonBufferedDatabaseManager != null)
		{
			return singletonBufferedDatabaseManager;
		}
		
		logger.trace("Is debug mode? "+isDebug);

		singletonBufferedDatabaseManager = new BufferedDatabaseManager(localDatabaseLocation, um);

		return singletonBufferedDatabaseManager;
	}

	public void reportMediaMadeForToolUsage(final String clipID, final ToolUsage toolUsage)
	{
		logger.debug("Reporting media exists for "+clipID);
		localThreadPool.execute(new Runnable() {
	
			@Override
			public void run()
			{
				
				localDB.createClipForToolUsage(clipID, toolUsage, new ClipOptions());
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

	public void writeToolStreamToDatabase(final List<ToolUsage> ts)
	{
		for (final ToolUsage tu : ts)
		{
			writeToolUsageToDatabase(tu);		
		}

	}

	public void writeToolUsageToDatabase(final ToolUsage tu)
	{
		logger.debug("Queueing up tool usage store to local");
		localThreadPool.execute(new Runnable() {

			@Override
			public void run()
			{
				localDB.storeToolUsage(tu);
			}
		});
	}

	private void startThreadPools()
	{
		if (localThreadPool == null)
			this.localThreadPool = Executors.newSingleThreadExecutor();
	}

	public void shutDown()
	{
		localThreadPool.shutdown();
		try
		{
			localThreadPool.awaitTermination(10, TimeUnit.SECONDS);
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

	public List<ToolUsage> getAllToolUsageHistoriesForPlugin(final String currentPluginName)
	{
		FutureTask<List<ToolUsage> > future = new FutureTask<List<ToolUsage>>(new Callable<List<ToolUsage>>() {

			@Override
			public List<ToolUsage> call() throws Exception
			{
				return localDB.getAllToolUsageHistoriesForPlugin(currentPluginName);
			}
		});
		
		this.localThreadPool.execute(future);
		
		try
		{
			return future.get();
		}
		catch (InterruptedException | ExecutionException e)
		{
			logger.error("Problem with query", e);
			return Collections.emptyList();
		}
		
	}


	public List<ToolUsage> getBestNInstancesOfToolUsage(final int n, final String pluginName, final String toolName, final boolean isKeyboardShortcut)
	{
		FutureTask<List<ToolUsage> > future = new FutureTask<List<ToolUsage>>(new Callable<List<ToolUsage>>() {

			@Override
			public List<ToolUsage> call() throws Exception
			{
				return localDB.getBestNInstancesOfToolUsage(n, pluginName, toolName, isKeyboardShortcut);
			}
		});
		
		this.localThreadPool.execute(future);
		
		try
		{
			return future.get();
		}
		catch (InterruptedException | ExecutionException e)
		{
			logger.error("Problem with query", e);
			return Collections.emptyList();
		}
	}


	public List<String> getNamesOfAllNonHiddenApplications()
	{
		FutureTask<List<String> > future = new FutureTask<List<String>>(new Callable<List<String>>() {

			@Override
			public List<String> call() throws Exception
			{
				return localDB.getNamesOfAllApplications();
			}
		});
			
		this.localThreadPool.execute(future);
		
		try
		{
			return filterOutHiddenPlugins(future.get());
		}
		catch (InterruptedException | ExecutionException e)
		{
			logger.error("Problem with query", e);
			return Collections.emptyList();
		}
	}

	private List<String> filterOutHiddenPlugins(List<String> plugins)
	{
		for (Iterator<String> iterator = plugins.iterator(); iterator.hasNext();)
		{
			String pluginName =  iterator.next();
			if (pluginName.startsWith(LocalHub.HIDDEN_PLUGIN_PREFIX))
			{
				iterator.remove();
			}
			
		}
		
		return plugins;
	}

	public List<ToolCountStruct> getAllToolAggregateForPlugin(String pluginName)
	{
		List<ToolUsage> toolUsages = getAllToolUsageHistoriesForPlugin(pluginName);
		Map<String, Integer> guiToolCountsMap = new HashMap<>();
		Map<String, Integer> keyboardToolCountsMap = new HashMap<>();
		// add the toolusages to the map
		for (ToolUsage tu : toolUsages)
		{
			
			if (ToolUsage.MENU_KEY_PRESS.equals(tu.getToolKeyPresses()))
			{
				Integer guiPreviousCount = guiToolCountsMap.get(tu.getToolName());
				if (guiPreviousCount == null)
				{
					guiPreviousCount = 0;
				}
				guiToolCountsMap.put(tu.getToolName(), guiPreviousCount + 1);
			}
			else 
			{
				Integer keyPreviousCount = keyboardToolCountsMap.get(tu.getToolName());
				if (keyPreviousCount == null)
				{
					keyPreviousCount = 0;
				}
				keyboardToolCountsMap.put(tu.getToolName(), keyPreviousCount + 1);
			}

		}
		// convert the map back to a list
		List<ToolCountStruct> retVal = new ArrayList<>();
		HashSet<String> bulkSet = new HashSet<>(guiToolCountsMap.keySet());
		bulkSet.addAll(keyboardToolCountsMap.keySet());
		
		for (String toolName : bulkSet)
		{
			retVal.add(new ToolCountStruct(toolName, guiToolCountsMap.get(toolName), keyboardToolCountsMap.get(toolName)));
		}
		// sort, using the internal comparator
		Collections.sort(retVal);
		
		return retVal;
	}

	public ToolCountStruct getToolAggregate(final String applicationName, final String toolName)
	{
		FutureTask<ToolCountStruct> future = new FutureTask<ToolCountStruct>(new Callable<ToolCountStruct>() {

			@Override
			public ToolCountStruct call() throws Exception
			{
				return localDB.getToolAggregate(applicationName, toolName);
			}
		});
		
		this.localThreadPool.execute(future);
		
		try
		{
			return future.get();
		}
		catch (InterruptedException | ExecutionException e)
		{
			logger.error("Problem with query", e);
			return new ToolCountStruct(toolName, 0, 0);
		}
	}

	public List<ToolUsage> getToolUsageInStaging(final String stagingTableName)
	{
		FutureTask<List<ToolUsage>> future = new FutureTask<List<ToolUsage>>(new Callable<List<ToolUsage>>() {

			@Override
			public List<ToolUsage> call() throws Exception
			{
				return localDB.getToolUsagesInStagingTable(stagingTableName);
			}
		});
		
		this.localThreadPool.execute(future);
		
		try
		{
			return future.get();
		}
		catch (InterruptedException | ExecutionException e)
		{
			logger.error("Problem with query", e);
			return Collections.emptyList();
		}
	}

	public void deleteToolUsageInStaging(final ToolUsage tu, final String stagingTableName)
	{
		// TODO Auto-generated method stub
		localThreadPool.execute(new Runnable() {

			@Override
			public void run()
			{
				localDB.deleteToolUsageInStaging(tu, stagingTableName);
			}
		});
	}

	public List<String> getExcesiveClipNames()
	{
		FutureTask<List<String> > future = new FutureTask<List<String>>(new Callable<List<String>>() {

			@Override
			public List<String> call() throws Exception
			{
				return localDB.getExcesiveTools();
			}
		});
			
		this.localThreadPool.execute(future);
		
		try
		{
			return future.get();
		}
		catch (InterruptedException | ExecutionException e)
		{
			logger.error("Problem with query", e);
			return Collections.emptyList();
		}
		
	}

	public boolean isClipUploaded(final String clipId)
	{
		FutureTask<Boolean> future = new FutureTask<Boolean>(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception
			{
				return localDB.isClipUploaded(clipId);
			}
		});
		
		this.localThreadPool.execute(future);
		
		try
		{
			return future.get();
		}
		catch (InterruptedException | ExecutionException e)
		{
			logger.error("Problem with query", e);
			return false;		//assume it hasn't
		}
	}

	public ToolUsage getToolUsageById(final String clipId)
	{
		FutureTask<ToolUsage> future = new FutureTask<ToolUsage>(new Callable<ToolUsage>() {

			@Override
			public ToolUsage call() throws Exception
			{
				return localDB.getToolUsageById(clipId);
			}
		});
		
		this.localThreadPool.execute(future);
		
		try
		{
			return future.get();
		}
		catch (InterruptedException | ExecutionException e)
		{
			logger.error("Problem with query", e);
			return null;		//assume it hasn't
		}
	}

	public void setClipUploaded(final String clipId, final boolean b)
	{
		localThreadPool.execute(new Runnable() {

			@Override
			public void run()
			{
				localDB.setClipUploaded(clipId, b);
			}
		});
	}

	public static BufferedDatabaseManager quickAndDirtyDatabase()
	{
		UserManager um = UnitTestUserManager.quickAndDirtyUser();
		
		return createBufferedDatabasemanager("./toolstreams.sqlite", um, true);
	}

}
