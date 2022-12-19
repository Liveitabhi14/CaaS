package com.blibli.caas.impl;

import com.blibli.caas.DTO.ClusterNodes;
import com.blibli.caas.DTO.NodeStats;
import com.blibli.caas.service.ClusterService;
import com.blibli.caas.service.ExecuteCommandOnRemoteMachineService;
import com.blibli.caas.service.MetricService;
import io.lettuce.core.RedisURI;
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
import java.util.Collections;
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

  @Value("${redis.uri.node}")
  private String redisUriNode;
  @Value("${ssh.username}")
  private String userName;

  @Value("${ssh.password}")
  private String password;


  @Value("${upper_memory_threshold_in_percent}")
  private String upperMemoryThreshold;

  @Value("${lower_memory_threshold_in_percent}")
  private String lowerMemoryThreshold;

  @Value("${redis.new.node.host}")
  private String newRedisHost;

  @Value("${redis.new.node.port}")
  private String newRedisPort;

  private static final String USED_MEMORY = "used_memory";
  private static final String TOTAL_MEMORY = "maxmemory";
  private static final String USED_CPU = "used_cpu_user";
  private static final String INSTANTANEOUS_OPS_PER_SEC = "instantaneous_ops_per_sec";
  private static final String INSTANTANEOUS_INPUT_KBPS = "instantaneous_input_kbps";
  private static final String INSTANTANEOUS_OUTPUT_KBPS = "instantaneous_output_kbps";
  private static final String KEYSPACE_MISSES = "keyspace_misses";
  private static final String KEYSPACE_HITS = "keyspace_hits";


  @Override
  public List<NodeStats> checkNodeMemory(String userName, String password, boolean isAllData) {
    List<NodeStats> nodeStatsList = getNodeStats(isAllData);

    checkNodeForUtilizationThreshold(nodeStatsList,userName,password);
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
        connection.sendCommand(Protocol.Command.INFO, "cpu", "memory", "Replication","Stats");

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
      clusterNodesList.add(
          ClusterNodes.builder().nodeHostPort(nodeStats.getHost() + COLON + nodeStats.getPort())
              .nodeId(nodeStats.getNodeId()).isSlave(nodeStats.isSlave())
              .usedMemory(nodeStats.getUsedMemory()).totalMemory(nodeStats.getTotalMemory())
              .masterNodeHostPort(nodeStats.isSlave() ?
                  nodeStats.getMasterHost() + COLON + nodeStats.getMasterPort() :
                  EMPTY_STRING).slots(nodeStats.getSlots()).totalMemory(nodeStats.getTotalMemory())
              .memoryUsage(getMemoryUsage(nodeStats)).role(nodeStats.isSlave() ? SLAVE : MASTER)
              .currentTime(System.currentTimeMillis())
              .instantaneousOpsPerSec(nodeStats.getInstantaneousOpsPerSec())
              .instantaneousInputKbps(nodeStats.getInstantaneousInputKbps())
              .instantaneousOutputKbps(nodeStats.getInstantaneousOutputKbps())
              .keyspaceHits(nodeStats.getKeyspaceHits())
              .keyspaceMisses(nodeStats.getKeyspaceMisses()).build());
    }
    clusterNodesList.sort(Comparator.comparing(ClusterNodes::getNodeHostPort));
    clusterNodesList.sort(
        Comparator.comparing(ClusterNodes::isSlave).thenComparing(ClusterNodes::getNodeHostPort));
    return clusterNodesList;
  }

  private static double getMemoryUsage(NodeStats nodeStats) {

    double memoryUsagePercentage =
        (nodeStats.getUsedMemory() * 100) / nodeStats.getTotalMemory();
    return BigDecimal.valueOf(memoryUsagePercentage).setScale(2, RoundingMode.CEILING)
        .doubleValue();

  }
  private void checkNodeForUtilizationThreshold(List<NodeStats> nodeStatsList, String userName,
      String password) {
    List<NodeStats> removeNodeList = new ArrayList<>();
    log.info("Checking Nodes for utilization -  {}", nodeStatsList);
    for (NodeStats nodeStats : nodeStatsList) {
      if (((nodeStats.getUsedMemory() / nodeStats.getTotalMemory()) * 100) >= Integer.parseInt(
          upperMemoryThreshold)) {
        log.info(
            "Adding new node host - {} and port - {} for over utilization on host - {} and port -"
                + " {}",
            newRedisHost, newRedisPort, nodeStats.getHost(), nodeStats.getPort());
        clusterService.addNewNodeToCLuster(newRedisHost, newRedisPort, nodeStats.getHost(),
            nodeStats.getPort(), false, nodeStats.getNodeId(), true, userName, password);
        removeNodeList.clear();
        break;
      }

      if (nodeStatsList.size() - removeNodeList.size() > 2 && removeNodeList.size()<1
          && ((nodeStats.getUsedMemory() / nodeStats.getTotalMemory()) * 100) <= Integer.parseInt(
          lowerMemoryThreshold)) {
        removeNodeList.add(nodeStats);
      }
    }
    if (!CollectionUtils.isEmpty(removeNodeList)) {

      nodeStatsList.removeAll(removeNodeList);
      String host = nodeStatsList.get(0).getHost();
      String port = nodeStatsList.get(0).getPort();

      for (NodeStats nodeStats : removeNodeList) {
        log.info("Deleting node - {} using cluster host and port - {}:{}",nodeStats.getPort()+nodeStats.getPort(),host,port);
        clusterService.deleteNodeFromCluster(host, port, nodeStats.getHost(),
            Integer.parseInt(nodeStats.getPort()), userName, password);
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
          break;
        case USED_CPU:
          nodeStats.setUsedCPU(
              Double.parseDouble(statSplit.get(1).replaceAll("(\\r|\\n|\\t)", "")));
          break;
        case ROLE:
          nodeStats.setSlave(!MASTER.equals(statSplit.get(1).replace("(\\r|\\n|\\t)", "")));
          break;
        case MASTER_HOST:
          nodeStats.setMasterHost(getMasterHostMethod(statSplit.get(1).replace("(\\r|\\n|\\t)", "")));
          break;
        case MASTER_PORT:
          nodeStats.setMasterPort(statSplit.get(1).replace("(\\r|\\n|\\t)", ""));
          break;
        case INSTANTANEOUS_OPS_PER_SEC:
          nodeStats.setInstantaneousOpsPerSec(
              Double.parseDouble(statSplit.get(1).replaceAll("(\\r|\\n|\\t)", "")));
          break;
        case INSTANTANEOUS_INPUT_KBPS:
          nodeStats.setInstantaneousInputKbps(
              Double.parseDouble(statSplit.get(1).replaceAll("(\\r|\\n|\\t)", "")));
          break;
        case INSTANTANEOUS_OUTPUT_KBPS:
          nodeStats.setInstantaneousOutputKbps(
              Double.parseDouble(statSplit.get(1).replaceAll("(\\r|\\n|\\t)", "")));
          break;
        case KEYSPACE_HITS:
          nodeStats.setKeyspaceHits(
              Double.parseDouble(statSplit.get(1).replaceAll("(\\r|\\n|\\t)", "")));
          break;
        case KEYSPACE_MISSES:
          nodeStats.setKeyspaceMisses(
              Double.parseDouble(statSplit.get(1).replaceAll("(\\r|\\n|\\t)", "")));
          break;

      }
    }
  }

  private String getMasterHostMethod(String replaceHost) {
    if(replaceHost.equals("127.0.0.1")) {
      return RedisURI.create(redisUriNode).getHost();
    }
    return replaceHost;
  }

}
