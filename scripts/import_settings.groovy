#! /usr/bin/env groovy
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValue
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.BoundedExponentialBackoffRetry
import org.apache.zookeeper.KeeperException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Grapes([
        @Grab(group = 'org.slf4j', module = 'slf4j-simple', version = '1.7.12'),
        @Grab('org.apache.curator:curator-framework:2.8.0'),
        @Grab('com.typesafe:config:1.3.0'),
        @Grab('commons-cli:commons-cli:1.3.1')
])

/**
 * The default path to the file with the HOCON configuration data to be imported.
 */
final String DEFAULT_CONFIG_PATH = "settings.conf"

/**
 * The default list of ZooKeeper servers in the ZooKeeper ensemble.
 */
final String DEFAULT_ZK_CONNECTION_STRING = "localhost:2181"

/**
 * The default number of connection retries in case of ZooKeeper communication failure occurs.
 */
final int NUMBER_OF_CONNECTION_RETRIES = 5

/**
 * The minimum interval between connection retry attempts (in milliseconds).
 */
final int MIN_INTERVAL_BETWEEN_RETRIES = 250

/**
 * The maximum interval between connection retry attempts (in milliseconds).
 */
final int MAX_INTERVAL_BETWEEN_RETRIES = 25000

/**
 * The script logger instance.
 */
final Logger logger = LoggerFactory.getLogger(this.getClass())

/**
 * The CLI Builder.
 */
final CliBuilder cli = new CliBuilder(usage: 'groovy import_settings.groovy -[fcn]')
cli.with {
    f(longOpt: 'file', args: 1, argName: 'filename', 'Path to the settings file. Default is "settings.conf"')
    c(longOpt: 'connect-string', args: 1, argName: 'connect-string', 'The list of Zookeeper servers to connect to. Default is "localhost:2181"')
    n(longOpt: 'namespace', args: 1, argName: 'namespace', 'Optional ZooKeeper namespace to use when importing configuration data')
    help 'Show usage information'
}

/**
 * The Options accessor; contains parsed CLI options.
 */
final OptionAccessor options = cli.parse(args)

/**
 * The descriptor of the file with the configuration to import.
 */
File configFile

/**
 * The connection string with a list of server addresses in the ZooKeeper ensemble.
 */
String connectString

/**
 * The ZooKeeper namespace for the specified configuration data.
 */
String namespace

// Terminates execution if invalid options are provided.
if (!options) {
    cli.usage()
    return
}

// Displays script usage information and terminates the script execution if help option is specified.
if (options.help) {
    cli.usage()
    return
}

// Sets a path to the file with the configuration to import (or uses the default one).
if (options.f) {
    configFile = new File(options.f.toString())
} else {
    configFile = new File(DEFAULT_CONFIG_PATH)
}

// Sets a ZooKeeper ensemble connection string (or uses the default one).
if (options.c) {
    connectString = options.c.toString()
} else {
    connectString = DEFAULT_ZK_CONNECTION_STRING
}

// Sets a ZooKeeper namespace for the imported configuration (or doesn't set namespace at all).
if (options.n) {
    namespace = options.n.toString()
} else {
    namespace = ""
}

/**
 * The ZooKeeper client's connection retry policy.
 */
final BoundedExponentialBackoffRetry retryPolicy = new BoundedExponentialBackoffRetry(MIN_INTERVAL_BETWEEN_RETRIES,
        MAX_INTERVAL_BETWEEN_RETRIES, NUMBER_OF_CONNECTION_RETRIES)

/**
 * Configures ZooKeeper client.
 */
final CuratorFramework client = CuratorFrameworkFactory.builder()
        .connectString(connectString)
        .retryPolicy(retryPolicy)
        .namespace(namespace)
        .build()

try {
    // Checks if a file with the configuration exists and whether it is not a directory
    if (configFile.exists() && !configFile.isDirectory()) {
        // Tries to parse the configuration from the file
        final Config config = ConfigFactory.parseFileAnySyntax(configFile)
        // Flattens the configuration entries and sorts them by the key (asc)
        final Map<String, ConfigValue> entries = config.entrySet().collectEntries().sort()

        // Starts the ZooKeeper client
        client.start()

        entries.each { k, v ->
            // Builds the path to the ZooKeeper node
            final String path = "/" + k.replaceAll('\\.', '/')
            try {
                // Creates the appropriate node on ZooKeeper and assigns configuration value to it
                client.create().creatingParentsIfNeeded().forPath(path, v.unwrapped().toString().getBytes())
                logger.info("Node '${path}' is created")
            } catch (KeeperException e) {
                logger.warn("Unable to create node for path '${path}': ${e.code()}")
            }
        }
    } else {
        logger.warn("File with configuration at path ${configFile.getPath()} does not exist")
    }
} catch (Throwable t) {
    logger.error("Unable to import settings from file ${configFile.getPath()}: ${t.getMessage()}", t)
} finally {
    // Terminates the ZooKeeper client
    client.close()
}
