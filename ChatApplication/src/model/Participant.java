/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;

/**
 *
 * @author Daniel
 */
public class Participant implements Serializable{
    private String username;
    private String ip;
    private int port;
    private File fileShare;

    public Participant() {
    }

    public Participant(String username, String ip, int port, File fileShare) {
        this.username = username;
        this.ip = ip;
        this.port = port;
        this.fileShare = fileShare;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public File getFileShare() {
        return fileShare;
    }

    public void setFileShare(File fileShare) {
        this.fileShare = fileShare;
    }
    
    public String getKeyFileName() {
        String fileName= "";
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(fileShare));
            String line = bufferedReader.readLine();            
            fileName = line.split("-")[3];
            bufferedReader.close();
        } catch (FileNotFoundException ex) {
            
        } catch (IOException ex) {
            
        }
        return fileName;
    }
    
    @Override
    public String toString() {
        return username + "-" + ip + "-" + Integer.toString(port) + fileShare.getName();
    }
}
