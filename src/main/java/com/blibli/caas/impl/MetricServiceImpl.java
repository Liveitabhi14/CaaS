package com.blibli.caas.impl;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class MetricServiceImpl implements MetricService {
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

  @Value("${redis.new.node.host}")
  private String newRedisHost;

  @Value("${redis.new.node.port}")
  private String newRedisPort;


  private static final String USED_MEMORY = "used_memory";
  private static final String TOTAL_MEMORY = "maxmemory";
  private static final String USED_CPU = "used_cpu_user";

  @Override
  public void checkNodeMemory(String userName, String password) {
    try {

      List<RedisClusterNode> redisClusterNodeList = clusterService.getClusterNode();
      log.info("node INfo - {}", redisClusterNodeList.toString());
      List<NodeStats> nodeStatsList = new ArrayList<>();


      for (RedisClusterNode redisClusterNode : redisClusterNodeList) {
        if (Objects.isNull(redisClusterNode.getSlaveOf())) {

          Connection connection = new Connection(redisClusterNode.getUri().getHost(),
              redisClusterNode.getUri().getPort());
          log.info(" host - {} and port - {} ", redisClusterNode.getUri().getHost(),
              redisClusterNode.getUri().getPort());
          connection.sendCommand(Protocol.Command.INFO);

          String info = connection.getBulkReply();

          NodeStats nodeStats = NodeStats.builder().nodeId(redisClusterNode.getNodeId())
              .host(redisClusterNode.getUri().getHost())
              .port(String.valueOf(redisClusterNode.getUri().getPort())).build();
          convertStats(info, nodeStats);
          nodeStatsList.add(nodeStats);

        }
      }

      checkNodeForUtilizationThreshold(nodeStatsList, userName, password);

    } catch (Exception exception) {
      log.error("Error in checkMemory - {}", exception.getCause().toString());
    }

  }

  @Override
  @Scheduled(fixedDelay = 30000)
  public void scheduleNodeCheck() {

    log.info("Starting memory check cron");
    checkNodeMemory(userName, password);


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
      List<String> statSplit = Arrays.asList(stat.split(":"));
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
      }
    }
  }
}
