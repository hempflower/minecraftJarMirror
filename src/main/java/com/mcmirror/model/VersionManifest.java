package com.mcmirror.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the version_manifest.json from Mojang's launchermeta API.
 */
public class VersionManifest {

    @SerializedName("versions")
    public VersionEntry[] versions;

    public static class VersionEntry {
        /** e.g. "1.12", "1.16.5", "21w44a" */
        @SerializedName("id")
        public String id;

        /** URL to this version's detail JSON */
        @SerializedName("url")
        public String url;
    }
}
