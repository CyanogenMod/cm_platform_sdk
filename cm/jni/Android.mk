LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    src/org_cyanogenmod_platform_internal_CMAudioService.cpp \
    src/onload.cpp

LOCAL_C_INCLUDES += \
    $(JNI_H_INCLUDE) \
    $(TOP)/frameworks/base/core/jni \
    $(TOP)/frameworks/av/include

LOCAL_SHARED_LIBRARIES := \
    libandroid_runtime \
    libmedia \
    liblog \
    libcutils \
    libutils \

LOCAL_MODULE := libcmsdk_platform_jni
LOCAL_MODULE_TAGS := optional
LOCAL_CFLAGS += -Wall -Werror -Wno-unused-parameter

include $(BUILD_SHARED_LIBRARY)

