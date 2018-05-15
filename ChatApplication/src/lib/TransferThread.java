/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lib;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Map;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import model.*;

/**
 *
 * @author Daniel
 */
public class TransferThread implements Runnable {

    private Socket sock;

    private ServerThread serverThread;

    private JTextArea JNotify;
    private JTable JTblUser;

    private CryptoData hostCrypto;

    public TransferThread() {
    }

    public TransferThread(Socket sock, ServerThread serverThread, JTextArea JNotify, JTable JTblUser, CryptoData hostCrypto) {
        this.sock = sock;
        this.serverThread = serverThread;
        this.JNotify = JNotify;
        this.JTblUser = JTblUser;
        this.hostCrypto = hostCrypto;
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

//    private void sendPulicKey(Socket sock, String username) {
//        try {
//            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(sock.getOutputStream());
//            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);
//            dataOutputStream.writeUTF("KEY");
//            dataOutputStream.writeInt(hostCrypto.getPublickey().getEncoded().length);
//            dataOutputStream.write(hostCrypto.getPublickey().getEncoded());
//            JNotify.append("Sending public key to " + username + "\n");
//            dataOutputStream.close();
//        } catch (IOException ex) {
//
//        }
//    }

    private void BroadcastMessage(String message, String username) {
        Map<String, Participant> userList = serverThread.getUserList();
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
            Map<String, Participant> userList = serverThread.getUserList();
            Participant participant = userList.get(username);
            Socket socket = new Socket(participant.getIp(), participant.getPort());
            sendObject(socket, userList);
            JNotify.append("Sending userlist to " + username + "\n");
        } catch (IOException ex) {
            System.out.println(ex);
        }
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

            DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);

            File file = null;

            String mode = dataInputStream.readUTF().toUpperCase();
            String username = "";
            String messageStr = "";
            String info = "";
            String fileName = "";
            String ip = "";

            String[] messageSplit = null;
            String[] infoSplit = null;

            int port = -1;
            int messageLength = -1;
            long fileSize = 0;

            byte[] messageEnc;
            byte[] messageDec;

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
                    serverThread.setUserList(username, participant);
                    addInfoToTable(participant);
                    JNotify.append(username + " has connected\n");
                    BroadcastMessage(username + ": has connnected\nEnter '/c GET LIST' if you want to update your participant list\n", username);
                    Thread.interrupted();
                    break;
                case "SHUTDOWN":
                    messageStr = decryptMessage(dataInputStream);
                    serverThread.removeUser(messageStr);
                    removeInfoFromTable(messageStr);
                    BroadcastMessage(messageStr + ": has disconnnected\nEnter '/c GET LIST' if you want to update your participant list\n", messageStr);
                    JNotify.append(messageStr + " has disconnected\n");
                    Thread.interrupted();
                    break;
            }
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }
}
