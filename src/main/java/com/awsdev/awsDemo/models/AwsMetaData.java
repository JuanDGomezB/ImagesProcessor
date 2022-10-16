package com.awsdev.awsDemo.models;

import lombok.Data;

@Data
public class AwsMetaData {
  private String region;
  private String availabilityZone;

  public AwsMetaData(String region, String availabilityZone) {
    this.region = region;
    this.availabilityZone = availabilityZone;
  }
}
