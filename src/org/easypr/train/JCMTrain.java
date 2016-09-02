package org.easypr.train;


import static org.bytedeco.javacpp.opencv_core.CV_32F;
import static org.bytedeco.javacpp.opencv_core.CV_32FC1;
import static org.bytedeco.javacpp.opencv_core.CV_32SC1;
import static org.bytedeco.javacpp.opencv_core.CV_STORAGE_WRITE;
import static org.bytedeco.javacpp.opencv_core.getTickCount;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgproc.resize;
import static org.easypr.scanner.core.CashCoreFunc.features;
import static org.easypr.scanner.core.CashCoreFunc.projectedHistogram;

import java.awt.image.SampleModel;
import java.util.Vector;

import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.IntIndexer;
import org.bytedeco.javacpp.opencv_core.CvFileStorage;
import org.bytedeco.javacpp.opencv_core.CvMemStorage;
import org.bytedeco.javacpp.opencv_core.FileStorage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_ml.ANN_MLP;
import org.easypr.scanner.core.CashCoreFunc;
import org.easypr.scanner.core.CashCoreFunc.Direction;
import org.easypr.util.Convert;
import org.easypr.util.Util;

/*
 * Created by fanwenjie
 * @version 1.1
 */
public class JCMTrain {

    private ANN_MLP ann = ANN_MLP.create();

    // 中国车牌
    private final char strCharacters[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E',
            'F', 'G', 'H', 'I',
            'J', 'K', 'L', 'M', 'N', 'O','P', 'Q', 'R', 'S', 'T', 'U', 'W', 'X', 'Y', 'Z' };
    private final int numCharacter = 35; 

    private final int numAll = numCharacter; 

    public Mat features(Mat in, int sizeData) {
    	System.out.println(" 00000" + sizeData);
        // Histogram features
        float[] vhist = projectedHistogram(in, Direction.VERTICAL);
        float[] hhist = projectedHistogram(in, Direction.HORIZONTAL);

        // Low data feature
        Mat lowData = new Mat();
        resize(in, lowData, new Size(sizeData, sizeData));

        // Last 10 is the number of moments components
        int numCols = vhist.length + hhist.length + lowData.cols() * lowData.cols();

        Mat out = Mat.zeros(1, numCols, CV_32F).asMat();
        // Asign values to feature,ANN的样本特征为水平、垂直直方图和低分辨率图像所组成的矢量
        int j = 0;
        for (int i = 0; i < vhist.length; i++, ++j) {
        	System.out.println(" 1111 start" + vhist[i]);
        	System.out.println(" 1111 " + Convert.getBytes(vhist[i])[0] + "," +  Convert.getBytes(vhist[i])[1] + "," + Convert.getBytes(vhist[i])[2] + "," + Convert.getBytes(vhist[i])[3]);
            out.ptr(j).put(Convert.getBytes(vhist[i]));
            System.out.println(" 1111 end");
            
        }
        for (int i = 0; i < hhist.length; i++, ++j) {
        	System.out.println(" 2222 start" + hhist[i]);
        	System.out.println(" 2222 " + Convert.getBytes(hhist[i])[0] + "," +  Convert.getBytes(hhist[i])[1] + "," + Convert.getBytes(hhist[i])[2] + "," + Convert.getBytes(hhist[i])[3]);
            out.ptr(j).put(Convert.getBytes(hhist[i]));
            System.out.println(" 2222 end");
        }
        for (int x = 0; x < lowData.cols(); x++) {
            for (int y = 0; y < lowData.rows(); y++, ++j) {
                float val = lowData.ptr(x, y).get() & 0xFF;
                System.out.println(" 3333 start" + val);
                System.out.println(" 3333 " + Convert.getBytes(val)[0] + "," +  Convert.getBytes(val)[1] + "," + Convert.getBytes(val)[2] + "," + Convert.getBytes(val)[3]);
                out.ptr(j).put(Convert.getBytes(val));
                System.out.println(" 3333 end");
            }
        }
        // if(DEBUG)
        // cout << out << "\n===========================================\n";
        System.out.println("*****************");
        return out;
    }

    public void annTrain(Mat TrainData, Mat classes, int nNeruns) {
        ann.clear();
        Mat layers = new Mat(1, 3, CV_32SC1);
        IntIndexer idx = layers.createIndexer();
        idx.put(0, 0, TrainData.cols());
        idx.put(0, 1, nNeruns);
        idx.put(0, 2, numAll);
//        layers.ptr(0).put(Convert.getBytes(TrainData.cols()));
//        layers.ptr(1).put(Convert.getBytes(nNeruns));
//        layers.ptr(2).put(Convert.getBytes(numAll));
        ann.setLayerSizes(layers);
        ann.setActivationFunction(ANN_MLP.SIGMOID_SYM,1,1);
//        ann.create(layers, ANN_MLP.SIGMOID_SYM, 1, 1);

        // Prepare trainClases
        // Create a mat with n trained data by m classes
        Mat trainClasses = new Mat();
        trainClasses.create(TrainData.rows(), numAll, CV_32FC1);
        for (int i = 0; i < trainClasses.rows(); i++) {
            for (int k = 0; k < trainClasses.cols(); k++) {
                // If class of data i is same than a k class
                if (k == Convert.toInt(classes.ptr(i)))
                    trainClasses.ptr(i, k).put(Convert.getBytes(1f));
                else
                    trainClasses.ptr(i, k).put(Convert.getBytes(0f));
            }
        }
        Mat weights = new Mat(1, TrainData.rows(), CV_32FC1, Scalar.all(1));
        // Learn classifier
        
        org.bytedeco.javacpp.opencv_ml.TrainData tadata = org.bytedeco.javacpp.opencv_ml.TrainData.create(TrainData, 0, trainClasses);
        ann.train(tadata);
//        ann.train(TrainData, 0, trainClasses);
    }

    public int saveTrainData() {
        System.out.println("Begin saveTrainData");
        Mat classes = new Mat();
//        Mat trainingDataf5 = new Mat();
        Mat trainingDataf10 = new Mat();
//        Mat trainingDataf15 = new Mat();
//        Mat trainingDataf20 = new Mat();

        Vector<Integer> trainingLabels = new Vector<Integer>();
        String path = "C:\\Users\\tangpg\\Documents\\EasyPR-Java\\res\\train\\data\\jcm-data";

        for (int i = 0; i < numCharacter; i++) {
            System.out.println("Character: " + strCharacters[i]);
            String str = path + '/' + strCharacters[i];
            Vector<String> files = new Vector<String>();
            Util.getFiles(str, files);

            int size = (int) files.size();
            for (int j = 0; j < size; j++) {
                System.out.println(files.get(j));
                Mat img = imread(files.get(j), 0);
//                Mat f5 = features(img, 5);
                Mat f10 = CashCoreFunc.features(img, 10);
//                Mat f15 = features(img, 15);
//                Mat f20 = features(img, 20);
//                trainingDataf5.push_back(f5);
                trainingDataf10.push_back(f10);
//                trainingDataf15.push_back(f15);
//                trainingDataf20.push_back(f20);
                trainingLabels.add(i); // 每一幅字符图片所对应的字符类别索引下标
            }
        }

//        path = "res/train/data/chars_recognise_ann/charsChinese/charsChinese";
//
//        for (int i = 0; i < strChinese.length; i++) {
//            System.out.println("Character: " + strChinese[i]);
//            String str = path + '/' + strChinese[i];
//            Vector<String> files = new Vector<String>();
//            Util.getFiles(str, files);
//
//            int size = (int) files.size();
//            for (int j = 0; j < size; j++) {
//                System.out.println(files.get(j));
//                Mat img = imread(files.get(j), 0);
////                Mat f5 = features(img, 5);
//                Mat f10 = CoreFunc.features(img, 10);
////                Mat f15 = features(img, 15);
////                Mat f20 = features(img, 20);
//
////                trainingDataf5.push_back(f5);
//                trainingDataf10.push_back(f10);
////                trainingDataf15.push_back(f15);
////                trainingDataf20.push_back(f20);
//                trainingLabels.add(i + numCharacter);
//            }
//        }

//        trainingDataf5.convertTo(trainingDataf5, CV_32FC1);
        trainingDataf10.convertTo(trainingDataf10, CV_32FC1);
//        trainingDataf15.convertTo(trainingDataf15, CV_32FC1);
//        trainingDataf20.convertTo(trainingDataf20, CV_32FC1);
        int[] labels = new int[trainingLabels.size()];
        for (int i = 0; i < labels.length; ++i)
            labels[i] = trainingLabels.get(i).intValue();
        new Mat(labels).copyTo(classes);

//        FileStorage fs = new FileStorage("res/train/ann_data.xml", FileStorage.WRITE);
////        fs.writeObj("TrainingDataF5", trainingDataf5.data());
////        fs.writeObj("TrainingDataF10", trainingDataf10.data());
////        fs.writeObj("TrainingDataF15", trainingDataf15.data());
////        fs.writeObj("TrainingDataF20", trainingDataf20.data());
//        fs.writeObj("classes", classes.data());
//        fs.release();

        System.out.println("End saveTrainData");
        
        
        
        // train the Ann
        System.out.println("Begin to saveModelChar predictSize:" + Integer.valueOf(10).toString());
        System.out.println(" neurons:" + Integer.valueOf(40).toString());

        long start = getTickCount();
        annTrain(trainingDataf10, classes, 40);
        long end = getTickCount();
        System.out.println("GetTickCount:" + Long.valueOf((end - start) / 1000).toString());

        System.out.println("End the saveModelChar");
        
        String model_name = "res/model/ann.xml";

        // if(1)
        // {
        // String str =
        // String.format("ann_prd:%d\tneu:%d",_predictsize,_neurons);
        // model_name = str;
        // }

//        FileStorage fsto = FileStorage.open(model_name, CvMemStorage.create(), CV_STORAGE_WRITE);
        ann.save(model_name);
             
        return 0;
    }

    public void saveModel(int _predictsize, int _neurons) {
        FileStorage fs = new FileStorage("res/train/ann_data.xml", FileStorage.READ);
        String training = "TrainingDataF" + _predictsize;
        Mat TrainingData = new Mat(fs.get(training).readObj());
        Mat Classes = new Mat(fs.get("classes"));

        // train the Ann
        System.out.println("Begin to saveModelChar predictSize:" + Integer.valueOf(_predictsize).toString());
        System.out.println(" neurons:" + Integer.valueOf(_neurons).toString());

        long start = getTickCount();
        annTrain(TrainingData, Classes, _neurons);
        long end = getTickCount();
        System.out.println("GetTickCount:" + Long.valueOf((end - start) / 1000).toString());

        System.out.println("End the saveModelChar");

        String model_name = "res/train/ann.xml";

        // if(1)
        // {
        // String str =
        // String.format("ann_prd:%d\tneu:%d",_predictsize,_neurons);
        // model_name = str;
        // }
        
        
        CvFileStorage fsto = CvFileStorage.open(model_name, CvMemStorage.create(), CV_STORAGE_WRITE);
        ann.write(new FileStorage(fsto));
//        ann.save("test.xml");
    }

    public int annMain() {
        System.out.println("To be begin.");

        saveTrainData();

        // 可根据需要训练不同的predictSize或者neurons的ANN模型
        // for (int i = 2; i <= 2; i ++)
        // {
        // int size = i * 5;
        // for (int j = 5; j <= 10; j++)
        // {
        // int neurons = j * 10;
        // saveModel(size, neurons);
        // }
        // }

        // 这里演示只训练model文件夹下的ann.xml，此模型是一个predictSize=10,neurons=40的ANN模型。
        // 根据机器的不同，训练时间不一样，但一般需要10分钟左右，所以慢慢等一会吧。
//        saveModel(10, 40);

        System.out.println("To be end.");
        return 0;
    }
    
    public void testPredict(){
        Mat src = imread("C:\\Users\\tangpg\\Documents\\EasyPR-Java\\res\\train\\data\\chars_recognise_ann\\chars2\\chars2\\2\\100-2.jpg");
        Mat f = CashCoreFunc.features(src, 10);
        
        Mat output = new Mat(1, numAll, CV_32FC1);
        
        ANN_MLP pann = ANN_MLP.create();

        ANN_MLP.loadANN_MLP("C:\\Users\\tangpg\\Documents\\EasyPR-Java\\res\\train/ann.xml",null).predict(f, output,1);
        
        float maxVal = -2;
        int result = -1;
        for (int j = 0; j < 34; j++) {
            float val = Convert.toFloat(output.ptr(0, j));

            if (val > maxVal) {
                maxVal = val;
                result = j;
                System.out.println(j);
            }
        }
        System.out.println("******");
        System.out.println(result);
    }
}
