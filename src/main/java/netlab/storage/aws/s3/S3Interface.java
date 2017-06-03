package netlab.storage.aws.s3;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import lombok.extern.slf4j.Slf4j;
import netlab.storage.aws.config.AwsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.File;

@Slf4j
@Controller
public class S3Interface {

    private AmazonS3 s3;
    private TransferManager transferManager;
    private AwsConfig awsConfig;

    @Autowired
    public S3Interface(AwsConfig config){
        // Store the configuration
        awsConfig = config;
        log.info(awsConfig.toString());

        if(awsConfig.allFieldsDefined()){
            // Build up the credentials
            BasicAWSCredentials awsCreds = new BasicAWSCredentials(awsConfig.getAccessKeyId(), awsConfig.getSecretAccessKey());
            Regions regions = Regions.fromName(awsConfig.getRegion());
            AWSSecurityTokenServiceClient client = new AWSSecurityTokenServiceClient(awsCreds);
            STSAssumeRoleSessionCredentialsProvider provider = new STSAssumeRoleSessionCredentialsProvider
                    .Builder(awsConfig.getRoleArn(), awsConfig.getRoleSessionName())
                    .withStsClient(client)
                    .build();

            // Build the S3 client
            s3 = AmazonS3ClientBuilder.standard()
                    .withRegion(regions)
                    .withCredentials(provider)
                    .build();

            // and then the Transfer Manager
            transferManager = TransferManagerBuilder.standard().withS3Client(s3).build();
        }
        else{
            s3 = null;
            transferManager = null;
        }

    }

    public boolean allFieldsDefined(){
        return s3 != null && transferManager != null && awsConfig != null;
    }

    public boolean uploadToRaw(File f, String keyName){
        return uploadFile(f, awsConfig.getRawBucket(), keyName);
    }

    public boolean uploadToAnalyzed(File f, String keyName){
        return uploadFile(f, awsConfig.getAnalyzedBucket(), keyName);
    }

    public boolean uploadFile(File f, String bucketName, String keyName){
        try {
            Upload xfer = transferManager.upload(bucketName, keyName, f);
            xfer.waitForCompletion();
            return true;
        } catch (AmazonServiceException | InterruptedException | NullPointerException e) {
            log.error(e.getMessage());
            return false;
        }
    }

    public File downloadFromRaw(File f, String keyName){
        return downloadFile(f, awsConfig.getRawBucket(), keyName);
    }

    public File downloadFromAnalyzed(File f, String keyName){
        return downloadFile(f, awsConfig.getAnalyzedBucket(), keyName);
    }

    public File downloadFile(File f, String bucketName, String keyName){
        try {
            Download xfer = transferManager.download(bucketName, keyName, f);
            xfer.waitForCompletion();
            return f;
            // loop with Transfer.isDone()
            // or block with Transfer.waitForCompletion()
        } catch (AmazonServiceException | InterruptedException | NullPointerException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public void shutdown(){
        if(transferManager != null) transferManager.shutdownNow(true);
    }
}