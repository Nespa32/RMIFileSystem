import java.util.*;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.util.Scanner;

public class Client {

    private final Registry registry;
    private final MetaServerInterface metaServer;
    private String myPwd;
    private String myUser;
    private String myMachine;

    // Colors for ls command
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLUE = "\u001B[34m";

    public Client(Registry _registry, MetaServerInterface _metaServer) {

        registry = _registry;
        metaServer = _metaServer;
        myPwd = "/";
        myUser = "user";
        myMachine = "machine";
    }

    // Aux Functions

    // Returns a path given a pwd and a string of directories to navigate to
    public String buildPath(String tempPwd, String []remainingPwd){

        int length = remainingPwd.length;
        // Path creation ended
        if (length == 0)
            return tempPwd;

        String nextDir = remainingPwd[0];
        String []nextPwd = Arrays.copyOfRange(remainingPwd, 1, length);

        // Deal with changing to ..
        if (nextDir.equals("..") && !tempPwd.equals("/")) {
            // Remove the last /
            String newString = tempPwd.substring(0, tempPwd.lastIndexOf('/'));
            // Remove the last dir
            return buildPath(newString.substring(0, newString.lastIndexOf('/')) + "/", nextPwd);
        }
        // When we don't change to any dir
        else if (nextDir.equals(".") || nextDir.equals(".."))
            return buildPath(tempPwd, nextPwd);
        else
            return buildPath(tempPwd + nextDir + "/", nextPwd);
    }

    // Builds the absulute path
    public String buildAbsPath (String path) {

        String tempPwd = (path.charAt(0) == '/' ? "/" : myPwd);
        String[] pathSplited = path.split("/");
        return buildPath(tempPwd, pathSplited);

    }

    public String getObjectName (String path) {

        if (path.length() == 0)
            return "";
        String newString = path;
        if(path.charAt(path.length()-1) == '/') {
            newString = newString.substring(0, newString.lastIndexOf('/'));
        }
        return newString.substring(newString.lastIndexOf('/'), newString.length());
    }

    // Checks if an absulute path "path" exists.
    public boolean checkPath(String path, boolean isDir, boolean ...wantException) {

        try {

            if (isDir)
                metaServer.list(path);
            else
                metaServer.find(path);

        }
        catch (Exception e){
            if (wantException.length == 0 || !wantException[0])
                System.err.println(e);
            return false;
        }
        return true;
    }

    // Command Functions

    public void pwd(String[] cmd) {

        if (cmd.length != 1) {
            System.err.println("pwd doesn't have any arguments");
            return ;
        }
        System.out.println(myPwd);
    }

    public void ls(String[] cmd) {

        if (cmd.length != 1) {
            System.err.println("ls doesn't have any arguments");
            return ;
        }
        List<String> l;
        try {

            l = metaServer.list(myPwd);
        }
        catch (Exception e) {

            System.err.println(e);
            return;
        }
        // Iterates trough the list and prints the items
        for (String i : l) {
            if(checkPath(myPwd + i, true, true))
                System.out.println(ANSI_BLUE + i + " " +  ANSI_RESET);
            else
                System.out.println(i + " ");
        }
    }

    public void cd(String[] cmd) {

        if (cmd.length != 2) {
            System.err.println("Expected format cd dir");
            return ;
        }
        // Checking if the dir path is relative or absolute
        String absPath = buildAbsPath(cmd[1]);

        if (checkPath(absPath, true))
            myPwd = absPath;
    }

    public void mv(String[] cmd) {

        if (cmd.length != 3) {
            System.err.println("Expected format mv file1 file2");
            return ;
        }

        String a = cmd[1], b = cmd[2]
            , absPathA = buildAbsPath(a), absPathB = buildAbsPath(b);
        System.out.println("Moving " + absPathA + " to " + absPathB);

        if (!checkPath(absPathA, false, false)) {
            System.err.println("Object at <" + absPathA + "> is not a file");
            // devo criar um caso particular para o caso em que o caminho nao existe ?
            return ;
        }

        // Check if the second arg is a valid dir
        if(checkPath(absPathB, true, false)) {
            // Move file to absPathB with the same name
            System.out.println("Moving file " + getObjectName(absPathA) + " to dir " + absPathB);
            System.out.println("Deleting original file");
            return;
        }
        // Checking if the second arg is a valid file
        else if(checkPath(absPathB, false, false)) {
            // Delete file at absPathB and move the file with the old name
            System.out.println("Deleting original file " + getObjectName(absPathB) + " at " + absPathB);
            System.out.println("Moving file " + getObjectName(absPathA) + " to " + absPathB);
            return;
        }
        else {
            System.out.println("Final else");
            return;
        }
        //
    }

    public void open(String[] cmd) {

        if (cmd.length != 2) {
            System.err.println("Expected format open file");
            return ;
        }
    }

    public static void main(String args[]) {

        Registry registry = null;
        MetaServerInterface metaServer = null;

        try {
            registry = LocateRegistry.getRegistry();
            metaServer = (MetaServerInterface)registry.lookup("MS");

            Client client = new Client(registry, metaServer);
            client.run();
        }
        catch (Exception e) {
            // @todo
        }

    }

    void run() {
        // @todo: commands, etc
        Scanner scanner = new Scanner(System.in);
        while(true) {
            System.out.print(myUser + "@" + myMachine + ":" + myPwd + "$ ");
            String[] cmd = scanner.nextLine().split(" ");
            switch(cmd[0]) {
            case "": // empty string case, user pressed ENTER, simply ignore
                break;
            case "pwd":
                pwd(cmd);
                break;
            case "ls":
                ls(cmd);
                break;
            case "cd":
                cd(cmd);
                break;
            case "mv":
                mv(cmd);
                break;
            case "open":
                open(cmd);
                break;
            default:
                System.err.println("Invalid command " + cmd[0]);
                break;
            }
        }
    }
}
