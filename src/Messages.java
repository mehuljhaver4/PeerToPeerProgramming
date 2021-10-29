import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Messages {
    public static byte[] createFinalMessage(char type, byte[] payload) {
        byte[] finalMessage;
        byte messageType = (byte) type;
        int payloadLength = payload.length;
        int messageTypeLength = 1;
        int messageLength = payloadLength + messageTypeLength;

        int index;
        switch (type) {
            case MessageTypes.CHOKE, MessageTypes.UNCHOKE, MessageTypes.INTERESTED, MessageTypes.NOTINTERESTED:
                finalMessage = new byte[messageLength + 4];
                index = 0;
                for (byte b : ByteBuffer.allocate(4).putInt(messageLength).array()) {
                    finalMessage[index] = b;
                    index += 1;
                }
                finalMessage[index] = messageType;
                break;
            case MessageTypes.HAVE, MessageTypes.BITFIELD, MessageTypes.REQUEST, MessageTypes.PIECE:
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
        return createFinalMessage(MessageTypes.BITFIELD, payload);
    }

    public static byte[] getChokeMessage() {
        return createFinalMessage(MessageTypes.CHOKE, new byte[0]);
    }

    public static byte[] getUnchokeMessage() {
        return createFinalMessage(MessageTypes.UNCHOKE, new byte[0]);
    }

    public static byte[] getInterestedMessage() {
        return createFinalMessage(MessageTypes.INTERESTED, new byte[0]);
    }

    public static byte[] getNotInterestedMessage() {
        return createFinalMessage(MessageTypes.NOTINTERESTED, new byte[0]);
    }

    public static byte[] getRequestMessage(int pieceIndex) {
        byte[] payload = ByteBuffer.allocate(4).putInt(pieceIndex).array();
        return createFinalMessage(MessageTypes.REQUEST, payload);
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
        return createFinalMessage(MessageTypes.PIECE, payload);
    }

    public static byte[] getHaveMessage(int pieceIndex) {
        byte[] payload = ByteBuffer.allocate(4).putInt(pieceIndex).array();
        return createFinalMessage(MessageTypes.HAVE, payload);
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
