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

public class CreateCollection {

    private JsonArray jsonArray;
    private ProcessBuilder processBuilder;
    private String currentContainerName;

    public CreateCollection(){
        jsonArray = JsonReader.getInstance().getJsonArray();
        processBuilder = new ProcessBuilder();
    }

    public void run() {
        for(int i = 0; i < jsonArray.size(); i++){
            RMetaData rMetaData = JsonReader.getInstance().deserializeRepositoryFromJsonArray(i);

            //Repository was already processed. Skip to next repo.
            if(!rMetaData.getBuildStatus().equals("UNKNOWN")) {
                System.out.println("Repository at index " + i + " was already processed with build status: " + rMetaData.getBuildStatus() + "\nSkipping to the next repository.");
            } else {
                initDockerCommand(rMetaData, i); // could use multithreading at this point, however the json reader/writer must be thread safe first!
            }
        }
        System.out.println("---------------------------------------");
        ElasticsearchConnector.getInstance().closeClient(); // Free all resource of the ElasticsearchConnector
        System.out.println("Finished collection creation.\nProcessed all repositories of json file.");
    }

    public void runByIndex(int arrayIndex) {
        JsonReader.getInstance().checkArgInRange(arrayIndex);

        RMetaData rMetaData = JsonReader.getInstance().deserializeRepositoryFromJsonArray(arrayIndex);

        initDockerCommand(rMetaData, arrayIndex);

        System.out.println("---------------------------------------");
        ElasticsearchConnector.getInstance().closeClient();
        System.out.println("Finished collection creation for repository at index "+arrayIndex+".");
    }

    private void initDockerCommand(RMetaData rMetaData, int arrayIndex){

        // -- Linux --
        // Run a shell command
        //processBuilder.command("bash", "-c", "COMMAND");
        // -- Windows --
        // Run a command
        //processBuilder.command("cmd.exe", "/c", "COMMAND");

        // Using a switch statement for future easier extension with further build systems
        switch (rMetaData.getBuildSystem()){
            case "CMAKE":
                System.out.println("Starting cmake docker");
                currentContainerName = "cmake";
                processBuilder.command("bash", "-c", "docker run --name=cmake -v "+ Config.HOSTPATH + ":" + Config.CONTAINERPATH + " --entrypoint '/bin/bash' conan-cmake:clang -c 'cd /Program && java -jar ContainerProgram.jar " + arrayIndex+ "'");
                break;
        }
        dockerRun(rMetaData, arrayIndex);
    }

    private void dockerRun(RMetaData rMetaData, int arrayIndex) {
        int exitVal = ProcessHelper.executeProcess(processBuilder);

        if (exitVal == 0) {
            dockerSuccess(exitVal, arrayIndex);
            cleanSharedDir();
        } else {
            //abnormal behaviour...
            dockerFailed(exitVal);
            cleanSharedDir();
        }
    }

    private void dockerSuccess(int exitVal, int arrayIndex) {
        System.out.println("Docker exited with status: " + exitVal);
        ElasticsearchConnector.getInstance().indexRepository(arrayIndex, getContainerInfo());
    }

    private  void dockerFailed(int exitVal) {
        System.err.println("Internal docker error");
        System.err.println("Docker exited with status: " + exitVal);
        System.out.println("Continuing with next repository.");
    }

    private JsonArray getContainerInfo() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        StringBuilder containerInfo = new StringBuilder();

        processBuilder.command("bash", "-c", "docker inspect " + currentContainerName);
        ProcessHelper.executeProcessAndGetOutput(processBuilder, containerInfo);
        dockerRemove(currentContainerName);
        JsonElement containerObj = new JsonParser().parse(containerInfo.toString());
        return containerObj.getAsJsonArray();
    }

    private void dockerRemove(String containerName) {
        System.out.println("Removing container instance:");
        processBuilder.command("bash", "-c", "docker rm " + containerName);
        ProcessHelper.executeProcess(processBuilder);
    }

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
