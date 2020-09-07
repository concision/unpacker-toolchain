module me.concision.unnamed.unpacker.api {
    requires static lombok;
    requires org.mongodb.bson;

    exports me.concision.unnamed.decacher.api;
    exports me.concision.unnamed.unpacker.api;
}
