package cyanogenmod.externalviews;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

public abstract class ExternalViewProvider {

    private final Context mContext;
    private final WindowManager mWindowManager;
    private final Handler mHandler = new Handler();
    private final Provider mProvider = new Provider();

    private final View mRootView;
    private final WindowManager.LayoutParams mParams;

    private boolean mShouldShow = true;
    private boolean mAskedShow = false;

    public ExternalViewProvider(Context context) {
        mContext = context;

        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mRootView = onCreateView();

        mParams = new WindowManager.LayoutParams();
        mParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager
                .LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams
                .FLAG_HARDWARE_ACCELERATED;
        mParams.gravity = Gravity.LEFT | Gravity.TOP;
        mParams.format = PixelFormat.OPAQUE;
        mParams.preferredRefreshRate = 63;
    }

    private final class Provider extends IExternalViewProvider.Stub {
        @Override
        public void onAttach(IBinder windowToken) throws RemoteException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mWindowManager.addView(mRootView, mParams);

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
                    mShouldShow = true;

                    updateVisibility();

                    ExternalViewProvider.this.onResume();
                }
            });
        }

        @Override
        public void onPause() throws RemoteException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mShouldShow = false;

                    updateVisibility();

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
                    mWindowManager.removeView(mRootView);

                    ExternalViewProvider.this.onDetach();
                }
            });
        }

        @Override
        public void alterWindow(final int x, final int y, final int width, final int height, final boolean visible) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateLayoutParams(x, y, width, mParams);

                    mAskedShow = visible;

                    updateVisibility();

                    if (mRootView.getVisibility() != View.GONE)
                        mWindowManager.updateViewLayout(mRootView, mParams);
                }
            });
        }

        private void updateLayoutParams(int x, int y, int width, WindowManager.LayoutParams layoutParams) {
            layoutParams.x = x + 50;
            layoutParams.y = y + 50;
            layoutParams.width = width - 100;
            layoutParams.height = 1500;
        }

        private void updateVisibility() {
            mRootView.setVisibility(mShouldShow && mAskedShow ? View.VISIBLE : View.GONE);
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
