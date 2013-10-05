package edu.ncsu.lubick.localHub;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.ToolStream.ToolUsage;
import edu.ncsu.lubick.localHub.database.DBAbstraction;
import edu.ncsu.lubick.localHub.database.DBAbstractionFactory;

/**
 * An implementation of a database that prioritizes quick writes at the expenses of blocking on data pulls.
 * 
 * However, this implementation is NOT completely thread safe.  Do not try to send stuff to the database while a request
 * from the database is blocking. There is a good chance things will fail
 * @author Kevin Lubick
 *
 */
public class BufferedDatabaseManager 
{

	private DBAbstraction dbAbstraction = null;
	private ExecutorService threadPool;
	private static BufferedDatabaseManager singletonBufferedDatabaseManager = null;
	
	private static Logger logger = Logger.getLogger(BufferedDatabaseManager.class.getName());
	
	
	public BufferedDatabaseManager(String databaseLocation) {
		this.dbAbstraction = DBAbstractionFactory.createAndInitializeDatabase(databaseLocation, DBAbstractionFactory.SQL_IMPLEMENTATION);

		resetThreadPool();
	}


	//This is synchronized to appease FindBugs.  I doubt this will ever be called from a multi thread environment, but
	//this is a bit more bullet proof.  It's not time critical, so we should be alright.
	public static synchronized BufferedDatabaseManager createBufferedDatabasemanager(String databaseLocation) {
		if (singletonBufferedDatabaseManager != null)
		{
			return singletonBufferedDatabaseManager;
		}
		
		singletonBufferedDatabaseManager = new BufferedDatabaseManager(databaseLocation);
		
		
		return singletonBufferedDatabaseManager;
	}
	
	public void writeToolStreamToDatabase(final ToolStream ts) 
	{
		for(final ToolUsage tu : ts.getAsList())
		{
			logger.debug("Queueing up tool usage store");
			threadPool.execute(new Runnable() {
				
				@Override
				public void run() {

						dbAbstraction.storeToolUsage(tu,ts.getAssociatedPlugin());

				}
			});
			
		}
		
	}


	public List<ToolUsage> getAllToolUsageHistoriesForPlugin(String currentPluginName) {
		threadPool.shutdown();
		logger.debug("Waiting for the threadpool to finish tabulating");
		try 
		{
			threadPool.awaitTermination(30, TimeUnit.SECONDS);
		} 
		catch (InterruptedException e) {
			logger.error("was interrupted trying to wait for the threadpool to complete all transactions");
		}
		
		List<ToolUsage> retval = dbAbstraction.getAllToolUsageHistoriesForPlugin(currentPluginName);
		
		resetThreadPool();

		return retval;
	}


	private void resetThreadPool() {
		this.threadPool = Executors.newSingleThreadExecutor();
		
	}


	public void shutDown() {
		threadPool.shutdown();
		try {
			threadPool.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.error("was interrupted trying to wait for the threadpool to close");
		}
		
		dbAbstraction.close();
	}

}
