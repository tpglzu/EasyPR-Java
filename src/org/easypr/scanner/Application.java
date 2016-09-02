package org.easypr.scanner;

import java.io.File;

public class Application {
    public static final File SD_HOME = new File("C:\\Users\\tangpg\\Documents\\EasyPR-Java");
    public static final String APP_NAME = "CashScanner";
    public static final String APP_HOME = SD_HOME.getAbsolutePath() + File.separator + APP_NAME;
    public static final String APP_HISTORY_DIR = APP_HOME + File.separator + "history";
    public static final String APP_TEMP_DIR = APP_HOME + File.separator + "tmp";
    public static final String APP_DETECT_DIR = APP_HOME + File.separator + "detect";
    public static final String APP_MODEL_DIR =  APP_HOME + File.separator + "model";
    public static final String APP_TRAIN_DIR =  APP_HOME + File.separator + "train";
    public static final String TAG =  "cash-scanner";

    public static final boolean DEBUG = true;
}
