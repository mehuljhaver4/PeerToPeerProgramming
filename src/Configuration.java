import java.io.*;
import java.util.Arrays;
import java.util.LinkedHashMap;

public class Configuration {

    static LinkedHashMap<Integer, Peer> getAllPeersInfo() {
        LinkedHashMap<Integer, Peer> peers = new LinkedHashMap<>();
        try {
            BufferedReader peerInfo =
                    new BufferedReader(new FileReader("Config Files/PeerInfo.cfg"));
            Object[] peerInfoLines = peerInfo.lines().toArray();
            for (var peerInfoLine : peerInfoLines) {
                int peerId = Integer.parseInt(peerInfoLine.toString().split(" ")[0]);
                String hostName = peerInfoLine.toString().split(" ")[1];
                int portNumber = Integer.parseInt(peerInfoLine.toString().split(" ")[2]);
                boolean hasFile = Integer.parseInt(peerInfoLine.toString().split(" ")[3]) == 1;
                Peer peer = new Peer(peerId, hostName, portNumber, hasFile);
                peers.put(peerId, peer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return peers;
    }

    static void setThisPeerFileInfo(LinkedHashMap<Integer, Peer> peers, int thisPeerId
            , CommonProperties commonProperties) {
        try {
            Peer thisPeer = peers.get(thisPeerId);
            int fileSize = commonProperties.getFileSize();
            int pieceSize = commonProperties.getPieceSize();
            int numberOfPieces = commonProperties.getNumberOfPieces();
            int[] bitField = new int[numberOfPieces];
            byte[][] filePieces = new byte[numberOfPieces][];

            if (thisPeer.hasFile()) {
                //Bitfield will have all 1s if this peer has the full file
                Arrays.fill(bitField, 1);
                thisPeer.setBitField(bitField);

                //Read the file using stream and assign to fileBytes
                BufferedInputStream file = new BufferedInputStream(new FileInputStream(commonProperties.getFileName()));
                byte[] fileBytes = new byte[fileSize];
                file.read(fileBytes);
                file.close();
                int filePart = 0;

                //Assigning file pieces to filePieces
                for (int counter = 0; counter < fileSize; counter += pieceSize) {

                    //Fill the filePieces for the part bytes from range counter to counter + pieceSize
                    if (counter + pieceSize <= fileSize)
                        filePieces[filePart] = Arrays.copyOfRange(fileBytes, counter, counter + pieceSize);

                        //Else will be used for the final few bytes left which is less than the piece size
                    else
                        filePieces[filePart] = Arrays.copyOfRange(fileBytes, counter, fileSize);
                    filePart += 1;
                    thisPeer.updateNumberOfPieces();
                }
            } else {
                Arrays.fill(bitField, 0);
                thisPeer.setBitField(bitField);
            }
            thisPeer.setFilePieces(filePieces);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static CommonProperties getCommonProperties() {
        CommonProperties commonProperties = new CommonProperties();
        try {
            BufferedReader commonInfo =
                    new BufferedReader(new FileReader("Config Files/Common.cfg"));

            Object[] commonLines = commonInfo.lines().toArray();
            commonProperties.setPreferredNeighbors(Integer.parseInt(commonLines[0].toString().split(" ")[1]));
            commonProperties.setUnchokingInterval(Integer.parseInt(commonLines[1].toString().split(" ")[1]));
            commonProperties.setOptimisticUnchokingInterval(Integer.parseInt(commonLines[2].toString().split(" ")[1]));
            commonProperties.setFileName(commonLines[3].toString().split(" ")[1]);
            commonProperties.setFileSize(Integer.parseInt(commonLines[4].toString().split(" ")[1]));
            commonProperties.setPieceSize(Integer.parseInt(commonLines[5].toString().split(" ")[1]));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return commonProperties;
    }
}
