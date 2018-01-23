package netlab.storage.services;

import lombok.extern.slf4j.Slf4j;
import netlab.analysis.analyzed.Analysis;
import netlab.storage.aws.dynamo.DynamoInterface;
import netlab.storage.aws.s3.S3Interface;
import netlab.submission.request.Request;
import netlab.submission.request.SimulationParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StorageService {

    private S3Interface s3Interface;
    private DynamoInterface dynamoInterface;

    @Autowired
    public StorageService(S3Interface s3Interface, DynamoInterface dynamoInterface) {
        this.s3Interface = s3Interface;
        this.dynamoInterface = dynamoInterface;
    }

    public boolean storeRequestSet(Request request, boolean useAws) {
        File outputFile = createFile(request.getId(), "raw");
        if(useAws){
            writeLocal(request, outputFile);
            return s3Interface.uploadToRaw(outputFile, request.getId());
        }
        else {
            return writeLocal(request, outputFile);
        }
    }

    public Request retrieveRequestSet(String requestSetId, boolean useAws){
        Request rs = null;
        File f = new File(System.getProperty("user.dir") + "/results/raw/" + requestSetId);
        if(!f.exists() && useAws){
            f = s3Interface.downloadFromRaw(f, requestSetId);
        }
        if(f != null && f.exists()){
            rs = readRequestSetLocal(f);
        }
        return rs;
    }

    public boolean storeAnalyzedSet(Analysis analysis, boolean useAws){
        File outputFile = createFile(analysis.getRequestId(), "analyzed");
        if(useAws){
            writeLocal(analysis, outputFile);
            return s3Interface.uploadToAnalyzed(outputFile, analysis.getRequestId());
        }
        else {
            return writeLocal(analysis, outputFile);
        }
    }

    public Analysis retrieveAnalyzedSet(String requestSetId, boolean useAws){
        return retrieveAnalyzedSet(requestSetId, useAws, false);
    }

    public Analysis retrieveAnalyzedSet(String requestSetId, boolean useAws, boolean deleteAfter){
        Analysis as = null;
        File f = new File(System.getProperty("user.dir") + "/results/analyzed/" + requestSetId);
        if(!f.exists() && useAws){
            f = s3Interface.downloadFromAnalyzed(f, requestSetId);
        }
        if(f != null && f.exists()){
            as = readAnalyzedSetLocal(f);
            if(deleteAfter){
                f.delete();
            }
        }
        return as;
    }

    public boolean putSimulationParameters(SimulationParameters params){
        return dynamoInterface.put(params);
    }

    public List<SimulationParameters> getMatchingSimulationParameters(SimulationParameters params){
        return dynamoInterface.getSimulationParameters(params);
    }

    public List<SimulationParameters> queryForSeed(Long seed){
        return dynamoInterface.queryForSeed(seed);
    }

    public List<SimulationParameters> queryForId(String requestSetId){
        return dynamoInterface.queryForId(requestSetId);
    }


    public List<Analysis> getAnalyzedSets(SimulationParameters params){
        List<String> requestSetIds = dynamoInterface.getRequestSetIds(params);
        List<Analysis> sets = new ArrayList<>();
        for(String id : requestSetIds){
            Analysis set = retrieveAnalyzedSet(id, params.getUseAws());
            if(set != null){
                sets.add(set);
            }
        }
        return sets;
    }

    public Boolean deleteRequests(Long seed){
        List<SimulationParameters> matchingParams = dynamoInterface.queryForSeed(seed);
        List<String> requestSetIds = matchingParams.stream().map(SimulationParameters::getRequestId).collect(Collectors.toList());
        Boolean deleteRequests = s3Interface.deleteFromBucket(requestSetIds, "raw") && s3Interface.deleteFromBucket(requestSetIds, "analyzed");
        Boolean deleteRecords = false;
        if(deleteRequests){
            deleteRecords = dynamoInterface.deleteRecords(matchingParams);
        }
        return deleteRecords;
    }

    // Private subfunctions

    private Request readRequestSetLocal(File file){
        return (Request) readLocal(file);
    }

    private Analysis readAnalyzedSetLocal(File file){
        return (Analysis) readLocal(file);
    }

    private Object readLocal(File file){
        Object obj = null;
        try{
            FileInputStream fi = new FileInputStream(file);
            ObjectInputStream oi = new ObjectInputStream(fi);
            // Read object
            obj = oi.readObject();
            oi.close();
            fi.close();

        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public boolean writeLocal(Object object, File outputFile){
        try {
            FileOutputStream f = new FileOutputStream(outputFile);
            ObjectOutputStream o = new ObjectOutputStream(f);

            // Write object to file
            o.writeObject(object);

            o.close();
            f.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public File createFile(String id, String subDir){
        //String fileName = nameComponents.stream().reduce("", (s1, s2) -> s1 + "_" + s2);
        //fileName = fileName.substring(1);
        String outputPath = System.getProperty("user.dir") + "/results/" + subDir + "/";
        if(Files.notExists(Paths.get(outputPath))){
            try {
                Files.createDirectory(Paths.get(outputPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new File(outputPath + id);
    }

}
