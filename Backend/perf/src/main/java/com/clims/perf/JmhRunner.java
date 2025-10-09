package com.clims..nperf;

import org.openjdk.jmh.Main;

public class JmhRunner {
    public static void main(String[] args) throws Exception {
        String[] effective = args;
        if (effective.length == 0) {
            effective = new String[] {
                    "-f", "1",
                    "-wi", "1",
                    "-i", "2",
                    "AssetListBenchmark.mapProjection"
            };
        }
        Main.main(effective);
    }
}
