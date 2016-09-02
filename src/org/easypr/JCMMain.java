package org.easypr;

import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.easypr.scanner.detect.CashNumberDetect;
import org.easypr.scanner.recognise.CashNumberCharsRecognise;
import org.easypr.train.JCMTrain;

public class JCMMain {
	private static String jcmDataDir = "C:\\JCM_DATA\\Renamed_CHN";
	private static String okFold = "C:\\Users\\tangpg\\Documents\\EasyPR-Java\\res\\train\\data\\jcm-data";
	private static String ngFold0 = "C:\\Users\\tangpg\\Documents\\EasyPR-Java\\res\\train\\data\\jcm-data-ng-0";
	private static String ngFold1 = "C:\\Users\\tangpg\\Documents\\EasyPR-Java\\res\\train\\data\\jcm-data-ng-1";

	public static Map<String, Integer> charInexMap = initCharInexMap();

	private static Map<String, Integer> initCharInexMap() {
		Map<String, Integer> retMap = new HashMap<String, Integer>();

		for (int i = 0; i < 10; i++) {
			File targetFold = new File(okFold + File.separator + i);
			if (!targetFold.exists()) {
				targetFold.mkdirs();
			}
			int length = targetFold.list().length;
			retMap.put(String.valueOf(i), length);
		}

		for (int i = 65; i < 91; i++) {
			File targetFold = new File(okFold + File.separator + (char) i);
			if (!targetFold.exists()) {
				targetFold.mkdirs();
			}
			int length = targetFold.list().length;
			retMap.put(String.valueOf((char) i), length);
		}

		return retMap;
	}


	private static void generateTrainData() throws IOException {
		List<String> FoldList = new ArrayList<String>();
		getFoldList(new File(jcmDataDir), FoldList,"Adir");
		int totalCount = 0;
		int okCount = 0;
		int ngCount = 0;
		for (String fold : FoldList) {

			File[] files = new File(fold).listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".bmp");
				}
			});

			for (File file : files) {
				totalCount++;

				System.out.println("start : " + file);
				String fileName = file.getName();

				String[] fileNameArr = fileName.substring(0,
						fileName.indexOf(".")).split("_");
				String realNumber = fileNameArr[fileNameArr.length - 1];
				if (realNumber.length() != 10) {
					System.err.println("NG-9 : " + file);
					ngCount++;
					continue;
				}

				Mat src = imread(file.getAbsolutePath());
				CashNumberDetect jcmDetect = new CashNumberDetect();
				jcmDetect.setPDLifemode(1);
				Vector<Mat> matVector = new Vector<Mat>();
				if (0 == jcmDetect.plateDetect(src, matVector)) {
					CashNumberCharsRecognise cr = new CashNumberCharsRecognise();

					if (matVector.size() != 1) {
						System.err.println("NG-0 : " + file);
						FileUtils.copyFile(file, new File(ngFold0
								+ File.separator + fileName));
						ngCount++;
						continue;
					}

					Vector<Mat> charsSegment = new Vector<Mat>();
					String result = cr.charsRecognise(matVector.get(0),
							charsSegment);
					if (charsSegment.size() != 10) {
						System.err.println("NG-1 : " + file);
						FileUtils.copyFile(file, new File(ngFold1
								+ File.separator + fileName));
						ngCount++;
						continue;
					}

					char[] numbers = realNumber.toCharArray();
					for (int i = 0; i < numbers.length; i++) {
						String indexMapKey = String.valueOf(numbers[i]);
						int indexMapValue = charInexMap.get(indexMapKey);
						String targetFileName = okFold + File.separator
								+ numbers[i] + File.separator + indexMapValue
								+ ".jpg";
						imwrite(targetFileName, charsSegment.get(i));
						charInexMap.put(indexMapKey, ++indexMapValue);
						okCount++;
					}

					System.out.println("Chars Recognised: " + result);
				}
			}
		}
		System.out.println("total : "+totalCount+", ng : " + ngCount + ", ok : " + okCount);

	}

	private static void getFoldList(File dir, List<String> result,String contailName) {
		if (dir.isDirectory()) {
			File[] fileList = dir.listFiles();
			boolean hasSubDir = false;
			for (File file : fileList) {
				if (file.isDirectory()) {
					hasSubDir = true;
					getFoldList(file, result,contailName);
				}
			}
			if (!hasSubDir && dir.getName().contains(contailName)) {
				result.add(dir.getAbsolutePath());
			}
		}
	}
	
	private static void trainJCMAnn(){
		new JCMTrain().annMain();
	}
	
	private static void testJCMAnn(){
			List<String> FoldList = new ArrayList<String>();
			getFoldList(new File(jcmDataDir), FoldList,"Adir");
			for (String fold : FoldList) {

				File[] files = new File(fold).listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.endsWith(".bmp");
					}
				});

				for (File file : files) {
					System.out.println("start : " + file);
					String fileName = file.getName();

					String[] fileNameArr = fileName.substring(0,
							fileName.indexOf(".")).split("_");
					String realNumber = fileNameArr[fileNameArr.length - 1];
					if (realNumber.length() != 10) {
						continue;
					}

					Mat src = imread(file.getAbsolutePath());
					CashNumberDetect jcmDetect = new CashNumberDetect();
					jcmDetect.setPDLifemode(1);
					Vector<Mat> matVector = new Vector<Mat>();
					if (0 == jcmDetect.plateDetect(src, matVector)) {
						CashNumberCharsRecognise cr = new CashNumberCharsRecognise();

						if (matVector.size() != 1) {
							continue;
						}

						Vector<Mat> charsSegment = new Vector<Mat>();
						String result = cr.charsRecognise(matVector.get(0),
								charsSegment);
						if (charsSegment.size() != 10) {
							continue;
						}
						
						System.out.println(realNumber + " --- " + result+ " :: " + getSimilarityRatio(realNumber,result));
					}
				}
			}
	}
	
    private static int compare(String str, String target)
    {
        int d[][];              // 矩阵
        int n = str.length();
        int m = target.length();
        int i;                  // 遍历str的
        int j;                  // 遍历target的
        char ch1;               // str的
        char ch2;               // target的
        int temp;               // 记录相同字符,在某个矩阵位置值的增量,不是0就是1
        if (n == 0) { return m; }
        if (m == 0) { return n; }
        d = new int[n + 1][m + 1];
        for (i = 0; i <= n; i++)
        {                       // 初始化第一列
            d[i][0] = i;
        }

        for (j = 0; j <= m; j++)
        {                       // 初始化第一行
            d[0][j] = j;
        }

        for (i = 1; i <= n; i++)
        {                       // 遍历str
            ch1 = str.charAt(i - 1);
            // 去匹配target
            for (j = 1; j <= m; j++)
            {
                ch2 = target.charAt(j - 1);
                if (ch1 == ch2 || ch1 == ch2+32 || ch1+32 == ch2)
                {
                    temp = 0;
                } else
                {
                    temp = 1;
                }
                // 左边+1,上边+1, 左上角+temp取最小
                d[i][j] = min(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + temp);
            }
        }
        return d[n][m];
    }

    private static int min(int one, int two, int three)
    {
        return (one = one < two ? one : two) < three ? one : three;
    }
    
	private static void predictJCMAnn(){
		List<String> FoldList = new ArrayList<String>();
		getFoldList(new File("C:\\Users\\tangpg\\Documents\\EasyPR-Java\\res\\train\\data\\cash"), FoldList,"");
		int totalCount = 0;
		int detectCount = 0;
		int recogniseCount = 0;
		float recogniseRate = 0;
		for (String fold : FoldList) {

			File[] files = new File(fold).listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".jpg");
				}
			});

			for (File file : files) {
				String fileName = file.getName();

				String[] fileNameArr = fileName.substring(0,
						fileName.indexOf(".")).split("_");
				String realNumber = fileNameArr[fileNameArr.length - 1];
//				if (realNumber.length() != 10) {
//					continue;
//				}
				totalCount ++;

				Mat src = imread(file.getAbsolutePath());
				CashNumberDetect jcmDetect = new CashNumberDetect();
				jcmDetect.setPDLifemode(3);
				Vector<Mat> matVector = new Vector<Mat>();
				if (0 == jcmDetect.plateDetect(src, matVector)) {
					CashNumberCharsRecognise cr = new CashNumberCharsRecognise();

//					if (matVector.size() != 1) {
//						continue;
//					}
					
					String result = "";
					for (int i = 0; i < matVector.size(); i++) {
						Vector<Mat> charsSegment = new Vector<Mat>();
						String tmp = cr.charsRecognise(matVector.get(i),
								charsSegment);
						if(Math.abs(tmp.length() - 10) < Math.abs(result.length() - 10)){
							result = tmp;
						}
					}
					if(result != ""){
						detectCount++;
						recogniseCount++;
						recogniseRate += getSimilarityRatio(realNumber,result);
					}
					
					System.out.println(realNumber + " --- " + result+ " :: " + getSimilarityRatio(realNumber,result));
				}
			}
		}
}

    /**
     * 获取两字符串的相似度
     */

    public static float getSimilarityRatio(String str, String target)
    {
        return 1 - (float) compare(str, target) / Math.max(str.length(), target.length());
    }
	
	public static void main(String[] args) throws IOException {
//		generateTrainData();
//		trainJCMAnn();
//		testJCMAnn();
		predictJCMAnn();
	}
	
	
}
