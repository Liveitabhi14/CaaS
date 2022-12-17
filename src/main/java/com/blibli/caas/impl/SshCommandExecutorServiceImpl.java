package com.blibli.caas.impl;

import com.blibli.caas.service.SshCommandExecutorService;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
@Slf4j
public class SshCommandExecutorServiceImpl implements SshCommandExecutorService {

  @Override
  public String executeCommandOnRemoteMachineViaSSH(String host, Integer port, String username,
      String password, String command) throws Exception {
    Session session = null;
    ChannelExec channel = null;
    try {

      log.info("Executing command : {} on host : {}; port : {}; username : {}", command, host, port,
          username);
      session = new JSch().getSession(username, host, port);
      session.setPassword(password);
      session.setConfig("StrictHostKeyChecking", "no");
      session.connect();

      channel = (ChannelExec) session.openChannel("sftp");
      channel.setCommand(command);
      ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
      channel.setOutputStream(responseStream);
      channel.connect();

      while (channel.isConnected()) {
        Thread.sleep(100);
      }

      log.info("Command execution response : {}", responseStream);
      return (responseStream.toString());
    } finally {
      if (session != null) {
        session.disconnect();
      }
      if (channel != null) {
        channel.disconnect();
      }
    }
  }
}
