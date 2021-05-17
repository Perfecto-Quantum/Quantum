package com.quantum.axe;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public class AxeTestResultCheckDetail {
  public String id;
  public String impact;
  public String message;

  @JsonIgnore
  public List<String> data; // CAN BE AN ARRAY OR A SINGLE ITEM!
  public List<AxeTestResultRelatedNode> relatedNodes;
}

