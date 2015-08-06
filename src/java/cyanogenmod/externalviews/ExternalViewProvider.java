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

    private View mRootView;

    public ExternalViewProvider(Context context) {
        mContext = context;

        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
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
                    mWindowManager.addView(mRootView, mRootView.getLayoutParams());

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
                    if (mRootView == null) {
                        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                        layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
                        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager
                                .LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams
                                .FLAG_HARDWARE_ACCELERATED;
                        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
                        layoutParams.format = PixelFormat.OPAQUE;
                        layoutParams.preferredRefreshRate = 63;

                        mRootView = onCreateView();

                        if (updateLayoutParams(x, y, width, layoutParams))
                            return;

                        // TODO make sure we don't add it when it's supposed to be invisible
                        mWindowManager.addView(mRootView, layoutParams);
                    } else {
                        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mRootView
                                .getLayoutParams();

                        if (updateLayoutParams(x, y, width, layoutParams))
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
                            mWindowManager.updateViewLayout(mRootView, layoutParams);
                    }
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
    protected void onDestroyView() { }
    protected void onDetach() { }
}
