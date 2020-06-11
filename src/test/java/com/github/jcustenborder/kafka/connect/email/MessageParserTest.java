/**
 * Copyright © 2020 Jeremy Custenborder (jcustenborder@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jcustenborder.kafka.connect.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.FilterBuilder;

import javax.mail.Session;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class MessageParserTest {

//  MessageParser parser;
//
//  @BeforeEach
//  public void before() {
//    IncomingEmailSourceConnectorConfig config = new IncomingEmailSourceConnectorConfig(IncomingEmailSourceConnectorConfigTest.settings());
//    this.parser = new MessageParser(config);
//  }
////
////  @TestFactory
////  public Stream<DynamicTest> enron() throws IOException {
////    return Files.walk(Paths.get("/Users/jeremy/Downloads/enron/emails")).map(s -> dynamicTest(s.getFileName().toString(), () -> {
////
////      try (FileInputStream inputStream = new FileInputStream(s.toFile())) {
////        this.parser.parse(inputStream);
////      }
////
////    }));
////  }
//
//
//  @TestFactory
//  public Stream<DynamicTest> parse() {
//    Reflections reflections = new Reflections("com.github.jcustenborder.kafka.connect.email.messages", new ResourcesScanner());
//    Set<String> resources = reflections.getResources(new FilterBuilder.Include(".*"));
//
//    String host = "host.com";
//    java.util.Properties properties = System.getProperties();
//    properties.setProperty("mail.smtp.host", host);
//    Session session = Session.getDefaultInstance(properties);
//    session.setDebug(true);
//
//
//    return resources.stream().map(s -> dynamicTest(s, () -> {
//      final String resourcePath = "/" + s;
//      try (InputStream inputStream = this.getClass().getResourceAsStream(resourcePath)) {
//        this.parser.parse(inputStream);
//
//      }
//
//    }));
//  }

}
