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

    public static byte[] createFinalMessage(char type, byte[] payload) {
        byte[] finalMessage;
        byte messageType = (byte) type;
        int payloadLength = payload.length;
        int messageTypeLength = 1;
        int messageLength = payloadLength + messageTypeLength;

        int index;
        switch (type) {
            case CHOKE, UNCHOKE, INTERESTED, NOTINTERESTED:
                finalMessage = new byte[messageLength + 4];
                index = 0;
                for (byte b : ByteBuffer.allocate(4).putInt(messageLength).array()) {
                    finalMessage[index] = b;
                    index += 1;
                }
                finalMessage[index] = messageType;
                break;
            case HAVE, BITFIELD, REQUEST, PIECE:
                finalMessage = new byte[messageLength + 4];
                index = 0;
                for (byte b : ByteBuffer.allocate(4).putInt(messageLength).array()) {
                    finalMessage[index] = b;
                    index += 1;
                }
                finalMessage[index++] = messageType;
                for (byte b : payload) {
                    finalMessage[index] = b;
                    index += 1;
                }
                break;
            default:
                finalMessage = new byte[0];
                break;
        }
        return finalMessage;
    }

    public static byte[] getHandshakeMessage(int peerId) {
        byte[] handShakeMessage = new byte[32];
        byte[] header = "P2PFILESHARINGPROJ".getBytes(StandardCharsets.UTF_8);
        byte[] zeroBits = "0000000000".getBytes(StandardCharsets.UTF_8);
        byte[] thisPeerId = ByteBuffer.allocate(4).putInt(peerId).array();

        int index = 0;
        for (var headerByte : header) {
            handShakeMessage[index] = headerByte;
            index += 1;
        }

        for (var zeroByte : zeroBits) {
            handShakeMessage[index] = zeroByte;
            index += 1;
        }

        for (var peerIdByte : thisPeerId) {
            handShakeMessage[index] = peerIdByte;
            index += 1;
        }
        return handShakeMessage;
    }

    public static byte[] getBitFieldMessage(int[] bitField) {
        int payloadLength = 4 * bitField.length;
        byte[] payload = new byte[payloadLength];
        int index = 0;
        for (int bit : bitField) {
            byte[] bitBytes = ByteBuffer.allocate(4).putInt(bit).array();
            for (byte b : bitBytes) {
                payload[index] = b;
                index++;
            }
        }
        return createFinalMessage(BITFIELD, payload);
    }

    public static byte[] getChokeMessage() {
        return createFinalMessage(CHOKE, new byte[0]);
    }

    public static byte[] getUnchokeMessage() {
        return createFinalMessage(UNCHOKE, new byte[0]);
    }

    public static byte[] getInterestedMessage() {
        return createFinalMessage(INTERESTED, new byte[0]);
    }

    public static byte[] getNotInterestedMessage() {
        return createFinalMessage(NOTINTERESTED, new byte[0]);
    }

    public static byte[] getRequestMessage(int pieceIndex) {
        byte[] payload = ByteBuffer.allocate(4).putInt(pieceIndex).array();
        return createFinalMessage(REQUEST, payload);
    }

    public static byte[] getPieceMessage(int pieceIndex, byte[] piece) {
        int pieceIndexLength = 4;
        byte[] payload = new byte[pieceIndexLength + piece.length];

        byte[] pieceIndexBytes = ByteBuffer.allocate(4).putInt(pieceIndex).array();
        int index = 0;
        for (byte b : pieceIndexBytes) {
            payload[index] = b;
            index += 1;
        }
        for (byte b : piece) {
            payload[index] = b;
            index += 1;
        }
        return createFinalMessage(PIECE, payload);
    }

    public static byte[] getHaveMessage(int pieceIndex) {
        byte[] payload = ByteBuffer.allocate(4).putInt(pieceIndex).array();
        return createFinalMessage(HAVE, payload);
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
