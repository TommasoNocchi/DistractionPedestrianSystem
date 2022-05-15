package it.unipi.sample.samples.common;

public class Rilevation {
    private String nodeId;
    private int rssi;
    private long timestamp;

    public Rilevation(String nodeId, int rssi, long timestamp){
        this.nodeId = nodeId;
        this.rssi = rssi;
        this.timestamp = timestamp;
    }

    public String getNodeId() {
        return nodeId;
    }

    public int getRssi() {
        return rssi;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
