package com.blibli.caas.service;

import com.blibli.caas.DTO.ClusterNodes;
import com.blibli.caas.DTO.NodeStats;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import org.springframework.stereotype.Service;

import java.util.List;

public interface MetricService {
   List<NodeStats> checkNodeMemory(String userName, String password, boolean isAllData);

  List<ClusterNodes> getAllNodeInfo();
}
