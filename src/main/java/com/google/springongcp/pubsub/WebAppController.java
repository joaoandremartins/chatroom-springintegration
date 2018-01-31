/*
 *  Copyright 2017 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.springongcp.pubsub;

import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.SubscriptionName;
import com.google.pubsub.v1.Topic;
import com.google.pubsub.v1.TopicName;
import com.google.springongcp.pubsub.PubsubApplication.PubsubOutboundGateway;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.pubsub.PubSubAdmin;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
//import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class WebAppController {

  @Autowired
  private PubsubOutboundGateway messagingGateway;

  @Autowired
  private PubSubAdmin admin;

  @Autowired
  private PubSubTemplate pubSubTemplate;

//  @Autowired
//  private JdbcTemplate jdbcTemplate;

  private static final Log LOGGER = LogFactory.getLog(WebAppController.class);

  /**
   * Lists every topic in the project.
   *
   * @return a list of the names of every topic in the project
   */
  @GetMapping("/listTopics")
  public List<String> listTopics() {
    return admin
        .listTopics()
        .stream()
        .map(Topic::getNameAsTopicName)
        .map(TopicName::getTopic)
        .collect(Collectors.toList());
  }

  /**
   * Lists every subscription in the project.
   *
   * @return a list of the names of every subscription in the project
   */
  @GetMapping("/listSubscriptions")
  public List<String> listSubscriptions() {
    return admin
        .listSubscriptions()
        .stream()
        .map(Subscription::getNameAsSubscriptionName)
        .map(SubscriptionName::getSubscription)
        .collect(Collectors.toList());
  }

  /**
   * Posts a message to a Google Cloud Pub/Sub topic, through Spring's messaging gateway, and
   * redirects the user to the home page.
   *
   * @param message the message posted to the Pub/Sub topic
   */
  @PostMapping("/postMessage")
  public RedirectView addMessage(@RequestParam("message") String message) {
    messagingGateway.sendToPubsub(message);
    return new RedirectView("/");
  }

  /**
   * Creates a new topic on Google Cloud Pub/Sub, through Spring's Pub/Sub admin class, and
   * redirects the user to the home page.
   *
   * @param topicName the name of the new topic
   */
  @PostMapping("/newTopic")
  public RedirectView newTopic(@RequestParam("name") String topicName) {
    admin.createTopic(topicName);
    return new RedirectView("/");
  }

  /**
   * Creates a new subscription on Google Cloud Pub/Sub, through Spring's Pub/Sub admin class, and
   * redirects the user to the home page.
   *
   * @param topicName the name of the new subscription
   */
  @PostMapping("/newSubscription")
  public RedirectView newSubscription(
      @RequestParam("name") String subscriptionName, @RequestParam("topic") String topicName) {
    admin.createSubscription(subscriptionName, topicName);
    return new RedirectView("/");
  }

  @GetMapping("/queryDb")
  public List<String> queryDb() throws SQLException {
    String jdbcUrl = String.format(
        "jdbc:mysql://google/%s?cloudSqlInstance=%s&"
            + "socketFactory=com.google.cloud.sql.mysql.SocketFactory",
        "test",
        "sodium-gateway-790:us-central1:gfx");

    Connection connection = DriverManager.getConnection(jdbcUrl, "root", "");

    Statement statement = connection.createStatement();
    ResultSet queryResult = statement.executeQuery("SELECT * FROM user;");
    List<String> result = new ArrayList<>();
    while (queryResult.next()) {
      result.add(queryResult.getString("email") + " "
          + queryResult.getString("name"));
    }
    statement.close();
    return result;
  }

//  @GetMapping("/queryDbSpecial")
//  public List<Map<String, Object>> queryDbSpecial() {
//    return jdbcTemplate.queryForList("SELECT * FROM user;");
//  }

  @Autowired
  private SampleConfig config;

  @GetMapping("/testConfig")
  public String testConfig() {
    return config.getUrl();
  }

  @Value("gs://jamnotifications/LockHolderKIlled_RequestProceeds.png")
  private Resource file;

  @GetMapping("/file")
  public ResponseEntity<Resource> serveFile() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.IMAGE_JPEG);
    return new ResponseEntity<>(file, headers, HttpStatus.OK);
  }

  @Value("gs://springintegrationz/IMG_1377.JPG")
  private Resource gcsImage;

  @GetMapping("/pic")
  public ResponseEntity<Resource> servePic() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.IMAGE_JPEG);
    return new ResponseEntity<>(gcsImage, headers, HttpStatus.OK);
  }

//  @Autowired
//  PubsubApplication.SIFileGateway gateway;
//
//  @GetMapping("/writefilesi")
//  public void writeFileSi() {
//    gateway.sendFileToGCS(new File("/usr/local/google/home/joaomartins/Downloads/IMG_1377.JPG"));
//  }


}
