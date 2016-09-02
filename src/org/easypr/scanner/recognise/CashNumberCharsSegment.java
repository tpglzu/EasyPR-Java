package org.easypr.scanner.recognise;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import org.easypr.scanner.util.Convert;

import java.util.Vector;

import static org.bytedeco.javacpp.opencv_core.CV_32F;
import static org.bytedeco.javacpp.opencv_core.countNonZero;
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;
import static org.bytedeco.javacpp.opencv_imgproc.CV_CHAIN_APPROX_NONE;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RETR_EXTERNAL;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RGB2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_BINARY_INV;
import static org.bytedeco.javacpp.opencv_imgproc.CV_THRESH_OTSU;
import static org.bytedeco.javacpp.opencv_imgproc.INTER_LINEAR;
import static org.bytedeco.javacpp.opencv_imgproc.boundingRect;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.findContours;
import static org.bytedeco.javacpp.opencv_imgproc.resize;
import static org.bytedeco.javacpp.opencv_imgproc.threshold;
import static org.bytedeco.javacpp.opencv_imgproc.warpAffine;

/**
 * @author tang_penggui
 * 
 */
public class CashNumberCharsSegment {

    // 是否开启调试模式常量，默认false代表关闭
    final static boolean DEFAULT_DEBUG = true;

    // preprocessChar所用常量
    final static int CHAR_SIZE = 20;

    private boolean isDebug = DEFAULT_DEBUG;

    /**
     * 字符分割
     * 
     * @param input
     * @param resultVec
     * @return <ul>
     *         <li>more than zero: the number of chars;
     *         <li>-3: null;
     *         </ul>
     */
    public int charsSegment(final Mat input, Vector<Mat> resultVec) {
        if (input.data().isNull())
            return -3;

        Mat img_threshold = new Mat();

        Mat input_grey = new Mat();
        cvtColor(input, input_grey, CV_RGB2GRAY);
        
        threshold(input_grey, img_threshold, 10, 255, CV_THRESH_OTSU + CV_THRESH_BINARY_INV);

        if (this.isDebug) {	
            imwrite("debug_char_threshold.jpg", img_threshold);
        }

        // 找轮廓
        Mat img_contours = new Mat();
        img_threshold.copyTo(img_contours);

        MatVector contours = new MatVector();

        findContours(img_contours, contours, // a vector of contours
                CV_RETR_EXTERNAL, // retrieve the external contours
                CV_CHAIN_APPROX_NONE); // all pixels of each contours

        // 将不符合特定尺寸的图块排除出去
        Vector<Rect> vecRect = new Vector<Rect>();
        for (int i = 0; i < contours.size(); ++i) {
            Rect mr = boundingRect(contours.get(i));
            if (verifySizes(new Mat(img_threshold, mr)))
                vecRect.add(mr);
        }

        if (vecRect.size() == 0)
            return -3;

        Vector<Rect> sortedRect = new Vector<Rect>();
        // 对符合尺寸的图块按照从左到右进行排序
        SortRect(vecRect, sortedRect);

        if (sortedRect.size() == 0)
            return -3;

        for (int i = 0; i < sortedRect.size(); i++) {
            Rect mr = sortedRect.get(i);
            Mat auxRoi = new Mat(img_threshold, mr);

            auxRoi = preprocessChar(auxRoi);
            
            if (this.isDebug) {
                String str = "debug_char_auxRoi_" + Integer.valueOf(i).toString() + ".jpg";
                imwrite(str, auxRoi);
            }
            resultVec.add(auxRoi);
        }
        return 0;
    }

    /**
     * 字符尺寸验证
     * 
     * @param r
     * @return
     */
    private Boolean verifySizes(Mat r) {
        float aspect = 45.0f / 90.0f;
        float charAspect = (float) r.cols() / (float) r.rows();
        float error = 0.7f;
        float minHeight = 10f;
        float maxHeight = 35f;
        // We have a different aspect ratio for number 1, and it can be ~0.2
        float minAspect = 0.05f;
        float maxAspect = aspect + aspect * error;
        // area of pixels
        float area = countNonZero(r);
        // bb area
        float bbArea = r.cols() * r.rows();
        // % of pixel in area
        float percPixels = area / bbArea;

        return percPixels <= 1 && charAspect > minAspect && charAspect < maxAspect && r.rows() >= minHeight
                && r.rows() < maxHeight;
    }

    /**
     * 字符预处理: 统一每个字符的大小
     * 
     * @param in
     * @return
     */
    private Mat preprocessChar(Mat in) {
        int h = in.rows();
        int w = in.cols();
        int charSize = CHAR_SIZE;
        Mat transformMat = Mat.eye(2, 3, CV_32F).asMat();
        int m = Math.max(w, h);
        transformMat.ptr(0, 2).put(Convert.getBytes(((m - w) / 2f)));
        transformMat.ptr(1, 2).put(Convert.getBytes((m - h) / 2f));

        Mat warpImage = new Mat(m, m, in.type());
        warpAffine(in, warpImage, transformMat, warpImage.size(), INTER_LINEAR, 0, new Scalar(0));

        Mat out = new Mat();
        resize(warpImage, out, new Size(charSize, charSize));

        return out;
    }



    /**
     * 将Rect按位置从左到右进行排序
     * 
     * @param vecRect
     * @param out
     * @return
     */
    private void SortRect(final Vector<Rect> vecRect, Vector<Rect> out) {
        Vector<Integer> orderIndex = new Vector<Integer>();
        Vector<Integer> xpositions = new Vector<Integer>();
        for (int i = 0; i < vecRect.size(); ++i) {
            orderIndex.add(i);
            xpositions.add(vecRect.get(i).x());
        }

        float min = xpositions.get(0);
        int minIdx;
        for (int i = 0; i < xpositions.size(); ++i) {
            min = xpositions.get(i);
            minIdx = i;
            for (int j = i; j < xpositions.size(); ++j) {
                if (xpositions.get(j) < min) {
                    min = xpositions.get(j);
                    minIdx = j;
                }
            }
            int aux_i = orderIndex.get(i);
            int aux_min = orderIndex.get(minIdx);
            orderIndex.remove(i);
            orderIndex.insertElementAt(aux_min, i);
            orderIndex.remove(minIdx);
            orderIndex.insertElementAt(aux_i, minIdx);

            float aux_xi = xpositions.get(i);
            float aux_xmin = xpositions.get(minIdx);
            xpositions.remove(i);
            xpositions.insertElementAt((int) aux_xmin, i);
            xpositions.remove(minIdx);
            xpositions.insertElementAt((int) aux_xi, minIdx);
        }

        for (int i = 0; i < orderIndex.size(); i++)
            out.add(vecRect.get(orderIndex.get(i)));

        return;
    }

    public boolean getDebug() {
        return this.isDebug;
    }

    public void setDebug(boolean isDebug) {
        this.isDebug = isDebug;
    }

}
