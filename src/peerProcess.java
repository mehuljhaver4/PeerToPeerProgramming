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
    private static int numberOfPieces;
    private static byte[][] filePieces;
    private static PeerInfo thisPeer;
    private static CommonProperties commonProperties;
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
            commonProperties = new CommonProperties();
            commonProperties.setCommonProperties();
            int fileSize = commonProperties.getFileSize();
            int pieceSize = commonProperties.getPieceSize();
            numberOfPieces = (int) Math.ceil((double) fileSize / pieceSize);
            int[] bitField = new int[numberOfPieces];
            filePieces = new byte[numberOfPieces][];

            //Set pieces of file if this peer has the full file
            if (thisPeer.hasFile()) {

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
                    thisPeer.updateNumberOfPieces();
                }
            } else {
                Arrays.fill(bitField, 0);
                thisPeer.setBitField(bitField);
            }

            //Starting all threads to start the protocol
            ConnectToPeers connectToPeers = new ConnectToPeers();
            AcceptConnectionsFromPeers acceptConnectionsFromPeers = new AcceptConnectionsFromPeers();
            UnchokePeers unchokePeers = new UnchokePeers();
            connectToPeers.start();
            acceptConnectionsFromPeers.start();
            unchokePeers.start();
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
                        new ExchangeMessages(socket, peerId).start();
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

                    new ExchangeMessages(socket, peerId).start();
                    peerSockets.put(peerId, socket);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class UnchokePeers extends Thread {
        @Override
        public void run() {
            try {
                while (peersWithCompleteFile < peers.size()) {
                    int preferredNeighbors = commonProperties.getPreferredNeighbors();
                    ArrayList<Integer> interestedPeers;
                    ArrayList<Integer> preferredPeers;

                    if (thisPeer.hasFile()) {
                        interestedPeers = new ArrayList<>();
                        preferredPeers = new ArrayList<>();
                        for (int peerId : peerSockets.keySet()) {
                            if (peers.get(peerId).isInterested())
                                interestedPeers.add(peerId);
                        }
                        if (interestedPeers.size() <= preferredNeighbors) {
                            preferredPeers.addAll(interestedPeers);
                        } else {
                            Random random = new Random();
                            int peerIndex;
                            for (int i = 0; i < preferredNeighbors; i++) {
                                peerIndex = random.nextInt(interestedPeers.size());
                                if (!preferredPeers.contains(peerIndex)) {
                                    int randomPeer = interestedPeers.get(peerIndex);
                                    preferredPeers.add(randomPeer);
                                    interestedPeers.remove(peerIndex);
                                }
                            }
                        }
                    } else {
                        interestedPeers = new ArrayList<>();
                        preferredPeers = new ArrayList<>();
                        for (int peerId : peerSockets.keySet()) {
                            if (peers.get(peerId).isInterested())
                                interestedPeers.add(peerId);
                        }
                        if (interestedPeers.size() <= preferredNeighbors) {
                            preferredPeers.addAll(interestedPeers);
                        } else {
                            for (int i = 0; i < preferredNeighbors; i++) {
                                int maxDownloadRatePeer = interestedPeers.get(0);
                                int maxDownloadRatePeerIndex = 0;
                                for (int j = 1; j < interestedPeers.size(); j++) {
                                    if (peers.get(interestedPeers.get(j)).getDownloadRate() > peers.get(maxDownloadRatePeer).getDownloadRate()) {
                                        maxDownloadRatePeer = interestedPeers.get(j);
                                        maxDownloadRatePeerIndex = j;
                                    }

                                }
                                preferredPeers.add(maxDownloadRatePeer);
                                interestedPeers.remove(maxDownloadRatePeerIndex);
                            }
                        }
                    }
                    System.out.println("Preferred neighbors - " + preferredPeers);
                    for (int peerId : preferredPeers) {
                        peers.get(peerId).setChoked(false);
                        Messages.sendMessage(peerSockets.get(peerId), Messages.getUnchokeMessage());
                    }

                    for (int peerId : interestedPeers) {
                        if (!peers.get(peerId).isChoked()) {
                            peers.get(peerId).setChoked(true);
                            Messages.sendMessage(peerSockets.get(peerId), Messages.getChokeMessage());
                        }
                    }

                    Thread.sleep(commonProperties.getUnchokingInterval() * 1000L);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class ExchangeMessages extends Thread {
        private Socket socket;
        private final int peerId;

        public ExchangeMessages(Socket socket, int peerId) {
            this.socket = socket;
            this.peerId = peerId;
        }

        public boolean checkIfInteresting(int[] thisPeerBitfield, int[] connectedPeerBitField, int length) {
            for (int i = 0; i < length; i++) {
                if (thisPeerBitfield[i] == 0 && connectedPeerBitField[i] == 1) {
                    return true;
                }
            }
            return false;
        }

        public void checkIfCompleteFileDownloaded() {
            int parts = 0;
            byte[] mergedFile = new byte[commonProperties.getFileSize()];
            for (int bit : thisPeer.getBitField()) {
                if (bit == 1)
                    parts += 1;
            }
            int index = 0;
            if (parts == thisPeer.getBitField().length) {
                peersWithCompleteFile += 1;
                thisPeer.setHasFile(true);
                for (int piece = 0; piece < numberOfPieces; piece++) {
                    for (int i = 0; i < filePieces[piece].length; i++) {
                        mergedFile[index] = filePieces[piece][i];
                        index += 1;
                    }
                }
                try {
                    FileOutputStream file = new FileOutputStream(thisPeerId + commonProperties.getFileName());
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(file);
                    bufferedOutputStream.write(mergedFile);
                    bufferedOutputStream.close();
                    file.close();
                    System.out.println("File downloaded");
                    thisPeer.setHasFile(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public int getPieceIndex(int[] thisPeerBitfield, int[] connectedPeerBitfield, int len) {
            ArrayList<Integer> indices = new ArrayList<>();
            int i;
            for (i = 0; i < len; i++) {
                if (thisPeerBitfield[i] == 0 && connectedPeerBitfield[i] == 1) {
                    indices.add(i);
                }
            }
            Random r = new Random();
            if (indices.size() > 0) {
                int index = indices.get(Math.abs(r.nextInt() % indices.size()));
                return index;
            }
            return -1;
        }

        @Override
        public synchronized void run() {
            synchronized (this) {
                try {
                    long startTime;
                    long endTime;
                    double elapsedTime;
                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                    byte[] bitFieldMessage = Messages.getBitFieldMessage(thisPeer.getBitField());
                    Messages.sendMessage(socket, bitFieldMessage);
                    while (peersWithCompleteFile < peers.size()) {
                        int messageLengthField = dataInputStream.readInt();
                        byte[] input = new byte[messageLengthField];
                        startTime = System.nanoTime();
                        dataInputStream.readFully(input);
                        endTime = System.nanoTime();
                        elapsedTime = (double) (endTime - startTime) / 1000000000;

                        char messageType = (char) input[0];
                        byte[] message = new byte[messageLengthField - 1];
                        int index = 0;
                        for (int i = 1; i < messageLengthField; i++) {
                            message[index] = input[i];
                            index++;
                        }
                        int pieceIndex;
                        int bits;
                        switch (messageType) {
                            case CHOKE -> {
                                peers.get(peerId).setChoked(true);
                                System.out.println("Received CHOKE from " + peerId);
                            }
                            case UNCHOKE -> {
                                peers.get(peerId).setChoked(false);
                                pieceIndex = getPieceIndex(thisPeer.getBitField(), peers.get(peerId).getBitField()
                                        , thisPeer.getBitField().length);

                                if (pieceIndex != -1)
                                    Messages.sendMessage(socket, Messages.getRequestMessage(pieceIndex));
                            }
                            case INTERESTED -> {
                                peers.get(peerId).setInterested(true);
                                System.out.println("Received INTERESTED from - " + peerId);
                            }
                            case NOTINTERESTED -> {
                                System.out.println("Received NOTINTERESTED from " + peerId);
                                peers.get(peerId).setInterested(false);
                                if (!peers.get(peerId).isChoked()) {
                                    peers.get(peerId).setChoked(true);
                                    Messages.sendMessage(socket, Messages.getChokeMessage());
                                }
                            }
                            case HAVE -> {
                                System.out.println("Received HAVE from " + peerId);
                                pieceIndex = ByteBuffer.wrap(message).getInt();
                                peers.get(peerId).updateBitField(pieceIndex);
                                bits = 0;
                                for (int bit : peers.get(peerId).getBitField()) {
                                    if (bit == 1)
                                        bits++;
                                }
                                if (bits == thisPeer.getBitField().length) {
                                    peers.get(peerId).setHasFile(true);
                                    peersWithCompleteFile++;
                                }
                                if (checkIfInteresting(thisPeer.getBitField(), peers.get(peerId).getBitField()
                                        , thisPeer.getBitField().length))
                                    Messages.sendMessage(socket, Messages.getInterestedMessage());
                                else
                                    Messages.sendMessage(socket, Messages.getNotInterestedMessage());
                            }
                            case BITFIELD -> {
                                int[] bitField = new int[message.length / 4];
                                index = 0;
                                for (int i = 0; i < message.length; i += 4) {
                                    bitField[index] = ByteBuffer.wrap(Arrays.copyOfRange(message, i, i + 4)).getInt();
                                    index++;
                                }
                                System.out.println(Arrays.toString(bitField));
                                peers.get(peerId).setBitField(bitField);
                                bits = 0;
                                for (int x : peers.get(peerId).getBitField()) {
                                    if (x == 1)
                                        bits++;
                                }
                                if (bits == thisPeer.getBitField().length) {
                                    peers.get(peerId).setHasFile(true);
                                    peersWithCompleteFile++;
                                } else {
                                    peers.get(peerId).setHasFile(false);
                                }

                                if (checkIfInteresting(thisPeer.getBitField(), peers.get(peerId).getBitField()
                                        , thisPeer.getBitField().length))
                                    Messages.sendMessage(socket, Messages.getInterestedMessage());
                                else
                                    Messages.sendMessage(socket, Messages.getNotInterestedMessage());
                            }
                            case REQUEST -> {
                                pieceIndex = ByteBuffer.wrap(message).getInt();
                                Messages.sendMessage(socket, Messages.getPieceMessage(pieceIndex
                                        , filePieces[pieceIndex]));

                                System.out.println("Received REQUEST from " + peerId + " for piece " + ByteBuffer.wrap(message).getInt());
                            }
                            case PIECE -> {
                                pieceIndex = ByteBuffer.wrap(Arrays.copyOfRange(message, 0, 4)).getInt();
                                System.out.println("Received PIECE" + pieceIndex + " from " + peerId);
                                index = 0;
                                filePieces[pieceIndex] = new byte[message.length - 4];
                                for (int i = 4; i < message.length; i++) {
                                    filePieces[pieceIndex][index] = message[i];
                                    index++;
                                }
                                thisPeer.updateBitField(pieceIndex);
                                thisPeer.updateNumberOfPieces();
                                if (!peers.get(peerId).isChoked()) {
                                    int requestPieceIndex = getPieceIndex(thisPeer.getBitField(), peers.get(peerId).getBitField()
                                            , thisPeer.getBitField().length);

                                    if (requestPieceIndex != -1)
                                        Messages.sendMessage(socket, Messages.getRequestMessage(requestPieceIndex));
                                }
                                double rate = (double) (message.length + 5) / elapsedTime;
                                if (peers.get(peerId).hasFile()) {
                                    peers.get(peerId).setDownloadRate(-1);
                                } else {
                                    peers.get(peerId).setDownloadRate(rate);
                                }
                                System.out.println("Downloaded " + thisPeer.getNumberOfPieces()
                                        + " out of " + numberOfPieces);

                                checkIfCompleteFileDownloaded();
                                for (int peerId : peerSockets.keySet()) {
                                    Messages.sendMessage(peerSockets.get(peerId), Messages.getHaveMessage(pieceIndex));
                                }
                            }
                            default -> {
                            }
                        }
                    }
                    Thread.sleep(5000);
                    System.exit(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
    private boolean isChoked = true;
    private boolean isInterested = false;
    private double downloadRate = 0;

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

    public boolean hasFile() {
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

    public void updateBitField(int index) {
        this.bitField[index] = 1;
    }

    public int getNumberOfPieces() {
        return numberOfPieces;
    }

    public void updateNumberOfPieces() {

        //Bitfield length is the total number of pieces in the file. If total number of downloaded pieces is equal to
        // bitField.length it means that the peer has the complete file.
        this.numberOfPieces += 1;
        if (this.numberOfPieces == bitField.length)
            this.hasFile = true;
    }

    public boolean isChoked() {
        return isChoked;
    }

    public void setChoked(boolean choked) {
        this.isChoked = choked;
    }

    public boolean isInterested() {
        return isInterested;
    }

    public void setInterested(boolean interested) {
        isInterested = interested;
    }

    public double getDownloadRate() {
        return downloadRate;
    }

    public void setDownloadRate(double downloadRate) {
        this.downloadRate = downloadRate;
    }
}