LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_MODULE:= migration-interface

include $(BUILD_HOST_JAVA_LIBRARY)
