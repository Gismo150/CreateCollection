package utils;

import Models.RMetaData;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.http.HttpHost;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import java.io.IOException;

public class ElasticsearchConnector {


    private RestHighLevelClient client;
    // static variable single_instance of type Singleton
    private static ElasticsearchConnector single_instance = null;

    private ElasticsearchConnector () {
        client = getClient();
    }

    // static method to create instance of Singleton class
    public static ElasticsearchConnector getInstance()
    {
        if (single_instance == null)
            single_instance = new ElasticsearchConnector();

        return single_instance;
    }

    private RestHighLevelClient getClient () {
        return new RestHighLevelClient(clientBuilder());
    }

    private RestClientBuilder clientBuilder () {
        return RestClient.builder(new HttpHost("localhost", 9200, "http"));
    }


    public void indexRepository (int arrayIndex, JsonArray containerInfo) {
        //Get the deserialized updated metadata
        RMetaData rMetaData = JsonReader.getInstance().deserializeRepositoryFromJsonArray(arrayIndex);

        //Get the updated metadata as Json object
        JsonObject jsonObject = JsonReader.getInstance().getRepositoryJsonObjectFromJsonArray(arrayIndex);

        //Append the Results.json
        if(rMetaData.getBuildStatus().equals("SUCCESS") && FileHelper.checkFileExists(FileHelper.getResultsJsonFilePath())) {
            JsonArray resultsJsonArray = JsonReader.getInstance().readJsonArrayFromFile(FileHelper.getResultsJsonFilePath());
            jsonObject.add("results", resultsJsonArray); //If Results.json file exists
        } else {
            jsonObject.add("results", new JsonArray());//otherwise
        }

        //Append the container information coming from the $docker inspect command.
        jsonObject.add("containerInfo", containerInfo);

        System.out.println("Saving repository info into elastic.");
        IndexRequest indexRequest = new IndexRequest("repository")
                                    .source(jsonObject.toString(), XContentType.JSON)
                                    .id(Long.toString(rMetaData.getId()));
        try {
            indexResponseHandler(client.index(indexRequest, RequestOptions.DEFAULT));
        } catch (IOException e) {
            System.err.println("IOException when trying to index the json into elastic");
            System.err.println("Please check if the Elasticsearch service is active and running on localhost:9200.");
            System.out.println(e.getMessage());
        }
    }

    private void indexResponseHandler (IndexResponse indexResponse) {
        System.out.println("INDEX SUMMARY:");
        // Index name
        String _index = indexResponse.getIndex();
        // Type name
        String _type = indexResponse.getType();
        // Document ID (generated or not)
        String _id = indexResponse.getId();
        // Version (if it's the first time you index this document, you will get: 1)
        long _version = indexResponse.getVersion();
        // status has stored current instance statement.
        RestStatus status = indexResponse.status();

        System.out.println("Index: " + _index + "\nType: " + _type + "\nId: " + _id + "\nVersion: " + _version + "\nStatus: " + status);

        if (indexResponse.getResult() == DocWriteResponse.Result.CREATED) {
            //Handle the case where the document was created for the first time
            System.out.println("A new entry has been created.");
        } else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {
            //Handle the case where the document was rewritten as it was already existing
            System.out.println("An existing entry has been updated.");
        }
        ReplicationResponse.ShardInfo shardInfo = indexResponse.getShardInfo();
        if (shardInfo.getFailed() > 0) {
            System.out.println("Shard information failure:");
            for (ReplicationResponse.ShardInfo.Failure failure : shardInfo.getFailures()) {
                //Handle the potential failures
                String reason = failure.reason();
                System.out.println(reason);
            }
        }
        System.out.println("---------------------------------------");
    }

    public void closeClient() {
        try {
            client.close();
        } catch (IOException e) {
            System.err.println("IOException when trying to close the client connection.");
            System.out.println(e.getMessage());
        }
    }
}
