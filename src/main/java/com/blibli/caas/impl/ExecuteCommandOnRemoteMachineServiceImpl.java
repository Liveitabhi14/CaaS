package com.blibli.caas.impl;

import com.blibli.caas.service.ExecuteCommandOnRemoteMachineService;
import com.blibli.caas.service.SshCommandExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ExecuteCommandOnRemoteMachineServiceImpl implements
    ExecuteCommandOnRemoteMachineService {

  private SshCommandExecutorService sshCommandExecutorService;
  @Autowired
  void ExecuteCommandOnRemoteMachineService(SshCommandExecutorService sshCommandExecutorService) {
    this.sshCommandExecutorService = sshCommandExecutorService;
  }

  @Override
  public String executeCommandOnRemoteMachine(String command, String targetHost,
      String targetPort) {
    String userName = "krishankumarrao";
    String password = "1234";
    String commandOutput = "";
    try {
      commandOutput = sshCommandExecutorService.executeCommandOnRemoteMachineViaSSH(targetHost,
          22, userName, password, command);
    } catch (Exception e) {
      log.error("getting error", e);
    }
    return commandOutput;
  }
}
