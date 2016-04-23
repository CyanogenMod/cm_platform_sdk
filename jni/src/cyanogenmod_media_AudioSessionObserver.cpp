/*
**
** Copyright 2016, The CyanogenMod Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

//#define LOG_NDEBUG 0

#define LOG_TAG "AudioSessionObserver-JNI"

#include <utils/Log.h>

#include <jni.h>
#include <JNIHelp.h>
#include "core_jni_helpers.h"
#include "android_media_AudioErrors.h"

#include <media/AudioSystem.h>
#include <media/AudioSession.h>

#include <system/audio.h>

// ----------------------------------------------------------------------------

using namespace android;

static const char* const kClassPathName = "cyanogenmod/media/AudioSessionObserver";

static jclass gArrayListClass;
static struct {
    jmethodID    add;
    jmethodID    toArray;
} gArrayListMethods;

static struct {
    jmethodID postAudioSessionEventFromNative;
} gAudioSessionEventHandlerMethods;

static jclass gAudioSessionInfoClass;
static jmethodID gAudioSessionInfoCstor;

// ----------------------------------------------------------------------------

static void
cyanogenmod_media_AudioSessionObserver_session_info_callback(int event,
		sp<AudioSessionInfo>& info, bool added)
{
    JNIEnv *env = AndroidRuntime::getJNIEnv();
    if (env == NULL) {
        return;
    }

    jclass clazz = env->FindClass(kClassPathName);
    jobject jSession = env->NewObject(gAudioSessionInfoClass, gAudioSessionInfoCstor,
            info->mSessionId, info->mStream, info->mFlags, info->mChannelMask, info->mUid);

    env->CallStaticVoidMethod(clazz,
			gAudioSessionEventHandlerMethods.postAudioSessionEventFromNative,
            event, jSession, added);

    env->DeleteLocalRef(jSession);
    env->DeleteLocalRef(clazz);
}

static void
cyanogenmod_media_AudioSessionObserver_registerAudioSessionCallback(
		JNIEnv *env, jobject thiz, jboolean enabled)
{
    AudioSystem::setAudioSessionCallback( enabled ?
			cyanogenmod_media_AudioSessionObserver_session_info_callback : NULL);
}

static jint
cyanogenmod_media_AudioSessionObserver_listAudioSessions(JNIEnv *env, jobject clazz,
		jint streams, jobject jSessions)
{
    ALOGV("listAudioSessions");

    if (jSessions == NULL) {
        ALOGE("listAudioSessions NULL AudioSessionObserver ArrayList");
        return (jint)AUDIO_JAVA_BAD_VALUE;
    }
    if (!env->IsInstanceOf(jSessions, gArrayListClass)) {
        ALOGE("listAudioSessions not an arraylist");
        return (jint)AUDIO_JAVA_BAD_VALUE;
    }
	
    status_t status;
	Vector< sp<AudioSessionInfo>> sessions;
	
    status = AudioSystem::listAudioSessions((audio_stream_type_t)streams, sessions);
    if (status != NO_ERROR) {
        ALOGE("AudioSystem::listAudioSessions error %d", status);
    }

    jint jStatus = nativeToJavaStatus(status);
    if (jStatus != AUDIO_JAVA_SUCCESS) {
        goto exit;
    }

	for (size_t i = 0; i < sessions.size(); i++) {
        sp<AudioSessionInfo> s = sessions.itemAt(i);

        jobject jSession = env->NewObject(gAudioSessionInfoClass, gAudioSessionInfoCstor,
                s->mSessionId, s->mStream, s->mFlags, s->mChannelMask, s->mUid);

        if (jSession == NULL) {
            jStatus = (jint)AUDIO_JAVA_ERROR;
            goto exit;
        }

        env->CallBooleanMethod(jSessions, gArrayListMethods.add, jSession);
        env->DeleteLocalRef(jSession);
        jSession = NULL;
    }

exit:
    return jStatus;
}


// ----------------------------------------------------------------------------

static JNINativeMethod gMethods[] = {
     {"listAudioSessions", "(ILjava/util/ArrayList;)I",
            (void *)cyanogenmod_media_AudioSessionObserver_listAudioSessions},
     {"native_register_audio_session_callback", "(Z)V",
            (void *)cyanogenmod_media_AudioSessionObserver_registerAudioSessionCallback},
};

int register_cyanogenmod_media_AudioSessionObserver(JNIEnv *env)
{
    jclass arrayListClass = FindClassOrDie(env, "java/util/ArrayList");
    gArrayListClass = MakeGlobalRefOrDie(env, arrayListClass);
    gArrayListMethods.add = GetMethodIDOrDie(env, arrayListClass, "add", "(Ljava/lang/Object;)Z");
    gArrayListMethods.toArray = GetMethodIDOrDie(env, arrayListClass, "toArray", "()[Ljava/lang/Object;");

    jclass audioSessionInfoClass = FindClassOrDie(env, "cyanogenmod/media/AudioSessionInfo");
    gAudioSessionInfoClass = MakeGlobalRefOrDie(env, audioSessionInfoClass);
    gAudioSessionInfoCstor = GetMethodIDOrDie(env, audioSessionInfoClass, "<init>",
"(IIIII)Z");

    gAudioSessionEventHandlerMethods.postAudioSessionEventFromNative =
            GetStaticMethodIDOrDie(env, env->FindClass(kClassPathName),
            "audioSessionCallbackFromNative", "(ILcyanogenmod/media/AudioSessionInfo;Z)V");

    return RegisterMethodsOrDie(env, kClassPathName, gMethods, NELEM(gMethods));
}
