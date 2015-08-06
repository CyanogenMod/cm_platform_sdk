LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_STATIC_JAVA_LIBRARIES := \
    org.cyanogenmod.platform.sdk

LOCAL_SRC_FILES := $(call all-java-files-under, src/)

LOCAL_PACKAGE_NAME := CMProfiles

include $(BUILD_PACKAGE)