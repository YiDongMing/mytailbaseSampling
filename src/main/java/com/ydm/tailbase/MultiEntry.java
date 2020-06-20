package com.ydm.tailbase;

import com.ydm.tailbase.bankendprocess.BackendController;
import com.ydm.tailbase.bankendprocess.BackendProvider;
import com.ydm.tailbase.bankendprocess.BackendServiceImpl;
import com.ydm.tailbase.bankendprocess.CheckSumService;
import com.ydm.tailbase.clientprocess.ClientProcessData;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@EnableAutoConfiguration
@ComponentScan(basePackages = "com.ydm.tailbase")
public class MultiEntry {
    public static void main(String[] args) {
        if (Util.isBackendProcess()) {
            BackendServiceImpl.init();
            CheckSumService.start();
            BackendProvider.start();
        }
        if (Util.isClientProcess()) {
            ClientProcessData.init();
        }
        String port = System.getProperty("server.port", "8080");
        SpringApplication.run(MultiEntry.class,
                "--server.port=" + port
        );
    }
}
