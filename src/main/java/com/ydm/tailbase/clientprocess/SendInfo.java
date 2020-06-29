package com.ydm.tailbase.clientprocess;

public class SendInfo{

    private String json;

    private int batchPos;

    private String port;

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public int getBatchPos() {
        return batchPos;
    }

    public void setBatchPos(int batchPos) {
        this.batchPos = batchPos;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
}
