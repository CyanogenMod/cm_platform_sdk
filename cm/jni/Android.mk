# Copyright (C) 2016 The CyanogenMod Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    src/org_cyanogenmod_platform_internal_CMAudioService.cpp \
    src/org_cyanogenmod_platform_internal_PerformanceManagerService.cpp \
    src/onload.cpp

LOCAL_C_INCLUDES := \
    $(JNI_H_INCLUDE) \
    $(TOP)/frameworks/base/core/jni \
    $(TOP)/frameworks/av/include \
    $(TOP)/hardware/libhardware/include

LOCAL_SHARED_LIBRARIES := \
    libandroid_runtime \
    libcutils \
    libhardware \
    liblog \
    libmedia \
    libutils

LOCAL_MODULE := libcmsdk_platform_jni
LOCAL_MODULE_TAGS := optional
LOCAL_CFLAGS := -Wall -Werror -Wno-unused-parameter

include $(BUILD_SHARED_LIBRARY)

