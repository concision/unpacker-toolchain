module me.concision.unnamed.packages.ioapi {
    requires static lombok;
    requires org.mongodb.bson;
    requires jsr305;
    requires api; // org.semver
    requires org.apache.commons.codec;

    exports me.concision.unnamed.packages.ioapi;
}
