package com.google.springongcp;

import java.time.LocalDateTime;

/**
 * Created by joaomartins on 4/28/17.
 */
public class LoggableMessage {
  private String user;
  private String body;
  private LocalDateTime createdAt;

  public LoggableMessage(String user, String body, LocalDateTime createdAt) {
    this.user = user;
    this.body = body;
    this.createdAt = createdAt;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
