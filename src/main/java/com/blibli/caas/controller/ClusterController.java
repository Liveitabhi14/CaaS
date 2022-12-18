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

  @Autowired
  private ClusterService clusterService;

  @GetMapping(value = "/addNode")
  public String addNodeToCluster(@RequestParam String newRedisHost,
      @RequestParam String newRedisPort, @RequestParam String clusterHost,
      @RequestParam String clusterPort, @RequestParam Boolean isSlave,
      @RequestParam(required = false) String masterId, @RequestParam Boolean isNeedRebalance,
      @RequestParam String username, @RequestParam String password) {
    return clusterService.addNewNodeToCLuster(newRedisHost, newRedisPort, clusterHost, clusterPort,
        isSlave, masterId, isNeedRebalance, username, password);
  }

  @GetMapping(value = "/rebalance")
  public String clusterRebalance(@RequestParam String clusterHost, @RequestParam String clusterPort,
      @RequestParam Boolean isEmptySlotReBalance, @RequestParam String username,
      @RequestParam String password) {
    return clusterService.clusterRebalance(clusterHost, clusterPort, isEmptySlotReBalance, username,
        password);
  }

  @GetMapping(value = "/hashSlot/reshard")
  public String reshardHashSlots(@RequestParam String clusterHost, @RequestParam String clusterPort,
      @RequestParam String sourceNodeId, @RequestParam String targetNodeId,
      @RequestParam int noOfSlots, @RequestParam String username, @RequestParam String password) {
    return clusterService.reshardHashSlotsBetweenNodes(clusterHost, clusterPort, sourceNodeId,
        targetNodeId, noOfSlots, username, password);
  }

  @GetMapping(value = "/deleteNode")
  public String deleteNodeFromCluster(@RequestParam String clusterHost,
      @RequestParam String clusterPort, @RequestParam String deleteNodeHost, @RequestParam Integer deleteNodePort, @RequestParam String username,
      @RequestParam String password) {
    return clusterService.deleteNodeFromCluster(clusterHost, clusterPort, deleteNodeHost, deleteNodePort, username,
        password);
  }

  @GetMapping(value = "/getInfo")
  public String getClusterInfo(@RequestParam String clusterHost,
      @RequestParam Integer clusterPort) {
    return clusterService.getClusterInfo(clusterHost, clusterPort);
  }

  @GetMapping(value = "/getNodeId")
  public String getNodeId(@RequestParam String clusterHost, @RequestParam Integer clusterPort) {
    return clusterService.getNodeIdInCluster(clusterHost, clusterPort);
  }

  @GetMapping(value = "/hashSlot/countInNode")
  public Integer countHashSlotsInNode(@RequestParam String clusterHost,
      @RequestParam Integer clusterPort, @RequestParam String nodeId) {
    return clusterService.countSlotsInNode(clusterHost, clusterPort, nodeId);
  }

  @GetMapping(value = "/resetHard")
  public String resetHard(@RequestParam String clusterHost, @RequestParam Integer clusterPort) {
    return clusterService.clusterResetHard(clusterHost,clusterPort);
  }

  @GetMapping(value = "/flushDb")
  public String flushDb(@RequestParam String clusterHost, @RequestParam Integer clusterPort) {
    return clusterService.flushDb(clusterHost,clusterPort);
  }

}
