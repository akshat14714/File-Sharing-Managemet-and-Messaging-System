import java.io.* ;
import java.net.* ;
import java.nio.file.Files;
import java.nio.file.*;
import java.text.*;
import java.util.*;
import java.awt.* ;

public class Client {
    public static String ip="127.0.0.1";
    public static int port = 5001;
    public static DatagramSocket clientSocUDP;
    public static void main(String args[]) {
        try {
            Socket clientSoc;
            DataInputStream din;
            DataOutputStream dout;
            String UserName;
            clientSocUDP = new DatagramSocket();
            
            clientSoc = new Socket(ip,5000) ;
            System.out.println("Connected to Server at localhost Port-5000(TCP)");
            
            din = new DataInputStream(clientSoc.getInputStream());
            dout = new DataOutputStream(clientSoc.getOutputStream());
            
            String a = "hello";
            byte[] file_contents = new byte[1000];
            file_contents = a.getBytes();
            DatagramPacket initial = new DatagramPacket(file_contents,file_contents.length,InetAddress.getByName(ip),port);

            clientSocUDP.send(initial);

            //Send messages
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            String inputLine=null;
            
            while(true)
            {
                try
                {
                    inputLine = bufferedReader.readLine();
                    dout.writeUTF(inputLine);
                    
                    StringTokenizer tokenedcommand = new StringTokenizer(inputLine);
                    String comm;

                    comm = tokenedcommand.nextToken();

                    if(comm.equals("create_user")) {
                        UserName = tokenedcommand.nextToken();
                        new Thread(new RecievedMessagesHandler(din,UserName)).start();
                    }

                    else if(comm.equals("upload")) {

                        try {

                            if(tokenedcommand.hasMoreTokens()) {
                                String filePath = tokenedcommand.nextToken();
                                
                                File file = new File(filePath);
                                FileInputStream fpin = new FileInputStream(file);
                                BufferedInputStream bpin = new BufferedInputStream(fpin);
                                
                                long fileLength=file.length(), current=0;
                                
                                dout.writeUTF("LENGTH " + fileLength);
                                
                                while(current != fileLength) {

                                    int size = 1024;
                                    
                                    if(fileLength - current >= size) {
                                        current += size;
                                    }
                                    else {
                                        size = (int)(fileLength-current);
                                        current=fileLength;
                                    }

                                    file_contents = new byte[size];
                                    bpin.read(file_contents, 0, size);
                                    dout.write(file_contents);
                                    
                                }
                                System.out.println("File Sent");
                            }
                            else {
                                System.out.println("Give the path of the file that you want to upload");
                            }

                        }
                        catch(Exception e) {
                            dout.writeUTF("Something went wrong, please try again");
                        }

                    }

                    else if(comm.equals("upload_udp")) {

                        try {

                            if(tokenedcommand.hasMoreTokens()) {

                                int size = 1024;

                                String filePath = tokenedcommand.nextToken();

                                File file = new File(filePath);
                                FileInputStream fpin = new FileInputStream(file);
                                BufferedInputStream bpin = new BufferedInputStream(fpin);

                                long fileLength = file.length(), current=0;
                                dout.writeUTF("LENGTH "+fileLength);

                                while(current!=fileLength) {

                                    if(fileLength - current >= size) {
                                        current = current + size;
                                    }
                                    else {
                                        size = (int)(fileLength - current);
                                        current = fileLength;
                                    }

                                    file_contents = new byte[size];
                                    bpin.read(file_contents,0,size);
                                    DatagramPacket sendPacket = new DatagramPacket(file_contents,size,InetAddress.getByName(ip),port);
                                    clientSocUDP.send(sendPacket);
                                }
                                System.out.println("File Sent");
                            }
                            else {
                                System.out.println("Give the path of the file that you want to upload");
                            }
                        }
                        catch(Exception e) {
                            dout.writeUTF("Something went wrong, please try again");
                        }
                    }
                }
                catch(Exception e){
                    System.out.println(e);
                    break;
                }
            }
        }
        catch(Exception e){
            System.out.println(e);
            System.exit(0);
        }

    }

}


class RecievedMessagesHandler implements Runnable {

    private String UserName;
    private DataInputStream server;
    
    public RecievedMessagesHandler(DataInputStream server,String UserName) {
        
        this.UserName = UserName;
        this.server = server;
    
    }

    @Override
    public void run() {
        
        String inputLine = null;

        while(true) {

            try {
                inputLine=server.readUTF();
                StringTokenizer st = new StringTokenizer(inputLine);

                if(st.nextToken().equals("FILE")) {
                    
                    String fileName = st.nextToken();
                    String user = st.nextToken();
                    int fileLength = Integer.parseInt(st.nextToken());

                    System.out.println("Recieving file "+fileName);

                    File newDir = new File(user);

                    if(newDir.exists()) {
                        int po=1;
                    }
                    else {
                        newDir.mkdir();
                    }
                    
                    byte[] file_contents = new byte[1000];
                    
                    FileOutputStream fpout = new FileOutputStream(user + '/' + fileName);
                    BufferedOutputStream bpout = new BufferedOutputStream(fpout);
                    DatagramPacket receivePacket;
                    int bytesRead = 0;
                    int size = 1000;

                    if(size > fileLength) {
                        size = fileLength;
                    }

                    while((bytesRead=server.read(file_contents,0,size))!=-1 && fileLength>0) {
                        bpout.write(file_contents,0,size);
                        fileLength-=size; if(size>fileLength)size=fileLength;
                    }

                    bpout.flush();
                    System.out.println("File Recieved");
                }
                else {
                    System.out.println(inputLine);
                }
            }
            catch(Exception e) {
                e.printStackTrace(System.out); 
                break;
            }
        }
    }
}
