import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.CipherOutputStream;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;

import javax.crypto.spec.SecretKeySpec;

import java.security.spec.X509EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.IOException;

import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;

import java.util.Arrays;

public class Utils {
    private static final PrintStream DEBUG = System.out;
    private static final int AES_KEY_SIZE = 128;

    private static byte[] readFile(String s) throws IOException {
        return readFile(new File(s));
    }
    private static byte[] readFile(File f) throws IOException {
        byte[] bytes = new byte[(int) f.length()];
        new DataInputStream(new FileInputStream(f)).readFully(bytes);
        return bytes;
    }

    private static void saveAESKey(byte[] aesKey, File out, String pubKey)
        throws IOException {
        Cipher rsaCipher = null;

        try {
            rsaCipher = Cipher.getInstance("RSA");
            rsaCipher.init(Cipher.ENCRYPT_MODE, KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(readFile(pubKey))));
        } catch (Exception ex) {
            throw new IOException("Something wrong with the public keyfile");
        }

        CipherOutputStream cos = new CipherOutputStream(
            new FileOutputStream(out), rsaCipher);
        cos.write(aesKey);
        cos.close();
    }

    private static byte[] loadAESKey(File file, String privateKey)
        throws IOException {
        Cipher rsaCipher = null;

        try {
            rsaCipher = Cipher.getInstance("RSA");
            rsaCipher.init(Cipher.DECRYPT_MODE, KeyFactory.getInstance("RSA")
                .generatePrivate(
                 new PKCS8EncodedKeySpec(readFile(privateKey))));
        } catch (Exception ex) {
            throw new IOException("Something wrong with the private keyfile");
        }

        byte[] outBytes = new byte[AES_KEY_SIZE / 8];

        CipherInputStream cos = new CipherInputStream(
            new FileInputStream(file), rsaCipher);
        cos.read(outBytes);
        cos.close();
        return outBytes;
    }

    private static void encrypt(File file, File out, File aesOut,
        String pubKey) throws IOException, InvalidKeyException {

        Cipher aesCipher = null;
        try {
            aesCipher = Cipher.getInstance("AES");
            KeyGenerator gen = KeyGenerator.getInstance("AES");
            gen.init(AES_KEY_SIZE);
            byte[] aesKey = gen.generateKey().getEncoded();

            DEBUG.println("Saving AES Key");
            saveAESKey(aesKey, aesOut, pubKey);
            DEBUG.println("Creating Spec for AES Key");
            SecretKeySpec aesKeySpec = new SecretKeySpec(aesKey, "AES");
            DEBUG.println("Initializing Cipher");
            aesCipher.init(Cipher.ENCRYPT_MODE,
                aesKeySpec);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException
            | InvalidKeyException ex) {
            throw new InvalidKeyException("Problem creating the AES key", ex);
        }

        CipherOutputStream cos = new CipherOutputStream(
            new FileOutputStream(out), aesCipher);
        cos.write(readFile(file));
        cos.close();
    }

    private static void decrypt(File file, File out, File aesIn,
        String privateKey) throws IOException {

        Cipher aesCipher = null;
        try {
            aesCipher = Cipher.getInstance("AES");

            DEBUG.println("Loading AES Key");
            byte[] aesKey = loadAESKey(aesIn, privateKey);

            aesCipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(aesKey, "AES"));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException
            | InvalidKeyException ex) {
            throw new IOException("Problem loading the AES key");
        }

        CipherInputStream cos = new CipherInputStream(
            new FileInputStream(file), aesCipher);

        int c;
        FileOutputStream fos = new FileOutputStream(out);
        while ((c = cos.read()) != -1) {
            fos.write(c);
        }
        fos.close();
        cos.close();
    }


    public static File zipFiles(String name, File... files)
        throws IOException {
        File zip = new File(name);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));

        for (File f : files) {
            out.putNextEntry(new ZipEntry(f.getName()));
            byte[] data = readFile(f);
            out.write(data, 0, data.length);
            out.closeEntry();
        }

        out.close();
        return zip;
    }

    public static void main(String... args)
        throws IOException, InvalidKeyException {
        
        if (args[0].equals("help") || args[0].equals("-h")) {
            System.out.println("Encrypt:\n java Bundler public_key File1 File2"
                + " File3");
            System.out.println("Decrypt:\n java Bundler -d private_key File "
                + " encrypted_aes_key");
        } else if (args[0].equals("-d")) {
            decrypt(new File(args[2]), new File("decrypted-" + args[1]),
                new File(args[3]), args[1]);
        } else {
            File zip = zipFiles("files.zip", Arrays.stream(
                Arrays.copyOfRange(args, 1, args.length))
                .map(File::new).toArray(File[]::new));
            File aes = new File("encrypted-aes.der");
            encrypt(zip, new File("encrypted-files.zip"),
                aes, "public_key.der");

            File submissionZip = zipFiles("submission.zip", zip, aes);
        }
    }


}

