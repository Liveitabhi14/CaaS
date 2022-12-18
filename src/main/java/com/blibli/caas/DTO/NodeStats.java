package com.blibli.caas.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NodeStats implements Serializable {
  private String host;
  private String port;
  private String nodeId;
  private long usedMemory;
  private long totalMemory;
  private long usedCPU;
  private long totalCPU;
}