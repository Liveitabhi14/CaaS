package com.blibli.caas.controller;

import com.blibli.caas.service.ClusterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value = "/cluster")
public class ClusterController {

  private ClusterService clusterService;

  @Autowired
  ClusterController(ClusterService clusterService) {
    this.clusterService = clusterService;
  }

  @GetMapping(value = "/addNewNode")
  public String  addNewNodeToRedisController(@RequestParam String newRedisHost,
      @RequestParam String newRedisPort, @RequestParam String clusterHost,
      @RequestParam String clusterPort, @RequestParam Boolean isSlave,
      @RequestParam String masterId,@RequestParam Boolean isNeedRebalance) {
    return clusterService.addNewNodeToCLuster(newRedisHost, newRedisPort, clusterHost, clusterPort,
        isSlave, masterId,isNeedRebalance);
  }

  @GetMapping(value = "/clusterRebalance")
  public String clusterRebalance(@RequestParam String clusterHost, @RequestParam String  clusterPort
  , @RequestParam Boolean isEmptySlotReBalance) {
    return clusterService.clusterRebalance(clusterHost,clusterPort,isEmptySlotReBalance);
  }

}
