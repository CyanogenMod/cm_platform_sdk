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
                    mWindowManager.addView(mRootView, mParams);

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
                    ExternalViewProvider.this.onDetach();
                }
            });
        }

        @Override
        public void alterWindow(final int x, final int y, final int width, final int height, final boolean visible) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (updateLayoutParams(x, y, width, mParams))
                        return;

                    if (mRootView.getVisibility() != View.VISIBLE && !visible) {
                        return;
                    }

                    mRootView.setVisibility(visible ? View.VISIBLE : View.GONE);

//                        if (mRootView instanceof TextureVideoView) {
//                            TextureVideoView videoView = ((TextureVideoView) view);
//                            if (view.getVisibility() == View.GONE && videoView.isPlaying()) {
//                                ((TextureVideoView) view).pause();
//                                mVideoPosition = videoView.getCurrentPosition();
//                            } else if (!videoView.isPlaying()){
//                                videoView.seekTo(mVideoPosition);
//                                ((TextureVideoView) view).start();
//                            }
//                        }

                    if (mRootView.getVisibility() != View.GONE)
                        mWindowManager.updateViewLayout(mRootView, mParams);
                }
            });
        }

        private boolean updateLayoutParams(int x, int y, int width, WindowManager.LayoutParams layoutParams) {
            if (layoutParams.x == x + 50 && layoutParams.y == y + 50 && layoutParams.width == width - 100 && layoutParams.height == 1500) {

                return true;
            }
            layoutParams.x = x + 50;
            layoutParams.y = y + 50;
            layoutParams.width = width - 100;
            layoutParams.height = 1500;
            return false;
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
