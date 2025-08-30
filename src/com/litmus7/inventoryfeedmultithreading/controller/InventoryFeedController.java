package com.litmus7.inventoryfeedmultithreading.controller;


import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import com.litmus7.inventoryfeedmultithreading.constant.Constant;
import com.litmus7.inventoryfeedmultithreading.constant.LoggerConstants;
import com.litmus7.inventoryfeedmultithreading.dto.Response;
import com.litmus7.inventoryfeedmultithreading.exception.ProductServiceException;
import com.litmus7.inventoryfeedmultithreading.service.InventoryFeedService;
import com.litmus7.inventoryfeedmultithreading.util.ErrorMessageUtil;


public class InventoryFeedController {
	
	private static final Logger logger = LogManager.getLogger(InventoryFeedController.class);
	
	
	
	InventoryFeedService inventoryService =new InventoryFeedService();
	
	public Response<Integer> csvtoDB(URI folder)  {

	    ExecutorService executor = Executors.newFixedThreadPool(3);

	    logger.trace(LoggerConstants.ENTER_METHOD_LOG_MESSAGE, "csvtoDB()");
	    Path folderPath = Paths.get(folder);

	    List<Future<Boolean>> futures = new ArrayList<>();

	    int successCount = 0;
	    int failureCount = 0;

	    try (Stream<Path> paths = Files.list(folderPath)) {
	        logger.info("Scanning folder for files: {}", folderPath);

	        for (Path file : (Iterable<Path>) paths.filter(Files::isRegularFile)::iterator) {

	            Callable<Boolean> task = () -> {
	                String fileName = file.getFileName().toString().toLowerCase();
	                logger.debug("Found file: {}", fileName);

	                if (!fileName.endsWith(".csv")) {
	                    logger.warn("Skipping non-CSV file: {}", fileName);
	                    return false;
	                }

	                try {
	                    logger.info("Processing file: {}", fileName);
	                    return inventoryService.writeFromProductFileToDb(file.toString());
	                } catch (ProductServiceException e) {
	                    logger.error(ErrorMessageUtil.getErrorMessage(e.getErrorCode()), fileName, e.getMessage());
	                    return false;  
	                } catch (Exception e) {
	                    logger.error("Unexpected error while processing file {}: {}", fileName, e.getMessage());
	                    return false;  
	                }
	            };

	            futures.add(executor.submit(task));
	        }

	        for (Future<Boolean> f : futures) {
	            try {
	                if (f.get()) {
	                    successCount++;
	                } else {
	                    failureCount++;
	                }
	            } catch (Exception e) {
	                logger.error("Task execution failed: {}", e.getMessage());
	                failureCount++;
	            }
	        }
	        if (successCount > 0 && failureCount == 0) {
	            logger.info("All files processed successfully. Total = {}", successCount);
	            return new Response<>(Constant.SUCCESS, "All files processed successfully", successCount);
	        } else if (successCount > 0) {
	            logger.warn("Partial success. Success = {}, Failures = {}", successCount, failureCount);
	            return new Response<>(Constant.PARTIAL_SUCCESS,
	                    "Processed " + successCount + " files successfully, " + failureCount + " failed",
	                    successCount);
	        } else {
	            logger.error("No files processed successfully");
	            return new Response<>(Constant.FAILURE, "No files processed successfully", successCount);
	        }

	    } catch (Exception e) {
	        logger.error("Unexpected error while scanning folder: {}", e.getMessage());
	        return new Response<>(Constant.FAILURE, ErrorMessageUtil.getErrorMessage("APP-500"), successCount);
	    } finally {
	        executor.shutdown();
	        try {
				executor.awaitTermination(1, TimeUnit.HOURS);
			} catch (InterruptedException e) {
				logger.error(" error while executor.awaitTermination()",  e);
			}
	        logger.trace(LoggerConstants.EXIT_METHOD_LOG_MESSAGE, "csvtoDB()");
	    }
	}
}
	
