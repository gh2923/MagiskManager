package com.topjohnwu.magisk.asyncs;

import android.content.Context;
import android.os.Build;

import com.topjohnwu.magisk.utils.Utils;
import com.topjohnwu.magisk.utils.WebService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class DownloadBusybox extends ParallelTask<Void, Void, Void> {

    private static final String BUSYBOX_ARM = "https://github.com/topjohnwu/ndk-busybox/releases/download/1.27.1/busybox-arm";
    private static final String BUSYBOX_X86 = "https://github.com/topjohnwu/ndk-busybox/releases/download/1.27.1/busybox-x86";
    private static final String BUSYBOXPATH = "/dev/magisk/bin";

    private File busybox;

    public DownloadBusybox(Context context) {
        super(context);
        busybox = new File(context.getCacheDir(), "busybox");
    }

    @Override
    protected void onPreExecute() {
        getShell().su_raw("export PATH=" + BUSYBOXPATH + ":$PATH");
    }

    @Override
    protected Void doInBackground(Void... voids) {
        Context context = getMagiskManager();
        if (!Utils.itemExist(getShell(), BUSYBOXPATH + "/busybox")) {
            if (!busybox.exists() && Utils.checkNetworkStatus(context)) {
                Utils.removeItem(getShell(), context.getApplicationInfo().dataDir + "/busybox");
                try {
                    FileOutputStream out  = new FileOutputStream(busybox);
                    InputStream in = WebService.request(WebService.GET,
                            Build.SUPPORTED_32_BIT_ABIS[0].contains("x86") ?
                                    BUSYBOX_X86 :
                                    BUSYBOX_ARM,
                            null
                    );
                    if (in == null) throw new IOException();
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.close();
                    in.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (busybox.exists()) {
                getShell().su_raw(
                        "rm -rf " + BUSYBOXPATH,
                        "mkdir -p " + BUSYBOXPATH,
                        "cp " + busybox + " " + BUSYBOXPATH,
                        "chmod -R 755 " + BUSYBOXPATH,
                        BUSYBOXPATH + "/busybox --install -s " + BUSYBOXPATH
                );
            }
        }
        return null;
    }
}
