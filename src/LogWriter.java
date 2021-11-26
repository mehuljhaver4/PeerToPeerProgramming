import java.io.*;
import java.util.ArrayList;
import java.util.Date;

public class LogWriter {
    private static int thisPeerId;
    private static BufferedWriter bufferedWriter;
    private static int numberOfPieces = 0;

    public static void startLogger(int peerId) {
        try {
            thisPeerId = peerId;
            FileOutputStream fileOutputStream = new FileOutputStream("log_peer_" + thisPeerId + ".log");
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
        } catch (FileNotFoundException e) {
            System.err.println(e);
        }
    }

    public static String makeTCPConnection(int peerId) {
        String s = "";
        try {
            String date = (new Date()).toString();
            s = date + " : Peer " + thisPeerId + " makes a connection to Peer " + peerId + ".";
            bufferedWriter.append(s);
            bufferedWriter.newLine();
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            System.err.println(e);
        }
        return s;
    }

    public static String madeTCPConnection(int PeerId) {
        String s = "";
        try {
            String date = (new Date()).toString();
            s = date + " : Peer " + thisPeerId + " is connected from Peer " + PeerId + ".";
            bufferedWriter.append(s);
            bufferedWriter.newLine();
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            System.err.println(e);
        }
        return s;
    }

    public static String changePreferredNeighbors(ArrayList<Integer> neighbors) {
        String s = "";
        StringBuffer sb = new StringBuffer();
        try {
            String date = (new Date()).toString();
            s = date + " : Peer " + thisPeerId + " has the preferred neighbors ";
            sb = new StringBuffer(s);

            for (int neighbor : neighbors) {
                sb.append(neighbor).append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append(".");

            bufferedWriter.append(sb);
            bufferedWriter.newLine();
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            System.err.println(e);
        }
        return sb.toString();
    }

    public static String changeOptimisticallyUnchokedNeighbor(int peerId) {
        String s = "";
        try {
            String date = (new Date()).toString();
            s = date + " : Peer " + thisPeerId + " has the optimistically unchoked neighbor " + peerId + ".";
            bufferedWriter.append(s);
            bufferedWriter.newLine();
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            System.err.println(e);
        }
        return s;
    }

    public static String unchoked(int PeerId) {
        String s = "";
        try {
            String date = (new Date()).toString();
            s = date + " : Peer " + thisPeerId + " is unchoked by " + PeerId + ".";
            bufferedWriter.append(s);
            bufferedWriter.newLine();
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            System.err.println(e);
        }
        return s;
    }

    public static String choked(int PeerId) {
        String s = "";
        try {
            String date = (new Date()).toString();
            s = date + " : Peer " + thisPeerId + " is choked by " + PeerId + ".";
            bufferedWriter.append(s);
            bufferedWriter.newLine();
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            System.err.println(e);
        }
        return s;
    }

    public static String receiveHave(int PeerId, int pieceIndex) {
        String s = "";
        try {
            String date = (new Date()).toString();
            s = date + " : Peer " + thisPeerId + " received the 'have' message from Peer " + PeerId + " for the piece " + pieceIndex + ".";
            bufferedWriter.append(s);
            bufferedWriter.newLine();
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            System.err.println(e);
        }
        return s;
    }

    public static String receiveInterested(int PeerId) {
        String s = "";
        try {
            String date = (new Date()).toString();
            s = date + " : Peer " + thisPeerId + " received the 'interested' message from Peer " + PeerId + ".";
            bufferedWriter.append(s);
            bufferedWriter.newLine();
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            System.err.println(e);
        }
        return s;
    }

    public static String receiveNotInterested(int PeerId) {
        String s = "";
        try {
            String date = (new Date()).toString();
            s = date + " : Peer " + thisPeerId + " received the 'not interested' message from Peer " + PeerId + ".";
            bufferedWriter.append(s);
            bufferedWriter.newLine();
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            System.err.println(e);
        }
        return s;
    }

    public static String downloadPiece(int PeerId, int pieceIndex) {
        String s = "";
        ++numberOfPieces;
        try {
            String date = (new Date()).toString();
            s = date + " : Peer " + thisPeerId + " has downloaded the piece " + pieceIndex + " from Peer " + PeerId + "."
                    + " Now the number of pieces it has is " + numberOfPieces + ".";
            bufferedWriter.append(s);
            bufferedWriter.newLine();
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            System.err.println(e);
        }
        return s;
    }

    public static String downloadComplete() {
        String s = "";
        try {
            String date = (new Date()).toString();
            s = date + " : Peer " + thisPeerId + " has downloaded the complete file.";
            bufferedWriter.append(s);
            bufferedWriter.newLine();
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            System.err.println(e);
        }
        return s;
    }

    public static void closeLogger() {
        try {
            bufferedWriter.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}
