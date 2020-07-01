package com.ydm.tailbase.bankendprocess;

import com.ydm.tailbase.Constants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TraceIdBatch {
    private int batchPos = -1;
    private int processCount = 0;
    private Set<String> traceIdList = new HashSet<>(Constants.BATCH_SIZE / 10);
    private String port = "0"; //标记需要去哪个客户端取数据，8000表示客户端1，8002表示客户端2，

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public int getBatchPos() {
        return batchPos;
    }

    public void setBatchPos(int batchPos) {
        this.batchPos = batchPos;
    }

    public int getProcessCount() {
        return processCount;
    }

    public void setProcessCount(int processCount) {
        this.processCount = processCount;
    }

    public Set<String> getTraceIdList() {
        return traceIdList;
    }

    public void setTraceIdList(Set<String> traceIdList) {
        this.traceIdList = traceIdList;
    }

    @Override
    public String toString() {
        return "TraceIdBatch{" +
                "batchPos=" + batchPos +
                ", processCount=" + processCount +
                ", traceIdList=" + traceIdList +
                ", port='" + port + '\'' +
                '}';
    }
}
