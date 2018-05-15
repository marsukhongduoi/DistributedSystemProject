/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lib;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 *
 * @author Daniel
 */
public class CryptoData {
    private PrivateKey privatekey;
    private PublicKey publickey;

    public CryptoData() {
        
    }
    
    public CryptoData(final int keysize) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(keysize);
            KeyPair keyPair = keyGen.genKeyPair();
            publickey = keyPair.getPublic();
            privatekey = keyPair.getPrivate();
        } catch (NoSuchAlgorithmException ex) {
        }
    }

    public CryptoData(PrivateKey privatekey, PublicKey publickey) {
        this.privatekey = privatekey;
        this.publickey = publickey;
    }

    public PrivateKey getPrivatekey() {
        return privatekey;
    }

    public void setPrivatekey(PrivateKey privatekey) {
        this.privatekey = privatekey;
    }

    public PublicKey getPublickey() {
        return publickey;
    }

    public void setPublickey(PublicKey publickey) {
        this.publickey = publickey;
    }

    public byte[] encryptText(PublicKey pubKey, String message) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException{
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        return cipher.doFinal(message.getBytes());
    }

    public byte[] decryptText(byte[] encrypt) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privatekey);
        return cipher.doFinal(encrypt);
    }
    
    public PublicKey convertByteToPublicKey(byte[] publicKeyBytes) {
        PublicKey pubKey = null;
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        } catch (NoSuchAlgorithmException ex) {
            
        } catch (InvalidKeySpecException ex) {
            
        }
        return pubKey;
    }
    
    public PrivateKey convertByteToPrivateKey(byte[] privateKeyBytes) {
        PrivateKey privKey = null;
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
        } catch(NoSuchAlgorithmException ex) {
            
        } catch (InvalidKeySpecException ex) {
            
        }
        return privKey;
    }

    public File encryptFile(PublicKey pubKey, File file) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] outputBuff = {};
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        InputStream inputStream = null;
        OutputStream outputStream = null;
        File encryptedFile = new File(file.getAbsoluteFile()+ ".enc");
        try {
            inputStream = new FileInputStream(file);
            outputStream = new FileOutputStream(encryptedFile);
            byte[] inputBuff = new byte[2048];
            int data;
            while ((data = inputStream.read(inputBuff)) != -1) {
                outputBuff = cipher.update(inputBuff, 0, data);
                if (outputBuff != null) {
                    outputStream.write(outputBuff);
                }
            }
            outputBuff = cipher.doFinal();
            if (outputBuff != null) {
                outputStream.write(outputBuff);
            }
            inputStream.close();
            outputStream.close();
        } catch (FileNotFoundException ex) {

        } catch (IOException ex) {

        }
        return encryptedFile;
    }

    public void decryptFile(File encryptedFile, File decryptedFile) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privatekey);
        InputStream inputStream = null;
        BufferedWriter bufferedWriter = null;
        try {
            inputStream = new FileInputStream(encryptedFile);
            bufferedWriter = new BufferedWriter(new FileWriter(decryptedFile));
            byte[] byteRead = new byte[2048];
            int data;
            while((data = inputStream.read(byteRead)) != -1) {
                byte[] byteWrite = cipher.update(byteRead, 0, data);
                String text = new String(byteWrite);
                bufferedWriter.write(text);
            }
            byte[] byteWrite = cipher.doFinal();
            if(byteWrite != null) {
                bufferedWriter.write(new String(byteWrite));
            }
            inputStream.close();
            bufferedWriter.close();
        } catch(FileNotFoundException ex) {
            
        } catch(IOException ex) {
            
        }
    }
    
    public void exportPublicKeyToFile(File file) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(publickey.getEncoded());
            fileOutputStream.close();
        } catch (FileNotFoundException ex) {
            
        } catch (IOException ex) {
            
        }
        
    }
    
    public void exportPrivateKeyToFile(File file) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(privatekey.getEncoded());
            fileOutputStream.close();
        } catch (FileNotFoundException ex) {
            
        } catch (IOException ex) {
            
        }
        
    }
}
