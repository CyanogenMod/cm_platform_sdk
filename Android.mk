# Copyright (C) 2015 The CyanogenMod Project
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

LOCAL_PATH := $(call my-dir)

# The CyanogenMod Platform Framework Library
# ============================================================
include $(CLEAR_VARS)

cyanogenmod_app_src := src/java/
library_src := cm/lib/main/java

LOCAL_MODULE := org.cyanogenmod.platform
LOCAL_MODULE_TAGS := optional
LOCAL_JAVA_LIBRARIES := services
LOCAL_REQUIRED_MODULES := services

LOCAL_SRC_FILES := \
           $(call all-java-files-under, $(cyanogenmod_app_src)) \
           $(call all-java-files-under, $(library_src))

## READ ME: ########################################################
##
## When updating this list of aidl files, consider if that aidl is
## part of the SDK API.  If it is, also add it to the list below that
## is preprocessed and distributed with the SDK.  This list should
## not contain any aidl files for parcelables, but the one below should
## if you intend for 3rd parties to be able to send those objects
## across process boundaries.
##
## READ ME: ########################################################
LOCAL_SRC_FILES += \
           $(call all-Iaidl-files-under, $(cyanogemod_app_src))

# Include aidl files from cyanogenmod.app namespace as well as internal src aidl files
LOCAL_AIDL_INCLUDES := $(LOCAL_PATH)/src/java

include $(BUILD_JAVA_LIBRARY)
framework_module := $(LOCAL_INSTALLED_MODULE)

cm_framework_built := $(call java-lib-deps, org.cyanogenmod.platform)

# ====  org.cyanogenmod.platform.xml lib def  ========================
include $(CLEAR_VARS)

LOCAL_MODULE := org.cyanogenmod.platform.xml
LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_CLASS := ETC

# This will install the file in /system/etc/permissions
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/permissions

LOCAL_SRC_FILES := $(LOCAL_MODULE)

include $(BUILD_PREBUILT)

# the sdk
# ============================================================
include $(CLEAR_VARS)

LOCAL_MODULE:= org.cyanogenmod.platform.sdk
LOCAL_MODULE_TAGS := optional
LOCAL_REQUIRED_MODULES := services

LOCAL_SRC_FILES := \
           $(call all-java-files-under, $(cyanogenmod_app_src)) \
           $(call all-Iaidl-files-under, $(cyanogenmod_app_src))

# Included aidl files from cyanogenmod.app namespace
LOCAL_AIDL_INCLUDES := $(LOCAL_PATH)/src/java

$(full_target): $(cm_framework_built) $(gen)
include $(BUILD_STATIC_JAVA_LIBRARY)

# ===========================================================
# Common Droiddoc vars
cmplat.docs.src_files := \
    $(call all-java-files-under, $(cyanogenmod_app_src)) \
    $(call all-html-files-under, $(cyanogenmod_app_src))
cmplat.docs.java_libraries := \
    org.cyanogenmod.platform.sdk

# Documentation
# ===========================================================
include $(CLEAR_VARS)

LOCAL_MODULE := org.cyanogenmod.platform.sdk
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE_TAGS := optional

intermediates.COMMON := $(call intermediates-dir-for,$(LOCAL_MODULE_CLASS), org.cyanogenmod.platform.sdk,,COMMON)

LOCAL_SRC_FILES := $(cmplat.docs.src_files)
LOCAL_ADDITONAL_JAVA_DIR := $(intermediates.COMMON)/src

LOCAL_SDK_VERSION := 21
LOCAL_IS_HOST_MODULE := false
LOCAL_DROIDDOC_CUSTOM_TEMPLATE_DIR := build/tools/droiddoc/templates-sdk
LOCAL_ADDITIONAL_DEPENDENCIES := \
        services

LOCAL_JAVA_LIBRARIES := $(cmplat.docs.java_libraries)

LOCAL_DROIDDOC_OPTIONS := \
    -offlinemode \
    -hdf android.whichdoc offline \
    -federate Android http://developer.android.com \
    -federationapi Android prebuilts/sdk/api/21.txt

$(full_target): $(cm_framework_built) $(gen)
include $(BUILD_DROIDDOC)

# Cleanup temp vars
# ===========================================================
cmplat.docs.src_files :=
cmplat.docs.java_libraries :=
intermediates.COMMON :=
