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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Connection;
import redis.clients.jedis.Protocol;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  @Value("${ssh.username}")
  private String userName;

  @Value("${ssh.password}")
  private String password;


  @Value("${upper_memory_threshold_in_percent}")
  private String upperMemoryThreshold;

  @Value("${lower_memory_threshold_in_percent}")
  private String lowerMemoryThreshold;

  @Value("${redis.new.master.node.host}")
  private String newMasterHost;

  @Value("${redis.new.master.node.port}")
  private String newMasterPort;

  @Value("${redis.new.slave.node.host}")
  private String newSlaveHost;

  @Value("${redis.new.slave.node.port}")
  private String newSlavePort;

  @Value("${primary.redis.uri.host}")
  private String primaryRedisHost;

  @Value("${primary.redis.uri.port}")
  private String primaryRedisPort;

  @Value("${redis.minimum.master}")
  private String minimumMasterNode;

  private static final String USED_MEMORY = "used_memory";
  private static final String TOTAL_MEMORY = "maxmemory";
  private static final String USED_CPU = "used_cpu_user";

  @Override
  public List<NodeStats> checkNodeMemory(String userName, String password, boolean isAllData) {
    List<NodeStats> nodeStatsList = getNodeStats(isAllData);

    Map<String,List<NodeStats>> masterNodeIdToSlaveMap = getSlaveNodeMap(nodeStatsList);
    checkNodeForUtilizationThreshold(nodeStatsList,masterNodeIdToSlaveMap,userName,password);
    return nodeStatsList;
  }

  private Map<String, List<NodeStats>> getSlaveNodeMap(List<NodeStats> nodeStatsList) {
    Map<String, List<NodeStats>> slaveNodeMap = new HashMap<>();
    for (NodeStats nodeStats : nodeStatsList) {
      List slaveList = slaveNodeMap.getOrDefault(nodeStats.getMasterId(), new ArrayList<>());
      slaveList.add(nodeStats);
      slaveNodeMap.put(nodeStats.getMasterId(), slaveList);
    }
    return slaveNodeMap;
  }

  private List<NodeStats> getNodeStats(boolean isAllData) {
    List<RedisClusterNode> redisClusterNodeList = clusterService.getClusterNode();
    log.info("node INfo - {}", redisClusterNodeList.toString());
    List<NodeStats> nodeStatsList = new ArrayList<>();


    for (RedisClusterNode redisClusterNode : redisClusterNodeList) {

        Connection connection = new Connection(redisClusterNode.getUri().getHost(),
            redisClusterNode.getUri().getPort());
        log.info(" host - {} and port - {} ", redisClusterNode.getUri().getHost(),
            redisClusterNode.getUri().getPort());
        connection.sendCommand(Protocol.Command.INFO, "cpu", "memory", "Replication");

          String info = connection.getBulkReply();

        NodeStats nodeStats = NodeStats.builder().nodeId(redisClusterNode.getNodeId())
            .host(redisClusterNode.getUri().getHost())
            .port(String.valueOf(redisClusterNode.getUri().getPort())).masterId(redisClusterNode.getSlaveOf())
            .slots(redisClusterNode.getSlots().size()).build();
        convertStats(info, nodeStats);

        nodeStatsList.add(nodeStats);

      }

    return nodeStatsList;
  }

  @Override
  @Scheduled(fixedDelay = 30000)
  public void scheduleNodeCheck() {

    log.info("Starting memory check cron");
    checkNodeMemory(userName, password,false);


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
        (nodeStats.getUsedMemory() * 100) / nodeStats.getTotalMemory();
    return BigDecimal.valueOf(memoryUsagePercentage).setScale(2, RoundingMode.CEILING)
        .doubleValue();

  }
  private void checkNodeForUtilizationThreshold(List<NodeStats> nodeStatsList,
      Map<String, List<NodeStats>> masterNodeIdToSlaveMap, String userName, String password) {
    List<NodeStats> removeNodeList = new ArrayList<>();
    log.info("Checking Nodes for utilization -  {}", nodeStatsList);
    long masterNodeSize = nodeStatsList.stream().filter(nodeStats -> !nodeStats.isSlave()).count();
    for (NodeStats nodeStats : nodeStatsList) {
      if(!nodeStats.isSlave()) {
        if (((nodeStats.getUsedMemory() / nodeStats.getTotalMemory()) * 100) >= Integer.parseInt(
            upperMemoryThreshold)) {

          log.info(
              "Adding new master node host - {} and port - {} for over utilization on host - {} and port -"
                  + " {}", newMasterHost, newMasterPort, nodeStats.getHost(), nodeStats.getPort());
          clusterService.addNewNodeToCLuster(newMasterHost, newMasterPort, nodeStats.getHost(),
              nodeStats.getPort(), false, nodeStats.getNodeId(), true, userName, password);
          String masterNodeId = clusterService.getNodeIdInCluster(newMasterHost,Integer.parseInt(newMasterPort));

          log.info(
              "Adding new slave node host - {} and port - {} for master host - {} and Port" + " {}",
              newSlaveHost, newSlavePort, newMasterHost, newMasterPort);
          clusterService.addNewNodeToCLuster(newSlaveHost, newSlavePort, nodeStats.getHost(),
              nodeStats.getPort(), true, masterNodeId, false, userName, password);
          removeNodeList.clear();
          break;
        }

        if (masterNodeSize - removeNodeList.size() > Integer.parseInt(minimumMasterNode) && removeNodeList.size() < 1 && (
            !nodeStats.getHost().equals(primaryRedisHost) || !nodeStats.getPort()
                .equals(primaryRedisPort))
            && ((nodeStats.getUsedMemory() / nodeStats.getTotalMemory()) * 100) <= Integer.parseInt(
            lowerMemoryThreshold)) {

          removeNodeList.add(nodeStats);

        }
      }
    }
    if (!CollectionUtils.isEmpty(removeNodeList)) {
      nodeStatsList.removeAll(removeNodeList);
      NodeStats clusterNode =
          nodeStatsList.stream().filter(nodeStats -> !nodeStats.isSlave()).findFirst().get();
      String host = clusterNode.getHost();
      String port = clusterNode.getPort();

      for (NodeStats nodeStats : removeNodeList) {

        List<NodeStats> slaveNodeList = masterNodeIdToSlaveMap.get(nodeStats.getMasterId());
        if (Objects.nonNull(slaveNodeList)) {
          for (NodeStats slaveNode : nodeStatsList) {
            log.info(
                "Deleting slave node host - {} and port - {} of master - {} using cluster host - {}"
                    + " and port - {}", slaveNode.getHost(), slaveNode.getPort(),
                slaveNode.getMasterId(), host, port);
            clusterService.deleteNodeFromCluster(host, port, slaveNode.getHost(),
                Integer.parseInt(slaveNode.getPort()), userName, password, true);
          }
        }
        log.info("Deleting node - {} using cluster host and port - {}:{}",nodeStats.getPort()+nodeStats.getPort(),host,port);
        clusterService.deleteNodeFromCluster(host, port, nodeStats.getHost(),
            Integer.parseInt(nodeStats.getPort()), userName, password,false);
      }

    }

  }

  private void convertStats(String info, NodeStats nodeStats) {
    List<String> infoString = Arrays.asList(info.split(System.lineSeparator()));
    for (String stat : infoString) {
      List<String> statSplit = Arrays.asList(stat.split(COLON));
      switch (statSplit.get(0)) {
        case USED_MEMORY:
          nodeStats.setUsedMemory(
              Double.parseDouble(statSplit.get(1).replaceAll("(\\r|\\n|\\t)", "")));
          break;
        case TOTAL_MEMORY:
          nodeStats.setTotalMemory(
              Double.parseDouble(statSplit.get(1).replaceAll("(\\r|\\n|\\t)", "")));
        case USED_CPU:
          nodeStats.setUsedCPU(
              Double.parseDouble(statSplit.get(1).replaceAll("(\\r|\\n|\\t)", "")));
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
