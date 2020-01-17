package main;

import Models.RMetaData;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import utils.ElasticsearchConnector;
import utils.JsonReader;
import utils.ProcessHelper;

import java.io.BufferedReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * The CollectionCreator class that is responsible for the collection creation and storage of all information collected
 * into the Elasticsearch database.
 * @author Daniel Braun
 */
public class CollectionCreator {

    private JsonArray jsonArray;
    private ProcessBuilder processBuilder;
    private String currentContainerName;
    private long startTime;
    private String systemStartTime;
    private int counterSuccess;
    private int counterFailed;
    private Logger logger;

    public CollectionCreator(Logger logger, long startTime, String systemStartTime){
        this.logger = logger;
        this.startTime = startTime;
        this.systemStartTime = systemStartTime;
        jsonArray = JsonReader.getInstance().getJsonArray();
        processBuilder = new ProcessBuilder();
    }

    /**
     * Main entry function that must be called to start the collection creation process.
     * It will instantiate docker containers sequentially for each repository within the repositories.json file.
     */
    public void run() {
        for(int i = 0; i < jsonArray.size(); i++){
            RMetaData rMetaData = JsonReader.getInstance().deserializeRepositoryFromJsonArray(i);

            //Repository was already processed. Skip to next repo.
            if(!rMetaData.getBuildStatus().equals("UNKNOWN")) {
                System.out.println("Repository at index " + i + " was already processed with build status: " + rMetaData.getBuildStatus() + "\nSkipping to the next repository.");
            } else {
                logger.info("-----------------------------------");
                logger.info("Running collection creation at index: "+ i + "for repository with id/owner/name: " + rMetaData.getId() + "/" + rMetaData.getOwner() + "/" + rMetaData.getName());
                long startTimeDockerExe   = System.nanoTime();
                initDockerCommand(rMetaData, i); // could use multithreading at this point, however the json reader/writer must be thread safe first!
                long endTimeDockerExe   = System.nanoTime();
                long durationDockerExe = endTimeDockerExe - startTimeDockerExe;
                logger.info("Overall execution time in seconds: " + TimeUnit.NANOSECONDS.toSeconds(durationDockerExe));
                logger.info("Overall execution time in minutes: " + (double)TimeUnit.NANOSECONDS.toSeconds(durationDockerExe)/60);
            }
        }
        ElasticsearchConnector.getInstance().closeClient(); // Free all resource of the ElasticsearchConnector


        logger.info("-----------------------------------");
        logger.info("Number of build successes: " + counterSuccess);
        logger.info("Number of build failures: " + counterFailed);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        long endTime   = System.nanoTime();
        long duration = endTime - startTime;

        logger.info("-----------------------------------");
        logger.info("CollectionCreator started at: " + systemStartTime);
        logger.info("CollectionCreator terminated at: " + formatter.format(calendar.getTime()));
        logger.info("Overall execution time in seconds: " + TimeUnit.NANOSECONDS.toSeconds(duration));
        logger.info("Overall execution time in minutes: " + (double)TimeUnit.NANOSECONDS.toSeconds(duration)/60);
        logger.info("Overall execution time in hours: " + (double)TimeUnit.NANOSECONDS.toSeconds(duration)/3600);
        logger.info("CollectionCreator finished. Processed all repositories of json file. Shutting down");

        System.out.println("---------------------------------------");
        System.out.println("Finished collection creation.\nProcessed all repositories of json file.");
    }

    /**
     * Main entry function used to run the collection creation process only for one specific repository at the given index.
     * @param arrayIndex The index to the repository within the repositories.json file.
     */
    public void runByIndex(int arrayIndex) {
        JsonReader.getInstance().checkArgInRange(arrayIndex);

        RMetaData rMetaData = JsonReader.getInstance().deserializeRepositoryFromJsonArray(arrayIndex);

        initDockerCommand(rMetaData, arrayIndex);

        System.out.println("---------------------------------------");
        ElasticsearchConnector.getInstance().closeClient();
        System.out.println("Finished collection creation for repository at index "+arrayIndex+".");
    }

    /**
     * Initializes the docker run command.
     * @param rMetaData The metadata read from the json file.
     * @param arrayIndex The index within the repositories.json file of the repository.
     */
    private void initDockerCommand(RMetaData rMetaData, int arrayIndex){

        // -- Linux --
        // Run a shell command
        //processBuilder.command("bash", "-c", "COMMAND");
        // -- Windows --
        // Run a command
        //processBuilder.command("cmd.exe", "/c", "COMMAND");

        // Using a switch statement for future easier extension with other build systems
        switch (rMetaData.getBuildSystem()){
            case "CMAKE":
                System.out.println("Starting cmake docker");
                currentContainerName = "cmake";
                processBuilder.command("bash", "-c", "docker run --name=cmake -v "+ Config.HOSTPATH + ":" + Config.CONTAINERPATH + " --entrypoint '/bin/bash' conan-cmake:clang -c 'cd /Program && java -jar ContainerCoordinator.jar " + arrayIndex+ "'");
                break;
        }
        dockerRun(rMetaData, arrayIndex);
    }

    /**
     * Starts a new docker container and inherits its stdout and stderr streams.
     * @param rMetaData The metadata read from the json file.
     * @param arrayIndex The index within the repositories.json file of the repository.
     */
    private void dockerRun(RMetaData rMetaData, int arrayIndex) {
        int exitVal = ProcessHelper.executeProcess(processBuilder);

        if (exitVal == 0) {
            dockerSuccess(exitVal, arrayIndex);
            cleanSharedDir();
        } else {
            //abnormal behaviour...
            dockerFailed(exitVal);
            logger.severe("Docker crashed with exit code: " + exitVal + " for repository at index: " + arrayIndex);
            cleanSharedDir();
        }


        RMetaData rMetaDataUpdated = JsonReader.getInstance().deserializeRepositoryFromJsonArray(arrayIndex);
        if(rMetaDataUpdated.getBuildStatus().equals("SUCCESS"))
            counterSuccess++;
        else if (rMetaDataUpdated.getBuildStatus().equals("FAILED"))
            counterFailed++;
    }

    /**
     * Function to call when the docker exited without internal errors.
     * @param exitVal The exit code of the docker container process.
     * @param arrayIndex The index within the repositories.json file of the repository.
     */
    private void dockerSuccess(int exitVal, int arrayIndex) {
        System.out.println("Docker exited with status: " + exitVal);
        ElasticsearchConnector.getInstance().indexRepository(arrayIndex, getContainerInfo());
    }

    /**
     * Function to call when the docker exited with internal errors.
     * @param exitVal The exit code of the docker container process.
     */
    private  void dockerFailed(int exitVal) {
        System.err.println("Internal docker error");
        System.err.println("Docker exited with status: " + exitVal);
        System.out.println("Continuing with next repository.");
    }

    /**
     * Collects the docker container low level information and returns it as an JsonArray.
     * @return The JsonArray returned by the docker inspect command.
     */
    private JsonArray getContainerInfo() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        StringBuilder containerInfo = new StringBuilder();

        processBuilder.command("bash", "-c", "docker inspect " + currentContainerName);
        ProcessHelper.executeProcessAndGetOutput(processBuilder, containerInfo);
        dockerRemove(currentContainerName);
        JsonElement containerObj = new JsonParser().parse(containerInfo.toString());
        return containerObj.getAsJsonArray();
    }

    /**
     * Function to remove the exited docker container by its name. Required to be able to restart another docker container
     * with the same name.
     * @param containerName The docker container's name to remove.
     */
    private void dockerRemove(String containerName) {
        System.out.println("Removing container instance:");
        processBuilder.command("bash", "-c", "docker rm " + containerName);
        ProcessHelper.executeProcess(processBuilder);
    }

    /**
     * Deletes the results.json file within the specified shared folder.
     */
    private void cleanSharedDir() {
        processBuilder.command("bash", "-c", "cd " + Config.HOSTPATH + " && rm -f -r ./results.json");
        int exitVal1 = ProcessHelper.executeProcess(processBuilder);
        if (exitVal1 == 0) {
            System.out.println("results.json file has been deleted!");
        } else {
            System.err.println("Failed to delete results.json file!");
        }
    }

}
