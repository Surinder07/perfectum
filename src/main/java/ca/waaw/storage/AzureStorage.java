package ca.waaw.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.specialized.BlockBlobClient;

@Service
public class AzureStorage {
	private static final Logger lOGGER = LogManager.getLogger(AzureStorage.class);
	public static final String storageConnectionString = "DefaultEndpointsProtocol=http;"
			+ "AccountName=your_storage_account"
			+ "AccountKey=your_storage_account_key";
	
	//URL Smaple -> https://myaccount.blob.core.windows.net/myblob 
	public String uploadBlobIntoContainer (String containerName, String blobNameWithFileExtension, byte[] data) {
		BlobContainerClient blobContainerClient = new BlobContainerClientBuilder()
			    .endpoint("<your-storage-account-url>")
			    .sasToken("<your-sasToken>")
			    .containerName(containerName)
			    .buildClient();
		BlockBlobClient blockBlobClient =  blobContainerClient.getBlobClient(blobNameWithFileExtension).getBlockBlobClient();
	    blockBlobClient.upload(BinaryData.fromBytes(data));
	    lOGGER.info("File: " + blockBlobClient.getBlobName() + " has been uploaded");
		return "https://myaccount.blob.core.windows.net/" + blockBlobClient.getBlobName();
	}
	
	public byte[] retriveBlobIntoContainer (String containerName, String blobNameWithFileExtension) {
		BlobContainerClient blobContainerClient = new BlobContainerClientBuilder()
			    .endpoint("<your-storage-account-url>")
			    .sasToken("<your-sasToken>")
			    .containerName(containerName)
			    .buildClient();
		BlockBlobClient blockBlobClient =  blobContainerClient.getBlobClient(blobNameWithFileExtension).getBlockBlobClient();
		lOGGER.info("File: " + blockBlobClient.getBlobName() + " has been downloaded");
		return blockBlobClient.downloadContent().toBytes();
	}

}
