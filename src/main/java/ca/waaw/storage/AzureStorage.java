package ca.waaw.storage;

import ca.waaw.config.applicationconfig.AppAzureConfig;
import ca.waaw.enumration.FileType;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.specialized.BlockBlobClient;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AzureStorage {

    private static final Logger log = LogManager.getLogger(AzureStorage.class);

    public final AppAzureConfig config;

    public String uploadFile(MultipartFile file, FileType fileType) throws IOException {
        try {
            String fileName = (UUID.randomUUID().toString().split("-")[0]) + "_" + Objects.requireNonNull(file.getOriginalFilename()).toLowerCase().replaceAll(" ", "_");
            return uploadFile(fileName, file.getBytes(), fileType);
        } catch (Exception e) {
            log.error("Exception while reading multipart file", e);
            throw e;
        }
    }

    public String uploadFile(String fileName, byte[] data, FileType fileType) {
        BlobContainerClient blobContainerClient = new BlobContainerClientBuilder()
                .endpoint(config.getBlob().getContainerUrl(fileType))
                .sasToken(config.getBlob().getContainerKey(fileType))
                .containerName(config.getBlob().getContainerName(fileType))
                .buildClient();
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(fileName).getBlockBlobClient();
        blockBlobClient.upload(BinaryData.fromBytes(data));
        log.info("File: " + blockBlobClient.getBlobName() + " has been uploaded");
        return blockBlobClient.getBlobName();
    }

    public byte[] retrieveFileData(String fileNameWithExtension, FileType type) {
        BlobContainerClient blobContainerClient = new BlobContainerClientBuilder()
                .endpoint(config.getBlob().getContainerUrl(type))
                .sasToken(config.getBlob().getContainerKey(type))
                .containerName(config.getBlob().getContainerName(type))
                .buildClient();
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(fileNameWithExtension).getBlockBlobClient();
        log.info("File: " + blockBlobClient.getBlobName() + " has been downloaded");
        return blockBlobClient.downloadContent().toBytes();
    }

}
