package com.blibli.caas.service;

public interface SshCommandExecutorService {

  String executeCommandOnRemoteMachineViaSSH(String host, Integer port, String username,
      String password, String command) throws Exception;
}
