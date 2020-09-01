module me.concision.unnamed.unpacker.cli {
    requires static lombok;
    requires static me.concision.unnamed.unpacker.api;

    requires static org.apache.logging.log4j;
    requires static org.apache.logging.log4j.core;
    requires static argparse4j;
    requires static org.json;
    requires static org.mongodb.bson;
    requires static org.apache.httpcomponents.httpclient;
    requires static org.apache.httpcomponents.httpcore;
    requires static jna.platform;
    requires static org.apache.commons.compress;

    requires java.base;
}
