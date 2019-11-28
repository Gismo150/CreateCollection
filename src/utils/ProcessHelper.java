package utils;

import java.io.*;

public class ProcessHelper {

    public static int executeProcess(ProcessBuilder processBuilder) {
        try {
            Process process = processBuilder.inheritIO().start();
            return process.waitFor(); //Blocking until process finished

        } catch (IOException e) {
            System.err.println("Internal process IOException error");
            System.err.println(e.getMessage());
            return 1;
        } catch (InterruptedException e) {
            System.err.println("Internal process InterruptedException error");
            System.err.println(e.getMessage());
            return 1;
        }
    }
    public static int executeProcessAndGetOutput(ProcessBuilder processBuilder, StringBuilder builder) {
        try {
            Process process = processBuilder.start();

           BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return process.waitFor(); //Blocking until process finished

        } catch (IOException e) {
            System.err.println("Internal process IOException error");
            System.err.println(e.getMessage());
            return 1;
        } catch (InterruptedException e) {
            System.err.println("Internal process InterruptedException error");
            System.err.println(e.getMessage());
            return 1;
        }
    }
}
