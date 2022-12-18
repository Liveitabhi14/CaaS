package com.blibli.caas.impl;

import com.blibli.caas.service.ExecuteCommandOnRemoteMachineService;
import com.blibli.caas.service.SshCommandExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.blibli.caas.service.SshCommandExecutorService.SSH_DEFAULT_PORT;

@Service
@Slf4j
public class ExecuteCommandOnRemoteMachineServiceImpl
    implements ExecuteCommandOnRemoteMachineService {

  @Autowired
  private SshCommandExecutorService sshCommandExecutorService;

  @Override
  public String executeCommandOnRemoteMachine(String command, String targetHost, String targetPort,
      String username, String password, Integer timeout) {

    String commandOutput = "";
    try {
      commandOutput =
          sshCommandExecutorService.executeCommandOnRemoteMachineViaSSHUsingJSchLibrary(targetHost,
              SSH_DEFAULT_PORT, username, password, command,timeout);
    } catch (Exception e) {
      log.error("getting error", e);
    }
    return commandOutput;
  }
}
