package main;
/**
 * Main entry point of the whole program
 *
 * @author Daniel Braun
 */
public class MainCollectionCreator {

    public static void main(String[] args) {
        if(args.length == 0){
            System.out.println("Starting collection creation for all repositories inside repositories.json");
            //Init crawler with configuration.
            CollectionCreator collectionCreator = new CollectionCreator();
            //Start the crawler.
            collectionCreator.run();
        } else if (args.length == 1) {
            try {
                int arrayIndex = Integer.parseInt(args[0]);
                if(arrayIndex >= 0) {
                    System.out.println("Starting collection creation for repository at index "+arrayIndex+".");
                    //Init crawler with configuration.
                    CollectionCreator collectionCreator = new CollectionCreator();
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

