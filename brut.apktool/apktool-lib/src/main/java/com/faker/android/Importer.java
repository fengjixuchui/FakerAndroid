package com.faker.android;

import brut.androlib.AndrolibException;
import brut.androlib.meta.MetaInfo;
import brut.common.BrutException;
import brut.directory.DirectoryException;
import com.googlecode.d2j.reader.DexFileReader;
import com.googlecode.dex2jar.tools.BaksmaliBaseDexExceptionHandler;
import com.luhuiguo.chinese.ChineseUtils;
import com.luhuiguo.chinese.pinyin.PinyinFormat;
import org.dom4j.DocumentException;
import java.io.*;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class Importer extends IImporter {

    public Importer(XSrcTarget xSrcTarget, SourceCode sourceCode,ILogCat iLogCat) {
        super(xSrcTarget, sourceCode,iLogCat);
    }
    @Override
    boolean unZipTarget() {
        try {
            return xSrcTarget.decode();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.sendLog(iLogCat,e.toString());
            return false;
        }
    }

    @Override
    boolean orlderXTarget(XSrcTarget xSrcTarget) throws IOException {
        //so
//        File libDir = xSrcTarget.getLibDir();
//        libDir.renameTo(xSrcTarget.getjniLibs());

        //java
        File javaDir = xSrcTarget.getJava();
        javaDir.mkdirs();

        //libs
        File libs = xSrcTarget.getLibs();
        libs.mkdir();

        //cpp
        File cpp = xSrcTarget.getCpp();
        cpp.mkdir();

        //resources
        File resources = xSrcTarget.getResources();
        resources.mkdir();


        //TODO  source the smali dir by apk tool generate but apk tool dex generate leave a cover misstake waiting me to fix
        // if dex2 has a class dex1 already have  android system use the  dex1 class to perform
        //smali
//        File smalis = xSrcTarget.getSmalis();
//        smalis.mkdirs();
//        File files[] = xSrcTarget.getDecodeDir().listFiles();
//        for (File f:files) {
//            if(f.isDirectory()&&f.getName().startsWith("smali")&&!f.getName().equals("smali")){
//                //f.renameTo(new File(smalis,f.getName()));
//                File smaliFiles[] = f.listFiles();
//                for (File file :smaliFiles){
//                    File ms = new File(smalis,file.getName());
//                    System.out.println("ms "+ms.getAbsolutePath());
//                    if(ms.exists()) {//TODO smali路径优先
//                       continue;
//                    }
//                    file.renameTo(ms);
//                }
//            }
//        }


        return true;
    }

    @Override
    boolean mergeSourceCode(SourceCode sourceCode, XSrcTarget xSrcTarget) throws IOException {


        boolean isIl2cpp = false;
        if(new File(xSrcTarget.getCpp(),"Il2cpp-Scaffolding-ARM").exists()||new File(xSrcTarget.getCpp(),"Il2cpp-Scaffolding-ARM64").exists()){
            isIl2cpp = true;
        }

        PatchManger.copyDirFromJar(sourceCode.getGradle(),xSrcTarget.getGradle().getAbsolutePath());

        //拷贝cpp
        PatchManger.copyDirFromJar(sourceCode.getCpp(isIl2cpp),xSrcTarget.getCpp().getAbsolutePath());


        PatchManger.copyDirFromJar(sourceCode.getCppLibs(),xSrcTarget.getCppLibs().getAbsolutePath());

        //拷贝Java
        PatchManger.copyDirFromJar(sourceCode.getJava(isIl2cpp),xSrcTarget.getJava().getAbsolutePath());

        PatchManger.copyDirFromJar(sourceCode.getRes(),xSrcTarget.getResDir().getAbsolutePath());

        PatchManger.copyDirFromJar(sourceCode.getBuildGame(),xSrcTarget.getGameDir().getAbsolutePath());

        //TODO 修复build gradle
        File gameBuildGrandle = xSrcTarget.getGameBuild();

        try {
            ManifestEditor manifestEditor = new ManifestEditor(xSrcTarget.getManifestFile());
            FileUtil.autoReplaceStr(gameBuildGrandle,"{pkg}",manifestEditor.getPackagenName());

            File fileSmail = xSrcTarget.getSmalis();

            //deleteR(manifestEditor.getPackagenName(),fileSmail);
            if(isIl2cpp){
                File MainA = new File(xSrcTarget.getJava(),"com/faker/android/FakerUnityActivity.java");
                FileUtil.autoReplaceStr(MainA,"{R}",manifestEditor.getPackagenName()+".R");
            }else {
                File MainA = new File(xSrcTarget.getJava(),"com/faker/android/FakerActivity.java");
                FileUtil.autoReplaceStr(MainA,"{R}",manifestEditor.getPackagenName()+".R");
            }
//            File MainW = new File(xSrcTarget.getJava(),"com\\faker\\android\\WebViewActivity.java");
//            FileUtil.autoReplaceStr(MainW,"{R}",manifestEditor.getPackagenName()+".R");

        } catch (DocumentException e) {
            e.printStackTrace();
        }

        MetaInfo metaInfo = null;
        try {
            metaInfo = MetaInfo.load(new FileInputStream(xSrcTarget.getYmlFile()));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        FileUtil.autoReplaceStr(gameBuildGrandle,"{versionCode}",metaInfo.versionInfo.versionCode);
        FileUtil.autoReplaceStr(gameBuildGrandle,"{versionName}",metaInfo.versionInfo.versionName);
        String minSdkVersion = metaInfo.sdkInfo.get("minSdkVersion");
        //FileUtil.autoReplaceStr(gameBuildGrandle,"{minSdkVersion}",minSdkVersion);
        String targetSdkVersion =metaInfo.sdkInfo.get("targetSdkVersion");

        if(TextUtil.isEmpty(targetSdkVersion)){
            FileUtil.autoReplaceStr(gameBuildGrandle,"{targetSdkVersion}","26");
        }else {
            FileUtil.autoReplaceStr(gameBuildGrandle,"{targetSdkVersion}",targetSdkVersion);
        }
        PatchManger.copyDirFromJar(sourceCode.getBuildProject(),xSrcTarget.getProjectDir().getAbsolutePath());

        //PatchManger.copyDirFromJar(sourceCode.getBuildJavaScffoing(),xSrcTarget.getJavaScaffoding().getAbsolutePath());

        return true;
    }

    @Override
    boolean makeCppScaffolding (XSrcTarget xSrcTarget) throws IOException {
        Logger.sendLog(iLogCat,"checking or making il2cpp scaffolding...");
        exportCppScaffolding(xSrcTarget);

        File scaffolding_ARM = new File(xSrcTarget.getCpp(),"Il2cpp-Scaffolding-ARM");
        formatScaffolding(scaffolding_ARM);
        if(scaffolding_ARM.exists()){
            Logger.sendLog(iLogCat,"abi armeabi-v7a il2cpp scaffolding have generated.");
        }

        File Scaffolding_ARM64 = new File(xSrcTarget.getCpp(),"Il2cpp-Scaffolding-ARM64");
        formatScaffolding(Scaffolding_ARM64);
        if(Scaffolding_ARM64.exists()){
            Logger.sendLog(iLogCat,"abi arme64-v8a il2cpp scaffolding have generated.");
        }

//        File file1 = new File(xSrcTarget.getDecodeDir(),"kotlin");
//        delete(file1);
//        File file2 = new File(xSrcTarget.getDecodeDir(),"META-INF");
//        delete(file2);
//
//        File file3 = new File(xSrcTarget.getDecodeDir(),"original");
//        delete(file3);
//
//        File file4 = new File(xSrcTarget.getDecodeDir(),"unknown");
//        delete(file4);
        return true;
    }

    @Override
    boolean makeJavaScaffolding(SourceCode sourceCode, XSrcTarget xSrcTarget) throws IOException {
        Logger.sendLog(iLogCat,"java scaffolding is generateding...");
//        File file = xSrcTarget.getJavaScaffoding();
//        if(!file.exists()){
//            file.mkdir();
//        }
//        File fileJavaScaffodingJava = xSrcTarget.getJavaScaffodingJava();
//        if(!fileJavaScaffodingJava.exists()){
//            fileJavaScaffodingJava.mkdirs();
//        }
//        File xSrcTargetJavaScaffodingLibs = xSrcTarget.getJavaScaffodingLibs();
//        if(!xSrcTargetJavaScaffodingLibs.exists()){
//            xSrcTargetJavaScaffodingLibs.mkdir();
//        }
        //PatchManger.copyDirFromJar(sourceCode.getJavaScaffodingLibs(),xSrcTargetJavaScaffodingLibs.getAbsolutePath());
        //PatchManger.copyDirFromJar(sourceCode.getJavaScaffodingJava(),fileJavaScaffodingJava.getAbsolutePath());
        //PatchManger.copyDirFromJar(sourceCode.ManifestjavaScaffoding(),xSrcTarget.getJavaScaffodingMain().getAbsolutePath());
        return true;
    }

    @Override
    boolean makeJavaScaffoldingLib(SourceCode sourceCode, XSrcTarget xSrcTarget) throws IOException {
        try {
            File apkPath = new File(xSrcTarget.getOriginalApkFile().getAbsolutePath());
            File basePath = xSrcTarget.getJavaScaffoding();
            String baseName = ChineseUtils.toPinyin(xSrcTarget.getOriginalApkFile().getName().replace(".apk",""), PinyinFormat.TONELESS_PINYIN_FORMAT).replace(" ","-");

//          BaseDexFileReader reader = MultiDexFileReader.open(Files.readAllBytes(apkPath.toPath()));

            TreeMap<String, DexFileReader> fileReaderTreeMap =  FakerMultiDexFileReader.open(Files.readAllBytes(apkPath.toPath()));

            BaksmaliBaseDexExceptionHandler handler = false ? null : new BaksmaliBaseDexExceptionHandler();
            Iterator it = fileReaderTreeMap.entrySet().iterator();
            File outPath = new File(basePath,"classes.all.dex.jar");
            if(outPath.exists()){
                outPath.delete();
            }
            while (it.hasNext()){
                Map.Entry<String, DexFileReader>  entry = (Map.Entry) it.next();

                DexFileReader dexFileReader = entry.getValue();
                MDex2jar.from(dexFileReader).withExceptionHandler(handler).reUseReg(false).topoLogicalSort()
                        .skipDebug(false).optimizeSynchronized(false).printIR(false)
                        .noCode(false).skipExceptions(false).to(outPath.toPath());
            }
            Logger.sendLog(iLogCat,"java scaffolding lib jar "+outPath.getName()+" has been generated success...");

        }catch (Exception e){
            Logger.sendLog(iLogCat,"javascaffoding lib genertion failed");
        }

        return true;
    }

    @Override
    boolean mergeFaker(SourceCode sourceCode, XSrcTarget xSrcTarget) throws IOException {
        File gameBuildGrandle = xSrcTarget.getGameBuild();
        String abiStr = "";
        File targetjniLibs = xSrcTarget.getjniLibs();
        File jniLibsARMV7A = new File(targetjniLibs,"armeabi-v7a");
        File armeabi = new File(targetjniLibs,"armeabi");
        if(armeabi.exists()&&!jniLibsARMV7A.exists()){
            armeabi.renameTo(jniLibsARMV7A);
        }
        if(jniLibsARMV7A.exists()){
            //PatchManger.copyDirFromJar(sourceCode.getJniLibs()+"/armeabi-v7a",jniLibsARMV7A.getAbsolutePath());
            if(TextUtil.isEmpty(abiStr)){
                abiStr = "'armeabi-v7a'";
            }else {
                abiStr = abiStr+",'armeabi-v7a'";
            }
        }

        File jniLibsARM64V8A = new File(targetjniLibs,"arm64-v8a");
        if(jniLibsARM64V8A.exists()){
            //PatchManger.copyDirFromJar(sourceCode.getJniLibs()+"/arm64-v8a",jniLibsARM64V8A.getAbsolutePath());
            if(TextUtil.isEmpty(abiStr)){
                abiStr = "'arm64-v8a'";
            }else {
                abiStr = abiStr+",'arm64-v8a'";
            }
        }

        if(!jniLibsARMV7A.exists()&&!jniLibsARM64V8A.exists()&&!armeabi.exists()){
                abiStr = "'armeabi-v7a','arm64-v8a'";
        }
        FileUtil.autoReplaceStr(gameBuildGrandle,"{abi}",abiStr);
        return true;
    }

    @Override
    boolean modManifest(SourceCode sourceCode, XSrcTarget xSrcTarget) throws IOException {
        try {
            ManifestEditor manifestEditor = new ManifestEditor(xSrcTarget.getManifestFile());
            String applicationName = manifestEditor.getApplicationName();
            File file  = new File(xSrcTarget.getJava(),"com/faker/android/FakerApp.java");
            if(!TextUtil.isEmpty(applicationName)){
                FileUtil.autoReplaceStr(file,"{APPLICATION_NAME}",applicationName);
            }else {
                FileUtil.autoReplaceStr(file,"{APPLICATION_NAME}","Application");
            }
            manifestEditor.modApplication("com.faker.android.FakerApp");//
            manifestEditor.save();

        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    boolean fixRes(SourceCode sourceCode, XSrcTarget xSrcTarget) throws IOException {
        File fileRes = xSrcTarget.getResDir();
        func(fileRes,fileRes);
        return true;
    }
    private void func(File res, File file){
        File[] fs = file.listFiles();
        for(File f:fs){
            if(f.isDirectory())	//
                func(res,f);
            if(f.isFile()){
                if(f.getName().startsWith("$")){
                    Logger.sendLog(iLogCat,"fix res name"+f.getAbsolutePath());
                    File fixName = new File(f.getParent(),f.getName().replace("$",""));
                    funcRe(res,getFileNameNoEx(f.getName()),getFileNameNoEx(fixName.getName()));
                    f.renameTo(fixName);
                }
            }
        }
    }
    /*
     * */
    public static String getFileNameNoEx(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot >-1) && (dot < (filename.length()))) {
                return filename.substring(0, dot);
            }
        }
        return filename;
    }
    private static void funcRe(File file, String oStr, String nStr){
        File[] fs = file.listFiles();
        for(File f:fs){
            if(f.isDirectory())	//
                funcRe(f,oStr,nStr);
            if(f.isFile()){
                try {
                    if(f.getName().endsWith(".xml")){
                        FileUtil.autoReplaceStr(f,oStr,nStr);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void formatScaffolding(File scaffolding) throws IOException {
       // PatchManger.copyDirFromJar("/patch","C:\\patch");
        if(scaffolding.exists()&&scaffolding.isDirectory()){
            File scaffolding_ARM_spam[] =scaffolding.listFiles();

            for (File f:scaffolding_ARM_spam ) {//删除无用文件
                if(!f.isDirectory()){
                    f.delete();
                }
            }
            File userDir = new File(scaffolding,"user");
            delete(userDir);

            File frameworkDir = new File(scaffolding,"framework");
            delete(frameworkDir);
            File appdataDir = new File(scaffolding,"appdata");
            File appdataFils[] = appdataDir.listFiles();
            for (File f:appdataFils) {
                f.renameTo(new File(scaffolding,f.getName()));
            }
            delete(appdataDir);

            PatchManger.copyDirFromJar(sourceCode.getScaffolding_cpp(),scaffolding.getAbsolutePath());

        }
    }

    public void deleteR(String pkg, File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                deleteR(pkg,f);
            }
        }
        String path = pkg.replace(".","/");
        String filePath = file.getAbsolutePath();
        if(filePath.contains(path)&&(file.getName().startsWith("R$")||file.getName().equals("R.smali"))){
            file.delete();
        }
    }

    public void delete(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                delete(f);
             }
        }
        file.delete();
    }

    public void exportCppScaffolding (XSrcTarget xSrcTarget) {

        File fileScaffoldingHelper =  new File(xSrcTarget.getCpp(),"ScaffoldingHelper");
        fileScaffoldingHelper.mkdir();
        File il2cppBinary = null;
        try {
            il2cppBinary = Il2cppManager.getIl2cppBinary();
        } catch (BrutException e) {
            e.printStackTrace();
        }
        if(il2cppBinary==null){
            return;
        }
        String cmd = il2cppBinary.getAbsolutePath()+" -i "+xSrcTarget.getOriginalApkFile().getAbsolutePath()+ " -h "
                +new File(xSrcTarget.getCpp(),"Il2cpp-Scaffolding").getAbsolutePath()+" -e none -c "+new File(fileScaffoldingHelper,"help.cs").getAbsolutePath()
                +" -p "+new File(fileScaffoldingHelper,"help.py").getAbsolutePath() +" -o" +new File(fileScaffoldingHelper,"help.json").getAbsolutePath();
        BufferedReader br = null;
        BufferedReader brError = null;
        try {
            //执行exe  cmd
            Process p = Runtime.getRuntime().exec(cmd);
            String line = null;
            br = new BufferedReader(new InputStreamReader(p.getInputStream(),"utf-8"));
            brError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((line = br.readLine()) != null  || (line = brError.readLine()) != null) {
                Logger.sendLog(iLogCat,line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
