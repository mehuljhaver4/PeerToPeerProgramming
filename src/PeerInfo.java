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