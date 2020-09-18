module me.concision.unnamed.unpacker.cli {
    // shaded maven dependencies
    requires static me.concision.unnamed.unpacker.api;
    requires static lombok;
    requires static argparse4j;
    requires static com.google.gson;
    requires static org.apache.httpcomponents.httpclient;
    requires static org.apache.httpcomponents.httpcore;
    requires static com.sun.jna.platform;
    requires static com.sun.jna;
    requires static org.apache.commons.compress;


    // java runtime environment
    requires java.base;
    requires java.logging;
    // required by httpclient
    requires java.naming;


    // export all CLI packages
    exports me.concision.unnamed.unpacker.cli;
    exports me.concision.unnamed.unpacker.cli.output;
    exports me.concision.unnamed.unpacker.cli.output.writers.multi;
    exports me.concision.unnamed.unpacker.cli.output.writers.single;
    exports me.concision.unnamed.unpacker.cli.source;
    exports me.concision.unnamed.unpacker.cli.source.collectors;
}
