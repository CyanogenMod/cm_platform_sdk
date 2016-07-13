package cyanogenmod.voice;

import android.annotation.Nullable;
import android.app.VoiceInteractor;
import android.app.VoiceInteractor.Prompt;
import android.os.Bundle;
import android.os.RemoteException;

import com.android.internal.app.IVoiceInteractor;
import com.android.internal.app.IVoiceInteractorCallback;
import com.android.internal.app.IVoiceInteractorRequest;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * A voice interaction request that can receive the basic String that the user spoke
 * as part of an interaction.
 */
public class FreeFormVoiceRequest extends VoiceInteractor.Request {
    final VoiceInteractor.Prompt mPrompt;
    final Bundle mExtras;

    /**
     * Create a new completed voice interaction request.
     * @param prompt Optional message to speak to the user about the completion status of
     *     the task or null if nothing should be spoken.
     * @param extras Additional optional information or null.
     */
    public FreeFormVoiceRequest(@Nullable Prompt prompt, @Nullable Bundle extras) {
        mPrompt = prompt;
        mExtras = extras;
    }

    /**
     * Create a new completed voice interaction request.
     * @param message Optional message to speak to the user about the completion status of
     *     the task or null if nothing should be spoken.
     * @param extras Additional optional information or null.
     * @hide
     */
    public FreeFormVoiceRequest(CharSequence message, Bundle extras) {
        mPrompt = (message != null ? new Prompt(message) : null);
        mExtras = extras;
    }

    /**
     * Called with the text that was heard by the voice interactor before
     * a timeout or pause in speech occurred.
     * @param result The text String as understood by the VoiceInteractionService
     */
    public void onCompleteResult(String result) {
    }

    void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        writer.print(prefix); writer.print("mPrompt="); writer.println(mPrompt);
        if (mExtras != null) {
            writer.print(prefix); writer.print("mExtras="); writer.println(mExtras);
        }
    }

    String getRequestTypeName() {
        return "FreeFormVoice";
    }

    public IVoiceInteractorRequest submit(IVoiceInteractor interactor, String packageName,
                                   IVoiceInteractorCallback callback) throws RemoteException {
        return interactor.startFreeFormVoice(packageName, callback, mPrompt, mExtras);
    }
}
