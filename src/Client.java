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
            System.err.println("pwn doesn't have any arguments");
            return ;
        }
        System.out.println(myPwd);
    }
    
    public void ls(String[] cmd) {
        
        if (cmd.length != 1) {
            System.err.println("ls doesn't have any arguments");
            return ;
        }
        // String[] l = metaServer.list(myPwd);
        String[] s = { "a.out", "temp/" };
        // Iterates trough the list and prints the items
        for (String i: s) {
            System.out.println(i + " ");
        }
        // ...
    }
    
    public void cd(String[] cmd) {

        if (cmd.length != 2) {
            System.err.println("Expected format cd dir");
            return ;
        }
    }

    public void mv(String[] cmd) {
    
        if (cmd.length != 3) {
            System.err.println("Expected format mv file1 file2");
            return ;
        }
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
            // registry = LocateRegistry.getRegistry();
            // metaServer = (MetaServerInterface)registry.lookup("MS");

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
