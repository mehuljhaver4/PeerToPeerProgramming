import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class peerProcess extends Thread {
    private static int peersWithCompleteFile = 0;
    private static int thisPeerId;
    private static byte[][] filePieces;
    private static PeerInfo thisPeer;
    private static LinkedHashMap<Integer, PeerInfo> peers;
    private static ConcurrentHashMap<Integer, Socket> peerSockets;
    private static final char CHOKE = '0';
    private static final char UNCHOKE = '1';
    private static final char INTERESTED = '2';
    private static final char NOTINTERESTED = '3';
    private static final char HAVE = '4';
    private static final char BITFIELD = '5';
    private static final char REQUEST = '6';
    private static final char PIECE = '7';

    public static void main(String[] args) {
        try {
            thisPeerId = Integer.parseInt(args[0]);
            System.out.println("This peer - " + thisPeerId);
            peers = new LinkedHashMap<>();
            peerSockets = new ConcurrentHashMap<>();

            //Read PeerInfo.cfg and set properties for all peers
            setPeerInfo(peers);
            thisPeer = peers.get(thisPeerId);

            //Read Common.cfg and set the common properties
            CommonProperties commonProperties = new CommonProperties();
            commonProperties.setCommonProperties();
            int fileSize = commonProperties.getFileSize();
            int pieceSize = commonProperties.getPieceSize();
            int numberOfPieces = (int) Math.ceil((double) fileSize / pieceSize);
            int[] bitField = new int[numberOfPieces];
            filePieces = new byte[numberOfPieces][];

            //Set pieces of file if this peer has the full file
            if (thisPeer.isHasFile()) {

                //Bitfield will have all 1s if this peer has the full file
                Arrays.fill(bitField, 1);
                thisPeer.setBitField(bitField);

                //Increment this variable by 1 so that total peers with the full file can be tracked
                peersWithCompleteFile += 1;

                //Read the file using stream and assign to fileBytes
                BufferedInputStream file = new BufferedInputStream(new FileInputStream(commonProperties.getFileName()));
                byte[] fileBytes = new byte[fileSize];
                file.read(fileBytes);
                file.close();
                int part = 0;

                //Assigning file pieces to filePieces
                for (int counter = 0; counter < fileSize; counter += pieceSize) {

                    //Fill the filePieces for the part bytes from range counter to counter + pieceSize
                    if (counter + pieceSize <= fileSize)
                        filePieces[part] = Arrays.copyOfRange(fileBytes, counter, counter + pieceSize);

                        //Else will be used for the final few bytes left which is less than the piece size
                    else
                        filePieces[part] = Arrays.copyOfRange(fileBytes, counter, fileSize);
                    part += 1;
                    thisPeer.setNumberOfPieces();
                }
            } else {
                Arrays.fill(bitField, 0);
                thisPeer.setBitField(bitField);
            }

            //Starting all threads to start the protocol
            ConnectToPeers connectToPeers = new ConnectToPeers();
            AcceptConnectionsFromPeers acceptConnectionsFromPeers = new AcceptConnectionsFromPeers();
            connectToPeers.start();
            acceptConnectionsFromPeers.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void setPeerInfo(HashMap<Integer, PeerInfo> peers) {
        try {
            BufferedReader peerInfo =
                    new BufferedReader(new FileReader("Config Files/PeerInfo.cfg"));
            Object[] peerInfoLines = peerInfo.lines().toArray();
            for (var peerInfoLine : peerInfoLines) {
                int peerId = Integer.parseInt(peerInfoLine.toString().split(" ")[0]);
                String hostName = peerInfoLine.toString().split(" ")[1];
                int portNumber = Integer.parseInt(peerInfoLine.toString().split(" ")[2]);
                boolean hasFile = Integer.parseInt(peerInfoLine.toString().split(" ")[3]) == 1;
                PeerInfo peer = new PeerInfo(peerId, hostName, portNumber, hasFile);
                peers.put(peerId, peer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ConnectToPeers extends Thread {
        @Override
        public void run() {
            byte[] inputData = new byte[32];
            try {
                byte[] handShakeMessage = Messages.getHandshakeMessage(thisPeerId);

                //Iterate through the peers hashmap
                for (int peerId : peers.keySet()) {

                    //We break here because we only want to connect to peers started before this peer. If this peer is
                    // 1003 we only want to connect to 1001 and 1002. When the loop reaches 1003 we break
                    if (peerId == thisPeerId)
                        break;

                    //Writing the handshake on the output stream
                    Socket socket = new Socket(peers.get(peerId).getHostName(), peers.get(peerId).getPortNumber());
                    Messages.sendMessage(socket, handShakeMessage);

                    //The other peer sends a handshake message which is retrieved here
                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                    dataInputStream.readFully(inputData);
                    int receivedPeerId = ByteBuffer.wrap(Arrays.copyOfRange(inputData, 28, 32)).getInt();

                    //This is the check that is mentioned in the project description. If the peer id is different
                    //from the one we connected to , we close the socket
                    if (receivedPeerId != peerId)
                        socket.close();
                    else {
                        StringBuilder handshakeMsg = new StringBuilder();
                        handshakeMsg.append(new String(Arrays.copyOfRange(inputData, 0, 28)));
                        handshakeMsg.append(receivedPeerId);
                        System.out.println(handshakeMsg);
                        peerSockets.put(peerId, socket);
                        new Communicate(socket, peerId).start();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class AcceptConnectionsFromPeers extends Thread {
        @Override
        public void run() {
            byte[] data = new byte[32];
            try {
                byte[] handShakeMessage = Messages.getHandshakeMessage(thisPeerId);
                ServerSocket serverSocket = new ServerSocket(peers.get(thisPeerId).getPortNumber());

                //While loop runs peers.size() - 1 times because we want to connect to all other peers
                while (peerSockets.size() < peers.size() - 1) {
                    Socket socket = serverSocket.accept();
                    DataInputStream input = new DataInputStream(socket.getInputStream());
                    input.readFully(data);
                    StringBuilder handshakeMsg = new StringBuilder();
                    int peerId = ByteBuffer.wrap(Arrays.copyOfRange(data, 28, 32)).getInt();
                    handshakeMsg.append(new String(Arrays.copyOfRange(data, 0, 28)));
                    handshakeMsg.append(peerId);
                    System.out.println(handshakeMsg);

                    //Sending handshake message to connected peer
                    Messages.sendMessage(socket, handShakeMessage);

                    new Communicate(socket, peerId).start();
                    peerSockets.put(peerId, socket);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class Communicate extends Thread {
        private Socket socket;
        private final int peerId;

        public Communicate(Socket socket, int peerId) {
            this.socket = socket;
            this.peerId = peerId;
        }

        @Override
        public synchronized void run() {
            synchronized (this) {
                try {
                    //Send bitfield immediately after handshake
                    byte[] bitFieldMessage = Messages.getBitfieldMessage(thisPeer.getBitField());
                    Messages.sendMessage(socket, bitFieldMessage);

                    while (peersWithCompleteFile < peers.size()) {
                        //Retrieve the incoming message and get the message type and decide the next action
                        DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                        int messageLength = dataInputStream.readInt();
                        byte[] message = new byte[messageLength];
                        dataInputStream.readFully(message);
                        char messageType = (char) message[0];

                        switch (messageType) {
                            case BITFIELD -> {
                                byte[] payload;
                                payload = Arrays.copyOfRange(message, 1, messageLength);
                                int index = 0;
                                int bitFieldIndex = 0;
                                int[] bitField = new int[(messageLength - 1) / 4];
                                for (index = 0; index < payload.length; index += 4) {
                                    bitField[bitFieldIndex] = ByteBuffer.wrap(Arrays
                                            .copyOfRange(payload, index, index + 4)).getInt();
                                    bitFieldIndex += 1;
                                }
                                System.out.println(Arrays.toString(bitField));
                                peers.get(peerId).setBitField(bitField);
                                int parts = 0;
                                for (int x : bitField) {
                                    if (x == 1)
                                        parts += 1;
                                }
                                if (parts == thisPeer.getBitField().length) {
                                    peers.get(peerId).setHasFile(true);
                                    peersWithCompleteFile += 1;
                                } else
                                    peers.get(peerId).setHasFile(false);

                                if (checkIfInteresting(thisPeer.getBitField(), bitField, thisPeer.getBitField().length))
                                    Messages.sendMessage(socket, Messages.getInterestedMessage());
                                else
                                    Messages.sendMessage(socket, Messages.getNotInterestedMessage());
                            }
                            case INTERESTED -> {
                                peers.get(peerId).setInterested(true);
//                                System.out.println("Received INTERESTED from - " + peerId);
                            }
                            case NOTINTERESTED -> {
                                peers.get(peerId).setInterested(false);
                                if (!peers.get(peerId).isChokedByPeer()) {
                                    peers.get(peerId).setChokedByPeer(true);
                                    Messages.sendMessage(socket, Messages.getChokeMessage());
                                }
//                                System.out.println("Received NOTINTERESTED from " + peerId);
                            }
                            case CHOKE -> {
                                peers.get(peerId).setChokedByPeer(true);
//                                System.out.println("Received CHOKE from " + peerId);
                            }
                            case UNCHOKE -> {
                                peers.get(peerId).setChokedByPeer(false);
                                byte[] requestMessageMessage = Messages.getRequestMessage(thisPeer.getBitField()
                                        , peers.get(peerId).getBitField(), thisPeer.getBitField().length);

                                if (requestMessageMessage != null)
                                    Messages.sendMessage(socket, requestMessageMessage);

//                                System.out.println("Received UNCHOKE from " + peerId);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public boolean checkIfInteresting(int[] thisPeerBitfield, int[] connectedPeerBitField, int length) {
            for (int i = 0; i < length; i++) {
                if (thisPeerBitfield[i] == 0 && connectedPeerBitField[i] == 1) {
                    return true;
                }
            }
            return false;
        }
    }

    static class Messages {
        static byte[] message;

        public static byte[] createMessage(char messageType, byte[] messagePayLoad) {
            int index;
            switch (messageType) {
                case BITFIELD:
                    int messageLength = messagePayLoad.length + 1;
                    message = new byte[messageLength + 4];
                    index = 0;
                    for (byte lengthByte : ByteBuffer.allocate(4).putInt(messageLength).array()) {
                        message[index] = lengthByte;
                        index += 1;
                    }
                    message[index] = (byte) messageType;
                    index += 1;

                    for (byte payloadByte : messagePayLoad) {
                        message[index] = payloadByte;
                        index += 1;
                    }
                    return message;

                case CHOKE:
                case UNCHOKE:
                case INTERESTED:
                case NOTINTERESTED:
                    message = new byte[5];
                    index = 0;

                    for (byte lengthByte : ByteBuffer.allocate(4).putInt(1).array()) {
                        message[index] = lengthByte;
                        index += 1;
                    }
                    message[index] = (byte) messageType;
                    return message;

                case REQUEST:
                case HAVE:
                    message = new byte[9];
                    index = 0;

                    for (byte lengthByte : ByteBuffer.allocate(4).putInt(1).array()) {
                        message[index] = lengthByte;
                        index += 1;
                    }
                    message[index] = (byte) messageType;
                    index += 1;

                    for (byte payloadByte : messagePayLoad) {
                        message[index] = payloadByte;
                        index += 1;
                    }
                    return message;
                default:
                    return null;
            }
        }

        public static byte[] getHandshakeMessage(int peerId) {
            byte[] handShakeMessage = new byte[32];
            byte[] header = "P2PFILESHARINGPROJ".getBytes(StandardCharsets.UTF_8);
            byte[] zeroBits = "0000000000".getBytes(StandardCharsets.UTF_8);
            byte[] thisPeer = ByteBuffer.allocate(4).putInt(peerId).array();

            int index = 0;
            for (var b : header) {
                handShakeMessage[index] = b;
                index += 1;
            }

            for (var b : zeroBits) {
                handShakeMessage[index] = b;
                index += 1;
            }

            for (var b : thisPeer) {
                handShakeMessage[index] = b;
                index += 1;
            }
            return handShakeMessage;
        }

        public static byte[] getBitfieldMessage(int[] bitField) {
            byte[] bitFieldPayload = new byte[4 * bitField.length];
            int index = 0;
            for (int bit : bitField) {
                byte[] bytes = ByteBuffer.allocate(4).putInt(bit).array();
                for (byte b : bytes) {
                    bitFieldPayload[index++] = b;
                }
            }

            return createMessage(BITFIELD, bitFieldPayload);
        }

        public static byte[] getInterestedMessage() {
            return createMessage(INTERESTED, null);
        }

        public static byte[] getNotInterestedMessage() {
            return createMessage(NOTINTERESTED, null);
        }

        public static byte[] getChokeMessage() {
            return createMessage(CHOKE, null);
        }

        public static byte[] getUnchokeMessage() {
            return createMessage(UNCHOKE, null);
        }

        public static byte[] getRequestMessage(int[] thisPeerBitField, int[] connectedPeerBitField, int length) {
            byte[] pieceIndexPayload = new byte[4];
            ArrayList<Integer> pieceIndices = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                if (thisPeerBitField[i] == 0 && connectedPeerBitField[i] == 1)
                    pieceIndices.add(i);
            }

            if (pieceIndices.isEmpty())
                return null;

            Random random = new Random();
            int pieceIndex = random.nextInt(pieceIndices.size());
            pieceIndexPayload = ByteBuffer.allocate(4).putInt(pieceIndex).array();
            return createMessage(REQUEST, pieceIndexPayload);
        }

        public static void sendMessage(Socket socket, byte[] data) {
            try {
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.flush();
                dataOutputStream.write(data);
                dataOutputStream.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}

class CommonProperties {
    private int preferredNeighbors;
    private int unchokingInterval;
    private int optimisticUnchokingInterval;
    private String fileName;
    private int fileSize;
    private int pieceSize;

    public int getPreferredNeighbors() {
        return preferredNeighbors;
    }

    public void setPreferredNeighbors(int preferredNeighbors) {
        this.preferredNeighbors = preferredNeighbors;
    }

    public int getUnchokingInterval() {
        return unchokingInterval;
    }

    public void setUnchokingInterval(int unchokingInterval) {
        this.unchokingInterval = unchokingInterval;
    }

    public int getOptimisticUnchokingInterval() {
        return optimisticUnchokingInterval;
    }

    public void setOptimisticUnchokingInterval(int optimisticUnchokingInterval) {
        this.optimisticUnchokingInterval = optimisticUnchokingInterval;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public int getPieceSize() {
        return pieceSize;
    }

    public void setPieceSize(int pieceSize) {
        this.pieceSize = pieceSize;
    }

    public void setCommonProperties() {
        try {
            BufferedReader commonInfo =
                    new BufferedReader(new FileReader("Config Files/Common.cfg"));

            Object[] commonLines = commonInfo.lines().toArray();
            this.setPreferredNeighbors(Integer.parseInt(commonLines[0].toString().split(" ")[1]));
            this.setUnchokingInterval(Integer.parseInt(commonLines[1].toString().split(" ")[1]));
            this.setOptimisticUnchokingInterval(Integer.parseInt(commonLines[2].toString().split(" ")[1]));
            this.setFileName(commonLines[3].toString().split(" ")[1]);
            this.setFileSize(Integer.parseInt(commonLines[4].toString().split(" ")[1]));
            this.setPieceSize(Integer.parseInt(commonLines[5].toString().split(" ")[1]));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class PeerInfo {
    private int peerId;
    private String hostName;
    private int portNumber;
    private boolean hasFile;
    private int[] bitField;
    private int numberOfPieces = 0;
    private boolean chokedByPeer = false;
    private boolean isInterested = false;

    public PeerInfo(int peerId, String hostName, int portNumber, boolean hasFile) {
        this.peerId = peerId;
        this.hostName = hostName;
        this.portNumber = portNumber;
        this.hasFile = hasFile;
    }

    public int getPeerId() {
        return peerId;
    }

    public void setPeerId(int peerId) {
        this.peerId = peerId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

    public boolean isHasFile() {
        return hasFile;
    }

    public void setHasFile(boolean hasFile) {
        this.hasFile = hasFile;
    }

    public int[] getBitField() {
        return bitField;
    }

    public void setBitField(int[] bitField) {
        this.bitField = bitField;
    }

    public int getNumberOfPieces() {
        return numberOfPieces;
    }

    public void setNumberOfPieces() {

        //Bitfield length is the total number of pieces in the file. If total number of downloaded pieces is equal to
        // bitField.length it means that the peer has the complete file.
        this.numberOfPieces += 1;
        if (this.numberOfPieces == bitField.length)
            this.hasFile = true;
    }

    public boolean isChokedByPeer() {
        return chokedByPeer;
    }

    public void setChokedByPeer(boolean chokedByPeer) {
        this.chokedByPeer = chokedByPeer;
    }

    public boolean isInterested() {
        return isInterested;
    }

    public void setInterested(boolean interested) {
        isInterested = interested;
    }
}
