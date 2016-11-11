
import java.rmi.RemoteException;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Scanner;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;

import jgroup.core.ExternalGMIService;
import jgroup.core.GroupManager;
import jgroup.core.MembershipService;
import jgroup.core.registry.DependableRegistry;
import jgroup.core.registry.RegistryFactory;

public class Client {

    public static void main(String args[]) throws Exception {

        try {

            DependableRegistry registry = RegistryFactory.getRegistry();

            GroupManager gm = GroupManager.getGroupManager(this);

            MembershipService membershipService = (MembershipService)gm.getService(MembershipService.class);
            ExternalGMIService externalGMIService = (ExternalGMIService)gm.getService(ExternalGMIService.class);

            DependableRegistry.RegistryEntry entry = egmis.getRegistryEntry(name);
            registry.bind("ASD", entry, serverClass, leaseTime)

            String[] l = registry.list();
            System.out.println("l size: " + l.length);
            for (String s : l)
                System.out.println(s);

            MetaServerInterface metaServer = (MetaServerInterface)registry.lookup("MS");

            Client client = new Client(registry, metaServer);
            client.run();
        }
        catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }

    private final DependableRegistry registry;
    private final MetaServerInterface metaServer;
    private String myPwd;
    private String myDir;
    private String configPath;
    private String cachePath;
    private Map<String, String> config;

    // Colors for ls command
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLUE = "\u001B[34m";

    public Client(DependableRegistry registry, MetaServerInterface metaServer) {

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

    void run() {

        Scanner scanner = new Scanner(System.in);
        while(true) {

            String myUser = System.getProperty("user.name");
            String myMachine = System.getProperty("os.name");
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

    void cleanCache() {

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
    public String buildAbsPath(String path) {

        String tempPwd = (path.charAt(0) == '/' ? "/" : myPwd);
        String[] pathSplited = path.split("/");
        String absPath = buildPath(tempPwd, pathSplited);

        if (absPath.length() == 1 || checkPath(absPath.substring(0, absPath.lastIndexOf('/')), true, false))
            return absPath;
        else
            return absPath.substring(0, absPath.length() - 1);

    }

    // Checks if an absolute path "path" exists.
    public boolean checkPath(String path, boolean isDir, boolean ...wantException) {

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

            l = metaServer.lstat(myPwd);
        }
        catch (Exception e) {

            System.err.println(e);
            return;
        }
        // Iterates trough the list and prints the items
        for (String i : l) {
            if (checkPath(myPwd + i, true, false))
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
        String storageNameA, storageNameB;
        StorageServerInterface storageServerA = null, storageServerB = null;

        if (!checkPath(absPathA, false, false)) {
            System.err.println("Path <" + absPathA + "> not valid");
            return ;
        }

        byte[] file;
        try {

            storageNameA = metaServer.find(absPathA);
            storageServerA = (StorageServerInterface)registry.lookup(storageNameA);
            file = storageServerA.getFile(absPathA);

            if (checkPath(absPathB, true, false) || checkPath(absPathB, false, false)) {
                storageNameB = metaServer.find(absPathB);
                storageServerB = (StorageServerInterface)registry.lookup(storageNameB);
            }
        }
        catch (Exception e) {
            System.err.println(e);
            return;
        }

        // Check if the second arg is a valid dir
        if (checkPath(absPathB, true, false)) {
            // Move file to absPathB with the same name
            try {

                storageServerB.createFile(absPathB + getObjectName(absPathA), file);
                storageServerA.delFile(absPathA);
            }
            catch (RemoteException e) {

                System.err.println(e);
            }
        }
        // Checking if the second arg is a valid file
        else if (checkPath(absPathB, false, false)) {
            // Delete file at absPathB and move the file with the old name
            try {

                storageServerA.delFile(absPathA);
                storageServerB.delFile(absPathB);
                storageServerB.createFile(absPathB, file);
            }
            catch (Exception e) {
                System.err.println(e);
            }
        }
        // Have to create a new file
        else {

            try {

                storageServerA.createFile(absPathB, file);
                storageServerA.delFile(absPathA);
            }
            catch (Exception e) {
                System.err.println(e);
            }
        }
        //
    }

    public void open(String[] cmd) {

        if (cmd.length != 2) {
            System.err.println("Expected format open file");
            return ;
        }

        String path = cmd[1], absPath = buildAbsPath(path);
        if (!checkPath(absPath, false, false))
            System.err.println("Path <" + path + "> is not a valid file path");

        // download file
        try {

            String storageName = metaServer.find(absPath);
            StorageServerInterface storageServer = (StorageServerInterface)registry.lookup(storageName);
            byte[] file = storageServer.getFile(absPath);

            // check file integrity
            String storageServerMD5 = getMD5Sum(file);
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
        catch (Exception e) {
            System.err.println(e);
        }
    }

    public void downloadFile(String[] cmd) {

         if (cmd.length != 3) {
            System.err.println("Expected format mv file1 file2");
            return ;
         }

         String a = cmd[1], b = cmd[2]
             ,absPathA = buildAbsPath(a), absPathB;

         if (b.charAt(0) == '/')
             absPathB = b;
         else
             absPathB = myDir + "/" + b;

         String storageNameA;
         StorageServerInterface storageServerA;

        byte[] file;
        try {

            storageNameA = metaServer.find(absPathA);
            storageServerA = (StorageServerInterface)registry.lookup(storageNameA);
            file = storageServerA.getFile(absPathA);

            // check file integrity
            String storageServerMD5 = getMD5Sum(file);
            String metaServerMD5 = metaServer.getMD5(absPathA);
            if (!storageServerMD5.equals(metaServerMD5))
                throw new Exception("MD5 integrity check failed! SS: " +
                        storageServerMD5 + " MS: " + metaServerMD5);
        }
        catch (Exception e) {
            System.err.println(e);
            return;
        }

        String destPath = absPathB;
        File dirTest = new File(absPathB);

        if (dirTest.isDirectory())
            destPath += "/" + getObjectName(absPathA);
        try {

            FileOutputStream fos = new FileOutputStream(destPath);
            fos.write(file);
            fos.close();
        }
        catch (Exception e) {
            System.err.println(e);
            return;
        }
    }

    public void uploadFile(String[] cmd) {

         if (cmd.length != 3) {
            System.err.println("Expected format mv file1 file2");
            return ;
        }

         String a = cmd[1], b = cmd[2]
             , absPathA, absPathB = buildAbsPath(b);
         System.out.println(absPathB);
         // Changing local path according if its absolute or relative
         if (a.charAt(0) == '/')
             absPathA = a;
         else
             absPathA = myDir + "/" + a;

         String storageNameB;
         StorageServerInterface storageServerB;

         // Getting both the wanted file and the destination SS
         byte[] file;
         try {

             Path path = Paths.get(absPathA);
             file = Files.readAllBytes(path);
             // Deals with the case where the dest dir/file exists and when the file has to be created
             if (checkPath(absPathB, true, false) || checkPath(absPathB, false, false)) 
                 storageNameB = metaServer.find(absPathB);
             else
                 storageNameB = metaServer.find(absPathB.substring(0, absPathB.lastIndexOf('/') + 1));
             storageServerB = (StorageServerInterface)registry.lookup(storageNameB);
         }
         catch (Exception e) {
             System.err.println(e);
             return;
         }

         // Check if the second arg is a valid dir
         if (checkPath(absPathB, true, false)) {
             // Move file to absPathB with the same name
             try {

                 storageServerB.createFile(absPathB + getObjectName(absPathA), file);
             }
             catch (RemoteException e) {

                 System.err.println(e);
                 return;
             }
         }
         // Checking if the second arg is a valid file
        else if (checkPath(absPathB, false, false)) {
            // Delete file at absPathB and move the file with the old name
            try {

                storageServerB.delFile(absPathB);
                storageServerB.createFile(absPathB, file);
            }
            catch (Exception e) {
                System.err.println(e);
            }
        }
        // Have to create a new file
        else {
            try {

                storageServerB.createFile(absPathB.substring(0, absPathB.lastIndexOf('/') + 1) + getObjectName(absPathB), file);
            }
            catch (Exception e) {
                System.err.println(e);
            }
        }
    }

    public void exit() {
        System.exit(0);
    }

    private String getMD5Sum(byte[] fileBytes) {

        try {

            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.reset();
            messageDigest.update(fileBytes);
            byte[] md5Hash = messageDigest.digest();
            String md5Str = new BigInteger(1, md5Hash).toString(16);
            return md5Str;
        }
        catch (NoSuchAlgorithmException e) {

            System.err.println("NoSuchAlgorithmException: " + e.toString());
            return null;
        }
    }
}
