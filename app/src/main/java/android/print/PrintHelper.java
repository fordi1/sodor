package android.print;

import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import java.io.File;

public class PrintHelper {

    public interface PrintCallback {
        void onComplete(boolean success);
    }

    public static void callPrintAdapter(
        final PrintDocumentAdapter adapter,
        final PrintAttributes attributes,
        final File outputFile,
        final PrintCallback callback
    ) {
        final ParcelFileDescriptor pfd;
        try {
            pfd = ParcelFileDescriptor.open(
                outputFile,
                ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE
            );
        } catch (Exception e) {
            e.printStackTrace();
            if (callback != null) {
                callback.onComplete(false);
            }
            return;
        }

        final PrintDocumentAdapter.LayoutResultCallback layoutCallback = new PrintDocumentAdapter.LayoutResultCallback() {
            @Override
            public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
                PrintDocumentAdapter.WriteResultCallback writeCallback = new PrintDocumentAdapter.WriteResultCallback() {
                    @Override
                    public void onWriteFinished(PageRange[] pages) {
                        try {
                            pfd.close();
                        } catch (Exception ignored) {}
                        if (callback != null) {
                            callback.onComplete(true);
                        }
                    }

                    @Override
                    public void onWriteFailed(CharSequence error) {
                        try {
                            pfd.close();
                        } catch (Exception ignored) {}
                        if (callback != null) {
                            callback.onComplete(false);
                        }
                    }

                    @Override
                    public void onWriteCancelled() {
                        try {
                            pfd.close();
                        } catch (Exception ignored) {}
                        if (callback != null) {
                            callback.onComplete(false);
                        }
                    }
                };

                try {
                    adapter.onWrite(new PageRange[]{PageRange.ALL_PAGES}, pfd, new CancellationSignal(), writeCallback);
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        pfd.close();
                    } catch (Exception ignored) {}
                    if (callback != null) {
                        callback.onComplete(false);
                    }
                }
            }

            @Override
            public void onLayoutFailed(CharSequence error) {
                try {
                    pfd.close();
                } catch (Exception ignored) {}
                if (callback != null) {
                    callback.onComplete(false);
                }
            }

            @Override
            public void onLayoutCancelled() {
                try {
                    pfd.close();
                } catch (Exception ignored) {}
                if (callback != null) {
                    callback.onComplete(false);
                }
            }
        };

        try {
            adapter.onLayout(null, attributes, new CancellationSignal(), layoutCallback, null);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                pfd.close();
            } catch (Exception ignored) {}
            if (callback != null) {
                callback.onComplete(false);
            }
        }
    }
}
