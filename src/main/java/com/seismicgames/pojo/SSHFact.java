package com.seismicgames.pojo;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

public class SSHFact extends PuppetDBResult {
    public class Keys {
        @SerializedName("key")
        public String key;

        @SerializedName("fingerprints")
        public HashMap<String, String> fingerprints;
    }

    public class KeyObject {
        @SerializedName("dsa")
        public Keys dsa;

        @SerializedName("rsa")
        public Keys rsa;

        @SerializedName("ecdsa")
        public Keys ecdsa;

        @SerializedName("ed25519")
        public Keys ed25519;
    }

    @SerializedName("certname")
    public String certname;

    @SerializedName("name")
    public String name;

    @SerializedName("value")
    public KeyObject value;

    @SerializedName("environment")
    public String environment;
}
