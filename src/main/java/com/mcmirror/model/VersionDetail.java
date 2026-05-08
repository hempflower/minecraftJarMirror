package com.mcmirror.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a single version's detail JSON (e.g. 1.12.json).
 */
public class VersionDetail {

    @SerializedName("assetIndex")
    public AssetIndex assetIndex;

    @SerializedName("downloads")
    public Downloads downloads;

    public static class AssetIndex {
        @SerializedName("id")
        public String id;

        @SerializedName("url")
        public String url;

        @SerializedName("sha1")
        public String sha1;
    }

    public static class Downloads {
        @SerializedName("client")
        public Artifact client;

        @SerializedName("server")
        public Artifact server;

        @SerializedName("client_mappings")
        public Artifact clientMappings;

        @SerializedName("server_mappings")
        public Artifact serverMappings;
    }

    public static class Artifact {
        @SerializedName("url")
        public String url;

        @SerializedName("sha1")
        public String sha1;

        @SerializedName("size")
        public long size;
    }
}
