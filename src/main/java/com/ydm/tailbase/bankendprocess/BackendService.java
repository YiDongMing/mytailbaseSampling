package com.ydm.tailbase.bankendprocess;

import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


public interface BackendService {
    public void setWrongTraceId(String traceIdListJson,int batchPos,String port);

    public void setWrongTraceToMap(Map<String, List<String>> map,int batchPos);
}
