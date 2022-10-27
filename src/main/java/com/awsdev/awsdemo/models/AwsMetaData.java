package com.awsdev.awsdemo.models;

import lombok.Data;

/**
 * AwsMetaData Model.
 */
@Data
public class AwsMetaData {
  private String region;
  private String availabilityZone;

  public AwsMetaData(String region, String availabilityZone) {
    this.region = region;
    this.availabilityZone = availabilityZone;
  }
}
