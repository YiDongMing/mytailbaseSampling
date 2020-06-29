package com.ydm.tailbase;

import com.ydm.tailbase.bankendprocess.*;
import com.ydm.tailbase.clientprocess.ClientMemory;
import com.ydm.tailbase.clientprocess.ClientProcessData;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.support.ClassPathXmlApplicationContext;

@EnableAutoConfiguration
@ComponentScan(basePackages = "com.ydm.tailbase")
public class MultiEntry {
    public static void main(String[] args) throws Exception{
        if (Util.isBackendProcess()) {
            BackendController.init();
            CheckSumService checkSumService = new CheckSumService();
            checkSumService.start();
            //BackendProvider.start();
        }
        if (Util.isClientProcess()) {
            ClientProcessData.init();
            //ClientMemory.start();
        }
        String port = System.getProperty("server.port", "8080");
        SpringApplication.run(MultiEntry.class,
                "--server.port=" + port
        );
    }
}
