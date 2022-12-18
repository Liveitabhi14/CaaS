package com.blibli.caas.impl;

import com.blibli.caas.constant.CommandsKeyword;
import com.blibli.caas.service.ClusterService;
import com.blibli.caas.service.ExecuteCommandOnRemoteMachineService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;

import java.nio.charset.StandardCharsets;
import java.util.List;


@Service
@Slf4j
public class ClusterServiceImpl implements ClusterService {

  @Autowired
  private ExecuteCommandOnRemoteMachineService executeCommandOnRemoteMachineService;

  @Override
  public String addNewNodeToCLuster(String newRedisHost, String newRedisPort, String clusterHost,
      String clusterPort, boolean isSlave, String masterId, Boolean isNeedRebalance,
      String username, String password) {
    String addNodeCommand =
        CommandsKeyword.REDIS_CLI_CLUSTER + " " + CommandsKeyword.ADD_NODE + " " + newRedisHost
            + ":" + newRedisPort + " " + clusterHost + ":" + clusterPort;
    if (isSlave) {
      addNodeCommand = addNodeCommand + " " + CommandsKeyword.CLUSTER_SLAVE;
      if (!StringUtils.isEmpty(masterId)) {
        addNodeCommand = addNodeCommand + " " + CommandsKeyword.CLUSTER_MASTER_ID + " " + masterId;
      }
    }
    log.info("add node command = {}", addNodeCommand);
    String addNodeOutput =
        executeCommandOnRemoteMachineService.executeCommandOnRemoteMachine(addNodeCommand,
            clusterHost, clusterPort, username, password);
    String clusterRebalanceOutput = "";
    if (isNeedRebalance) {
      clusterRebalanceOutput = clusterRebalance(clusterHost, clusterPort, true, username, password);
    }

    return addNodeOutput + clusterRebalanceOutput;
  }


  @Override
  public String clusterRebalance(String clusterHost, String clusterPort,
      Boolean isEmptySlotReBalance, String username, String password) {
    String clusterRebalanceCommand =
        CommandsKeyword.REDIS_CLI_CLUSTER + " " + CommandsKeyword.REBALANCE + " " + clusterHost
            + ":" + clusterPort;
    if (isEmptySlotReBalance) {
      clusterRebalanceCommand = clusterRebalanceCommand + " " + CommandsKeyword.USE_EMPTY_MASTER;
    }
    log.info("rebalance command = {}", clusterRebalanceCommand);
    return executeCommandOnRemoteMachineService.executeCommandOnRemoteMachine(
        clusterRebalanceCommand, clusterHost, clusterPort, username, password);
  }

  @Override
  public String reshardHashSlotsBetweenNodes(String clusterHost, String clusterPort,
      String sourceNodeId, String targetNodeId, int noOfSlots, String username, String password) {

    String reshardHashSlotsCommand =
        CommandsKeyword.REDIS_CLI_CLUSTER + " " + CommandsKeyword.RESHARD + " " + clusterHost + ":"
            + clusterPort + CommandsKeyword.CLUSTER_FROM + " " + sourceNodeId + " "
            + CommandsKeyword.CLUSTER_TO + " " + targetNodeId + " " + CommandsKeyword.CLUSTER_SLOTS
            + " " + noOfSlots + " " + CommandsKeyword.CLUSTER_YES;
    log.info("reshard hash slots command = {}", reshardHashSlotsCommand);
    return executeCommandOnRemoteMachineService.executeCommandOnRemoteMachine(
        reshardHashSlotsCommand, clusterHost, clusterPort, username, password);
  }

  @Override
  public String deleteNodeFromCluster(String clusterHost, String clusterPort, String nodeId,
      String username, String password) {

    //Reshard all slots from node to be deleted to a master
    String targetNodeId = getNodeIdInCluster(clusterHost, Integer.parseInt(clusterPort));
    int noOfSlots = countSlotsInNode(clusterHost, Integer.parseInt(clusterPort), nodeId);
    String reshardOutput =
        reshardHashSlotsBetweenNodes(clusterHost, clusterPort, nodeId, targetNodeId, noOfSlots,
            username, password);

    //Delete the node
    String deleteNodeCommand =
        CommandsKeyword.REDIS_CLI_CLUSTER + " " + CommandsKeyword.DELETE_NODE + clusterHost + ":"
            + clusterPort + " " + nodeId;
    log.info("delete node command = {}", deleteNodeCommand);
    String deleteNodeOutput =
        executeCommandOnRemoteMachineService.executeCommandOnRemoteMachine(deleteNodeCommand,
            clusterHost, clusterPort, username, password);

    //Rebalance
    String clusterRebalanceOutput =
        clusterRebalance(clusterHost, clusterPort, false, username, password);

    //Cluster reset hard for deleted node
    //todo - add logic
    String nodeResetOutput = "";

    return reshardOutput + deleteNodeOutput + clusterRebalanceOutput + nodeResetOutput;
  }

  @Override
  public String getClusterInfo(String clusterHost, Integer clusterPort) {
    try (Jedis jedis = new Jedis(new HostAndPort(clusterHost, clusterPort))) {
      return jedis.clusterInfo();
    } catch (Exception e) {
      log.error("Error connecting to cluster via Jedis : {}; {}", e, e.getMessage());
      return null;
    }
  }

  @Override
  public String getNodeIdInCluster(String nodeHost, Integer nodePort) {
    try (Jedis jedis = new Jedis(new HostAndPort(nodeHost, nodePort))) {
      return jedis.clusterMyId();
    } catch (Exception e) {
      log.error("Error connecting to cluster via Jedis : {}; {}", e, e.getMessage());
      return null;
    }
  }

  @Override
  public Integer countSlotsInNode(String clusterHost, Integer clusterPort, String nodeId) {
    try (Jedis jedis = new Jedis(new HostAndPort(clusterHost, clusterPort))) {
      List<Object> clusterSlotsDetails = jedis.clusterSlots();
      log.info("cluster slots : {}", clusterSlotsDetails);
      long slots = 0L;
      for (Object slotDetails : clusterSlotsDetails) {
        List<Object> slotDetailsList = (List<Object>) slotDetails;
        List<Object> masterNodeDetailsList = (List<Object>) slotDetailsList.get(2);
        String masterNodeId =
            new String((byte[]) masterNodeDetailsList.get(2), StandardCharsets.UTF_8);
        if (StringUtils.equals(masterNodeId, nodeId)) {
          slots += ((Long) slotDetailsList.get(1) - (Long) slotDetailsList.get(0)) + 1;
        }
      }
      return (int) slots;
    } catch (Exception e) {
      log.error("Error connecting to cluster via Jedis : {}; {}", e, e.getMessage());
      return null;
    }
  }
}
