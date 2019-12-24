package main;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point of the whole program
 *
 * @author Daniel Braun
 */
public class MainCollectionCreator {

    public static void main(String[] args) {
        Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        logger.setLevel(Level.ALL);
        Handler handler = null;
        try {
            handler = new FileHandler(Config.FILEPATH + "/CollectionCreator_Log.xml", true);
            handler.setLevel(Level.ALL);
            logger.addHandler(handler);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.setUseParentHandlers(false);
        logger.config("CONFIGURATION:");
        logger.config("Filepath: " + Config.FILEPATH);
        logger.config("Host path: " + Config.HOSTPATH);
        logger.config("Container path: " + Config.CONTAINERPATH);
        logger.config("repositories.json is output to: " + Config.FILEPATH + "/" + Config.JSONFILENAME);
        logger.config("results.json is read from: " + Config.FILEPATH + "/" + Config.RESULTFILENAME);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String systemStartTime = formatter.format(calendar.getTime());
        long startTime = System.nanoTime();



        if(args.length == 0){
            System.out.println("Starting collection creation for all repositories inside repositories.json");
            //Init crawler with configuration.
            CollectionCreator collectionCreator = new CollectionCreator(logger, startTime, systemStartTime);
            //Start the crawler.
            collectionCreator.run();
        } else if (args.length == 1) {
            try {
                int arrayIndex = Integer.parseInt(args[0]);
                if(arrayIndex >= 0) {
                    System.out.println("Starting collection creation for repository at index "+arrayIndex+".");
                    //Init crawler with configuration.
                    CollectionCreator collectionCreator = new CollectionCreator(logger, startTime, systemStartTime);
                    //Start the crawler.
                    collectionCreator.runByIndex(arrayIndex);
                } else {
                    System.err.println("Number must be greater or equals 0.");
                    System.err.println("Aborting.");
                    System.exit(1);
                }
            } catch (NumberFormatException e) {
                System.err.println("Expected a number as an argument.");
                System.err.println(e.getMessage());
                System.err.println("Aborting.");
                System.exit(1);
            }
        } else {
            System.err.println("Expected at most 1 optional argument.\nGot " + args.length + " argument(s).\nPlease provide at most one positive number (including 0).");
        }
    }
}

