module me.concision.unnamed.unpacker.cli {
    // require maven dependencies
    requires static me.concision.unnamed.unpacker.api;

    requires static lombok;
    requires static argparse4j;
    requires static org.json;
    requires static org.mongodb.bson;
    requires static org.apache.httpcomponents.httpclient;
    requires static org.apache.httpcomponents.httpcore;
    requires static jna;
    requires static jna.platform;
    requires static org.apache.commons.compress;

    // java runtime
    requires java.base;
    requires java.logging;

    requires java.naming; // required by httpclient

    // export all packages
    exports me.concision.unnamed.unpacker.cli;
    exports me.concision.unnamed.unpacker.cli.output;
    exports me.concision.unnamed.unpacker.cli.output.writers.multi;
    exports me.concision.unnamed.unpacker.cli.output.writers.single;
    exports me.concision.unnamed.unpacker.cli.source;
    exports me.concision.unnamed.unpacker.cli.source.collectors;
}
