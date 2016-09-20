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

library_src := cm/lib/main/java

# The CyanogenMod Platform Framework Library
# ============================================================
include $(CLEAR_VARS)
LOCAL_USE_AAPT2 := true
LOCAL_MODULE := org.cyanogenmod.platform
LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_ANDROID_LIBRARIES := \
    org.cyanogenmod.platform.internal

LOCAL_SHARED_ANDROID_LIBRARIES := \
    services \
    org.cyanogenmod.hardware

LOCAL_SRC_FILES := \
    $(call all-java-files-under, $(library_src))

include $(BUILD_JAVA_LIBRARY)
cm_framework_module := $(LOCAL_INSTALLED_MODULE)

# Make sure that R.java and Manifest.java are built before we build
# the source for this library.
cm_framework_res_R_stamp := \
    $(call intermediates-dir-for,APPS,org.cyanogenmod.platform-res,,COMMON)/src/R.stamp
$(full_classes_compiled_jar): $(cm_framework_res_R_stamp)
$(built_dex_intermediate): $(cm_framework_res_R_stamp)

$(cm_framework_module): | $(dir $(cm_framework_module))org.cyanogenmod.platform-res.apk

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


include $(call first-makefiles-under,$(LOCAL_PATH))

intermediates.COMMON :=
