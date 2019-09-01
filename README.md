# Distributed Systems Assignment-2 : File Sharing and Management System

Name - Akshat Maheshwari  
Roll Number - 20161024

## Running Server

Go to the ```Server``` directory, and run the following commands:  

        javac FileServer.java
        java FileServer <maximum number of clients>

The maximum number of clients that can be connected to the server has to be mentioned as the argument while running the server.

## Running Client

Go to the ```Client``` directory, and run the following command:

    javac Client.java
    java Client

## Things to be noted while running client

The local IP address of the server has to be given as a string in the ```ip``` variable in the Client.java file before compilation.

## Commands Implemented and Their Usage

### Account Creation

- ```create_user <username>``` : This will create a user in the server, and a folder with the same name as the user will be created in the server. 2 users with the same username are not allowed in the system.

### File Management

- ```upload <filename>``` : This command will uplaod any file that the user wants to uplaod on the server using TCP. The file will be stored in the folder of the user on the server that was created at the time of user creation.

- ```upload_udp <filename>``` : This command will upload any file that the user wants to upload on the server using UDP, and the file will be stored in the folder of the user on the server.

- ```create_folder <foldername>``` : This command will create a folder on the server. The folder will be created inside the folder of the user.

- ```move_file <source_path> <dest_path>``` : This command will move the file from the source location to the destination location. The source path and destination paths should be of the form  **<user_name>/<relative_path_to_file in user folder on server>**

### Group Management

- ```create_group <groupname>``` : This command will create a group with the name groupname.

- ```list_groups``` : This command will list all the groups present in the system.

- ```join_group <groupname>``` : This command will make the user join the group, groupname.

- ```leave_group <groupname>``` : This commad will remove the user from the group, groupname.

- ```list_detail <groupname>``` : This command will list all the details of the group, groupname, i.e., all the users and all the files that the users have already uploaded on the server.

- ```share_msg <groupname> <message>``` : This command will share the given message with all the members of the group groupname. You can also see which user has sent the message.

- ```get_file <groupname>/<username>/<file_path>``` : This command is used to get any of the uploaded file by the user from the user username, if they both belong to the same group.

*Note : Please try to keep the arguments as per the readme, because they might give errors if the arguments are given incorrectly*