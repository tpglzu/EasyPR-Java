package org.easypr.scanner.util;

import org.bytedeco.javacpp.opencv_core.Mat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;
import static org.easypr.scanner.Application.*;
import static org.tpglzu.utils.DateUtils.getTimeStamp;


/**
 * Created by tangpg on 2016/08/25.
 */
public class ImgFileUtils {

    public static void saveTmp(String fileName, Mat src) {
        File tempDir = new File(APP_TEMP_DIR);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        String targetFile = APP_TEMP_DIR + File.separator + getTimeStamp() + "_" + fileName;
        imwrite(targetFile, src);
    }

    public static void saveDetect(String fileName, Mat src) {
        File tempDir = new File(APP_DETECT_DIR);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        String targetFile = APP_DETECT_DIR + File.separator + fileName;
        imwrite(targetFile, src);
    }

    public static String getDetectPath(String fileName) {
        return APP_DETECT_DIR + File.separator + fileName;
    }

    public static Mat readScreenShotTmpToMat(String fileName) {
        return imread(APP_TEMP_DIR + File.separator + fileName);
    }

    public static String getScreenShotPath(String fileName){
        return APP_HISTORY_DIR + File.separator + fileName;
    }

}
