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

import com.github.jcustenborder.kafka.connect.utils.VersionUtil;
import com.github.jcustenborder.kafka.connect.utils.data.SourceRecordDeque;
import com.github.jcustenborder.kafka.connect.utils.data.SourceRecordDequeBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ServiceManager;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class IMAPSourceTask extends SourceTask {
  private static final Logger log = LoggerFactory.getLogger(IMAPSourceTask.class);
  IMAPSourceConnectorConfig config;
  SourceRecordDeque records;
  ServiceManager serviceManager;
  CountDownLatch stopLatch;

  @Override
  public String version() {
    return VersionUtil.version(this.getClass());
  }

  @Override
  public void start(Map<String, String> settings) {
    this.stopLatch = new CountDownLatch(1);
    this.config = new IMAPSourceConnectorConfig(settings);
    this.records = SourceRecordDequeBuilder.of()
        .build();
    this.serviceManager = new ServiceManager(
        ImmutableList.of(
            new IMAPEmailService(
                this.config,
                this.records,
                stopLatch,
                this.context.offsetStorageReader()
            )
        )
    );

    this.serviceManager.startAsync();
    this.serviceManager.awaitHealthy();
  }

  @Override
  public List<SourceRecord> poll() throws InterruptedException {
    return this.records.getBatch();
  }

  @Override
  public void stop() {
    this.stopLatch.countDown();
    this.serviceManager.stopAsync();
    try {
      this.serviceManager.awaitStopped(30, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      log.error("Timeout waiting for service to stop.", e);
    }
  }
}
