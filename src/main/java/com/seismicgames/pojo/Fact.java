package com.seismicgames.pojo;

import com.google.gson.annotations.SerializedName;

public class Fact extends PuppetDBResult {
    @SerializedName("certname")
    public String certname;

    @SerializedName("name")
    public String name;

    @SerializedName("value")
    public String value;

    @SerializedName("environment")
    public String environment;
}
