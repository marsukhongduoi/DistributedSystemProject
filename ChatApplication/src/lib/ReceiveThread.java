/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lib;

import com.sun.org.apache.bcel.internal.util.ByteSequence;
import gui.ChatClient;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.*;
import java.net.Socket;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import model.Participant;

/**
 *
 * @author Titan
 */
public class ReceiveThread implements Runnable {

    private Socket sock;

    private CryptoData cryptoData;

    private JComboBox jcomboUser;
    private JTextArea txtHistory;

    private ChatClient chatClient;

    private Map<String, Participant> userList;

    public ReceiveThread(ChatClient chatClient, JComboBox jcomboUser, Socket sock, JTextArea txtHistory, CryptoData cryptoData) {
        //this.chatClient = chatClient;
        this.sock = sock;
        this.txtHistory = txtHistory;
        this.cryptoData = cryptoData;
        this.chatClient = chatClient;
        this.jcomboUser = jcomboUser;
    }

    String formatFileSize(long size) {
        String hrsize = null;
        double b = size;
        double kb = size / 1024.0;
        double mb = (size / 1024.0) / 1024.0;
        double gb = ((size / 1024.0) / 1024.0) / 1024.0;
        double tb = (((size / 1024.0) / 1024.0) / 1024.0) / 1024.0;
        DecimalFormat dec = new DecimalFormat("0.00");
        if (tb > 1) {
            hrsize = dec.format(tb).concat(" TB");
        } else if (gb > 1) {
            hrsize = dec.format(gb).concat(" GB");
        } else if (mb > 1) {
            hrsize = dec.format(mb).concat(" MB");
        } else if (kb > 1) {
            hrsize = dec.format(kb).concat(" KB");
        } else {
            hrsize = dec.format(b).concat(" Bytes");
        }
        return hrsize;
    }

    String getCurrentDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        Calendar calendar = Calendar.getInstance();
        return dateFormat.format(calendar.getTime());
    }

    private void setUsernameJCombobox(Map<String, Participant> userList) {
        Participant participant = null;
        if (!userList.isEmpty()) {
            jcomboUser.removeAllItems();
            Iterator it = userList.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                participant = (Participant) pair.getValue();
                jcomboUser.addItem(participant.getUsername());
            }
        }
    }
    
    private void createDir(String path) {
        File file = new File(path);
        file.mkdir();
    }
    
    @Override
    public void run() {
        try {
            BufferedOutputStream bufferedOutputStream = null;
            BufferedInputStream bufferedInputStream = new BufferedInputStream(sock.getInputStream());

            DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);

            ObjectInputStream objectInputStream = null;

            FileOutputStream fileOutputStream = null;
            FileInputStream fileInputStream = null;

            File receivedFile = null;
            File decryptFile = null;

            String username = "";
            String filename = "";
            String message = "";
            String chatContent = "";
            String note = "";
            String mode = dataInputStream.readUTF();
            String decryptedFileName = "";
            String receivedDir = "";
            String decryptDir = "";

            String[] chatContentSplit;

            long filesize = 0;
            int keyLength = -1;
            
            byte[] keyBytes = {};

            switch (mode) {
                case "FILE":
                    filesize = dataInputStream.readLong();
                    filename = dataInputStream.readUTF();
                    receivedDir = "Users\\" + chatClient.getUsername() + "\\receivedFiles";
                    decryptDir = "Users\\" + chatClient.getUsername() + "\\decryptedFiles";
                    createDir(receivedDir);
                    createDir(decryptDir);
                    receivedFile = new File(receivedDir + "\\" + filename); 
                    fileOutputStream = new FileOutputStream(receivedFile);
                    bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                    for (int i = 0; i < filesize; i++) {
                        bufferedOutputStream.write(bufferedInputStream.read());
                    }
                    bufferedOutputStream.flush();
                    
                    cryptoData.decryptFile(receivedFile, new File(decryptDir + "\\" + filename.split(".enc")[0]));
                    txtHistory.append("Received " + filename + " " + formatFileSize(filesize) + "\n");
                    txtHistory.setCaretPosition(txtHistory.getDocument().getLength());
                    sock.close();
                    Thread.interrupted();
                    break;
                case "MESS":
                    chatContent = dataInputStream.readUTF();
                    chatContentSplit = chatContent.split(": ");
                    username = chatContentSplit[0];
                    message = chatContentSplit[1];
                    txtHistory.append(chatContent);
                    txtHistory.setCaretPosition(txtHistory.getDocument().getLength());
                    sock.close();
                    Thread.interrupted();
                    break;
                case "LIST":
                    objectInputStream = new ObjectInputStream(bufferedInputStream);
                    Map<String, Participant> userList = (Map<String, Participant>) objectInputStream.readObject();
                    objectInputStream.close();
                    this.userList = userList;
                    chatClient.setUserList(userList);
                    setUsernameJCombobox(userList);
                    sock.close();
                    Thread.interrupted();
                    break;
                case "REQUEST":
                    username = dataInputStream.readUTF();
                    keyLength = dataInputStream.readInt();
                    keyBytes = new byte[keyLength];
                    dataInputStream.readFully(keyBytes, 0, keyLength);
                    chatClient.setParticipantKey(keyBytes);
                    txtHistory.append("Get request from " + username + "\n");
                    break;
            }
        } catch (Exception ex) {

        }
    }

}
