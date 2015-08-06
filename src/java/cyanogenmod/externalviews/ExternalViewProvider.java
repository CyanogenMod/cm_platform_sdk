package cyanogenmod.externalviews;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Gravity;
import android.view.WindowManager;

public abstract class ExternalViewProvider {

    private final Context mContext;
    private final WindowManager mWindowManager;

    public ExternalViewProvider(Context context) {
        mContext = context;

        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
    }

    private final class Provider extends IExternalViewProvider.Stub {
        @Override
        public void onAttach(IBinder windowToken) throws RemoteException {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
            layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
            layoutParams.format = PixelFormat.OPAQUE;
            layoutParams.preferredRefreshRate = 63;

            ExternalViewProvider.this.onAttach();
        }

        @Override
        public void onCreateView() throws RemoteException {
            ExternalViewProvider.this.onCreateView();
        }

        @Override
        public void onStart() throws RemoteException {
            ExternalViewProvider.this.onStart();
        }

        @Override
        public void onResume() throws RemoteException {
            ExternalViewProvider.this.onResume();
        }

        @Override
        public void onPause() throws RemoteException {
            ExternalViewProvider.this.onPause();
        }

        @Override
        public void onStop() throws RemoteException {
            ExternalViewProvider.this.onStop();
        }

        @Override
        public void onDestroyView() throws RemoteException {
            ExternalViewProvider.this.onDestroyView();
        }

        @Override
        public void onDetach() throws RemoteException {
            ExternalViewProvider.this.onDetach();
        }
    }

    protected void onAttach() { }
    protected void onCreateView() { }
    protected void onStart() { }
    protected void onResume() { }
    protected void onPause() { }
    protected void onStop() { }
    protected void onDestroyView() { }
    protected void onDetach() { }
}
