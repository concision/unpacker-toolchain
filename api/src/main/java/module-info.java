module me.concision.unnamed.unpacker.api {
    requires static lombok;
    requires com.google.gson;

    exports me.concision.unnamed.decacher.api;
    exports me.concision.unnamed.unpacker.api;
}
