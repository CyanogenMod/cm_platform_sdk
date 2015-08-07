package cyanogenmod.externalviews;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import com.android.internal.policy.PolicyManager;

public abstract class ExternalViewProvider {

    public static final String TAG = "ExternalViewProvider";
    private final Context mContext;
    private final WindowManager mWindowManager;
    private final Handler mHandler = new Handler();
    private final Provider mProvider = new Provider();

    private final Window mWindow;
    private final WindowManager.LayoutParams mParams;

    public ExternalViewProvider(Context context) {
        mContext = context;

        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mWindow = PolicyManager.makeNewWindow(context);
        ((ViewGroup)mWindow.getDecorView()).addView(onCreateView());

        mParams = new WindowManager.LayoutParams();
        mParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        mParams.gravity = Gravity.LEFT | Gravity.TOP;
        mParams.format = PixelFormat.OPAQUE;
    }

    private final class Provider extends IExternalViewProvider.Stub {
        @Override
        public void onAttach(final IBinder remoteWindowToken) throws RemoteException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mParams.token = remoteWindowToken;
                    mWindowManager.addView(mWindow.getDecorView(), mParams);

                    ExternalViewProvider.this.onAttach();
                }
            });
        }

        @Override
        public void onStart() throws RemoteException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ExternalViewProvider.this.onStart();
                }
            });
        }

        @Override
        public void onResume() throws RemoteException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ExternalViewProvider.this.onResume();
                }
            });
        }

        @Override
        public void onPause() throws RemoteException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ExternalViewProvider.this.onPause();
                }
            });
        }

        @Override
        public void onStop() throws RemoteException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ExternalViewProvider.this.onStop();
                }
            });
        }

        @Override
        public void onDetach() throws RemoteException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mWindowManager.removeView(mWindow.getDecorView());

                    Log.d(TAG, "Detached, removed view");

                    ExternalViewProvider.this.onDetach();
                }
            });
        }
    }

    public IBinder getBinder() {
        return mProvider;
    }

    protected void onAttach() { }
    protected abstract View onCreateView();
    protected void onStart() { }
    protected void onResume() { }
    protected void onPause() { }
    protected void onStop() { }
    protected void onDetach() { }
}
