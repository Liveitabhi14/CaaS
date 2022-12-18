package com.blibli.caas.impl;

import com.blibli.caas.DTO.NodeStats;
import com.blibli.caas.service.ClusterService;
import com.blibli.caas.service.ExecuteCommandOnRemoteMachineService;
import com.blibli.caas.service.MetricService;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Connection;
import redis.clients.jedis.Protocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class MetricServiceImpl implements MetricService {
  @Autowired
  private ClusterService clusterService;

  @Autowired
  private ExecuteCommandOnRemoteMachineService executeCommandOnRemoteMachineService;



  @Value("${memory_threshold_in_percent}")
  private String memoryThreshold;

  private static final String USED_MEMORY = "used_memory";
  private static final String TOTAL_MEMORY = "total_system_memory";
  private static final String USED_CPU = "used_cpu_user";

  @Override
  public void checkNodeMemory(String userName, String password) {
    List<RedisClusterNode> redisClusterNodeList = clusterService.getClusterNode();
    log.info("node INfo - {}", redisClusterNodeList.toString());
    List<NodeStats> nodeStatsList = new ArrayList<>();


    for (RedisClusterNode redisClusterNode : redisClusterNodeList) {
      if (Objects.nonNull(redisClusterNode.getSlaveOf())) {

        Connection connection = new Connection(redisClusterNode.getUri().getHost(),
            redisClusterNode.getUri().getPort());
        log.info(" host - {} and port - {} ", redisClusterNode.getUri().getHost(),
            redisClusterNode.getUri().getPort());
        connection.sendCommand(Protocol.Command.INFO, "cpu", "memory");

        String info = connection.getBulkReply();

        NodeStats nodeStats = NodeStats.builder().nodeId(redisClusterNode.getNodeId())
            .host(redisClusterNode.getUri().getHost())
            .port(String.valueOf(redisClusterNode.getUri().getPort())).build();
        convertStats(info, nodeStats);

        nodeStatsList.add(nodeStats);

      }
    }

    checkNodeForUtilizationThreshold(nodeStatsList);
  }

  private void checkNodeForUtilizationThreshold(List<NodeStats> nodeStatsList) {
    log.info("Checking Nodes for utilization -  {}",nodeStatsList);
    for (NodeStats nodeStats : nodeStatsList){
        if(((nodeStats.getUsedMemory()/nodeStats.getTotalMemory()) * 100)>=Integer.parseInt(memoryThreshold)){
        }
    }
  }

  private void convertStats(String info, NodeStats nodeStats) {
    List<String> infoString = Arrays.asList(info.split(System.lineSeparator()));
    for (String stat : infoString) {
      List<String> statSplit = Arrays.asList(stat.split(":"));
      switch (statSplit.get(0)) {
        case USED_MEMORY:
          nodeStats.setUsedMemory(Long.parseLong(statSplit.get(1)));
          break;
        case TOTAL_MEMORY:
          nodeStats.setTotalMemory(Long.parseLong(statSplit.get(1)));
        case USED_CPU:
          nodeStats.setUsedCPU(Long.parseLong(statSplit.get(1)));
      }
    }
  }
}