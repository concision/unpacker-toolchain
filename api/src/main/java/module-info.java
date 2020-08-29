module me.concision.unnamed.packages.api {
    requires static lombok;
    requires org.mongodb.bson;
    requires org.apache.commons.codec;

    exports me.concision.unnamed.decacher.api;
    exports me.concision.unnamed.unpacker.api;
}
