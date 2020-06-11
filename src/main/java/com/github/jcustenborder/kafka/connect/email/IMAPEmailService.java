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

import com.github.jcustenborder.kafka.connect.utils.data.SourceRecordDeque;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AbstractScheduledService;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class IMAPEmailService extends AbstractScheduledService {
  private static final Logger log = LoggerFactory.getLogger(IMAPEmailService.class);
  private final IMAPSourceConnectorConfig config;
  private final SourceRecordDeque records;
  private final CountDownLatch stopLatch;
  private Session session;
  private Store store;
  private Folder folder;
  private final OffsetStorageReader offsetStorageReader;
  private final Map<String, ?> sourcePartition;

  Session session() {
    if (null == this.session) {
      Properties properties = new Properties();
      properties.setProperty("mail.imap.partialfetch", "false");
      properties.setProperty("mail.imaps.partialfetch", "false");
      this.session = Session.getInstance(properties);
    }

    return this.session;
  }

  Store store() throws MessagingException {
    if (null == this.store) {
      this.folder = null;
      log.info("store() - creating store for {}", this.config.loggingUrlName());
      this.store = session().getStore(this.config.urlName());
      this.store.connect();
    } else {
      if (!this.store.isConnected()) {
        this.folder = null;
        this.store.connect();
      }
    }

    return store;
  }

  Folder folder() throws MessagingException {
    if (null == this.folder) {
      log.info("folder() - opening folder for {}", this.config.loggingUrlName());
      this.folder = store().getFolder(this.config.urlName());
      this.folder.open(Folder.READ_WRITE);
    } else {
      if (!this.folder.isOpen()) {
        this.folder.open(Folder.READ_WRITE);
      }
    }
    return this.folder;
  }

  public IMAPEmailService(IMAPSourceConnectorConfig config, SourceRecordDeque records, CountDownLatch stopLatch, OffsetStorageReader offsetStorageReader) {
    this.config = config;
    this.records = records;
    this.stopLatch = stopLatch;
    this.offsetStorageReader = offsetStorageReader;
    this.sourcePartition = ImmutableMap.of(IMAPSourceConnectorConfig.FOLDER_CONFIG, this.config.folder);
  }


  int startIndex() {
    final int result;
    Map<String, Object> offset = this.offsetStorageReader.offset(this.sourcePartition);

    if (null == offset) {
      result = 1;
    } else {
      result = (int) offset.getOrDefault("index", 1);
    }

    return result;
  }

  static final Flags SEEN = new Flags(Flags.Flag.SEEN);

  @Override
  protected void runOneIteration() throws Exception {
    try {
      final MessageParser parser = new MessageParser(this.config.topic, session());

      while (true) {
        if (stopLatch.getCount() == 0) {
          break;
        }

        FlagTerm search = new FlagTerm(SEEN, false);

        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.ENVELOPE);
        fp.add(UIDFolder.FetchProfileItem.FLAGS);
        fp.add(UIDFolder.FetchProfileItem.CONTENT_INFO);
        fp.add(UIDFolder.FetchProfileItem.UID);
        fp.add("X-mailer");

        log.debug("runOneIteration() - Searching for unread messages.");
        final Message[] messages = folder().search(search);
        if (null == messages || messages.length == 0) {
          log.debug("runOneIteration() - No messages found.");
          break;
        }

        log.debug("runOneIteration() - Fetching information on {} message(s).", messages.length);
        folder().fetch(messages, fp);


        log.debug("runOneIteration() - Found {} unread message(s).", messages.length);
        List<Message> readMessages = new ArrayList<>(messages.length);
        for (Message message : messages) {
          if (stopLatch.getCount() == 0) {
            break;
          }

          log.debug("runOneIteration() - processing message.");
          SourceRecord record = parser.parse((MimeMessage) message);
          records.add(record);
          readMessages.add(message);

          if (readMessages.size() >= this.config.messageBatchSize) {
            log.debug("runOneIteration() - Settings {} message(s) as read.", readMessages.size());
            folder().setFlags(readMessages.toArray(new Message[0]), SEEN, true);
            readMessages.clear();
          }
        }
      }
    } catch (Exception ex) {
      log.error("Exception thrown", ex);
    }
  }


  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedDelaySchedule(0, this.config.checkInterval, TimeUnit.SECONDS);
  }
}
