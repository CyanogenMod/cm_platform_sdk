# CyanogenMod Platform SDK

The Platform SDK provides a set of APIs that give you easy access to a variety of different features within CyanogenMod. The SDK exposes APIs and system level framework access in the Android framework that aren't available in any other distribution.

## Setup

You can either [download](https://github.com/CyanogenMod/android_prebuilts_cmsdk/tree/master/current) from prebuilts hosted on github or pull directly via Gradle.

Within `build.gradle` make sure your `repositories` list sonatype OSS repos

```
repositories {
    maven {
        url "https://oss.sonatype.org/content/repositories/snapshots/"
    }
}
```

To target the current release (for retail devices), set your `dependency` for `1.0-SNAPSHOT`

```
dependencies {
    compile 'org.cyanogenmod:platform.sdk:1.0-SNAPSHOT'
}
```

Likewise, you can target the `future` or `development` branch by setting your `dependencies` for `2.0-SNAPSHOT`

```
dependencies {
    compile 'org.cyanogenmod:platform.sdk:2.0-SNAPSHOT'
}
```

### WIKI

For further inquiries regarding this project, please reference the [wiki](https://github.com/CyanogenMod/cm_platform_sdk/wiki)
