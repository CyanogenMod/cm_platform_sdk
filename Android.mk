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

# We have a special case here where we build the library's resources
# independently from its code, so we need to find where the resource
# class source got placed in the course of building the resources.
# Thus, the magic here.
# Also, this module cannot depend directly on the R.java file; if it
# did, the PRIVATE_* vars for R.java wouldn't be guaranteed to be correct.
# Instead, it depends on the R.stamp file, which lists the corresponding
# R.java file as a prerequisite.
cm_platform_res := APPS/org.cyanogenmod.platform-res_intermediates/src

# List of packages used in cm-api-stubs and cm-system-api-stubs
cm_stub_packages := cyanogenmod.alarmclock:cyanogenmod.app:cyanogenmod.content:cyanogenmod.externalviews:cyanogenmod.hardware:cyanogenmod.media:cyanogenmod.os:cyanogenmod.profiles:cyanogenmod.providers:cyanogenmod.platform:cyanogenmod.power:cyanogenmod.themes:cyanogenmod.util:cyanogenmod.weather:cyanogenmod.weatherservice

# The CyanogenMod Platform Framework Library
# ============================================================
include $(CLEAR_VARS)

cyanogenmod_sdk_src := sdk/src/java/cyanogenmod
cyanogenmod_sdk_internal_src := sdk/src/java/org/cyanogenmod/internal
library_src := cm/lib/main/java

LOCAL_MODULE := org.cyanogenmod.platform
LOCAL_MODULE_TAGS := optional

LOCAL_JAVA_LIBRARIES := \
    services \
    org.cyanogenmod.hardware

LOCAL_SRC_FILES := \
    $(call all-java-files-under, $(cyanogenmod_sdk_src)) \
    $(call all-java-files-under, $(cyanogenmod_sdk_internal_src)) \
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
    $(call all-Iaidl-files-under, $(cyanogenmod_sdk_src)) \
    $(call all-Iaidl-files-under, $(cyanogenmod_sdk_internal_src))

cmplat_LOCAL_INTERMEDIATE_SOURCES := \
    $(cm_platform_res)/cyanogenmod/platform/R.java \
    $(cm_platform_res)/cyanogenmod/platform/Manifest.java \
    $(cm_platform_res)/org/cyanogenmod/platform/internal/R.java

LOCAL_INTERMEDIATE_SOURCES := \
    $(cmplat_LOCAL_INTERMEDIATE_SOURCES)

# Include aidl files from cyanogenmod.app namespace as well as internal src aidl files
LOCAL_AIDL_INCLUDES := $(LOCAL_PATH)/sdk/src/java

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

# the sdk
# ============================================================
include $(CLEAR_VARS)

LOCAL_MODULE:= org.cyanogenmod.platform.sdk
LOCAL_MODULE_TAGS := optional
LOCAL_REQUIRED_MODULES := services

LOCAL_SRC_FILES := \
    $(call all-java-files-under, $(cyanogenmod_sdk_src)) \
    $(call all-Iaidl-files-under, $(cyanogenmod_sdk_src)) \
    $(call all-Iaidl-files-under, $(cyanogenmod_sdk_internal_src))

# Included aidl files from cyanogenmod.app namespace
LOCAL_AIDL_INCLUDES := $(LOCAL_PATH)/sdk/src/java

cmsdk_LOCAL_INTERMEDIATE_SOURCES := \
    $(cm_platform_res)/cyanogenmod/platform/R.java \
    $(cm_platform_res)/cyanogenmod/platform/Manifest.java

LOCAL_INTERMEDIATE_SOURCES := \
    $(cmsdk_LOCAL_INTERMEDIATE_SOURCES)

# Make sure that R.java and Manifest.java are built before we build
# the source for this library.
cm_framework_res_R_stamp := \
    $(call intermediates-dir-for,APPS,org.cyanogenmod.platform-res,,COMMON)/src/R.stamp
$(full_classes_compiled_jar): $(cm_framework_res_R_stamp)
$(built_dex_intermediate): $(cm_framework_res_R_stamp)
$(full_target): $(cm_framework_built) $(gen)
include $(BUILD_STATIC_JAVA_LIBRARY)

# the sdk as an aar for publish, not built as part of full target
# DO NOT LINK AGAINST THIS IN BUILD
# ============================================================
include $(CLEAR_VARS)

LOCAL_MODULE := org.cyanogenmod.platform.sdk.aar

LOCAL_JACK_ENABLED := disabled

# just need to define this, $(TOP)/dummy should not exist
LOCAL_SRC_FILES := $(call all-java-files-under, dummy)
LOCAL_CONSUMER_PROGUARD_FILE := $(LOCAL_PATH)/sdk/proguard.txt

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, sdk/res/res)
LOCAL_MANIFEST_FILE := sdk/AndroidManifest.xml

cmsdk_exclude_files := 'cyanogenmod/library'
LOCAL_JAR_EXCLUDE_PACKAGES := $(cmsdk_exclude_files)
LOCAL_JAR_EXCLUDE_FILES := none

LOCAL_STATIC_JAVA_LIBRARIES := org.cyanogenmod.platform.sdk

include $(BUILD_STATIC_JAVA_LIBRARY)
$(LOCAL_MODULE) : $(built_aar)

# full target for use by platform apps
#
include $(CLEAR_VARS)

LOCAL_MODULE:= org.cyanogenmod.platform.internal
LOCAL_MODULE_TAGS := optional
LOCAL_REQUIRED_MODULES := services

LOCAL_SRC_FILES := \
    $(call all-java-files-under, $(cyanogenmod_sdk_src)) \
    $(call all-java-files-under, $(cyanogenmod_sdk_internal_src)) \
    $(call all-Iaidl-files-under, $(cyanogenmod_sdk_src)) \
    $(call all-Iaidl-files-under, $(cyanogenmod_sdk_internal_src))

# Included aidl files from cyanogenmod.app namespace
LOCAL_AIDL_INCLUDES := $(LOCAL_PATH)/sdk/src/java

cmsdk_LOCAL_INTERMEDIATE_SOURCES := \
    $(cm_platform_res)/cyanogenmod/platform/R.java \
    $(cm_platform_res)/cyanogenmod/platform/Manifest.java \
    $(cm_platform_res)/org/cyanogenmod/platform/internal/R.java \
    $(cm_platform_res)/org/cyanogenmod/platform/internal/Manifest.java

LOCAL_INTERMEDIATE_SOURCES := \
    $(cmsdk_LOCAL_INTERMEDIATE_SOURCES)

$(full_target): $(cm_framework_built) $(gen)
include $(BUILD_STATIC_JAVA_LIBRARY)


# ===========================================================
# Common Droiddoc vars
cmplat_docs_src_files := \
    $(call all-java-files-under, $(cyanogenmod_sdk_src)) \
    $(call all-html-files-under, $(cyanogenmod_sdk_src))

cmplat_docs_java_libraries := \
    org.cyanogenmod.platform.sdk

# SDK version as defined
cmplat_docs_SDK_VERSION := 13.0

# release version
cmplat_docs_SDK_REL_ID := 6

cmplat_docs_LOCAL_MODULE_CLASS := JAVA_LIBRARIES

cmplat_docs_LOCAL_DROIDDOC_SOURCE_PATH := \
    $(cmplat_docs_src_files)

intermediates.COMMON := $(call intermediates-dir-for,$(LOCAL_MODULE_CLASS), org.cyanogenmod.platform.sdk,,COMMON)

# ====  the api stubs and current.xml ===========================
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
    $(cmplat_docs_src_files)
LOCAL_INTERMEDIATE_SOURCES:= $(cmplat_LOCAL_INTERMEDIATE_SOURCES)
LOCAL_JAVA_LIBRARIES:= $(cmplat_docs_java_libraries)
LOCAL_MODULE_CLASS:= $(cmplat_docs_LOCAL_MODULE_CLASS)
LOCAL_DROIDDOC_SOURCE_PATH:= $(cmplat_docs_LOCAL_DROIDDOC_SOURCE_PATH)
LOCAL_ADDITIONAL_JAVA_DIR:= $(intermediates.COMMON)/src
LOCAL_ADDITIONAL_DEPENDENCIES:= $(cmplat_docs_LOCAL_ADDITIONAL_DEPENDENCIES)

LOCAL_MODULE := cm-api-stubs

LOCAL_DROIDDOC_CUSTOM_TEMPLATE_DIR:= build/tools/droiddoc/templates-sdk

LOCAL_DROIDDOC_OPTIONS:= \
        -stubs $(TARGET_OUT_COMMON_INTERMEDIATES)/JAVA_LIBRARIES/cmsdk_stubs_current_intermediates/src \
        -stubpackages $(cm_stub_packages) \
        -exclude org.cyanogenmod.platform.internal \
        -api $(INTERNAL_CM_PLATFORM_API_FILE) \
        -removedApi $(INTERNAL_CM_PLATFORM_REMOVED_API_FILE) \
        -nodocs

LOCAL_UNINSTALLABLE_MODULE := true

include $(BUILD_DROIDDOC)

# $(gen), i.e. framework.aidl, is also needed while building against the current stub.
$(full_target): $(cm_framework_built) $(gen)
$(INTERNAL_CM_PLATFORM_API_FILE): $(full_target)
$(call dist-for-goals,sdk,$(INTERNAL_CM_PLATFORM_API_FILE))

# ====  the system api stubs ===================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
    $(cmplat_docs_src_files)
LOCAL_INTERMEDIATE_SOURCES:= $(cmplat_LOCAL_INTERMEDIATE_SOURCES)
LOCAL_JAVA_LIBRARIES:= $(cmplat_docs_java_libraries)
LOCAL_MODULE_CLASS:= $(cmplat_docs_LOCAL_MODULE_CLASS)
LOCAL_DROIDDOC_SOURCE_PATH:= $(cmplat_docs_LOCAL_DROIDDOC_SOURCE_PATH)
LOCAL_ADDITIONAL_JAVA_DIR:= $(intermediates.COMMON)/src

LOCAL_MODULE := cm-system-api-stubs

LOCAL_DROIDDOC_OPTIONS:=\
        -stubs $(TARGET_OUT_COMMON_INTERMEDIATES)/JAVA_LIBRARIES/cmsdk_system_stubs_current_intermediates/src \
        -stubpackages $(cm_stub_packages) \
        -showAnnotation android.annotation.SystemApi \
        -exclude org.cyanogenmod.platform.internal \
        -api $(INTERNAL_CM_PLATFORM_SYSTEM_API_FILE) \
        -removedApi $(INTERNAL_CM_PLATFORM_SYSTEM_REMOVED_API_FILE) \
        -nodocs

LOCAL_DROIDDOC_CUSTOM_TEMPLATE_DIR:= build/tools/droiddoc/templates-sdk

LOCAL_UNINSTALLABLE_MODULE := true

include $(BUILD_DROIDDOC)

# $(gen), i.e. framework.aidl, is also needed while building against the current stub.
$(full_target): $(cm_framework_built) $(gen)
$(INTERNAL_CM_PLATFORM_API_FILE): $(full_target)
$(call dist-for-goals,sdk,$(INTERNAL_CM_PLATFORM_API_FILE))

# Documentation
# ===========================================================
include $(CLEAR_VARS)

LOCAL_MODULE := org.cyanogenmod.platform.sdk
LOCAL_INTERMEDIATE_SOURCES:= $(cmplat_LOCAL_INTERMEDIATE_SOURCES)
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(cmplat_docs_src_files)
LOCAL_ADDITONAL_JAVA_DIR := $(intermediates.COMMON)/src

LOCAL_IS_HOST_MODULE := false
LOCAL_DROIDDOC_CUSTOM_TEMPLATE_DIR := vendor/cm/build/tools/droiddoc/templates-cmsdk
LOCAL_ADDITIONAL_DEPENDENCIES := \
    services \
    org.cyanogenmod.hardware

LOCAL_JAVA_LIBRARIES := $(cmplat_docs_java_libraries)

LOCAL_DROIDDOC_OPTIONS := \
        -offlinemode \
        -exclude org.cyanogenmod.platform.internal \
        -hidePackage org.cyanogenmod.platform.internal \
        -hdf android.whichdoc offline \
        -hdf sdk.version $(cmplat_docs_docs_SDK_VERSION) \
        -hdf sdk.rel.id $(cmplat_docs_docs_SDK_REL_ID) \
        -hdf sdk.preview 0 \
        -since $(CM_SRC_API_DIR)/1.txt 1 \
        -since $(CM_SRC_API_DIR)/2.txt 2 \
        -since $(CM_SRC_API_DIR)/3.txt 3 \
        -since $(CM_SRC_API_DIR)/4.txt 4 \
        -since $(CM_SRC_API_DIR)/5.txt 5 \
        -since $(CM_SRC_API_DIR)/6.txt 6

$(full_target): $(cm_framework_built) $(gen)
include $(BUILD_DROIDDOC)

include $(call first-makefiles-under,$(LOCAL_PATH))

# Cleanup temp vars
# ===========================================================
cmplat.docs.src_files :=
cmplat.docs.java_libraries :=
intermediates.COMMON :=
