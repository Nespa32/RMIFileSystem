import java.util.*;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.util.Scanner;

public class Client {

    private final Registry registry;
    private final MetaServerInterface metaServer;
    private String myPwd;

    public Client(Registry _registry, MetaServerInterface _metaServer) {

        registry = _registry;
        metaServer = _metaServer;
        myPwd = "/";
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
        catch (Exception e){

            System.err.println(e);
            return;
        }
        // Iterates trough the list and prints the items
        for (String i : l)
            System.out.println(i + " ");
    }

    public void cd(String[] cmd) {

        if (cmd.length != 2) {
            System.err.println("Expected format cd dir");
            return ;
        }
        // Checking if the dir path is relative or absolute
        String tempPwd = (cmd[1].charAt(0) == '/' ? "/" : myPwd);
        String[] pathSplited = cmd[1].split("/");
        String absPath = buildPath(tempPwd, pathSplited);

        if (!checkPath(absPath)) {
           System.err.println("Path not valid");
           return;
        }
        else
            myPwd = absPath;
    }
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

    public boolean checkPath(String path) {
        try {
            metaServer.find(path);
            return true;
        }
        catch (Exception e){
            System.out.println(e);
            return false;
        }
    }

    public void mv(String[] cmd) {

        if (cmd.length != 3) {
            System.err.println("Expected format mv file1 file2");
            return ;
        }

        String[] s = { "a.out", "temp/" };
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
