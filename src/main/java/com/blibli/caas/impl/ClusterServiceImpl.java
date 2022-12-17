package com.blibli.caas.impl;

import com.blibli.caas.constant.CommandsKeyword;
import com.blibli.caas.service.ClusterService;
import com.blibli.caas.service.ExecuteCommandOnRemoteMachineService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


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
        CommandsKeyword.REDIS_CLI + " " + CommandsKeyword.ADD_NODE + " " + newRedisHost + ":"
            + newRedisPort + " " + clusterHost + ":" + clusterPort;
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
        CommandsKeyword.REDIS_CLI + " " + CommandsKeyword.REBALANCE + " " + clusterHost + ":"
            + clusterPort;
    if (isEmptySlotReBalance) {
      clusterRebalanceCommand = clusterRebalanceCommand + " " + CommandsKeyword.USE_EMPTY_MASTER;
    }
    log.info("rebalance command = {}", clusterRebalanceCommand);
    return executeCommandOnRemoteMachineService.executeCommandOnRemoteMachine(
        clusterRebalanceCommand, clusterHost, clusterPort, username, password);
  }


}
