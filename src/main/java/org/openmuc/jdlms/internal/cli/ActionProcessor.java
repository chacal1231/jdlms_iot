package org.openmuc.jdlms.internal.cli;

import static java.lang.System.exit;
import static java.lang.System.out;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.File;

public class ActionProcessor {

    private static final String SEPARATOR_LINE = "------------------------------------------------------";

    private final BufferedReader reader;
    private final ActionListener actionListener;

    private final Map<String, Action> actionMap = new LinkedHashMap<>();

    private final Action helpAction = new Action("h", "print help message");
    private final Action quitAction = new Action("q", "quit the application");


    public ActionProcessor(ActionListener actionListener) {
        reader = new BufferedReader(new InputStreamReader(System.in));
        this.actionListener = actionListener;
    }

    public void addAction(Action action) {
        actionMap.put(action.getKey(), action);
    }

    public BufferedReader getReader() {
        return reader;
    }

    public void start() {

        actionMap.put(helpAction.getKey(), helpAction);
        actionMap.put(quitAction.getKey(), quitAction);

        printHelp();

        try {
            int ValToRead = 0;
            while (true) {
                do{
                    String actionKey = "r";
                    actionListener.actionCalled(actionKey, ValToRead);
                    ValToRead++;
                    
                }while(ValToRead <=12);

                ValToRead = 0;
                //Python lee envía a backend
                Process p = Runtime.getRuntime().exec("python ../sigfox/tx.py");
                Thread.sleep(60000);
                File file = new File("../sigfox.txt");
                file.delete();

                //System.out.println("\n** Enter action key: ");

                /*try {
                    actionKey = reader.readLine();
                } catch (IOException e) {
                    System.err.printf("%s. Application is being shut down.\n", e.getMessage());
                    exit(2);
                    return;
                }

                if (actionMap.get(actionKey) == null) {
                    System.err.println("Illegal action key.\n");
                    printHelp();
                    continue;
                }

                if (actionKey.equals(helpAction.getKey())) {
                    printHelp();
                    continue;
                }

                if (actionKey.equals(quitAction.getKey())) {
                    actionListener.quit();
                    return;
                }
                */

            }

        } catch (Exception e) {
            e.printStackTrace();
            actionListener.quit();
        } finally {
            close();
        }
    }

    private void printHelp() {
        final String message = " %s - %s\n";
        out.flush();
        out.println();
        out.println(SEPARATOR_LINE);

        for (Action action : actionMap.values()) {
            out.printf(message, action.getKey(), action.getDescription());
        }

        out.println(SEPARATOR_LINE);

    }

    public void close() {
        try {
            reader.close();
        } catch (IOException e) {
        }

    }

}
