/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lib;

import gui.ChatClient;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import model.Participant;

/**
 *
 * @author Daniel
 */
public class ClientThread implements Runnable {
    
    private ServerSocket serverSock;
    private Socket sock;
    
    
    private CryptoData cryptoData;
    
    private ChatClient chatClient;
    
    private JTextArea txtHistory;
    private JComboBox jcomboUser;
    
    private Map<String, Participant> userList;
    
    public ClientThread() {
    }

    public ClientThread(ChatClient chatClient, JComboBox jcomboUser, ServerSocket serverSock, JTextArea txtHistory, CryptoData cryptoData) {
        this.serverSock = serverSock;
        
        this.txtHistory = txtHistory;
        this.jcomboUser = jcomboUser;
        
        this.cryptoData = cryptoData;
        
        this.chatClient = chatClient;
    }
    
    @Override
    public void run() {
        while(true) {
            try {
                sock = serverSock.accept();
                new Thread(new ReceiveThread(chatClient, jcomboUser, sock, txtHistory, cryptoData)).start();
            } catch (IOException ex) {
            }
        }
    }
}
