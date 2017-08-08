package com.seismicgames.pojo;

import com.google.gson.annotations.SerializedName;

import java.time.ZonedDateTime;

public class Node extends PuppetDBResult {
    @SerializedName("certname")
    public String certname;
    
    @SerializedName("deactivated")
    public ZonedDateTime deactivated;

    @SerializedName("expired")
    public ZonedDateTime expired;

    @SerializedName("catalog_timestamp")
    public ZonedDateTime catalogTimestamp;

    @SerializedName("facts_timestamp")
    public ZonedDateTime factsTimestamp;

    @SerializedName("report_timestamp")
    public ZonedDateTime reportTimestamp;

    @SerializedName("catalog_environment")
    public String catalogEnvironment;

    @SerializedName("facts_environment")
    public String factsEnvironment;

    @SerializedName("report_environment")
    public String reportEnvironment;

    @SerializedName("latest_report_status")
    public String latestReportStatus;

    @SerializedName("latest_report_noop")
    public boolean latestReportNoop;

    @SerializedName("latest_report_noop_pending")
    public boolean latestReportNoopPending;

    @SerializedName("latest_report_hash")
    public String latestReportHash;
}
