package it.unipi.sample.samples.common;

public class Message {
    private String nodeId;
    private double rssi;
    private long timestamp;
    private double stepCount;

    public Message(String nodeId, double rssi, long timestamp, double stepCount){
        this.nodeId = nodeId;
        this.rssi = rssi;
        this.timestamp = timestamp;
        this.stepCount = stepCount;
    }

    public String getNodeId() {
        return nodeId;
    }

    public double getRssi() {
        return rssi;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setRssi(double rssi) {
        this.rssi = rssi;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getStepCount() { return stepCount;}

    public void setStepCount(double stepCount) { this.stepCount = stepCount;}
}
