package com.blibli.caas.DTO;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.BitSet;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClusterNodes implements Serializable {

  private String nodeHostPort;
  private String nodeId;
  private BitSet slots;
  private long totalMemory;
  private long usedMemory;
  private String masterNodeHostPort;
  private boolean isSlave;

}
