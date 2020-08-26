module me.concision.unnamed.packages.ioapi {
    requires static lombok;
    requires org.mongodb.bson;
    requires api; // org.semver.api
    requires org.apache.commons.codec;

    exports me.concision.unnamed.packages.ioapi;
}
