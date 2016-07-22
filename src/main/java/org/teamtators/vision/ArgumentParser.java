package org.teamtators.vision;

import java.util.Arrays;
import java.util.List;

public class ArgumentParser {
    private List<String> argsList;

    /**
     * Constructor with argument[]
     * @param args argument array
     */
    public ArgumentParser(String[] args) {
        argsList = Arrays.asList(args);
    }

    /**
     * Get configuration option
     * @return configuration option (should be a filepath)
     */
    public String getConfigFile() {
        int index;
        index = argsList.indexOf("--config") + 1;
        if (index > 0) {
            return argsList.get(index);
        }
        index = argsList.indexOf("-c") + 1;
        if (index > 0) {
            return argsList.get(index);
        }
        return "";
    }

    /**
     * Get native library option
     * @return native library option (should be a filepath)
     */
    public String getNativeLibrary() {
        int index;
        index = argsList.indexOf("--native-library") + 1;
        if (index > 0) {
            return argsList.get(index);
        }
        index = argsList.indexOf("-l") + 1;
        if (index > 0) {
            return argsList.get(index);
        }
        return "";
    }
}
