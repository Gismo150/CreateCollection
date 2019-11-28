package main;
/**
 * Main entry point of the whole program
 *
 * @author Daniel Braun
 */
public class MainCreateCollection {

    public static void main(String[] args) {
        if(args.length == 0){
            System.out.println("Starting create collection for all repositories inside repositories.json");
            //Init crawler with configuration.
            CreateCollection createCollection = new CreateCollection();
            //Start the crawler.
            createCollection.run();
        } else if (args.length == 1) {
            try {
                int arrayIndex = Integer.parseInt(args[0]);
                if(arrayIndex >= 0) {
                    System.out.println("Starting create collection for repository at index "+arrayIndex+".");
                    //Init crawler with configuration.
                    CreateCollection createCollection = new CreateCollection();
                    //Start the crawler.
                    createCollection.runByIndex(arrayIndex);
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

