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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Timestamp;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class MessageParser {
  public static final Logger log = LoggerFactory.getLogger(MessageParser.class);
  static final Schema ADDRESS_SCHEMA = Schema.STRING_SCHEMA;
  static final Schema ATTACHMENT_SCHEMA = SchemaBuilder.struct()
      .name("com.github.jcustenborder.kafka.connect.email.Attachment")
      .optional()
      .doc("This class represents an Internet email address using the syntax of :rfc:`822`.")
      .field("fileName", Schema.OPTIONAL_STRING_SCHEMA)
      .field("content", Schema.OPTIONAL_BYTES_SCHEMA)
      .field("contentType", Schema.OPTIONAL_STRING_SCHEMA)
      .build();

  static final Schema MESSAGE_SCHEMA = SchemaBuilder.struct()
      .name("com.github.jcustenborder.kafka.connect.email.Message")
      .optional()
      .doc("This class represents an Internet email address using the syntax of :rfc:`822`.")
      .field("from", SchemaBuilder.array(ADDRESS_SCHEMA).optional().doc("").build())
      .field("replyTo", SchemaBuilder.array(ADDRESS_SCHEMA).optional().doc("").build())
      .field("to", SchemaBuilder.array(ADDRESS_SCHEMA).optional().doc("").build())
      .field("cc", SchemaBuilder.array(ADDRESS_SCHEMA).optional().doc("").build())
      .field("bcc", SchemaBuilder.array(ADDRESS_SCHEMA).optional().doc("").build())
      .field("sender", ADDRESS_SCHEMA)
      .field("subject", SchemaBuilder.string().optional().doc("").build())
      .field("textBody", SchemaBuilder.string().optional().doc("").build())
      .field("htmlBody", SchemaBuilder.string().optional().doc("").build())
      .field("receivedDate", Timestamp.builder().optional().doc("").build())
      .field("sentDate", Timestamp.builder().optional().doc("").build())
      .field("contentType", Schema.OPTIONAL_STRING_SCHEMA)

      .build();


  final Session session;
  final String topic;

  private static final Properties DEFAULT_PROPERTIES;

  static {
    Properties properties = System.getProperties();
    properties.setProperty("mail.smtp.host", "dummy");
    DEFAULT_PROPERTIES = properties;
  }

  public MessageParser(String topic, Session session) {
    this.topic = topic;
    this.session = session;
  }

  Struct internetAddress(InternetAddress address) {
    log.trace("internetAddress({})", address);
    Struct struct = new Struct(ADDRESS_SCHEMA)
        .put("address", address.getAddress())
        .put("personal", address.getPersonal());
    return struct;
  }

  String address(Address address) {
    String result;
    if (null == address) {
      result = null;
    } else if (address instanceof InternetAddress) {
      result = ((InternetAddress) address).getAddress();
    } else {
      throw new DataException(address.getClass().getName() + " is not supported.");
    }

    return result;
  }

  List<String> addresses(Address[] addresses) {
    List<String> results = Arrays.stream(addresses)
        .filter(a -> a instanceof InternetAddress)
        .map(address -> (InternetAddress) address)
        .map(InternetAddress::getAddress)
        .collect(Collectors.toList());
    return results;
  }

  List<String> recipients(MimeMessage email, Message.RecipientType type) throws MessagingException {
    log.trace("recipients() - Looking for '{}'", type);
    Address[] addresses = email.getRecipients(type);
    List<String> results;

    if (null == addresses) {
      results = new ArrayList<>();
    } else {
      log.trace("recipients() - Found {} recipient(s).", addresses.length);
      results = addresses(addresses);
    }

    return results;
  }

  Struct attachment(MimeBodyPart bodyPart) throws MessagingException, IOException {

    if (null == bodyPart) {
      return null;
    }
    final String fileName = bodyPart.getFileName();
    final String contentType = bodyPart.getContentType();
    log.trace("attachment() - fileName = '{}' contentType = '{}'", fileName, contentType);

    final byte[] content;
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      try (InputStream inputStream = bodyPart.getInputStream()) {
        ByteStreams.copy(inputStream, outputStream);
      }
      content = outputStream.toByteArray();
    }


    final Struct struct = new Struct(ATTACHMENT_SCHEMA)
        .put("fileName", fileName)
        .put("content", content)
        .put("contentType", contentType);


    return struct;
  }

  public SourceRecord parse(InputStream inputStream) throws IOException, MessagingException {
    MimeMessage email = new MimeMessage(session, inputStream);
    return parse(email);
  }

  public SourceRecord parse(MimeMessage email) throws IOException, MessagingException {
    if (null == email) {
      log.error("Could not parse email.");
      return null;
    }

    final Struct value = new Struct(MESSAGE_SCHEMA)
        .put("from", addresses(email.getFrom()))
        .put("replyTo", addresses(email.getReplyTo()))
        .put("to", recipients(email, Message.RecipientType.TO))
        .put("cc", recipients(email, Message.RecipientType.CC))
        .put("bcc", recipients(email, Message.RecipientType.BCC))
        .put("sender", address(email.getSender()))
        .put("subject", email.getSubject())
        .put("receivedDate", email.getReceivedDate())
        .put("sentDate", email.getSentDate())
        .put("contentType", email.getContentType());

    if (email.isMimeType("multipart/*")) {
      log.trace("parse() - Email is multipart");
      final MimeMultipart mimeMultipart = (MimeMultipart) email.getContent();
      log.trace("parse() - Processing MimeMultipart count = {}", mimeMultipart.getCount());
      final List<Struct> attachments = new ArrayList<>(mimeMultipart.getCount());
      final List<Struct> inline = new ArrayList<>(mimeMultipart.getCount());
      for (int parentIndex = 0; parentIndex < mimeMultipart.getCount(); parentIndex++) {
        final MimeBodyPart parentPart = (MimeBodyPart) mimeMultipart.getBodyPart(parentIndex);
        final String contentType = parentPart.getContentType();
        log.trace("parse() - Processing BodyPart index = {} contentType = '{}'", parentIndex, contentType);

        if (parentPart.isMimeType("multipart/alternative")) {
          log.trace("parse() - Processing multipart/alternative");
          final Multipart mimePart = (Multipart) parentPart.getContent();
          for (int i = 0; i < mimePart.getCount(); i++) {
            final Part part = mimePart.getBodyPart(i);

            if (part.isMimeType("text/plain")) {
              log.trace("parse() - Found text/plain in multipart/alternative for textBody");
              value.put("textBody", part.getContent());
            } else if (part.isMimeType("text/html")) {
              log.trace("parse() - Found text/html in multipart/alternative for htmlBody");
              value.put("htmlBody", part.getContent());
            }
          }
        } else if (parentPart.isMimeType("text/plain")) {
          if (Strings.isNullOrEmpty(parentPart.getFileName())) {
            log.trace("parse() - parentPart.getFileName() is null so processing as textBody");
            value.put("textBody", parentPart.getContent());
          } else {
            log.trace("parse() - parentPart.getFileName() has value so processing as attachment.");
            final Struct attachment = attachment(parentPart);
            attachments.add(attachment);
          }
        } else if (parentPart.isMimeType("text/html")) {
          if (Strings.isNullOrEmpty(parentPart.getFileName())) {
            log.trace("parse() - parentPart.getFileName() is null so processing as htmlBody");
            value.put("htmlBody", parentPart.getContent());
          } else {
            log.trace("parse() - parentPart.getFileName() has value so processing as attachment.");
            final Struct attachment = attachment(parentPart);
            attachments.add(attachment);
          }
        } else if (parentPart.isMimeType("multipart/*")) {
          MimeMultipart childMultipart = (MimeMultipart) parentPart.getContent();
          for (int childIndex = 0; childIndex < childMultipart.getCount(); childIndex++) {
            log.trace("parse() - Processing Child MimeMultipart index = {}", parentIndex);
            final MimeBodyPart childPart = (MimeBodyPart) mimeMultipart.getBodyPart(parentIndex);
            log.trace("contentType = '{}'", childPart.getContentType());
          }
        } else {
          log.trace("{}", parentPart.getDisposition());
          Struct attachment = attachment(parentPart);
          attachments.add(attachment);
        }
      }

    } else if (email.isMimeType("text/plain")) {
      log.trace("parse() - email is text/plain");
      value.put("textBody", email.getContent());
    } else if (email.isMimeType("message/rfc822")) {
      log.trace("parse() - email is message/rfc822");

    } else if (email.isMimeType("text/html")) {
      log.trace("parse() - email is text/html");
      value.put("htmlBody", email.getContent());
    } else {
      log.trace("{}", email.getContent());
      log.trace("{}", email.getEncoding());

      log.error("Could not parse");
      return null;
    }

    //TODO Fix this.
    final Long timestamp = null;


//    switch (this.config.timestampMode) {
//      case SEND_DATE:
//        timestamp = null != email.getSentDate() ? email.getSentDate().getTime() : null;
//        break;
//      case RECEIVED_DATE:
//        timestamp = null != email.getReceivedDate() ? email.getReceivedDate().getTime() : null;
//        break;
//      case PROCESS_DATE:
//      default:
//        timestamp = null;
//        break;
//    }


    SourceRecord record = new SourceRecord(
        ImmutableMap.of(),
        ImmutableMap.of(),
        this.topic,
        null,
        null,
        null,
        value.schema(),
        value,
        timestamp
    );


    return record;
  }


}
