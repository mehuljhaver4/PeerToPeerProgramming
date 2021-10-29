import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Messages {
    private static final char CHOKE = '0';
    private static final char UNCHOKE = '1';
    private static final char INTERESTED = '2';
    private static final char NOTINTERESTED = '3';
    private static final char HAVE = '4';
    private static final char BITFIELD = '5';
    private static final char REQUEST = '6';
    private static final char PIECE = '7';

    public static byte[] createMessage(int len, char type, byte[] payload) {
        byte[] message;
        byte[] length;
        byte messageType = (byte) type;
        int index;
        switch (type) {
            case CHOKE, UNCHOKE, INTERESTED, NOTINTERESTED:
                message = new byte[len + 4];
                length = ByteBuffer.allocate(4).putInt(len).array();
                index = 0;
                for (byte x : length) {
                    message[index] = x;
                    index++;
                }
                message[index] = messageType;
                break;
            case HAVE, BITFIELD, REQUEST, PIECE:
                message = new byte[len + 4];
                length = ByteBuffer.allocate(4).putInt(len).array();
                index = 0;
                for (byte x : length) {
                    message[index] = x;
                    index++;
                }
                message[index++] = messageType;
                for (byte x : payload) {
                    message[index] = x;
                    index++;
                }
                break;
            default:
                message = new byte[0];
                break;
        }
        return message;
    }

    public static byte[] getHandshakeMessage(int peerId) {
        byte[] handShakeMessage = new byte[32];
        byte[] header = "P2PFILESHARINGPROJ".getBytes(StandardCharsets.UTF_8);
        byte[] zeroBits = "0000000000".getBytes(StandardCharsets.UTF_8);
        byte[] thisPeer = ByteBuffer.allocate(4).putInt(peerId).array();

        int index = 0;
        for (var headerByte : header) {
            handShakeMessage[index] = headerByte;
            index += 1;
        }

        for (var zeroByte : zeroBits) {
            handShakeMessage[index] = zeroByte;
            index += 1;
        }

        for (var peerIdByte : thisPeer) {
            handShakeMessage[index] = peerIdByte;
            index += 1;
        }
        return handShakeMessage;
    }

    public static byte[] getBitFieldMessage(int[] bitField) {
        int length = 1 + (4 * bitField.length);
        byte[] payload = new byte[length - 1];
        int index = 0;
        for (int bit : bitField) {
            byte[] bitBytes = ByteBuffer.allocate(4).putInt(bit).array();
            for (byte b : bitBytes) {
                payload[index] = b;
                index++;
            }
        }
        return createMessage(length, BITFIELD, payload);
    }

    public static byte[] getChokeMessage() {
        return createMessage(1, CHOKE, null);
    }

    public static byte[] getUnchokeMessage() {
        return createMessage(1, UNCHOKE, null);
    }

    public static byte[] getInterestedMessage() {
        return createMessage(1, INTERESTED, null);
    }

    public static byte[] getNotInterestedMessage() {
        return createMessage(1, NOTINTERESTED, null);
    }

    public static byte[] getRequestMessage(int pieceIndex) {
        byte[] payload = ByteBuffer.allocate(4).putInt(pieceIndex).array();
        return createMessage(5, REQUEST, payload);
    }

    public static byte[] getPieceMessage(int pieceIndex, byte[] piece) {
        byte[] payload = new byte[4 + piece.length];
        int counter = 0;
        byte[] indexBytes = ByteBuffer.allocate(4).putInt(pieceIndex).array();
        for (byte bit : indexBytes) {
            payload[counter] = bit;
            counter++;
        }
        for (byte bit : piece) {
            payload[counter] = bit;
            counter++;
        }
        return createMessage((5 + piece.length), PIECE, payload);
    }

    public static byte[] getHaveMessage(int pieceIndex) {
        byte[] payload = ByteBuffer.allocate(4).putInt(pieceIndex).array();
        return createMessage(5, HAVE, payload);
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
