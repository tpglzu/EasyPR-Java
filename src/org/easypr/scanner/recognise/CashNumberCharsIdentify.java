package org.easypr.scanner.recognise;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_ml.ANN_MLP;
import org.easypr.scanner.util.Convert;
import org.easypr.scanner.util.MLUtil;

import static org.bytedeco.javacpp.opencv_core.CV_32FC1;
import static org.easypr.scanner.core.CashCoreFunc.features;

/**
 * @author tang_penggui
 */
public class CashNumberCharsIdentify {

    private String path = MLUtil.getANNModel();

    private ANN_MLP ann = ANN_MLP.create();

    private int predictSize = 10;

    private final char strCharacters[] = {'0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
            'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'W', 'X',
            'Y', 'Z'};
    private final int numCharacter = 35;

    private final int numAll = numCharacter;

    public CashNumberCharsIdentify() {
        loadModel();
    }

    /**
     * @param input
     * @param isChinese
     * @return
     */
    public String charsIdentify(final Mat input, final Boolean isChinese,
                                final Boolean isSpeci) {
        String result = "";

        Mat f = features(input, this.predictSize);

        int index = classify(f, isChinese, isSpeci);
        if (index < 0) {
            return "?";
        }

        result = String.valueOf(strCharacters[index]);
        return result;
    }

    private int classify(final Mat f, final Boolean isChinses,
                         final Boolean isSpeci) {
        int result = -1;
        Mat output = new Mat(1, numAll, CV_32FC1);

        ann.predict(f, output, 1);

        int ann_min = (!isChinses) ? ((isSpeci) ? 10 : 0) : numCharacter;
        int ann_max = (!isChinses) ? numCharacter : numAll;

        float maxVal = -2;

        for (int j = ann_min; j < ann_max; j++) {
            float val = Convert.toFloat(output.ptr(0, j));

            if (val > maxVal) {
                maxVal = val;
                result = j;
            }
        }

        return result;
    }

    private void loadModel() {
        loadModel(this.path);
    }

    public void loadModel(String s) {
        this.ann.clear();
        this.ann = ANN_MLP.loadANN_MLP(s, null);
    }
}
