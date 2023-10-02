package com.heima.tess4j;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.io.File;

public class detect {
    public static void main(String[] args) throws TesseractException {
        ITesseract tesseract = new Tesseract();
        File file = new File("D:\\java3\\hmtt\\heima-leadnews\\tess4j\\123.PNG");
        //设置字体库路径和语言
        tesseract.setDatapath("D:\\java3\\hmtt\\heima-leadnews\\tess4j");   //只用设置外层文件夹
        tesseract.setLanguage("chi_sim");

        String s = tesseract.doOCR(file);
        System.out.println("识别结果为："+s.replaceAll("[\\r\\n]","-"));
    }
}
