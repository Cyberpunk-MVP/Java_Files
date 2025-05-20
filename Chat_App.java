import java.io.*;
import java.net.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SecureChatApp {

    private static final String SERVER_ADDRESS = "127.0.0.1"; // Localhost for testing
    private static final int SERVER_PORT = 12345;
    private static final String ALGORITHM = "AES";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static SecretKey secretKey;
    private static PublicKey publicKey;
    private static PrivateKey privateKey;

    public static void main(String[] args) {
        try {
            // Generate or load key pair
            generateKeyPair();

            // Choose to run as either a client or a server
            System.out.println("Run as (1) Server or (2) Client?");
            Scanner scanner = new Scanner(System.in);
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            if (choice == 1) {
                startServer();
            } else if (choice == 2) {
                startClient(scanner);
            } else {
                System.out.println("Invalid choice.");
            }
            scanner.close();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    //method to generate rsa key pair
    private static void generateKeyPair() throws NoSuchAlgorithmException, InvalidKeySpecException {
        //check if the key pair exists
        File publicKeyFile = new File("public.key");
        File privateKeyFile = new File("private.key");

        if (publicKeyFile.exists() && privateKeyFile.exists()) {
            // Load the keys
            try (ObjectInputStream pubKeyIn = new ObjectInputStream(new FileInputStream(publicKeyFile));
                 ObjectInputStream privKeyIn = new ObjectInputStream(new FileInputStream(privateKeyFile))) {
                publicKey = (PublicKey) pubKeyIn.readObject();
                privateKey = (PrivateKey) privKeyIn.readObject();
                System.out.println("Key pair loaded from files.");
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Error loading keys, generating new pair.");
                 // Generate a new key pair
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                keyPairGenerator.initialize(2048); // Use a strong key size
                KeyPair keyPair = keyPairGenerator.generateKeyPair();
                publicKey = keyPair.getPublic();
                privateKey = keyPair.getPrivate();
                //save the key pair
                 try (ObjectOutputStream pubKeyOut = new ObjectOutputStream(new FileOutputStream(publicKeyFile));
                      ObjectOutputStream privKeyOut = new ObjectOutputStream(new FileOutputStream(privateKeyFile))) {
                    pubKeyOut.writeObject(publicKey);
                    privKeyOut.writeObject(privateKey);
                }catch(IOException ex){
                    System.err.println("Error saving keys: " + ex.getMessage());
                }
            }

        } else {
             // Generate a new key pair
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048); // Use a strong key size
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();
            //save the key pair
             try (ObjectOutputStream pubKeyOut = new ObjectOutputStream(new FileOutputStream(publicKeyFile));
                      ObjectOutputStream privKeyOut = new ObjectOutputStream(new FileOutputStream(privateKeyFile))) {
                    pubKeyOut.writeObject(publicKey);
                    privKeyOut.writeObject(privateKey);
                }catch(IOException ex){
                    System.err.println("Error saving keys: " + ex.getMessage());
                }
            System.out.println("New key pair generated.");
        }
    }

    // Method to start the server
    private static void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Server started. Listening on port " + SERVER_PORT);

            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress());

            // Send public key to the client
            sendPublicKey(clientSocket);

            // Receive client's public key and generate shared secret
            PublicKey clientPublicKey = receivePublicKey(clientSocket);
            generateSharedSecret(clientPublicKey);

            // Set up secure communication channels
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Exchange messages
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received: " + decrypt(message));
                System.out.print("Server: ");
                String serverResponse = new Scanner(System.in).nextLine();
                out.println(encrypt(serverResponse));
                out.flush();
            }

            System.out.println("Client disconnected.");
        } catch (Exception e) {
            System.err.println("Server Error: " + e.getMessage());
        }
    }

    // Method to start the client
    private static void startClient(Scanner scanner) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT)) {
            System.out.println("Connected to server: " + SERVER_ADDRESS + ":" + SERVER_PORT);

            // Receive server's public key
            PublicKey serverPublicKey = receivePublicKey(socket);
            // Send public key to the server
            sendPublicKey(socket);
            // Generate shared secret
            generateSharedSecret(serverPublicKey);

            // Set up secure communication channels
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Exchange messages
            String message;
            while (true) {
                System.out.print("Client: ");
                message = scanner.nextLine();
                out.println(encrypt(message));
                out.flush();

                String serverResponse = in.readLine();
                if (serverResponse == null) {
                    System.out.println("Server disconnected.");
                    break;
                }
                System.out.println("Received: " + decrypt(serverResponse));
            }
        } catch (Exception e) {
            System.err.println("Client Error: " + e.getMessage());
        }
    }

    private static void sendPublicKey(Socket socket) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {
            oos.writeObject(publicKey);
        }
    }

    private static PublicKey receivePublicKey(Socket socket) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
            return (PublicKey) ois.readObject();
        }
    }

    private static void generateSharedSecret(PublicKey otherPublicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        try {
            // Use Diffie-Hellman to establish a shared secret
            KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
            keyAgreement.init(privateKey);
            keyAgreement.doPhase(otherPublicKey, true);
            byte[] sharedSecretBytes = keyAgreement.generateSecret();

            // Derive an AES key from the shared secret using a key derivation function (KDF)
            MessageDigest sha256 = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] keyBytes = sha256.digest(sharedSecretBytes);
            secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

            System.out.println("Shared secret generated.");
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Error initializing KeyAgreement: " + e.getMessage(), e);
        }
    }

    // Method to encrypt a message using AES
    private static String encrypt(String message) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(message.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            System.err.println("Encryption Error: " + e.getMessage());
            return null; // Handle error appropriately
        }
    }

    // Method to decrypt a message using AES
    private static String decrypt(String encryptedMessage) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedMessage);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, "UTF-8");
        } catch (Exception e) {
            System.err.println("Decryption Error: " + e.getMessage());
            return null; // Handle error appropriately
        }
    }
}

