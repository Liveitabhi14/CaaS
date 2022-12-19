package com.blibli.caas.service;

public interface SshCommandExecutorService {

  int SSH_DEFAULT_PORT = 22;

  String executeCommandOnRemoteMachineViaSSHUsingJSchLibrary(String host, Integer port,
      String username, String password, String command,Integer timeout);

  String executeCommandOnRemoteMachineViaSSHUsingJSchLibrary(String host, Integer port,
      String username, String password, String command);
}
