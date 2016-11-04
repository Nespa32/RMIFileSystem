import java.rmi.RemoteException;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.util.Scanner;

import java.io.FileReader;
import java.io.BufferedReader;

public class Client {

    private final Registry registry;
    private final MetaServerInterface metaServer;
    private String myPwd;
    private String myUser;
    private String myMachine;
    private String configPath;
    private String cachePath;
    private Map<String, String> config;
    

    // Colors for ls command
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLUE = "\u001B[34m";

    public Client(Registry _registry, MetaServerInterface _metaServer) {

        registry = _registry;
        metaServer = _metaServer;
        myPwd = "/";
        myUser = System.getProperty("user.name");
        myMachine = System.getProperty("os.name");
        // setup config file
        configPath = System.getProperty("user.dir") + "/apps.conf";
        config = new HashMap<>();
        updateConfig();

        cachePath = System.getProperty("user.dir") + "/cache/";
        cleanCache();
    }

    public void updateConfig() {

        try {
            // Clear existing config hash
            config.clear();
            // Opening config file and make a scanner
            File configFile = new File(configPath);
            Scanner s = new Scanner(configFile);
            
            while(s.hasNextLine()) {

                String line = s.nextLine();
                // Replaces , by /s to ease the split
                String str = line.replace(",", " ");
                // Splits str by any number of spaces
                String[] tokens = str.split(" +");
                String ProgramPath = tokens[tokens.length - 1];
                System.out.println(ProgramPath);
                for(String i:tokens) {
                    if(!ProgramPath.equals(i))
                        config.put(i, ProgramPath);
                }
            }
        }
        catch (Exception e){
            System.err.println(e);
        }
    }

    void cleanCache() {

         try {
            
             Process process = Runtime.getRuntime().exec("rm " + cachePath + "*");
             System.out.println("rm " + cachePath + "*");
         }
         catch(Exception e) {
            
             System.err.println(e);
        }   
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
        else if (nextDir.equals("") || nextDir.equals(".") || nextDir.equals(".."))
            return buildPath(tempPwd, nextPwd);
        else
            return buildPath(tempPwd + nextDir + "/", nextPwd);
    }

    // Builds the absulute path
    public String buildAbsPath (String path) {
        
        String tempPwd = (path.charAt(0) == '/' ? "/" : myPwd);
        String[] pathSplited = path.split("/");
        String absPath = buildPath(tempPwd, pathSplited);
        if (absPath.length() == 1 || checkPath(absPath.substring(0, absPath.lastIndexOf('/')), true, false))
            return absPath;
        else
            return absPath.substring(0, absPath.length() - 1);
                
    }

    public String getObjectName (String path) {

        if (path.length() == 0)
            return "";
        String newString = path;
        if(path.charAt(path.length()-1) == '/') 
            newString = newString.substring(0, newString.lastIndexOf('/'));
        return newString.substring(newString.lastIndexOf('/') + 1, newString.length());
    }

    public String getFileExtension (String fileName) {
        
        String[] tempList = fileName.split("\\.");
        String extension = tempList[tempList.length -1];
        return extension;
    }

    // Checks if an absulute path "path" exists.
    public boolean checkPath(String path, boolean isDir, boolean ...wantException) {

        try {

            if (isDir)
                metaServer.list(path);
            else
                metaServer.find(path);
        }
        catch (Exception e) {
            //System.out.println("Excep");
            if (wantException.length == 0 || wantException[0])
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
            if(checkPath(myPwd + i, true, false))
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
        String storageName;
        StorageServerInterface storageServer;
        if (!checkPath(absPathA, false, false)) {
            System.err.println("Path <" + absPathA + "> not valid");
            return ;
        }

        byte[] file;
        try {

            storageName = metaServer.find(absPathA);
            storageServer = (StorageServerInterface)registry.lookup(storageName);
            file = storageServer.getFile(absPathA);
        }

        catch(Exception e) {
            System.err.println(e);
            return;
        }
        // Check if the second arg is a valid dir
        if(checkPath(absPathB, true, false)) {
            // Move file to absPathB with the same name
            try {
                storageServer.createFile(absPathB + getObjectName(absPathA), file);
                storageServer.delFile(absPathA);
            }
            catch(RemoteException e) {

                System.err.println(e);
            }
        }
        // Checking if the second arg is a valid file
        else if(checkPath(absPathB, false, false)) {
            // Delete file at absPathB and move the file with the old name
            try {
                
                storageServer.delFile(absPathA);
                storageServer.delFile(absPathB);
                storageServer.createFile(absPathB, file);
            }
            catch(Exception e) {
                System.err.println(e);
            }
            return;
        }
        // Have to create a new file
        else {
            try {
                
                storageServer.createFile(absPathB, file);
                storageServer.delFile(absPathA);
            }
            catch(Exception e) {
                System.err.println(e);
            }
            return;
        }
        //
    }

    public void open(String[] cmd) {

        if (cmd.length != 2) {
            System.err.println("Expected format open file");
            return ;
        }
        String path = cmd[1], absPath = buildAbsPath(path);
        
        if(!checkPath(absPath, false, false))
            System.err.println("Path <" + path + "> is not a valid file path");

        // Downloading File
        String storageName;
        StorageServerInterface storageServer;
        byte[] file;

        try {

            storageName = metaServer.find(absPath);
            storageServer = (StorageServerInterface)registry.lookup(storageName);
            file = storageServer.getFile(absPath);
        }

        catch(Exception e) {
            System.err.println(e);
            return;
        }

        // Getting extension
        String name = getObjectName(absPath);
        String extension = getFileExtension(name);
        String extensionPath = config.get(extension);

        // Save file to cache

        String newFile = cachePath + name;
        try {
            
            FileOutputStream fos = new FileOutputStream(newFile);
            fos.write(file);
            fos.close();
        }
        
        catch(Exception e) {
            System.err.println(e);
            return;
        }

        try {
            
            Process process = Runtime.getRuntime().exec(extensionPath + " " + newFile);
        }
        
        catch(Exception e) {
            
            System.err.println(e);
        }   
    }

    public void exit() {
        System.exit(0);
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
            case "exit":
                exit();
                break;
            default:
                System.err.println("Invalid command " + cmd[0]);
                break;
            }
        }
    }
}
