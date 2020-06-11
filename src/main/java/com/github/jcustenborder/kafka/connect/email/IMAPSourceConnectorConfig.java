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

import com.github.jcustenborder.kafka.connect.utils.config.ConfigKeyBuilder;
import com.github.jcustenborder.kafka.connect.utils.config.validators.Validators;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

import javax.mail.URLName;
import java.util.Map;

public class IMAPSourceConnectorConfig extends AbstractConfig {
  public static final String SCHEME_CONFIG = "imap.scheme";
  public static final String FOLDER_CONFIG = "imap.folder";
  public static final String PASSWORD_CONFIG = "imap.password";
  public static final String USERNAME_CONFIG = "imap.username";
  public static final String HOSTNAME_CONFIG = "imap.hostname";
  public static final String PORT_CONFIG = "imap.port";
  public static final String TOPIC_CONFIG = "imap.topic";
  public static final String MESSAGE_BATCH_CONFIG = "imap.message.batch.size";
  public static final String CHECK_INTERVAL_CONFIG = "imap.check.interval.seconds";
  static final String CHECK_INTERVAL_DOC = "The interval in seconds to check for new messages.";
  static final String MESSAGE_BATCH_DOC = "The ideal number of messages to put in a batch.";
  static final String SCHEME_DOC = "The scheme to connect to the IMAP server with. ";
  static final String FOLDER_DOC = "The folder to look for messages.";
  static final String PASSWORD_DOC = "The password to connect to the IMAP server with.";
  static final String USERNAME_DOC = "The username to connect to the IMAP server with.";
  static final String HOSTNAME_DOC = "The hostname of the IMAP server to connect to.";
  static final String PORT_DOC = "The port of the IMAP server to connect to.";
  static final String TOPIC_DOC = "The topic to write the messages to.";
  public final String scheme;
  public final String folder;
  public final String username;
  public final String password;
  public final int port;
  public final String hostname;
  public final String topic;
  public final int messageBatchSize;
  public final int checkInterval;


  public IMAPSourceConnectorConfig(Map<?, ?> originals) {
    super(config(), originals);
    this.scheme = getString(SCHEME_CONFIG);
    this.folder = getString(FOLDER_CONFIG);
    this.username = getString(USERNAME_CONFIG);
    this.password = getPassword(PASSWORD_CONFIG).value();
    this.port = getInt(PORT_CONFIG);
    this.hostname = getString(HOSTNAME_CONFIG);
    this.topic = getString(TOPIC_CONFIG);
    this.messageBatchSize = getInt(MESSAGE_BATCH_CONFIG);
    this.checkInterval = getInt(CHECK_INTERVAL_CONFIG);
  }

  public static ConfigDef config() {
    return new ConfigDef()
        .define(
            ConfigKeyBuilder.of(SCHEME_CONFIG, ConfigDef.Type.STRING)
                .documentation(SCHEME_DOC)
                .importance(ConfigDef.Importance.LOW)
                .defaultValue("imaps")
                .build()
        ).define(
            ConfigKeyBuilder.of(PORT_CONFIG, ConfigDef.Type.INT)
                .documentation(PORT_DOC)
                .importance(ConfigDef.Importance.LOW)
                .validator(Validators.validPort(1, 65535))
                .defaultValue(993)
                .build()
        ).define(
            ConfigKeyBuilder.of(USERNAME_CONFIG, ConfigDef.Type.STRING)
                .documentation(USERNAME_DOC)
                .importance(ConfigDef.Importance.HIGH)
                .build()
        ).define(
            ConfigKeyBuilder.of(PASSWORD_CONFIG, ConfigDef.Type.PASSWORD)
                .documentation(PASSWORD_DOC)
                .importance(ConfigDef.Importance.HIGH)
                .build()
        ).define(
            ConfigKeyBuilder.of(HOSTNAME_CONFIG, ConfigDef.Type.STRING)
                .documentation(HOSTNAME_DOC)
                .importance(ConfigDef.Importance.HIGH)
                .build()
        ).define(
            ConfigKeyBuilder.of(FOLDER_CONFIG, ConfigDef.Type.STRING)
                .documentation(FOLDER_DOC)
                .importance(ConfigDef.Importance.HIGH)
                .defaultValue("INBOX")
                .build()
        ).define(
            ConfigKeyBuilder.of(TOPIC_CONFIG, ConfigDef.Type.STRING)
                .documentation(TOPIC_DOC)
                .importance(ConfigDef.Importance.HIGH)
                .build()
        ).define(
            ConfigKeyBuilder.of(MESSAGE_BATCH_CONFIG, ConfigDef.Type.INT)
                .documentation(MESSAGE_BATCH_DOC)
                .importance(ConfigDef.Importance.LOW)
                .defaultValue(100)
                .validator(ConfigDef.Range.atLeast(1))
                .build()
        ).define(
            ConfigKeyBuilder.of(CHECK_INTERVAL_CONFIG, ConfigDef.Type.INT)
                .documentation(CHECK_INTERVAL_DOC)
                .importance(ConfigDef.Importance.LOW)
                .defaultValue(600)
                .validator(ConfigDef.Range.atLeast(30))
                .build()
        );
  }

  public URLName loggingUrlName() {
    return new URLName(this.scheme,
        this.hostname,
        this.port,
        this.folder,
        this.username,
        "********");
  }

  public URLName urlName() {
    return new URLName(this.scheme,
        this.hostname,
        this.port,
        this.folder,
        this.username,
        this.password);
  }
}
