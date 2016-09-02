package org.easypr.scanner.recognise;

import java.util.Vector;

import org.bytedeco.javacpp.opencv_core.Mat;

/**
 * @author lin.yao
 * 
 */
public class CashNumberCharsRecognise {

    private CashNumberCharsSegment charsSegment = new CashNumberCharsSegment();

    private CashNumberCharsIdentify charsIdentify = new CashNumberCharsIdentify();

    /**
     * Chars segment and identify 字符分割与识别
     * 
     * @param plate
     *            the input plate
     * @return the result of plate recognition
     */
    public String charsRecognise(final Mat plate,Vector<Mat> matVec) {

        // the result of plate recognition
        String plateIdentify = "";

        int result = charsSegment.charsSegment(plate, matVec);
        if (0 == result) {
            for (int j = 0; j < matVec.size(); j++) {
                Mat charMat = matVec.get(j);
                String charcater = charsIdentify.charsIdentify(charMat, false, (1 == j));
                plateIdentify = plateIdentify + charcater;
            }
        }
        return plateIdentify;
    }
}
