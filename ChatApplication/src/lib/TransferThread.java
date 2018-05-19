/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lib;

import gui.ChatServer;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Iterator;
import java.util.Map;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import model.*;

/**
 *
 * @author Titan
 */
public class TransferThread implements Runnable {

    private Socket sock;

    private ServerThread serverThread;

    private JTextArea JNotify;
    private JTable JTblUser;

    private CryptoData hostCrypto;
    private ChatServer chatServer;

    public TransferThread() {
    }

    public TransferThread(Socket sock, ServerThread serverThread, JTextArea JNotify, JTable JTblUser, CryptoData hostCrypto, ChatServer chatServer) {
        this.sock = sock;
        this.serverThread = serverThread;
        this.JNotify = JNotify;
        this.JTblUser = JTblUser;
        this.hostCrypto = hostCrypto;
        this.chatServer = chatServer;
    }

    private void sendMess(String message, Socket s) {
        try {
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(s.getOutputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);

            dataOutputStream.writeUTF("MESS");
            dataOutputStream.writeUTF(message);
            dataOutputStream.flush();
        } catch (IOException ex) {
            System.out.print("Exception in sendMess TransferThread: ");
            System.out.println(ex);
        }
    }

    private void BroadcastMessage(String message, String username) {
        Map<String, Participant> userList = chatServer.getUserList();
        Participant participant = null;
        Socket socket = null;

        if (!userList.isEmpty()) {
            Iterator it = userList.entrySet().iterator();
            while (it.hasNext()) {
                try {
                    Map.Entry pair = (Map.Entry) it.next();
                    participant = (Participant) pair.getValue();
                    socket = new Socket(participant.getIp(), participant.getPort());
                    if (!pair.getKey().toString().equals(username)) {
                        sendMess(message, socket);
                    }
                } catch (IOException ex) {

                }
            }

            if (userList.containsKey(username)) {
                participant = (Participant) userList.get(username);
                try {
                    socket = new Socket(participant.getIp(), participant.getPort());
                } catch (IOException ex) {

                }
                sendMess(": Enter '/c GET LIST' if you want to update your participant list\nEnter '/?' to get help\n", socket);
            }
        }
    }

    private void sendObject(Socket sock, Object obj) {
        try {
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(sock.getOutputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);
            dataOutputStream.writeUTF("LIST");

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream);
            objectOutputStream.writeObject(obj);
            objectOutputStream.close();
            dataOutputStream.close();
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }

    void sendFile(File f, String mode, Socket socket) {
        try {
            int theByte = 0;
            long filesize = f.length();
            String fileName = f.getName();

            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);
            FileInputStream fileInputStream = new FileInputStream(f);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

            dataOutputStream.writeUTF(mode);
            dataOutputStream.writeLong(filesize);
            dataOutputStream.writeUTF(fileName);

            while ((theByte = bufferedInputStream.read()) != -1) {
                bufferedOutputStream.write(theByte);
            }
            bufferedOutputStream.flush();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, ex);
            return;
        }
    }

    private void addInfoToTable(Participant p) {
        DefaultTableModel model = (DefaultTableModel) JTblUser.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            String username = model.getValueAt(0, i).toString();
            if (username.equals(p.getUsername())) {
                model.removeRow(i);
            }
        }
        Object[] row = {p.getUsername(), p.getIp(), p.getPort(), p.getFileShare().getName()};
        model.addRow(row);
    }

    private void removeInfoFromTable(String username) {
        DefaultTableModel model = (DefaultTableModel) JTblUser.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            String user = model.getValueAt(0, i).toString();
            if (username.equals(username)) {
                model.removeRow(i);
            }
        }
    }

    private void getList(String username) {
        try {
            Map<String, Participant> userList = chatServer.getUserList();
            Participant participant = userList.get(username);
            Socket socket = new Socket(participant.getIp(), participant.getPort());
            sendObject(socket, userList);
            JNotify.append("Sending userlist to " + username + "\n");
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }

    private boolean checkUserExists(String username) {
        Map<String, Participant> userList = chatServer.getUserList();
        Iterator it = userList.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            Participant participant = (Participant) pair.getValue();
            if (participant.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    private void createDir(String path) {
        File file = new File(path);
        file.mkdirs();
    }

    PublicKey setParticipantKey(byte[] keyBytes) {
        PublicKey pubKey = null;
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (NoSuchAlgorithmException ex) {

        } catch (InvalidKeySpecException ex) {

        }
        return pubKey;
    }

    private String decryptMessage(DataInputStream dataInputStream) {
        String messageStr = "";
        try {
            int messageLength = -1;

            byte[] messageEnc = {};
            byte[] messageDec = {};

            messageLength = dataInputStream.readInt();
            messageEnc = new byte[messageLength];
            dataInputStream.readFully(messageEnc, 0, messageLength);
            messageDec = hostCrypto.decryptText(messageEnc);
            messageStr = new String(messageDec);
            return messageStr;
        } catch (IOException ex) {
            System.out.println(ex);
        } catch (NoSuchAlgorithmException ex) {
            System.out.println(ex);
        } catch (NoSuchPaddingException ex) {
            System.out.println(ex);
        } catch (InvalidKeyException ex) {
            System.out.println(ex);
        } catch (IllegalBlockSizeException ex) {
            System.out.println(ex);
        } catch (BadPaddingException ex) {
            System.out.println(ex);
        }
        return messageStr;
    }

    @Override
    public void run() {
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(sock.getInputStream());
            BufferedOutputStream bufferedOutputStream = null;

            DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);

            File file = null;
            File receivedFile = null;
            File encryptedFile = null;

            FileOutputStream fileOutputStream = null;

            String mode = dataInputStream.readUTF().toUpperCase();
            String username = "";
            String messageStr = "";
            String info = "";
            String fileName = "";
            String ip = "";
            String receivedDir = "Server\\file\\receivedFiles";
            String decryptDir = "Server\\file\\decryptedFiles";

            String[] messageSplit = null;
            String[] infoSplit = null;

            Participant specPart = null;

            PublicKey participantKey = null;

            int port = -1;
            int keyLength = -1;
            int messageLength = -1;
            long fileSize = 0;

            byte[] messageEnc;
            byte[] messageDec;
            byte[] keyBytes;

            switch (mode) {
                case "MESS":
                    //crypto
                    messageStr = decryptMessage(dataInputStream);
                    //end crypto here

                    messageSplit = messageStr.split(": ");
                    username = messageSplit[0];
                    info = messageSplit[1];
                    switch (info.toUpperCase()) {
                        case "/C GET LIST\n":
                            getList(username);
                            break;
                    }
                    Thread.interrupted();
                    break;

                case "INFORMATION":
                    //crypto
                    messageStr = decryptMessage(dataInputStream);
                    //end crypto here

                    messageSplit = messageStr.split(": ");
                    username = messageSplit[0];
                    info = messageSplit[1];
                    infoSplit = info.split("-");
                    ip = infoSplit[0];
                    port = Integer.parseInt(infoSplit[1]);
                    fileName = infoSplit[2];
                    file = new File(fileName);
                    Participant participant = new Participant(username, ip, port, file);
                    chatServer.setUserList(username, participant);
                    addInfoToTable(participant);
                    JNotify.append(username + " has connected\n");
                    BroadcastMessage(username + ": has connnected\nEnter '/c GET LIST' if you want to update your participant list\n", username);
                    Thread.interrupted();
                    break;

                case "SHUTDOWN":
                    messageStr = decryptMessage(dataInputStream);
                    chatServer.removeUser(messageStr);
                    removeInfoFromTable(messageStr);
                    BroadcastMessage(messageStr + ": has disconnnected\nEnter '/c GET LIST' if you want to update your participant list\n", messageStr);
                    JNotify.append(messageStr + " has disconnected\n");
                    Thread.interrupted();
                    break;

                case "FILE":
                    fileSize = dataInputStream.readLong();
                    fileName = dataInputStream.readUTF();
                    createDir(receivedDir);
                    createDir(decryptDir);
                    receivedFile = new File(receivedDir + "\\" + fileName);
                    fileOutputStream = new FileOutputStream(receivedFile);
                    bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                    for (int i = 0; i < fileSize; i++) {
                        bufferedOutputStream.write(bufferedInputStream.read());
                    }
                    bufferedOutputStream.flush();

                    hostCrypto.decryptFile(receivedFile, new File(decryptDir + "\\" + fileName.split(".enc")[0]));
                    sock.close();
                    Thread.interrupted();
                case "REQUEST":
                    username = dataInputStream.readUTF();
                    fileName = dataInputStream.readUTF();
                    keyLength = dataInputStream.readInt();
                    keyBytes = new byte[keyLength];
                    dataInputStream.readFully(keyBytes, 0, keyLength);
                    participantKey = setParticipantKey(keyBytes);
                    JNotify.append("Get request from " + username + "\n");
                    file = new File(decryptDir + "\\" + fileName);
                    encryptedFile = hostCrypto.encryptFile(participantKey, file);
                    specPart = chatServer.getSpecificUser(username);
                    sendFile(encryptedFile, "FILE", new Socket(specPart.getIp(), specPart.getPort()));
                    JNotify.append("Sending request file to " + username + "\n");
                    break;
            }
        } catch (IOException ex) {
            System.out.println(ex);
        } catch (NoSuchAlgorithmException ex) {
            System.out.println(ex);
        } catch (NoSuchPaddingException ex) {
            System.out.println(ex);
        } catch (InvalidKeyException ex) {
            System.out.println(ex);
        } catch (IllegalBlockSizeException ex) {
            System.out.println(ex);
        } catch (BadPaddingException ex) {
            System.out.println(ex);
        }
    }
}
