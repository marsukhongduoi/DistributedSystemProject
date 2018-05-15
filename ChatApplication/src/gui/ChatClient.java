/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import lib.ClientThread;
import lib.CryptoData;
import model.Participant;

/**
 *
 * @author Daniel
 */
public class ChatClient extends javax.swing.JFrame {

    Socket mainSock = null;
    ServerSocket servSock = null;

    File fileShare;

    String username = "";
    String path = "";

    ChatServer server;

    CryptoData hostCrypto;
    CryptoData guestCrypto;

    Thread t;

    PublicKey serverKey;
    PublicKey participantKey;

    Map<String, Participant> userList;

    public ChatClient() {
        initComponents();
        guestCrypto = new CryptoData(2048);
        txtHistory.setLineWrap(true);
        userList = new HashMap<String, Participant>();
        setServerButtonState(true);
        setClientButtonState(true);
    }
    
    public void setParticipantKey(byte[] keyBytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            participantKey = keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (NoSuchAlgorithmException ex) {

        } catch (InvalidKeySpecException ex) {

        }
    }

    public void setUsername(String username) {
        this.username = username;
        lbusername.setText(username);
        path = "Users\\" + username + "\\key";
        createUserDir();
    }

    public void setUserList(String username, Participant participant) {
        userList.put(username, participant);
    }

    public void setUserList(Map<String, Participant> userList) {
        this.userList = userList;
    }

    public Participant getParticipant(String username) {
        return (Participant) userList.get(username);
    }
    
    public String getUsername() {
        return username;
    } 

    void setServerButtonState(boolean bState) {
        btnServer.setEnabled(bState);
        btnDisconnet.setEnabled(!bState);
    }

    void setClientButtonState(boolean bState) {
        btnListen.setEnabled(bState);
        btnShutdown.setEnabled(!bState);
    }

    void setSendingControlState(boolean bState) {
        btnBrowse.setEnabled(bState);
        btnFileShare.setEnabled(bState);
        btnSendInfo.setEnabled(bState);
        txtChat.setEnabled(bState);
    }

    private void writeInformationToFile(File file, String information) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
            bufferedWriter.write(information);
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException ex) {

        }
    }

    private void createUserDir() {
        path = "Users\\" + lbusername.getText() + "\\key";
        File userDir = new File(path);
        if (!userDir.exists()) {
            if (userDir.mkdirs()) {
                return;
            }
        }
    }

    private void readPubKeyFromFile(String filename) {
        byte[] pubKeyByte = new byte[300];
        File keyFile = new File(path + "\\" + filename);
        try {
            FileInputStream fileInputStream = new FileInputStream(keyFile);
            fileInputStream.read(pubKeyByte);
            fileInputStream.close();

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            serverKey = keyFactory.generatePublic(new X509EncodedKeySpec(pubKeyByte));
        } catch (FileNotFoundException ex) {

        } catch (IOException ex) {

        } catch (NoSuchAlgorithmException ex) {

        } catch (InvalidKeySpecException ex) {

        }
    }

    void notifyQuit() {
        try {
            mainSock = new Socket(txtSIP.getText(), Integer.parseInt(txtSPort.getText()));
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(mainSock.getOutputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);
            byte[] notifyEnc = {};
            try {
                notifyEnc = guestCrypto.encryptText(serverKey, username);
            } catch (InvalidKeyException ex) {
                System.out.println(ex);
            } catch (NoSuchAlgorithmException ex) {
                System.out.println(ex);
            } catch (NoSuchPaddingException ex) {
                System.out.println(ex);
            } catch (IllegalBlockSizeException ex) {
                System.out.println(ex);
            } catch (BadPaddingException ex) {
                System.out.println(ex);
            }

            dataOutputStream.writeUTF("SHUTDOWN");
            dataOutputStream.writeInt(notifyEnc.length);
            dataOutputStream.write(notifyEnc);
            dataOutputStream.flush();

            txtChat.setText("");
            txtHistory.append("<Shutdown>");
            txtHistory.setCaretPosition(txtHistory.getDocument().getLength());

            mainSock.close();
            if (mainSock.isClosed()) {
                setClientButtonState(true);
            }
        } catch (IOException ex) {

        }
    }

    void sendMess(String mode, String message) {
        try {
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(mainSock.getOutputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);
            readPubKeyFromFile("serverpubkey.txt");
            byte[] messageEnc = guestCrypto.encryptText(serverKey, message);
            dataOutputStream.writeUTF(mode);
            dataOutputStream.writeInt(messageEnc.length);
            dataOutputStream.write(messageEnc);
            dataOutputStream.flush();

            txtChat.setText("");
            txtHistory.append(message + "\n");
            txtHistory.setCaretPosition(txtHistory.getDocument().getLength());

            mainSock.close();
            setServerButtonState(true);
            btnConnecting.setEnabled(true);
            setSendingControlState(false);
        } catch (IOException ex) {

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

    void sendInfomation() {
        try {
            if (fileShare == null) {
                JOptionPane.showMessageDialog(null, "Please choose your file to share");
                return;
            }
            String info = username + ": " + txtYIP.getText() + "-" + txtYPort.getText() + "-" + fileShare.getName();
            sendMess("INFORMATION", info);
            mainSock.close();
            setServerButtonState(true);
            btnConnecting.setEnabled(true);
            setSendingControlState(false);
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }

    void sendFile(File f, String mode) {
        try {
            int theByte = 0;
            long filesize = f.length();
            String name = f.getName();

            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(mainSock.getOutputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);
            FileInputStream fileInputStream = new FileInputStream(f);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

            dataOutputStream.writeUTF(mode);
            dataOutputStream.writeLong(filesize);
            dataOutputStream.writeUTF(name);

            while ((theByte = bufferedInputStream.read()) != -1) {
                bufferedOutputStream.write(theByte);
            }
            bufferedOutputStream.flush();

            txtHistory.append("Sending file " + name);
            txtHistory.setCaretPosition(txtHistory.getDocument().getLength());
            setSendingControlState(false);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, ex);
            return;
        }
    }

    private void sendPulicKey(String username) {
        try {
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(mainSock.getOutputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);
            dataOutputStream.writeUTF("REQUEST");
            dataOutputStream.writeUTF(username);
            dataOutputStream.writeInt(hostCrypto.getPublickey().getEncoded().length);
            dataOutputStream.write(hostCrypto.getPublickey().getEncoded());
            txtHistory.append("Sending request\n");
            dataOutputStream.close();
            setSendingControlState(false);
        } catch (IOException ex) {

        }
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

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        btnBrowse = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtChat = new javax.swing.JTextArea();
        btnSendInfo = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        btnFileShare = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtHistory = new javax.swing.JTextArea();
        jLabel3 = new javax.swing.JLabel();
        lbusername = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        txtSIP = new javax.swing.JTextField();
        txtSPort = new javax.swing.JTextField();
        btnServer = new javax.swing.JButton();
        btnDisconnet = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        txtYIP = new javax.swing.JTextField();
        txtYPort = new javax.swing.JTextField();
        btnListen = new javax.swing.JButton();
        btnShutdown = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jcbUser = new javax.swing.JComboBox<String>();
        btnConnecting = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        lbFileName = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Message box"));

        btnBrowse.setText("Send file");
        btnBrowse.setEnabled(false);
        btnBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseActionPerformed(evt);
            }
        });

        txtChat.setColumns(20);
        txtChat.setRows(5);
        txtChat.setEnabled(false);
        txtChat.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtChatKeyTyped(evt);
            }
        });
        jScrollPane2.setViewportView(txtChat);

        btnSendInfo.setText("Send Information");
        btnSendInfo.setEnabled(false);
        btnSendInfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSendInfoActionPerformed(evt);
            }
        });

        jLabel7.setText("(Press 'Enter' to send message)");
        jLabel7.setEnabled(false);

        btnFileShare.setText("File to share");
        btnFileShare.setEnabled(false);
        btnFileShare.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFileShareActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(btnBrowse)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSendInfo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnFileShare)
                .addContainerGap(44, Short.MAX_VALUE))
            .addComponent(jScrollPane2)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jLabel7))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnFileShare)
                    .addComponent(btnSendInfo)
                    .addComponent(btnBrowse))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("History"));

        txtHistory.setColumns(20);
        txtHistory.setRows(5);
        txtHistory.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtHistoryKeyTyped(evt);
            }
        });
        jScrollPane1.setViewportView(txtHistory);

        jLabel3.setText("username: ");

        lbusername.setText("jLabel5");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lbusername)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(lbusername))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Server"));

        jLabel1.setText("IP:");

        jLabel2.setText("Port:");

        txtSIP.setText("127.0.0.1");
        txtSIP.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtSIPKeyTyped(evt);
            }
        });

        txtSPort.setText("3333");
        txtSPort.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtSPortKeyTyped(evt);
            }
        });

        btnServer.setText("Connect");
        btnServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnServerActionPerformed(evt);
            }
        });

        btnDisconnet.setText("Disconnect");
        btnDisconnet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDisconnetActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtSIP, javax.swing.GroupLayout.DEFAULT_SIZE, 88, Short.MAX_VALUE)
                    .addComponent(txtSPort))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btnDisconnet, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnServer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(btnServer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(txtSIP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtSPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnDisconnet))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("You"));

        jLabel5.setText("IP:");

        jLabel6.setText("Port:");

        txtYIP.setText("127.0.0.1");
        txtYIP.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtYIPKeyTyped(evt);
            }
        });

        txtYPort.setText("3333");
        txtYPort.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtYPortKeyTyped(evt);
            }
        });

        btnListen.setText("Start listen");
        btnListen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnListenActionPerformed(evt);
            }
        });

        btnShutdown.setText("Shutdown");
        btnShutdown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnShutdownActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtYIP, javax.swing.GroupLayout.DEFAULT_SIZE, 88, Short.MAX_VALUE)
                    .addComponent(txtYPort))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 18, Short.MAX_VALUE)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnListen)
                    .addComponent(btnShutdown, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(txtYIP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnListen))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(txtYPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnShutdown))
                .addContainerGap(13, Short.MAX_VALUE))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Participants"));

        jcbUser.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jcbUserItemStateChanged(evt);
            }
        });

        btnConnecting.setText("Connect");
        btnConnecting.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConnectingActionPerformed(evt);
            }
        });

        jLabel8.setText("File share:");

        lbFileName.setText("lbFileName");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnConnecting, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jcbUser, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8)
                    .addComponent(lbFileName))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jcbUser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnConnecting)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lbFileName)
                .addGap(15, 15, 15))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 11, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(11, 11, 11)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 11, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnListenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnListenActionPerformed
        // TODO add your handling code here:
        int port = Integer.parseInt(txtYPort.getText());
        try {
            servSock = new ServerSocket(port);
            txtHistory.setText("<Listening incoming connection>\n");
            hostCrypto = new CryptoData(2048);
            t = new Thread(new ClientThread(this, jcbUser, servSock, txtHistory, hostCrypto));
            t.start();
            //disable control
            setClientButtonState(false);
            btnConnecting.setEnabled(true);
            txtYIP.setEnabled(false);
            txtYPort.setEnabled(false);
        } catch (IOException ex) {

        }
    }//GEN-LAST:event_btnListenActionPerformed

    private void btnBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseActionPerformed
        // TODO add your handling code here:
        try {
            File encryptedFile = guestCrypto.encryptFile(participantKey, fileShare);
            sendFile(encryptedFile, "FILE");
            setSendingControlState(false);
            btnConnecting.setEnabled(true);
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
    }//GEN-LAST:event_btnBrowseActionPerformed

    private void txtChatKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtChatKeyTyped
        // TODO add your handling code here:
        char c = evt.getKeyChar();
        if (c == KeyEvent.VK_ENTER) {
            String message = txtChat.getText();
            switch (message.toLowerCase()) {
                case "/?\n":
                    message = "/? to get help\n"
                            + "/c get list: get your participant list\n"
                            + "/c request: send request file to another users\n";
                    txtChat.setText("");
                    txtHistory.append(message);
                    btnConnecting.setEnabled(true);
                    break;
                case "/c request\n":
                    sendPulicKey(username);
                    txtChat.setText("");
                    txtHistory.append(message);
                    setSendingControlState(false);
                    break;
            }

            sendMess("MESS", username + ": " + message);
        }
    }//GEN-LAST:event_txtChatKeyTyped

    private void txtHistoryKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtHistoryKeyTyped
        evt.consume();
    }//GEN-LAST:event_txtHistoryKeyTyped

    private void btnSendInfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSendInfoActionPerformed
        sendInfomation();
    }//GEN-LAST:event_btnSendInfoActionPerformed

    private void txtSIPKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSIPKeyTyped
        // TODO add your handling code here:
        char c = evt.getKeyChar();
        if (!Character.isDigit(c) && c != '.' && c != KeyEvent.VK_BACK_SPACE) {
            evt.consume();
        }

        if (txtSIP.getText().length() >= 15) {
            evt.consume();
        }
    }//GEN-LAST:event_txtSIPKeyTyped

    private void txtSPortKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSPortKeyTyped
        // TODO add your handling code here:
        char c = evt.getKeyChar();

        if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE) {
            evt.consume();
        }

        if (txtSPort.getText().length() >= 5) {
            evt.consume();
        }
    }//GEN-LAST:event_txtSPortKeyTyped

    private void btnConnectingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConnectingActionPerformed
        try {
            Participant participant = getParticipant(jcbUser.getSelectedItem().toString());
            mainSock = new Socket(participant.getIp(), participant.getPort());
            btnConnecting.setEnabled(false);
            setSendingControlState(true);
        } catch (IOException ex) {

        }
    }//GEN-LAST:event_btnConnectingActionPerformed

    private void btnShutdownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnShutdownActionPerformed
        try {
            // TODO add your handling code here:
            notifyQuit();
            servSock.close();
            t.interrupt();
            //enable control
            setClientButtonState(true);
            btnConnecting.setEnabled(false);
            txtYIP.setEnabled(true);
            txtYPort.setEnabled(true);
        } catch (IOException ex) {

        }
    }//GEN-LAST:event_btnShutdownActionPerformed

    private void jcbUserItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jcbUserItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            Participant participant = getParticipant(evt.getItem().toString());
            String fileName = participant.getFileShare().getName();
            lbFileName.setText(fileName);
        }
    }//GEN-LAST:event_jcbUserItemStateChanged

    private void btnServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnServerActionPerformed
        String ip = txtSIP.getText();
        int port = Integer.parseInt(txtSPort.getText());
        try {
            mainSock = new Socket(ip, port);
//            new Thread(new ReceiveThread(this, jcbUser, mainSock, txtHistory, cryptoData)).start();
            setServerButtonState(false);
            txtSIP.setEnabled(false);
            txtSPort.setEnabled(false);
            setSendingControlState(true);
        } catch (IOException ex) {

        }
    }//GEN-LAST:event_btnServerActionPerformed

    private void txtYIPKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtYIPKeyTyped
        // TODO add your handling code here:

        String ipRegex = "\\b(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
                + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
                + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\."
                + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b";
        String ip = txtYIP.getText();
        Pattern pattern = Pattern.compile(ipRegex);
        Matcher matcher = pattern.matcher(ip);
        if (matcher.matches()) {
            btnListen.setEnabled(true);
        } else {
            btnListen.setEnabled(false);
        }
    }//GEN-LAST:event_txtYIPKeyTyped

    private void txtYPortKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtYPortKeyTyped
        // TODO add your handling code here:
        char c = evt.getKeyChar();

        if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE) {
            evt.consume();
        }

        if (txtYPort.getText().length() >= 5) {
            evt.consume();
        }
    }//GEN-LAST:event_txtYPortKeyTyped

    private void btnDisconnetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDisconnetActionPerformed
        try {
            // TODO add your handling code here:
            mainSock.close();
            setServerButtonState(true);
        } catch (IOException ex) {

        }
    }//GEN-LAST:event_btnDisconnetActionPerformed

    private void btnFileShareActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFileShareActionPerformed
        // TODO add your handling code here:
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            fileShare = chooser.getSelectedFile();
        }
    }//GEN-LAST:event_btnFileShareActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ChatClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ChatClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ChatClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ChatClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ChatClient().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBrowse;
    private javax.swing.JButton btnConnecting;
    private javax.swing.JButton btnDisconnet;
    private javax.swing.JButton btnFileShare;
    private javax.swing.JButton btnListen;
    private javax.swing.JButton btnSendInfo;
    private javax.swing.JButton btnServer;
    private javax.swing.JButton btnShutdown;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JComboBox<String> jcbUser;
    private javax.swing.JLabel lbFileName;
    private javax.swing.JLabel lbusername;
    private javax.swing.JTextArea txtChat;
    private javax.swing.JTextArea txtHistory;
    private javax.swing.JTextField txtSIP;
    private javax.swing.JTextField txtSPort;
    private javax.swing.JTextField txtYIP;
    private javax.swing.JTextField txtYPort;
    // End of variables declaration//GEN-END:variables
}
