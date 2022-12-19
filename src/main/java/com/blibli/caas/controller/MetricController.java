package com.blibli.caas.controller;

import com.blibli.caas.DTO.ClusterNodes;
import com.blibli.caas.DTO.ClusterNodesResponse;
import com.blibli.caas.service.MetricService;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController(value = "/metrics")
public class MetricController {

  @Autowired
  private MetricService metricService;

  @GetMapping("/allNodes")
  public List<ClusterNodes> getAllNodesMetric() {
    List<ClusterNodes> clusterNodesList = metricService.getAllNodeInfo();
    System.out.println(System.currentTimeMillis());
    return clusterNodesList;
//    return ClusterNodesResponse.builder().cluserNodes(clusterNodesList).build();
  }
}
