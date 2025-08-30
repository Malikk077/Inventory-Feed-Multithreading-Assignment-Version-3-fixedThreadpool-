package com.litmus7.inventoryfeedmultithreading.ui;

import java.net.URI;
import java.nio.file.Paths;

import com.litmus7.inventoryfeedmultithreading.controller.InventoryFeedController;
import com.litmus7.inventoryfeedmultithreading.dto.Response;


public class InventoryFeedApp {
	public static void main(String args[]) {
	
		
		InventoryFeedController inventoryController=new InventoryFeedController();
		
		URI folderUri = Paths.get("src/inventory-feed/input").toAbsolutePath().toUri();
		Response<Integer> csvToDbResponse=inventoryController.csvtoDB(folderUri);
		if (csvToDbResponse.getStatusCode()==200) {
			System.out.println(csvToDbResponse.getErrorMessage()+" Files Inserted "+ csvToDbResponse.getData());
		}else if (csvToDbResponse.getStatusCode() == 207) {
	        System.out.println("Message: " + csvToDbResponse.getErrorMessage());
	        System.out.println("Inserted Count: " + csvToDbResponse.getData());
	    } else {
	        System.out.println("Error Code : "+csvToDbResponse.getStatusCode());
	        System.out.println("Message: " + csvToDbResponse.getErrorMessage());
	    }
	
	


		
			
			
	
	
}
}