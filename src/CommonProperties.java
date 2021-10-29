import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

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