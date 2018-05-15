/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lib;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JTable;
import javax.swing.JTextArea;
import model.*;

public class ServerThread implements Runnable {

    private ServerSocket serverSocket;
    private CryptoData hostCrypto = null;
    private Map<String, Participant> userList;
    private JTextArea JNotify;
    private JTable JTblUser;

    public ServerThread() {

    }

    public ServerThread(ServerSocket serverSocket, JTextArea JNotify, JTable JTblUser, CryptoData hostCrypto) {
        this.JNotify = JNotify;
        this.JTblUser = JTblUser;
        this.serverSocket = serverSocket;
        this.hostCrypto = hostCrypto;
        userList = new HashMap();
    }

    public void removeUser(String username) {
        userList.remove(username);
    }

    public void setUserList(String username, Participant p) {
        this.userList.put(username, p);
    }

    public Map<String, Participant> getUserList() {
        return userList;
    }

    public Participant getSpecificUser(String username) {
        return userList.get(username);
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket sock = serverSocket.accept();
                new Thread(new TransferThread(sock, this, this.JNotify, this.JTblUser, hostCrypto)).start();
            } catch (IOException ex) {

            }
        }
    }

}
