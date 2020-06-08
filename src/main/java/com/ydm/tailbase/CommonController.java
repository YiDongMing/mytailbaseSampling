package com.ydm.tailbase;

import com.ydm.tailbase.clientprocess.ClientProcessData;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class CommonController {

  private static Integer DATA_SOURCE_PORT = 0;

  public static Integer getDataSourcePort() {
    return DATA_SOURCE_PORT;
  }

  @RequestMapping("/ready")
  public String ready() {
    return "suc";
  }

  @RequestMapping("/setParameter")
  public String setParamter(@RequestParam Integer port) {
    DATA_SOURCE_PORT = port;
    if (Util.isClientProcess()) {
      ClientProcessData.start();
    }
    return "suc";
  }

  @RequestMapping("/start")
  public String start() {
    return "suc";
  }



}
