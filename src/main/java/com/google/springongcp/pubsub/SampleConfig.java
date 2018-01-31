package com.google.springongcp.pubsub;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("myapp")
public class SampleConfig {

  private String url;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }
}
