import java.io.* ;
import java.net.* ;
import java.util.* ;
import java.awt.* ;
import java.nio.file.Files;
import java.nio.file.*;
import java.text.*;
import java.util.*;

public class FileServer {
    public static int max_no_clients;
    public static Vector<String> UserNames;
    public static Vector<Socket> ClientSockets;
    public static Vector<Integer> Ports;
    public static Vector<Chatroom> Chatrooms;
    public static Map<String,Chatroom> ConnectedChatroom;
    public static DatagramSocket SocUDP;
    public static Map<String, String> UserIP;

    FileServer(int max_no_clients_) {
        try {
            System.out.println("Server running on localhost Port-5000(TCP), 5001(UDP)");
            
            ServerSocket Soc = new ServerSocket(5000) ;
            DatagramSocket SocUDP = new DatagramSocket(5001);
            
            UserNames = new Vector<String>();
            max_no_clients=max_no_clients_;
            ClientSockets = new Vector<Socket>();
            Chatrooms = new Vector<Chatroom>();
            ConnectedChatroom = new HashMap<String,Chatroom>();
            Ports = new Vector<Integer>();
            UserIP = new HashMap<String, String>();
            
            while(true)
            {
                Socket CSoc = Soc.accept();
                AcceptClient client_ = new AcceptClient(CSoc,SocUDP) ;
            }
        }
        catch(Exception e) {
            e.printStackTrace(System.out);
            System.exit(0);
        }
    }

    public static void main(String args[]) throws Exception {

        if(args.length==0)
        {
            System.out.println("Maximum number of Users for the Server not given."); System.exit(0);
        }

        FileServer server = new FileServer(Integer.parseInt(args[0])) ;

    }
}

class AcceptClient extends Thread {
    
    Socket ClientSocket;
    DataInputStream din;
    DataOutputStream dout;
    String UserName;
    DatagramSocket SocUDP;
    
    AcceptClient (Socket CSoc, DatagramSocket SocUDP_) throws Exception {

        ClientSocket = CSoc;
        din = new DataInputStream(ClientSocket.getInputStream());
        dout = new DataOutputStream(ClientSocket.getOutputStream()) ;
        
        SocUDP = SocUDP_;

        byte[] intial = new byte[1000];
        DatagramPacket recieve_inital = new DatagramPacket(intial, intial.length);
        SocUDP.receive(recieve_inital);

        int port = recieve_inital.getPort();
        FileServer.Ports.add(port);

        start() ;
    }

    public void run() {
        while(true)
        {
            try
            {
                String commandfromClient = new String() ;

                commandfromClient = din.readUTF();
                
                StringTokenizer tokenedcommand = new StringTokenizer(commandfromClient);
                String command = tokenedcommand.nextToken();

                // System.out.println(command);

                if(command.equals("create_user")) {
                    UserName = tokenedcommand.nextToken();

                    if(FileServer.UserNames.size()==FileServer.max_no_clients) {
                        System.out.println("Cannot login user: Server's maximum limit reached");
                        dout.writeUTF("Cannot connect: Reached Server's maximum capacity");
                        ClientSocket.close() ; din.close() ; dout.close() ; return;
                    }

                    if(FileServer.UserNames.contains(UserName)) {
                        dout.writeUTF("UserName is already present, think something new!");
                        ClientSocket.close() ; din.close() ; dout.close() ; return;
                    }

                    System.out.println("User "+ UserName +" created");

                    FileServer.UserNames.add(UserName);
                    FileServer.ClientSockets.add(ClientSocket);
                    FileServer.ConnectedChatroom.put(UserName,null);

                    String userIP = ClientSocket.getInetAddress().toString();
                    userIP = userIP.substring(1, userIP.length());

                    FileServer.UserIP.put(UserName, userIP);

                    // System.out.println(ClientSocket.getRemoteSocketAddress().toString());

                    String folderName = UserName;
                    File newDir = new File(folderName);
                    if(newDir.exists()) {
                        // System.out.println("Directory Already Exists!");
                        int po=1;
                    }
                    else {
                        newDir.mkdir();
                    }
                }

                else if(command.equals("create_folder")) {
                    try {
                        String folderName = tokenedcommand.nextToken();
                        File newDir = new File(UserName + '/' + folderName);
                        if(newDir.exists()) {
                            dout.writeUTF("Directory Already Exists!");
                        }
                        else {
                            newDir.mkdir();
                        }
                        System.out.println("Folder " + folderName + " created by " + UserName);
                    }
                    catch(Exception e) {
                        dout.writeUTF("Something went wrong, please try again");
                    }
                }

                else if(command.equals("move_file")) {
                    try {
                        String sourcePath = tokenedcommand.nextToken();
                        String destPath = tokenedcommand.nextToken();

                        String filename = null;
                        if(sourcePath.indexOf('/') != -1) {
                            String[] names = sourcePath.split("/");
                            filename = names[names.length - 1];
                        }
                        else {
                            filename = sourcePath;
                        }

                        try {
                            if(destPath.endsWith("/")) {
                                if(destPath.length() > 0 && destPath!=null) {
                                    destPath = destPath.substring(0, destPath.length()-1);
                                }
                            }
                            Files.move(Paths.get(sourcePath), Paths.get(destPath + '/' + filename));
                        }
                        catch(IOException i) {
                            System.out.println(i);
                        }
                    }
                    catch(Exception e) {
                        dout.writeUTF("Something went wrong, please try again");
                    }
                }

                else if(command.equals("upload")) {
                    try {
                        String fl = tokenedcommand.nextToken();
                        
                        String st_ = din.readUTF();
                        StringTokenizer stt = new StringTokenizer(st_);
                        stt.nextToken();
                        int fileLength = Integer.parseInt(stt.nextToken());
                        
                        StringTokenizer fileName = new StringTokenizer(fl,"/");

                        while(fileName.hasMoreTokens()) {
                            fl = fileName.nextToken();
                        }

                        String nameOfFile = fl;
                        
                        fl = UserName + '/' + nameOfFile;

                        int size = 1000;

                        byte[] file_contents = new byte[size];
                        
                        FileOutputStream fpout = new FileOutputStream(fl);
                        BufferedOutputStream bpout = new BufferedOutputStream(fpout);

                        int bytesRead = 0;

                        if(size > fileLength) {
                            size = fileLength;
                        }

                        while((bytesRead=din.read(file_contents,0,size))!=-1 && fileLength>0) {
                            bpout.write(file_contents,0,size);
                            fileLength-=size;
                            if(size > fileLength) {
                                size = fileLength;
                            }
                        }

                        bpout.flush();
                        System.out.println("File uploaded by " + UserName);
                    }
                    catch(Exception e) {
                        dout.writeUTF("Something went wrong, please try again");
                    }
                }

                else if(command.equals("upload_udp")) {
                    try {
                        String fl = tokenedcommand.nextToken();
                        
                        String st_ = din.readUTF();
                        StringTokenizer stt = new StringTokenizer(st_);
                        stt.nextToken();
                        int fileLength = Integer.parseInt(stt.nextToken());
                        
                        StringTokenizer fileName = new StringTokenizer(fl,"/");

                        while(fileName.hasMoreTokens()) {
                            fl = fileName.nextToken();
                        }
                        
                        String nameOfFile = fl;

                        fl = UserName + '/' + nameOfFile;
                        
                        int size = 1024;
                        byte[] file_contents = new byte[size];

                        if(size > fileLength) {
                            size = fileLength;
                        }
                        //System.out.println(fileLength);
                        DatagramPacket packetUDP;

                        FileOutputStream fos1 = new FileOutputStream(fl);

                        while(fileLength>0)
                        {
                            packetUDP = new DatagramPacket(file_contents,size);
                            SocUDP.receive(packetUDP);

                            packetUDP = new DatagramPacket(file_contents,size,InetAddress.getByName(FileServer.UserIP.get(UserName)),5001);

                            fos1.write(packetUDP.getData(), 0, size);

                            SocUDP.send(packetUDP);

                            fileLength -= size;
                            
                            if(size > fileLength) {
                                size = fileLength;
                            }
                        }

                        fos1.close();

                        System.out.println("File Uploaded by " + UserName);
                    }
                    catch(Exception e) {
                        dout.writeUTF("Something went wrong, please try again");
                    }
                }

                else if(command.equals("create_group")) {
                    try {
                        String groupName = tokenedcommand.nextToken();

                        int existFlag = 0;

                        for(int i=0;i<FileServer.Chatrooms.size();i++) {
                            String grpName = FileServer.Chatrooms.elementAt(i).name;
                            if(grpName.equals(groupName)) {
                                existFlag = 1;
                                break;
                            }
                        }
                        
                        if(existFlag == 1) {
                            dout.writeUTF("Group "+ groupName + " already exists");
                        }
                        else {
                            Chatroom chatR = new Chatroom(groupName, UserName);
                            FileServer.Chatrooms.add(chatR);
                            dout.writeUTF("Group " + groupName + " created\nYou are in group " + groupName);

                            System.out.println("Group " + groupName + " created by " + UserName);
                        }
                    }
                    catch(Exception e) {
                        dout.writeUTF("Something went wrong, please try again");
                    }
                }

                else if(command.equals("list_groups")) {
                    try {
                        String outp="";
                        if(FileServer.Chatrooms.size() == 0) {
                            dout.writeUTF("No Groups exist");
                        }
                        else {
                            for(int i=0;i<FileServer.Chatrooms.size();i++) {
                                outp = outp + FileServer.Chatrooms.elementAt(i).name + "\n";
                            }

                            dout.writeUTF(outp);
                        }
                    }
                    catch(Exception e) {
                        dout.writeUTF("Something went wrong, please try again");
                    }
                }

                else if(command.equals("join_group")) {
                    try {
                        String groupName = tokenedcommand.nextToken();
                        
                        if(FileServer.ConnectedChatroom.get(UserName) == null) {

                            for(int i=0;i<FileServer.Chatrooms.size();i++) {
                                if(FileServer.Chatrooms.elementAt(i).name.equals(groupName)) {
                                    FileServer.Chatrooms.elementAt(i).Members.add(UserName);
                                    FileServer.Chatrooms.elementAt(i).Notify(UserName + " joined the group " + groupName, UserName);
                                    FileServer.ConnectedChatroom.put(UserName, FileServer.Chatrooms.elementAt(i));
                                    dout.writeUTF(UserName + " joined the group " + groupName);
                                    System.out.println(UserName + " joined the group " + groupName);
                                    break;
                                }
                            }
                        }
                        else if(FileServer.ConnectedChatroom.get(UserName).name.equals(groupName)) {
                            dout.writeUTF("You are already part of group " + FileServer.ConnectedChatroom.get(UserName).name);
                        }
                        else {
                            int i=0;
                            for(i=0;i<FileServer.Chatrooms.size();i++) {
                                if(FileServer.Chatrooms.elementAt(i).name.equals(groupName)) {
                                    if(FileServer.Chatrooms.elementAt(i).Members.contains(UserName)) {
                                        dout.writeUTF("You are already part of group " + groupName);
                                    }
                                    else {
                                        FileServer.Chatrooms.elementAt(i).Members.add(UserName);
                                        // reqGrp.Members.add(UserName);
                                        FileServer.Chatrooms.elementAt(i).Notify(UserName + " joined the group " + groupName, UserName);
                                        FileServer.ConnectedChatroom.put(UserName, FileServer.Chatrooms.elementAt(i));
                                        dout.writeUTF("You joined the group " + groupName);
                                        System.out.println(UserName + " joined the group " + groupName);
                                    }
                                    break;
                                }
                            }

                            if(i == FileServer.Chatrooms.size()) {
                                dout.writeUTF(groupName + " doesn't exist");
                            }
                        }
                    }
                    catch(Exception e) {
                        dout.writeUTF("Something went wrong, please try again");
                    }
                }

                else if(command.equals("leave_group")) {
                    try {
                        String groupName = tokenedcommand.nextToken();
                        
                        if(FileServer.ConnectedChatroom.get(UserName) == null) {
                            dout.writeUTF("You are not part of any group");
                        }
                        else {
                            Chatroom c = null;
                            // int ind = 0;
                            int i = 0;

                            for(i=0;i<FileServer.Chatrooms.size();i++) {
                                if(FileServer.Chatrooms.elementAt(i).name.equals(groupName)) {
                                    c = FileServer.Chatrooms.elementAt(i);
                                    // ind = i;
                                    break;
                                }
                            }

                            if(i == FileServer.Chatrooms.size()) {
                                dout.writeUTF(groupName + " doesn't exist");
                            }
                            else {
                                String name_ = c.name;
                                String outp = FileServer.Chatrooms.elementAt(i).Leave(UserName);
                                c.Notify(UserName + " left the group " + groupName, UserName);
                                if(outp.equals("DEL"))
                                {
                                    FileServer.Chatrooms.remove(c);
                                    c = null;
                                    dout.writeUTF(UserName + " left group " + name_ + '\n' + name_ + " deleted");
                                }
                                else {
                                    dout.writeUTF(outp);
                                }
                                System.out.println(UserName + " left the group " + groupName);
                            }
                        }
                    }
                    catch(Exception e) {
                        dout.writeUTF("Something went wrong, please try again");
                    }
                }

                else if(command.equals("list_detail")) {
                    try {
                        String groupName = tokenedcommand.nextToken();

                        int i = 0;

                        for(i=0;i<FileServer.Chatrooms.size();i++) {
                            if(FileServer.Chatrooms.elementAt(i).name.equals(groupName)) {
                                break;
                            }
                        }

                        if(i == FileServer.Chatrooms.size()) {
                            dout.writeUTF(groupName + " doesn't exist");
                        }
                        else {
                            Vector<String> groupUsers = FileServer.Chatrooms.elementAt(i).Members;

                            if(!groupUsers.contains(UserName)) {
                                dout.writeUTF("You are not a member of the group " + groupName);
                            }

                            else {
                                String outp="";
                                for(i=0;i<groupUsers.size();i++) {
                                    outp = outp + groupUsers.elementAt(i) + "\n";

                                    File directory = new File(groupUsers.elementAt(i));

                                    String path = new File("").getAbsolutePath();
                                    // path = path + '/' + groupUsers.elementAt(i) + '/';
                                    int rootLength = path.length() + groupUsers.elementAt(i).length() + 1;

                                    Set<String> outSet = new HashSet<String>();

                                    outSet = listFilesForFolder(outSet, directory, rootLength);

                                    for(String s : outSet) {
                                        outp = outp + '\t' + s;
                                    }

                                    outp = outp + '\n';
                                }
                                dout.writeUTF(outp);
                            }
                        }
                    }
                    catch(Exception e) {
                        dout.writeUTF("Something went wrong, please try again");
                    }
                }

                else if(command.equals("share_msg")) {
                    try {
                        String groupName = tokenedcommand.nextToken();
                        String message = "\n" + UserName + " :";
                        while(tokenedcommand.hasMoreTokens()) {
                            message = message + " " + tokenedcommand.nextToken();
                        }
                        message = message + "\n";
                        
                        if(FileServer.ConnectedChatroom.get(UserName) == null) {
                            dout.writeUTF("You are not part of any group");
                        }
                        else {
                            Chatroom grp = null;
                            int i = 0;
                            for(i=0;i<FileServer.Chatrooms.size();i++) {
                                if(FileServer.Chatrooms.elementAt(i).name.equals(groupName)) {
                                    break;
                                }
                            }

                            if(i == FileServer.Chatrooms.size()) {
                                dout.writeUTF(groupName + " doesn't exist");
                            }
                            else {
                                FileServer.Chatrooms.elementAt(i).Notify(message, null);
                            }
                        }
                    }
                    catch(Exception e) {
                        dout.writeUTF("Something went wrong, please try again");
                    }
                }

                else if(command.equals("get_file")) {
                    try {
                        String fl = tokenedcommand.nextToken();
                        StringTokenizer name = new StringTokenizer(fl,"/");
                        String groupName = name.nextToken();
                        String user = name.nextToken();
                        String filePath = "";
                        filePath = filePath + name.nextToken();

                        while(name.hasMoreTokens()) {
                            filePath = filePath + '/' + name.nextToken();
                        }

                        StringTokenizer name1 = new StringTokenizer(filePath, "/");
                        String fileName = "";

                        while(name1.hasMoreTokens()) {
                            fileName = name1.nextToken();
                        }

                        fl = fileName;
                        fileName = user + '/' + fileName;

                        int i=0;
                        for(i=0;i<FileServer.Chatrooms.size();i++) {
                            if(FileServer.Chatrooms.elementAt(i).name.equals(groupName)) {
                                break;
                            }
                        }
                        if(i == FileServer.Chatrooms.size()) {
                            dout.writeUTF(groupName + " doesn't exist");
                        }
                        else {
                            Vector<String> grpMembers = FileServer.Chatrooms.elementAt(i).Members;

                            if(!grpMembers.contains(user)) {
                                dout.writeUTF("The group " + groupName + " doesn't have the user " + user);
                            }
                            else {
                                int size = 1024;

                                File file = new File(user + '/' + filePath);
                                FileInputStream fpin = new FileInputStream(file);
                                BufferedInputStream bpin = new BufferedInputStream(fpin);

                                long fileLength = file.length(), current=0, start=System.nanoTime();
                                dout.writeUTF("FILE " + fl + " " + user + " " + Long.toString(fileLength));

                                while(current!=fileLength) {
                                    if(fileLength - current >= size) {
                                        current += size;
                                    }
                                    else {
                                        size = (int)(fileLength-current);
                                        current=fileLength;
                                    }

                                    byte[] file_contents = new byte[size];
                                    bpin.read(file_contents, 0, size);
                                    dout.write(file_contents);
                                }
                            }
                        }
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }
                }

                else {
                    dout.writeUTF("Unrecognised command");
                }

            }
            catch(Exception e) {

                e.printStackTrace(System.out);
                break;

            }
        }
    }

    private Set<String> listFilesForFolder(Set<String> set, File folder, int rootLength) {
        for (File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory() && !fileEntry.isFile()) {
                // set.add(fileEntry.getAbsolutePath().substring(rootLength + 1) + '/');
                set = listFilesForFolder(set, fileEntry, rootLength);
            } else {
                String relPath = fileEntry.getAbsolutePath();
                relPath = relPath.substring(rootLength + 1);
                set.add(relPath);
                // System.out.println(fileEntry.getName());
            }
        }
        return set;
    }

}

class Chatroom {

    Vector<String> Members = new Vector<String>();
    String name;

    Chatroom (String name,String member) {

        this.name = name;
        this.Members.add(member);
        FileServer.ConnectedChatroom.put(member,this);

    }

    public String Leave (String member) {

        this.Members.remove(member);
        FileServer.ConnectedChatroom.put(member,null);
        if(this.Members.isEmpty()) {
            return ("DEL");
        }
        else {
            return("You left group "+this.name);
        }

    }

    public void Notify(String msg, String no_notif) {

        for(int i=0;i<this.Members.size();i++) {
            if(!this.Members.elementAt(i).equals(no_notif)) {
                try {
                    Socket sendSoc = FileServer.ClientSockets.elementAt(FileServer.UserNames.indexOf(this.Members.elementAt(i)));
                    DataOutputStream senddout = new DataOutputStream(sendSoc.getOutputStream());
                    senddout.writeUTF(msg);
                }
                catch(Exception e) {
                    int ii=0;
                }
            }
        }

    }
}
