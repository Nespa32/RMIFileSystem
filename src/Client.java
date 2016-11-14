
import java.rmi.RemoteException;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileOutputStream;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.util.Scanner;

public class Client {

    public static void main(String args[]) {

        try {

            Registry registry = LocateRegistry.getRegistry();
            MetaServerInterface metaServer = (MetaServerInterface)registry.lookup("MS");

            Client client = new Client(registry, metaServer);
            client.run();
        }
        catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }

    private final Registry registry;
    private final MetaServerInterface metaServer;
    private String myPwd;
    private String myDir;
    private String configPath;
    private String cachePath;
    private Map<String, String> config;

    // Colors for ls command
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLUE = "\u001B[34m";

    public Client(Registry registry, MetaServerInterface metaServer) {

        this.registry = registry;
        this.metaServer = metaServer;
        this.myPwd = "/";
        this.myDir = System.getProperty("user.dir");
        // setup config file
        this.configPath =  myDir + "/apps.conf";
        this.config = new HashMap<>();
        this.cachePath = myDir + "/cache/";

        updateConfig();
        cleanCache();
    }

    public void run() throws Exception {

        Scanner scanner = new Scanner(System.in);

        while (true) {

            String myUser = System.getProperty("user.name");
            String myMachine = System.getProperty("os.name");
            System.out.print(myUser + "@" + myMachine + ":" + myPwd + "$ ");

            String[] cmd = scanner.nextLine().split(" ");

            try {
                runSingleCommand(cmd);
            }
            catch (Exception e) {
                System.out.println(e);
                e.printStackTrace();
            }
        }
    }

    private void runSingleCommand(String[] cmd) throws Exception {

        switch (cmd[0]) {
            case "": // empty string case, user pressed ENTER, simply ignore
                break;
            case "pwd":
                System.out.println(myPwd); // one-liner, no need for a method
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
            case "rm":
                rm(cmd);
                break;
            case "open":
                open(cmd);
                break;
            case "mkdir":
                mkdir(cmd);
                break;
            case "rmdir":
                rmdir(cmd);
                break;
            case "touch":
                touch(cmd);
                break;
            case "reload":
                updateConfig();
                break;
            case "help":
                showHelp();
                break;
            case "upload":
                uploadFile(cmd);
                break;
            case "download":
                downloadFile(cmd);
                break;
            case "exit":
            case "quit":
                exit();
                break;
            default:
                System.err.println("Invalid command " + cmd[0]);
                break;
        }
    }

    private void updateConfig() {

        try {

            // Clear existing config hash
            config.clear();
            // Opening config file and make a scanner
            File configFile = new File(configPath);
            Scanner s = new Scanner(configFile);

            while (s.hasNextLine()) {

                String line = s.nextLine();
                // Replaces , by /s to ease the split
                String str = line.replace(",", " ");
                // Splits str by any number of spaces
                String[] tokens = str.split(" +");
                String ProgramPath = tokens[tokens.length - 1];

                for(String i : tokens) {

                    if (!ProgramPath.equals(i))
                        config.put(i, ProgramPath);
                }
            }
        }
        catch (Exception e){
            System.err.println(e);
        }
    }

    private void cleanCache() {

         try {

             File dir = new File(cachePath);
             for (File file : dir.listFiles())
                 if (!file.isDirectory())
                     file.delete();
         }
         catch (Exception e) {
             System.err.println(e);
        }
    }
    // Aux Functions

    // Returns a path given a pwd and a string of directories to navigate to
    private String buildPath(String tempPwd, String[] remainingPwd) {

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

    // Builds the absolute path
    private String buildAbsPath(String path) {

        String tempPwd = (path.charAt(0) == '/' ? "/" : myPwd);
        String[] pathSplited = path.split("/");
        String absPath = buildPath(tempPwd, pathSplited);

        if (absPath.length() == 1 || checkPath(absPath.substring(0, absPath.lastIndexOf('/')), true, false))
            return absPath;
        else
            return absPath.substring(0, absPath.length() - 1);
    }

    private String getParentForPath(String path) {

        if (path.endsWith("/")) // directory path, removing trailing '/'
            path = path.substring(0, path.length() - 1);

        return path.substring(0, path.lastIndexOf('/'));
    }

    // Checks if an absolute path "path" exists.
    private boolean checkPath(String path, boolean isDir, boolean ...wantException) {

        try {

            if (isDir)
                metaServer.lstat(path);
            else
                metaServer.find(path);
        }
        catch (Exception e) {

            if (wantException.length == 0 || wantException[0])
                System.err.println(e);
            return false;
        }

        return true;
    }

    private String getObjectName(String path) {

        if (path.length() == 0)
            return "";

        String newString = path;
        if (path.charAt(path.length()-1) == '/')
            newString = newString.substring(0, newString.lastIndexOf('/'));

        return newString.substring(newString.lastIndexOf('/') + 1, newString.length());
    }

    private String getFileExtension(String fileName) {

        String[] tempList = fileName.split("\\.");
        String extension = tempList[tempList.length -1];
        return extension;
    }

    private StorageServerInterface getStorageServerForPath(String path) throws Exception {

        String storageServerId = metaServer.find(path);
        return (StorageServerInterface)registry.lookup(storageServerId);
    }

    // Command Functions

    public void ls(String[] cmd) throws Exception {

        if (cmd.length != 1)
            throw new Exception("Expected format: ls");

        String[] l = metaServer.lstat(myPwd);

        // Iterates trough the list and prints the items
        for (String i : l) {

            if (checkPath(myPwd + i, true, false))
                System.out.println(ANSI_BLUE + i + " " +  ANSI_RESET);
            else
                System.out.println(i + " ");
        }
    }

    public void cd(String[] cmd) throws Exception {

        if (cmd.length != 2)
            throw new Exception("Expected format: rm DIR");

        // Checking if the dir path is relative or absolute
        String absPath = buildAbsPath(cmd[1]);

        if (checkPath(absPath, true))
            myPwd = absPath;
    }

    public void mv(String[] cmd) throws Exception {

        if (cmd.length != 3)
            throw new Exception("Expected format: mv file1 file2");

        String absPathA = buildAbsPath(cmd[1]);
        String absPathB = buildAbsPath(cmd[2]);

        String storageNameA, storageNameB;
        StorageServerInterface storageServerA = null, storageServerB = null;

        if (!checkPath(absPathA, false, false))
            throw new Exception("Path <" + absPathA + "> not valid");

        byte[] file;
        storageNameA = metaServer.find(absPathA);
        storageServerA = (StorageServerInterface)registry.lookup(storageNameA);
        file = storageServerA.getFile(absPathA);

        if (checkPath(absPathB, true, false) || checkPath(absPathB, false, false)) {
            storageNameB = metaServer.find(absPathB);
            storageServerB = (StorageServerInterface)registry.lookup(storageNameB);
        }

        // Check if the second arg is a valid dir
        if (checkPath(absPathB, true, false)) {
            // Move file to absPathB with the same name
            storageServerB.createFile(absPathB + getObjectName(absPathA), file);
            storageServerA.delFile(absPathA);
        }
        // Checking if the second arg is a valid file
        else if (checkPath(absPathB, false, false)) {
            // Delete file at absPathB and move the file with the old name
            storageServerA.delFile(absPathA);
            storageServerB.delFile(absPathB);
            storageServerB.createFile(absPathB, file);
        }
        // Have to create a new file
        else {

            storageServerA.createFile(absPathB, file);
            storageServerA.delFile(absPathA);
        }
    }

    private void rm(String[] cmd) throws Exception {

        if (cmd.length != 2)
            throw new Exception("Expected format: rm file");

        String absPath = buildAbsPath(cmd[1]);
        StorageServerInterface storageServer = getStorageServerForPath(absPath);

        storageServer.delFile(absPath);
    }

    private void open(String[] cmd) throws Exception {

        if (cmd.length != 2)
            throw new Exception("Expected format: open file");

        String path = cmd[1], absPath = buildAbsPath(path);
        if (!checkPath(absPath, false, false))
            throw new Exception("Path <" + path + "> is not a valid file path");

        // download file
        String storageName = metaServer.find(absPath);
        StorageServerInterface storageServer = (StorageServerInterface)registry.lookup(storageName);
        byte[] file = storageServer.getFile(absPath);

        // check file integrity
        String storageServerMD5 = Util.getMD5Sum(file);
        String metaServerMD5 = metaServer.getMD5(absPath);
        if (!storageServerMD5.equals(metaServerMD5))
            throw new Exception("MD5 integrity check failed! SS: " +
                                storageServerMD5 + " MS: " + metaServerMD5);

        // Getting extension
        String name = getObjectName(absPath);
        String extension = getFileExtension(name);
        String extensionPath = config.get(extension);

        String newFile = cachePath + name;

        FileOutputStream fos = new FileOutputStream(newFile);
        fos.write(file);
        fos.close();

        Runtime.getRuntime().exec(extensionPath + " " + newFile);
    }

    public void mkdir(String[] cmd) throws Exception {

        if (cmd.length != 2)
            throw new Exception("Expected format: mkdir $path");

        String absPath = buildAbsPath(cmd[1]);
        String parentPath = getParentForPath(absPath);
        StorageServerInterface storageServer = getStorageServerForPath(parentPath);

        storageServer.createDir(absPath);
    }

    private void rmdir(String[] cmd) throws Exception {

        if (cmd.length != 2)
            throw new Exception("Expected format: rmdir $path");

        String absPath = buildAbsPath(cmd[1]);
        StorageServerInterface storageServer = getStorageServerForPath(absPath);

        storageServer.delDir(absPath);
    }

    private void touch(String[] cmd) throws Exception {

        if (cmd.length != 2)
            throw new Exception("Expected format: touch $path");

        String absPath = buildAbsPath(cmd[1]);
        String parentPath = getParentForPath(absPath);
        StorageServerInterface storageServer = getStorageServerForPath(parentPath);

        storageServer.createFile(absPath, new byte[0]);
    }

    private void showHelp() {

        System.out.println("Available commands:");
        System.out.println("- 'help'                            show this message");
        System.out.println("- 'pwd'                             show current path");
        System.out.println("- 'ls'                              list files in current directory");
        System.out.println("- 'cd $path'                        change directory to path");
        System.out.println("- 'mv $file $path'                  moves file to path");
        System.out.println("- 'rm $file'                        delete files");
        System.out.println("- 'open $file'                      opens file locally with program specified in apps.conf");
        System.out.println("- 'mkdir $dir'                      creates directory");
        System.out.println("- 'rmdir $dir'                      deletes empty directory");
        System.out.println("- 'touch $file'                     creates empty file");
        System.out.println("- 'reload'                          reloads apps.conf");
        System.out.println("- 'upload $localPath $path'         uploads local file to path");
        System.out.println("- 'download $path $localPath'       downloads file to local path");
        System.out.println("- 'exit' 'quit'                     exits console");
    }

    public void downloadFile(String[] cmd) throws Exception {

        if (cmd.length != 3)
            throw new Exception("Expected format: download file1 file2");

        String a = cmd[1], b = cmd[2]
             ,absPathA = buildAbsPath(a), absPathB;

        if (b.charAt(0) == '/')
            absPathB = b;
        else
            absPathB = myDir + "/" + b;

        StorageServerInterface storageServerA = getStorageServerForPath(absPathA);
        byte[] file = storageServerA.getFile(absPathA);

        // check file integrity
        String storageServerMD5 = Util.getMD5Sum(file);
        String metaServerMD5 = metaServer.getMD5(absPathA);
        if (!storageServerMD5.equals(metaServerMD5))
            throw new Exception("MD5 integrity check failed! SS: " +
                                storageServerMD5 + " MS: " + metaServerMD5);

        String destPath = absPathB;
        File dirTest = new File(absPathB);

        if (dirTest.isDirectory())
            destPath += "/" + getObjectName(absPathA);

        FileOutputStream fos = new FileOutputStream(destPath);
        fos.write(file);
        fos.close();
    }

    public void uploadFile(String[] cmd) throws Exception {

         if (cmd.length != 3)
             throw new Exception("Expected format: mv file1 file2");

         String a = cmd[1], b = cmd[2]
             , absPathA, absPathB = buildAbsPath(b);
         // Changing local path according if its absolute or relative
         if (a.charAt(0) == '/')
             absPathA = a;
         else
             absPathA = myDir + "/" + a;

         String storageNameB;
         StorageServerInterface storageServerB;

         // Getting both the wanted file and the destination SS
         byte[] file;

         Path path = Paths.get(absPathA);
         file = Files.readAllBytes(path);
             // Deals with the case where the dest dir/file exists and when the file has to be created
         if (checkPath(absPathB, true, false) || checkPath(absPathB, false, false)) 
             storageNameB = metaServer.find(absPathB);
         else
             storageNameB = metaServer.find(absPathB.substring(0, absPathB.lastIndexOf('/') + 1));
         storageServerB = (StorageServerInterface)registry.lookup(storageNameB);

         // Check if the second arg is a valid dir
         if (checkPath(absPathB, true, false))
             // Move file to absPathB with the same name
             storageServerB.createFile(absPathB + getObjectName(absPathA), file);
         // Checking if the second arg is a valid file
         else if (checkPath(absPathB, false, false)) {
             // Delete file at absPathB and move the file with the old name
             storageServerB.delFile(absPathB);
             storageServerB.createFile(absPathB, file);
        }
        // Have to create a new file
        else
            storageServerB.createFile(absPathB.substring(0, absPathB.lastIndexOf('/') + 1) + getObjectName(absPathB), file);

    }

    public void exit() {
        System.exit(0);
    }
}
