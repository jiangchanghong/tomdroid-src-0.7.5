package org.changhong.sync.baidu;

import android.util.Log;
import com.baidu.frontia.Frontia;
import com.baidu.frontia.FrontiaFile;
import com.baidu.frontia.api.FrontiaStorage;
import com.baidu.frontia.api.FrontiaStorageListener;
import org.changhong.ui.Tomdroid;
import org.changhong.util.TLog;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by changhong on 14-1-13.
 */
public class FileUtil {
    public static String tag = "AAAAAAAAAA";
    public static int failcount = 3;
    public static FrontiaFile mfile;

    public static int synPercent = 100;

    static {
        mfile = new FrontiaFile();
    }


    public static void ll(String info) {
        Log.i(tag, info);
    }

    public static FrontiaStorage storage = Frontia.getStorage();
    public static ArrayList<String> remotefilestrin = new ArrayList<String>();
    public static ArrayList<String> localfilestring = new ArrayList<String>();

    private static void setFile(String name) {
        try {
            mfile.setRemotePath("/path" + Frontia.getCurrentAccount().getId() + "/" + name);
            if (Tomdroid.NOTES_PATH.endsWith("/")) {
                mfile.setNativePath(Tomdroid.NOTES_PATH + name);
            } else {

                mfile.setNativePath(Tomdroid.NOTES_PATH + "/" + name);
            }
        } catch (Exception e) {
            e.printStackTrace();
            ll(e.toString());

//            Toast.makeText(Tomdroid.context, "没有登陆", Toast.LENGTH_SHORT).show();
        }

    }

    private static void push() {

        for (String file : FileTime.getPushable()) {
            setFile(file);
            storage.uploadFile(mfile, fileProgressListener, fileTransferListener);

//            ll("pushed" + mfile.getNativePath());
        }

    }

    private static void pull() {

        for (String file : FileTime.getPullable()) {

            setFile(file);
            ll(mfile.getNativePath() + "dellete");
            storage.downloadFile(mfile, fileProgressListener, fileTransferListenerDownload);
        }


    }

    private static void hebin() {
        String remo;
        String loca;
        for (Map<String, Object> map : FileTime.getReplacelocal()) {
            loca = (String) map.get(FileTime.LOCAL);
            remo = (String) map.get(FileTime.REMOT);
            delete(new File(Tomdroid.NOTES_PATH, loca));
            ll("delete local:" + loca);
            setFile(remo);
            storage.downloadFile(mfile, fileProgressListener, fileTransferListenerDownload);


        }
        for (Map<String, Object> map : FileTime.getReplaceremot()) {
            loca = (String) map.get(FileTime.LOCAL);
            remo = (String) map.get(FileTime.REMOT);

            final String finalLoca = loca;
            setFile(remo);
            final String finalLoca1 = loca;
            storage.deleteFile(mfile, new FrontiaStorageListener.FileOperationListener() {
                @Override
                public void onSuccess(String s) {
                    setFile(finalLoca1);
                    storage.uploadFile(mfile, fileProgressListener, fileTransferListener);
                    ll("删除文件成功:" + s);

                }

                @Override
                public void onFailure(String s, int i, String s2) {
                    ll("删除文件失败:" + s);

                   fail();
                }
            });

        }


    }

    private static void delete(File loca) {
        if (loca.exists()) {
            loca.delete();
        }
    }


    private static void getlocallist() {
        if (Tomdroid.NOTES_PATH.endsWith("/")) {
            Tomdroid.NOTES_PATH = Tomdroid.NOTES_PATH.substring(0, Tomdroid.NOTES_PATH.length() - 1);

        }
        localfilestring.clear();
        File file = new File(Tomdroid.NOTES_PATH);

        for (String a : file.list(new NotesFilter())) {

            localfilestring.add(a);

        }

    }

    static private class NotesFilter implements FilenameFilter {
        @Override
        public boolean accept(File pathname, String name) {
            return (name.endsWith(".note"));
        }

    }

    public static void sync() {
        File path = new File(Tomdroid.NOTES_PATH);
        if (!path.exists())
            path.mkdir();
        TLog.i("aaaaaaa", "Path {0} exists: {1}", path, path.exists());
        // Check a second time, if not the most likely cause is the volume doesn't exist
        if (!path.exists()) {
            TLog.w("aaaaaa", "Couldn't create {0}", path);
        }

        if (synPercent != 100) {
            return;
        }
//        Runnable runnable = new Runnable() {
//            @Override
//            public void run() {
        ll("in sync");
        getlocallist();
        synPercent = 20;
        remotefilestrin.clear();
        storage.listFiles(new FrontiaStorageListener.FileListListener() {
            @Override
            public void onSuccess(List<FrontiaFile> frontiaFiles) {
                for (FrontiaFile a : frontiaFiles) {
                    remotefilestrin.add(pathtoname(a.getRemotePath()));
                }
                ll("locallist :" + localfilestring.size());
                ll("relist:" + remotefilestrin.size());
                FileTime.init();
                dosync();
                ll("end sysc ");

            }

            @Override
            public void onFailure(int i, String s) {
                ll("列表失败:" + s);
                fail();
                synPercent = 100;
            }
        });
//            }
//        };

//        Pool.exe(runnable);

    }

    private static void dosync() {

        pull();
        push();
        synPercent = 80;
        hebin();
        synPercent = 100;
//        Pool.exe(push);
//        Pool.exe(pull);
//        Pool.exe(hebin);


    }

    private static Runnable push = new Runnable() {
        @Override
        public void run() {
            push();
        }
    };

    private static Runnable pull = new Runnable() {
        @Override
        public void run() {
            pull();
        }
    };

    private static Runnable hebin = new Runnable() {
        @Override
        public void run() {
            hebin();
        }
    };


    public static String pathtoname(String path) {
        int in = path.lastIndexOf("/");
        if (in == -1) {
            return path;
        }
        return path.substring(in + 1, path.length());

    }

    private static FrontiaStorageListener.FileOperationListener fileOperationListener = new FrontiaStorageListener.FileOperationListener() {
        @Override
        public void onSuccess(String s) {
            ll(s + "     文件操作成功");
        }

        @Override
        public void onFailure(String s, int i, String s2) {
            synPercent = 100;
            fail();
            ll("操作文件失败:" + s2);
        }
    };
    static FrontiaStorageListener.FileProgressListener fileProgressListener = new FrontiaStorageListener.FileProgressListener() {
        @Override
        public void onProgress(String s, long l, long l2) {

        }
    };
    static FrontiaStorageListener.FileTransferListener fileTransferListener = new FrontiaStorageListener.FileTransferListener() {
        @Override
        public void onSuccess(String s, String s2) {
            ll("上传文件成功:" + s + s2);
        }

        @Override
        public void onFailure(String s, int i, String s2) {
            synPercent = 100;
            fail();
            ll("上传文件失败:" + s + "原因" + s2);
        }
    };
    static FrontiaStorageListener.FileTransferListener fileTransferListenerDownload = new FrontiaStorageListener.FileTransferListener() {
        @Override
        public void onSuccess(String s, String s2) {
            ll("下载文件成功:" + s + s2);
        }

        @Override
        public void onFailure(String s, int i, String s2) {
            synPercent = 100;
            fail();
            ll("下载文件失败:" + s + "愿意" + s2);
        }
    };

    private static void fail() {
        if (failcount > 1) {
            failcount--;
        } else {
            failcount = 3;
        }

        ll("重新开始count:" + failcount);
        if (failcount == 0) {

        } else {
            sync();
        }
    }

}
