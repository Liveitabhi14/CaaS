package com.blibli.caas.impl;

import com.blibli.caas.service.SshCommandExecutorService;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

@Service
@Slf4j
public class SshCommandExecutorServiceImpl implements SshCommandExecutorService {

  @Value("${caas.default.ssh.command.execution.timeout}")
  private int defaultCommandExecutionTimeout;
  @Override
  public String executeCommandOnRemoteMachineViaSSHUsingJSchLibrary(String host, Integer port,
      String username, String password, String command, Integer commandExecutionTimeout) {

    StringBuilder response = new StringBuilder();
    Session session;
    Channel channel;
    try {

      log.info("Executing command : \"{}\" on host : {}; port : {}; username : {}", command, host,
          port, username);
      session = new JSch().getSession(username, host, port);
      session.setPassword(password);
      session.setConfig("StrictHostKeyChecking", "no");
      session.connect();

      channel = session.openChannel("shell");
      OutputStream ops = channel.getOutputStream();
      PrintStream ps = new PrintStream(ops, true);
      channel.connect();
      ps.println(command);
      ps.close();
      InputStream in = channel.getInputStream();
      byte[] bt = new byte[1024];

      while (true) {
        while (in.available() > 0) {
          int i = in.read(bt, 0, 1024);
          if (i < 0)
            break;
          String str = new String(bt, 0, i);
          response.append(str);
        }
        if (channel.isClosed())
          break;

        Thread.sleep(commandExecutionTimeout);
        channel.disconnect();
        session.disconnect();
      }
      log.info("Command execution response via JSch library : {}", response);
    } catch (Exception e) {
      log.error("Encountered exception while executing command : {}", e.getMessage(), e);
    }
    return response.toString();
  }

  @Override
  public String executeCommandOnRemoteMachineViaSSHUsingJSchLibrary(String host, Integer port,
      String username, String password, String command) {
    return executeCommandOnRemoteMachineViaSSHUsingJSchLibrary(host, port, username, password,
        command, defaultCommandExecutionTimeout);
  }

}
