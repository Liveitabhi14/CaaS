package com.blibli.caas.impl;

import com.blibli.caas.DTO.ClusterNodes;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class MetricServiceImpl implements MetricService {
  public static final String MASTER = "master";
  public static final String ROLE = "role";
  public static final String MASTER_HOST = "master_host";
  public static final String MASTER_PORT = "master_port";
  public static final String SLAVE = "slave";
  public static final String COLON = ":";
  public static final String EMPTY_STRING = "";
  @Autowired
  private ClusterService clusterService;

  @Autowired
  private ExecuteCommandOnRemoteMachineService executeCommandOnRemoteMachineService;



  @Value("${memory_threshold_in_percent}")
  private String memoryThreshold;

  @Value("${system.user.name}")
  private String userName;

  @Value("${system.user.password}")
  private String password;

  private static final String USED_MEMORY = "used_memory";
  private static final String TOTAL_MEMORY = "maxmemory";
  private static final String USED_CPU = "used_cpu_user";

  @Override
  public List<NodeStats> checkNodeMemory(String userName, String password, boolean isAllData) {
    List<NodeStats> nodeStatsList = getNodeStats(isAllData);

    checkNodeForUtilizationThreshold(nodeStatsList);
    return nodeStatsList;
  }

  private List<NodeStats> getNodeStats(boolean isAllData) {
    List<RedisClusterNode> redisClusterNodeList = clusterService.getClusterNode();
    log.info("node INfo - {}", redisClusterNodeList.toString());
    List<NodeStats> nodeStatsList = new ArrayList<>();


    for (RedisClusterNode redisClusterNode : redisClusterNodeList) {
      if (isAllData || Objects.isNull(redisClusterNode.getSlaveOf())) {

        Connection connection = new Connection(redisClusterNode.getUri().getHost(),
            redisClusterNode.getUri().getPort());
        log.info(" host - {} and port - {} ", redisClusterNode.getUri().getHost(),
            redisClusterNode.getUri().getPort());
        connection.sendCommand(Protocol.Command.INFO, "cpu", "memory", "Replication");

        String info = connection.getBulkReply();

        NodeStats nodeStats = NodeStats.builder().nodeId(redisClusterNode.getNodeId())
            .host(redisClusterNode.getUri().getHost())
            .port(String.valueOf(redisClusterNode.getUri().getPort()))
            .slots(redisClusterNode.getSlots().size()).build();
        convertStats(info, nodeStats);

        nodeStatsList.add(nodeStats);

      }
    }
    return nodeStatsList;
  }

  @Override
  public List<ClusterNodes> getAllNodeInfo() {
    List<NodeStats> redisClusterNodes =  getNodeStats(true);
    return createClusterNodes(redisClusterNodes);
  }

  private List<ClusterNodes> createClusterNodes(List<NodeStats> redisClusterNodes) {
    List<ClusterNodes> clusterNodesList = new ArrayList<>();
    for (NodeStats nodeStats : redisClusterNodes) {
      clusterNodesList.add(ClusterNodes.builder().nodeHostPort(nodeStats.getHost() + COLON + nodeStats.getPort())
          .nodeId(nodeStats.getNodeId()).isSlave(nodeStats.isSlave())
          .usedMemory(nodeStats.getUsedMemory()).totalMemory(nodeStats.getTotalMemory())
          .masterNodeHostPort(nodeStats.isSlave() ? nodeStats.getMasterHost() + COLON + nodeStats.getMasterPort():
              EMPTY_STRING).slots(
              nodeStats.getSlots()).totalMemory(nodeStats.getTotalMemory()).memoryUsage(
              getMemoryUsage(nodeStats)).role(nodeStats.isSlave() ? SLAVE : MASTER)
              .currentTime(System.currentTimeMillis())
          .build());
    }
    clusterNodesList.sort(Comparator.comparing(ClusterNodes::getNodeHostPort));
    clusterNodesList.sort(Comparator.comparing(ClusterNodes::isSlave).thenComparing(ClusterNodes::getNodeHostPort));
    return clusterNodesList;
  }

  private static double getMemoryUsage(NodeStats nodeStats) {

    double memoryUsagePercentage =
        ((double) nodeStats.getUsedMemory() * 100) / (double) nodeStats.getTotalMemory();
    return BigDecimal.valueOf(memoryUsagePercentage).setScale(2, RoundingMode.CEILING)
        .doubleValue();
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
      List<String> statSplit = Arrays.asList(stat.split(COLON));
      switch (statSplit.get(0)) {
        case USED_MEMORY:
          nodeStats.setUsedMemory(Long.parseLong(statSplit.get(1).replace("\r", "")));
          break;
        case TOTAL_MEMORY:
          nodeStats.setTotalMemory(Long.parseLong(statSplit.get(1).replace("\r", "")));
          break;
        case USED_CPU:
          nodeStats.setUsedCPU(Double.parseDouble(statSplit.get(1).replace("\r", "")));
          break;
        case ROLE:
          nodeStats.setSlave(!MASTER.equals(statSplit.get(1).replace("\r", "")));
          break;
        case MASTER_HOST:
          nodeStats.setMasterHost(statSplit.get(1).replace("\r", ""));
          break;
        case MASTER_PORT:
          nodeStats.setMasterPort(statSplit.get(1).replace("\r", ""));
          break;

      }
    }
  }
}
