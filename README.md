# Introduction
[Documentation](https://jcustenborder.github.io/kafka-connect-documentation/projects/kafka-connect-email) | [Confluent Hub](https://www.confluent.io/hub/jcustenborder/kafka-connect-email)

This plugin provides connector(s) and transformation(s) to use when integrating with email systems.

# Installation

## Confluent Hub

The following command can be used to install the plugin directly from the Confluent Hub using the
[Confluent Hub Client](https://docs.confluent.io/current/connect/managing/confluent-hub/client.html).

```bash
confluent-hub install jcustenborder/kafka-connect-email:latest
```

## Manually

The zip file that is deployed to the [Confluent Hub](https://www.confluent.io/hub/jcustenborder/kafka-connect-email) is available under
`target/components/packages/`. You can manually extract this zip file which includes all dependencies. All the dependencies
that are required to deploy the plugin are under `target/kafka-connect-target` as well. Make sure that you include all the dependencies that are required
to run the plugin.

1. Create a directory under the `plugin.path` on your Connect worker.
2. Copy all of the dependencies under the newly created subdirectory.
3. Restart the Connect worker.


# Source Connectors
## [IMAPSourceConnector](https://jcustenborder.github.io/kafka-connect-documentation/projects/kafka-connect-email/sources/IMAPSourceConnector.html)

```
com.github.jcustenborder.kafka.connect.email.IMAPSourceConnector
```

This connector is used to connect to an email server over the IMAP protocol. Each new message in the configured folder will be parsed as a MIME message and written to the configured Kafka topic.
### Configuration

#### General


##### `imap.hostname`

The hostname of the IMAP server to connect to.

*Importance:* HIGH

*Type:* STRING



##### `imap.password`

The password to connect to the IMAP server with.

*Importance:* HIGH

*Type:* PASSWORD



##### `imap.topic`

The topic to write the messages to.

*Importance:* HIGH

*Type:* STRING



##### `imap.username`

The username to connect to the IMAP server with.

*Importance:* HIGH

*Type:* STRING



##### `imap.folder`

The folder to look for messages.

*Importance:* HIGH

*Type:* STRING

*Default Value:* INBOX



##### `imap.check.interval.seconds`

The interval in seconds to check for new messages.

*Importance:* LOW

*Type:* INT

*Default Value:* 600

*Validator:* [30,...]



##### `imap.message.batch.size`

The ideal number of messages to put in a batch.

*Importance:* LOW

*Type:* INT

*Default Value:* 100

*Validator:* [1,...]



##### `imap.port`

The port of the IMAP server to connect to.

*Importance:* LOW

*Type:* INT

*Default Value:* 993

*Validator:* ValidPort{start=1, end=65535}



##### `imap.scheme`

The scheme to connect to the IMAP server with. 

*Importance:* LOW

*Type:* STRING

*Default Value:* imaps






# Development

## Building the source

```bash
mvn clean package
```

## Contributions

Contributions are always welcomed! Before you start any development please create an issue and
start a discussion. Create a pull request against your newly created issue and we're happy to see
if we can merge your pull request. First and foremost any time you're adding code to the code base
you need to include test coverage. Make sure that you run `mvn clean package` before submitting your
pull to ensure that all of the tests, checkstyle rules, and the package can be successfully built.