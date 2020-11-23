package com.faker.android;

import brut.androlib.Androlib;
import brut.androlib.AndrolibException;
import brut.androlib.ApkDecoder;
import brut.common.BrutException;
import brut.directory.DirectoryException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class ApkTool {
    /**
     * 拆开文件
     * @return
     */
    public static boolean decodeRes(File targetApkPath, File ouputDir) {
        ApkDecoder decoder = new ApkDecoder();
        try {
            decoder.setDecodeSources((short) 0);
            decoder.setOutDir(ouputDir);
            decoder.setForceDelete(true);
            decoder.setApkFile(targetApkPath);
            decoder.decode();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean decodeSrc(File targetApkPath, File ouputDir) throws AndrolibException, IOException, DirectoryException {
        ApkDecoder decoder = new ApkDecoder();
        decoder.setBaksmaliDebugMode(false);
        decoder.setOutDir(ouputDir);
        decoder.setForceDelete(true);
        decoder.setDecodeSources(ApkDecoder.DECODE_SOURCES_SMALI_ONLY_MAIN_CLASSES);
        decoder.setDecodeToolModel(ApkDecoder.DECODE_TOOL_MODEL_FAKER_ANDROID);
        decoder.setApkFile(targetApkPath);
        decoder.decode();
        return true;
    }

    /**
     * Android 重新打包
     * @return
     */
    public static boolean build(File sourceDir, File outPutApkPath) {
        Androlib instance = new Androlib();
        HashMap<String, Boolean> flags = new HashMap<String, Boolean>();
        flags.put("forceBuildAll", Boolean.valueOf(true));
        flags.put("debug", Boolean.valueOf(false));
        flags.put("verbose", Boolean.valueOf(false));
        flags.put("framework", Boolean.valueOf(false));
        flags.put("update", Boolean.valueOf(false));
        flags.put("copyOriginal", Boolean.valueOf(false));
        try {
            instance.build(sourceDir, outPutApkPath);
            return true;
        } catch (BrutException e) {
            e.printStackTrace();
        }
        return false;
    }
}
