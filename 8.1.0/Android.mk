LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

# --- protobuffer 文件：Google公司的一个开源项目，用于结构化数据串行化  https://www.cnblogs.com/chenny7/p/5157335.html
# --- SystemUI 中 proto文件只有一个src/com/android/systemui/statusbar/phone/touch_analytics.proto

LOCAL_MODULE := SystemUI-proto

LOCAL_SRC_FILES := $(call all-proto-files-under,src)

LOCAL_PROTOC_OPTIMIZE_TYPE := nano
LOCAL_PROTO_JAVA_OUTPUT_PARAMS := optional_field_style=accessors

include $(BUILD_STATIC_JAVA_LIBRARY)

# --- logtags 主要用来打印Event log的时候，定义Eent log中各个字段的含义

include $(CLEAR_VARS)

LOCAL_MODULE := SystemUI-tags

LOCAL_SRC_FILES := src/com/android/systemui/EventLogTags.logtags

include $(BUILD_STATIC_JAVA_LIBRARY)

# ------------------

include $(CLEAR_VARS)

LOCAL_USE_AAPT2 := true

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src) $(call all-Iaidl-files-under, src)

LOCAL_STATIC_ANDROID_LIBRARIES := \
    SystemUIPluginLib \
    android-support-v4 \
    android-support-v7-recyclerview \
    android-support-v7-preference \
    android-support-v7-appcompat \
    android-support-v7-mediarouter \
    android-support-v7-palette \
    android-support-v14-preference \
    android-support-v17-leanback

LOCAL_STATIC_JAVA_LIBRARIES := \
    SystemUI-tags \
    SystemUI-proto

LOCAL_JAVA_LIBRARIES := telephony-common
LOCAL_JAVA_LIBRARIES += android.car

LOCAL_PACKAGE_NAME := SystemUI
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags  # 代码混淆
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res-keyguard $(LOCAL_PATH)/res

ifneq ($(INCREMENTAL_BUILDS),)
    LOCAL_PROGUARD_ENABLED := disabled
    LOCAL_JACK_ENABLED := incremental
    LOCAL_DX_FLAGS := --multi-dex
    LOCAL_JACK_FLAGS := --multi-dex native
endif

include frameworks/base/packages/SettingsLib/common.mk  #有一些关联的模块在SetingsLib中

LOCAL_AAPT_FLAGS := --extra-packages com.android.keyguard

include $(BUILD_PACKAGE)

include $(call all-makefiles-under,$(LOCAL_PATH))
