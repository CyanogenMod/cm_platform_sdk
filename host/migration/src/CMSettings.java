/**
 * Copyright (c) 2015, The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Arrays;

/**
 * CMSettings contains CM specific preferences in System, Secure, and Global.
 */
public final class CMSettings {
    public static final String AUTHORITY = "cmsettings";

    /**
     * System settings, containing miscellaneous CM system preferences. This table holds simple
     * name/value pairs. There are convenience functions for accessing individual settings entries.
     */
    public static final class System {
        // region System Settings

        /**
         * Quick Settings Quick Pulldown
         * 0 = off, 1 = right, 2 = left
         * @hide
         */
        public static final String QS_QUICK_PULLDOWN = "qs_quick_pulldown";

        /**
         * Whether to attach a queue to media notifications.
         * 0 = 0ff, 1 = on
         * @hide
         */
        public static final String NOTIFICATION_PLAY_QUEUE = "notification_play_queue";

        /**
         * Whether the HighTouchSensitivity is activated or not.
         * 0 = off, 1 = on
         * @hide
         */
        public static final String HIGH_TOUCH_SENSITIVITY_ENABLE =
                "high_touch_sensitivity_enable";

        /**
         * Show the pending notification counts as overlays on the status bar
         * @hide
         */
        public static final String SYSTEM_PROFILES_ENABLED = "system_profiles_enabled";

        /**
         * Whether to hide the clock, show it in the right or left
         * position or show it in the center
         * 0: don't show the clock
         * 1: show the clock in the right position (LTR)
         * 2: show the clock in the center
         * 3: show the clock in the left position (LTR)
         * default: 1
         * @hide
         */
        public static final String STATUS_BAR_CLOCK = "status_bar_clock";

        /**
         * Display style of AM/PM next to clock in status bar
         * 0: Normal display (Eclair stock)
         * 1: Small display (Froyo stock)
         * 2: No display (Gingerbread/ICS stock)
         * default: 2
         * @hide
         */
        public static final String STATUS_BAR_AM_PM = "status_bar_am_pm";

        /**
         * Display style of the status bar battery information
         * 0: Display the battery an icon in portrait mode
         * 2: Display the battery as a circle
         * 4: Hide the battery status information
         * 5: Display the battery an icon in landscape mode
         * 6: Display the battery as plain text
         * default: 0
         * @hide
         */
        public static final String STATUS_BAR_BATTERY_STYLE = "status_bar_battery_style";

        /**
         * Status bar battery %
         * 0: Hide the battery percentage
         * 1: Display the battery percentage inside the icon
         * 2: Display the battery percentage next to the icon
         * @hide
         */
        public static final String STATUS_BAR_SHOW_BATTERY_PERCENT = "status_bar_show_battery_percent";
        // endregion

        /**
         * Whether the phone ringtone should be played in an increasing manner
         * @hide
         */
        public static final String INCREASING_RING = "increasing_ring";

        /**
         * Start volume fraction for increasing ring volume
         * @hide
         */
        public static final String INCREASING_RING_START_VOLUME = "increasing_ring_start_vol";

        /**
         * Ramp up time (seconds) for increasing ring
         * @hide
         */
        public static final String INCREASING_RING_RAMP_UP_TIME = "increasing_ring_ramp_up_time";

        /**
         * Volume Adjust Sounds Enable, This is the noise made when using volume hard buttons
         * Defaults to 1 - sounds enabled
         * @hide
         */
        public static final String VOLUME_ADJUST_SOUNDS_ENABLED = "volume_adjust_sounds_enabled";

        /**
         * Navigation controls to Use
         * @hide
         */
        public static final String NAV_BUTTONS = "nav_buttons";

        /**
         * Volume key controls ringtone or media sound stream
         * @hide
         */
        public static final String VOLUME_KEYS_CONTROL_RING_STREAM =
                "volume_keys_control_ring_stream";

        /**
         * boolean value. toggles using arrow key locations on nav bar
         * as left and right dpad keys
         * @hide
         */
        public static final String NAVIGATION_BAR_MENU_ARROW_KEYS = "navigation_bar_menu_arrow_keys";

        /**
         * Action to perform when the home key is long-pressed.
         * (Default can be configured via config_longPressOnHomeBehavior)
         * 0 - Nothing
         * 1 - Menu
         * 2 - App-switch
         * 3 - Search
         * 4 - Voice search
         * 5 - In-app search
         * 6 - Launch Camera
         * 7 - Action Sleep
         * 8 - Last app
         * @hide
         */
        public static final String KEY_HOME_LONG_PRESS_ACTION = "key_home_long_press_action";

        /**
         * Action to perform when the home key is double-tapped.
         * (Default can be configured via config_doubleTapOnHomeBehavior)
         * (See KEY_HOME_LONG_PRESS_ACTION for valid values)
         * @hide
         */
        public static final String KEY_HOME_DOUBLE_TAP_ACTION = "key_home_double_tap_action";

        /**
         * Whether to wake the screen with the back key, the value is boolean.
         * @hide
         */
        public static final String BACK_WAKE_SCREEN = "back_wake_screen";

        /**
         * Whether to wake the screen with the menu key, the value is boolean.
         * @hide
         */
        public static final String MENU_WAKE_SCREEN = "menu_wake_screen";

        /**
         * Whether to wake the screen with the volume keys, the value is boolean.
         * @hide
         */
        public static final String VOLUME_WAKE_SCREEN = "volume_wake_screen";

        /**
         * Action to perform when the menu key is pressed. (Default is 1)
         * (See KEY_HOME_LONG_PRESS_ACTION for valid values)
         * @hide
         */
        public static final String KEY_MENU_ACTION = "key_menu_action";

        /**
         * Action to perform when the menu key is long-pressed.
         * (Default is 0 on devices with a search key, 3 on devices without)
         * (See KEY_HOME_LONG_PRESS_ACTION for valid values)
         * @hide
         */
        public static final String KEY_MENU_LONG_PRESS_ACTION = "key_menu_long_press_action";

        /**
         * Action to perform when the assistant (search) key is pressed. (Default is 3)
         * (See KEY_HOME_LONG_PRESS_ACTION for valid values)
         * @hide
         */
        public static final String KEY_ASSIST_ACTION = "key_assist_action";

        /**
         * Action to perform when the assistant (search) key is long-pressed. (Default is 4)
         * (See KEY_HOME_LONG_PRESS_ACTION for valid values)
         * @hide
         */
        public static final String KEY_ASSIST_LONG_PRESS_ACTION = "key_assist_long_press_action";

        /**
         * Action to perform when the app switch key is pressed. (Default is 2)
         * (See KEY_HOME_LONG_PRESS_ACTION for valid values)
         * @hide
         */
        public static final String KEY_APP_SWITCH_ACTION = "key_app_switch_action";

        /**
         * Action to perform when the app switch key is long-pressed. (Default is 0)
         * (See KEY_HOME_LONG_PRESS_ACTION for valid values)
         * @hide
         */
        public static final String KEY_APP_SWITCH_LONG_PRESS_ACTION = "key_app_switch_long_press_action";

        /**
         * Whether to wake the screen with the home key, the value is boolean.
         * @hide
         */
        public static final String HOME_WAKE_SCREEN = "home_wake_screen";

        /**
         * Whether to wake the screen with the assist key, the value is boolean.
         * @hide
         */
        public static final String ASSIST_WAKE_SCREEN = "assist_wake_screen";

        /**
         * Whether to wake the screen with the app switch key, the value is boolean.
         * @hide
         */
        public static final String APP_SWITCH_WAKE_SCREEN = "app_switch_wake_screen";

        /**
         * Whether to wake the screen with the camera key half-press.
         * @hide
         */
        public static final String CAMERA_WAKE_SCREEN = "camera_wake_screen";

        /**
         * Whether or not to send device back to sleep if Camera button is released ("Peek")
         * @hide
         */
        public static final String CAMERA_SLEEP_ON_RELEASE = "camera_sleep_on_release";

        /**
         * Whether to launch secure camera app when key is longpressed
         * @hide
         */
        public static final String CAMERA_LAUNCH = "camera_launch";

        /**
         * Swap volume buttons when the screen is rotated
         * 0 - Disabled
         * 1 - Enabled (screen is rotated by 90 or 180 degrees: phone, hybrid)
         * 2 - Enabled (screen is rotated by 180 or 270 degrees: tablet)
         * @hide
         */
        public static final String SWAP_VOLUME_KEYS_ON_ROTATION = "swap_volume_keys_on_rotation";

        /**
         * Whether the battery light should be enabled (if hardware supports it)
         * The value is boolean (1 or 0).
         * @hide
         */
        public static final String BATTERY_LIGHT_ENABLED = "battery_light_enabled";

        /**
         * Whether the battery LED should repeatedly flash when the battery is low
         * on charge. The value is boolean (1 or 0).
         * @hide
         */
        public static final String BATTERY_LIGHT_PULSE = "battery_light_pulse";

        /**
         * What color to use for the battery LED while charging - low
         * @hide
         */
        public static final String BATTERY_LIGHT_LOW_COLOR = "battery_light_low_color";

        /**
         * What color to use for the battery LED while charging - medium
         * @hide
         */
        public static final String BATTERY_LIGHT_MEDIUM_COLOR = "battery_light_medium_color";

        /**
         * What color to use for the battery LED while charging - full
         * @hide
         */
        public static final String BATTERY_LIGHT_FULL_COLOR = "battery_light_full_color";

        /**
         * Sprint MWI Quirk: Show message wait indicator notifications
         * @hide
         */
        public static final String ENABLE_MWI_NOTIFICATION = "enable_mwi_notification";

        /**
         * Check the proximity sensor during wakeup
         * @hide
         */
        public static final String PROXIMITY_ON_WAKE = "proximity_on_wake";

        /**
         * Enable looking up of phone numbers of nearby places
         *
         * @hide
         */
        public static final String ENABLE_FORWARD_LOOKUP = "enable_forward_lookup";

        /**
         * Enable looking up of phone numbers of people
         *
         * @hide
         */
        public static final String ENABLE_PEOPLE_LOOKUP = "enable_people_lookup";

        /**
         * Enable looking up of information of phone numbers not in the contacts
         *
         * @hide
         */
        public static final String ENABLE_REVERSE_LOOKUP = "enable_reverse_lookup";

        /**
         * The forward lookup provider
         *
         * @hide
         */
        public static final String FORWARD_LOOKUP_PROVIDER = "forward_lookup_provider";

        /**
         * The people lookup provider
         *
         * @hide
         */
        public static final String PEOPLE_LOOKUP_PROVIDER = "people_lookup_provider";

        /**
         * The reverse lookup provider
         *
         * @hide
         */
        public static final String REVERSE_LOOKUP_PROVIDER = "reverse_lookup_provider";

        /**
         * The OpenCNAM paid account ID
         *
         * @hide
         */
        public static final String DIALER_OPENCNAM_ACCOUNT_SID = "dialer_opencnam_account_sid";

        /**
         * The OpenCNAM authentication token
         *
         * @hide
         */
        public static final String DIALER_OPENCNAM_AUTH_TOKEN = "dialer_opencnam_auth_token";

        /**
         * Whether wifi settings will connect to access point automatically
         * 0 = automatically
         * 1 = manually
         * @hide
         */
        public static final String WIFI_AUTO_CONNECT_TYPE = "wifi_auto_connect_type";

        /**
         * Color temperature of the display during the day
         * @hide
         */
        public static final String DISPLAY_TEMPERATURE_DAY = "display_temperature_day";

        /**
         * Color temperature of the display at night
         * @hide
         */
        public static final String DISPLAY_TEMPERATURE_NIGHT = "display_temperature_night";

        /**
         * Display color temperature adjustment mode, one of DAY (default), NIGHT, or AUTO.
         * @hide
         */
        public static final String DISPLAY_TEMPERATURE_MODE = "display_temperature_mode";

        /**
         * Automatic outdoor mode
         * @hide
         */
        public static final String DISPLAY_AUTO_OUTDOOR_MODE = "display_auto_outdoor_mode";

        /**
         * Use display power saving features such as CABC or CABL
         * @hide
         */
        public static final String DISPLAY_LOW_POWER = "display_low_power";

        /**
         * Use color enhancement feature of display
         * @hide
         */
        public static final String DISPLAY_COLOR_ENHANCE = "display_color_enhance";

        /**
         * Manual display color adjustments (RGB values as floats, separated by spaces)
         * @hide
         */
        public static final String DISPLAY_COLOR_ADJUSTMENT = "display_color_adjustment";

        /**
         * Did we tell about how they can stop breaking their eyes?
         * @hide
         */
        public static final String LIVE_DISPLAY_HINTED = "live_display_hinted";

        /**
         *  Enable statusbar double tap gesture on to put device to sleep
         * @hide
         */
        public static final String DOUBLE_TAP_SLEEP_GESTURE = "double_tap_sleep_gesture";

        /**
         * Boolean value on whether to show weather in the statusbar
         * @hide
         */
        public static final String STATUS_BAR_SHOW_WEATHER = "status_bar_show_weather";

        /**
         * Show search bar in recents
         * @hide
         */
        public static final String RECENTS_SHOW_SEARCH_BAR = "recents_show_search_bar";

        /**
         * Whether navigation bar is placed on the left side in landscape mode
         * @hide
         */
        public static final String NAVBAR_LEFT_IN_LANDSCAPE = "navigation_bar_left";

        /**
         * Locale for secondary overlay on dialer for t9 search input
         * @hide
         */
        public static final String T9_SEARCH_INPUT_LOCALE = "t9_search_input_locale";

        /**
         * If all file types can be accepted over Bluetooth OBEX.
         * @hide
         */
        public static final String BLUETOOTH_ACCEPT_ALL_FILES =
                "bluetooth_accept_all_files";

        /**
         * Whether to scramble a pin unlock layout
         * @hide
         */
        public static final String LOCKSCREEN_PIN_SCRAMBLE_LAYOUT =
                "lockscreen_scramble_pin_layout";

        /**
         * Whether keyguard will rotate to landscape mode
         * @hide
         */
        public static final String LOCKSCREEN_ROTATION = "lockscreen_rotation";

        /**
         * @hide
         */
        public static final String SHOW_ALARM_ICON = "show_alarm_icon";

        /**
         * Whether to show the IME switcher in the status bar
         * @hide
         */
        public static final String STATUS_BAR_IME_SWITCHER = "status_bar_ime_switcher";

        /** Whether to allow one finger quick settings expansion on the right side of the statusbar.
         *
         * @hide
         */
        public static final String STATUS_BAR_QUICK_QS_PULLDOWN = "status_bar_quick_qs_pulldown";

        /** Whether to show the brightness slider in quick settings panel.
         *
         * @hide
         */
        public static final String QS_SHOW_BRIGHTNESS_SLIDER = "qs_show_brightness_slider";

        /**
         * Whether to control brightness from status bar
         *
         * @hide
         */
        public static final String STATUS_BAR_BRIGHTNESS_CONTROL = "status_bar_brightness_control";

        /**
         * Whether or not volume button music controls should be enabled to seek media tracks
         * @hide
         */
        public static final String VOLBTN_MUSIC_CONTROLS = "volbtn_music_controls";

        /**
         * Use EdgeGesture Service for system gestures in PhoneWindowManager
         * @hide
         */
        public static final String USE_EDGE_SERVICE_FOR_GESTURES = "edge_service_for_gestures";

        /**
         * Show the pending notification counts as overlays on the status bar
         * @hide
         */
        public static final String STATUS_BAR_NOTIF_COUNT = "status_bar_notif_count";

        /**
         * Call recording format value
         * 0: AMR_WB
         * 1: MPEG_4
         * Default: 0
         * @hide
         */
        public static final String CALL_RECORDING_FORMAT = "call_recording_format";

        /**
         * Contains the notifications light maximum brightness to use.
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL =
                "notification_light_brightness_level";

        /**
         * Whether to use the all the LEDs for the notifications or just one.
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_MULTIPLE_LEDS_ENABLE =
                "notification_light_multiple_leds_enable";

        /**
         * Whether to allow notifications with the screen on or DayDreams.
         * The value is boolean (1 or 0). Default will always be false.
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_SCREEN_ON =
                "notification_light_screen_on_enable";

        /**
         * What color to use for the notification LED by default
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR =
                "notification_light_pulse_default_color";

        /**
         * How long to flash the notification LED by default
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_ON =
                "notification_light_pulse_default_led_on";

        /**
         * How long to wait between flashes for the notification LED by default
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_OFF =
                "notification_light_pulse_default_led_off";

        /**
         * What color to use for the missed call notification LED
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_PULSE_CALL_COLOR =
                "notification_light_pulse_call_color";

        /**
         * How long to flash the missed call notification LED
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_PULSE_CALL_LED_ON =
                "notification_light_pulse_call_led_on";

        /**
         * How long to wait between flashes for the missed call notification LED
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_PULSE_CALL_LED_OFF =
                "notification_light_pulse_call_led_off";
        /**
         * What color to use for the voicemail notification LED
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_PULSE_VMAIL_COLOR =
                "notification_light_pulse_vmail_color";

        /**
         * How long to flash the voicemail notification LED
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_PULSE_VMAIL_LED_ON =
                "notification_light_pulse_vmail_led_on";

        /**
         * How long to wait between flashes for the voicemail notification LED
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_PULSE_VMAIL_LED_OFF =
                "notification_light_pulse_vmail_led_off";

        /**
         * Whether to use the custom LED values for the notification pulse LED.
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE =
                "notification_light_pulse_custom_enable";

        /**
         * Which custom LED values to use for the notification pulse LED.
         * @hide
         */
        public static final String NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES =
                "notification_light_pulse_custom_values";

        /**
         * @hide
         */
        public static final String[] LEGACY_SYSTEM_SETTINGS = new String[]{
                CMSettings.System.QS_QUICK_PULLDOWN,
                CMSettings.System.NAV_BUTTONS,
                CMSettings.System.KEY_HOME_LONG_PRESS_ACTION,
                CMSettings.System.KEY_HOME_DOUBLE_TAP_ACTION,
                CMSettings.System.BACK_WAKE_SCREEN,
                CMSettings.System.MENU_WAKE_SCREEN,
                CMSettings.System.VOLUME_WAKE_SCREEN,
                CMSettings.System.KEY_MENU_ACTION,
                CMSettings.System.KEY_MENU_LONG_PRESS_ACTION,
                CMSettings.System.KEY_ASSIST_ACTION,
                CMSettings.System.KEY_ASSIST_LONG_PRESS_ACTION,
                CMSettings.System.KEY_APP_SWITCH_ACTION,
                CMSettings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION,
                CMSettings.System.HOME_WAKE_SCREEN,
                CMSettings.System.ASSIST_WAKE_SCREEN,
                CMSettings.System.APP_SWITCH_WAKE_SCREEN,
                CMSettings.System.CAMERA_WAKE_SCREEN,
                CMSettings.System.CAMERA_SLEEP_ON_RELEASE,
                CMSettings.System.CAMERA_LAUNCH,
                CMSettings.System.SWAP_VOLUME_KEYS_ON_ROTATION,
                CMSettings.System.BATTERY_LIGHT_ENABLED,
                CMSettings.System.BATTERY_LIGHT_PULSE,
                CMSettings.System.BATTERY_LIGHT_LOW_COLOR,
                CMSettings.System.BATTERY_LIGHT_MEDIUM_COLOR,
                CMSettings.System.BATTERY_LIGHT_FULL_COLOR,
                CMSettings.System.ENABLE_MWI_NOTIFICATION,
                CMSettings.System.PROXIMITY_ON_WAKE,
                CMSettings.System.ENABLE_FORWARD_LOOKUP,
                CMSettings.System.ENABLE_PEOPLE_LOOKUP,
                CMSettings.System.ENABLE_REVERSE_LOOKUP,
                CMSettings.System.FORWARD_LOOKUP_PROVIDER,
                CMSettings.System.PEOPLE_LOOKUP_PROVIDER,
                CMSettings.System.REVERSE_LOOKUP_PROVIDER,
                CMSettings.System.DIALER_OPENCNAM_ACCOUNT_SID,
                CMSettings.System.DIALER_OPENCNAM_AUTH_TOKEN,
                CMSettings.System.DISPLAY_TEMPERATURE_DAY,
                CMSettings.System.DISPLAY_TEMPERATURE_NIGHT,
                CMSettings.System.DISPLAY_TEMPERATURE_MODE,
                CMSettings.System.DISPLAY_AUTO_OUTDOOR_MODE,
                CMSettings.System.DISPLAY_LOW_POWER,
                CMSettings.System.DISPLAY_COLOR_ENHANCE,
                CMSettings.System.DISPLAY_COLOR_ADJUSTMENT,
                CMSettings.System.LIVE_DISPLAY_HINTED,
                CMSettings.System.DOUBLE_TAP_SLEEP_GESTURE,
                CMSettings.System.STATUS_BAR_SHOW_WEATHER,
                CMSettings.System.RECENTS_SHOW_SEARCH_BAR,
                CMSettings.System.NAVBAR_LEFT_IN_LANDSCAPE,
                CMSettings.System.T9_SEARCH_INPUT_LOCALE,
                CMSettings.System.BLUETOOTH_ACCEPT_ALL_FILES,
                CMSettings.System.LOCKSCREEN_PIN_SCRAMBLE_LAYOUT,
                CMSettings.System.LOCKSCREEN_ROTATION,
                CMSettings.System.SHOW_ALARM_ICON,
                CMSettings.System.STATUS_BAR_IME_SWITCHER,
                CMSettings.System.QS_SHOW_BRIGHTNESS_SLIDER,
                CMSettings.System.STATUS_BAR_BRIGHTNESS_CONTROL,
                CMSettings.System.VOLBTN_MUSIC_CONTROLS,
                CMSettings.System.SWAP_VOLUME_KEYS_ON_ROTATION,
                CMSettings.System.USE_EDGE_SERVICE_FOR_GESTURES,
                CMSettings.System.STATUS_BAR_NOTIF_COUNT,
                CMSettings.System.CALL_RECORDING_FORMAT,
                CMSettings.System.NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL,
                CMSettings.System.NOTIFICATION_LIGHT_MULTIPLE_LEDS_ENABLE,
                CMSettings.System.NOTIFICATION_LIGHT_SCREEN_ON,
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR,
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_ON,
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_OFF,
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_CALL_COLOR,
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_CALL_LED_ON,
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_CALL_LED_OFF,
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_VMAIL_COLOR,
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_VMAIL_LED_ON,
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_VMAIL_LED_OFF,
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE,
                CMSettings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES,
                CMSettings.System.STATUS_BAR_QUICK_QS_PULLDOWN,
                CMSettings.System.VOLUME_ADJUST_SOUNDS_ENABLED,
                CMSettings.System.SYSTEM_PROFILES_ENABLED,
                CMSettings.System.INCREASING_RING,
                CMSettings.System.INCREASING_RING_START_VOLUME,
                CMSettings.System.INCREASING_RING_RAMP_UP_TIME,
                CMSettings.System.STATUS_BAR_CLOCK,
                CMSettings.System.STATUS_BAR_AM_PM,
                CMSettings.System.STATUS_BAR_BATTERY_STYLE,
                CMSettings.System.STATUS_BAR_SHOW_BATTERY_PERCENT,
                CMSettings.System.VOLUME_KEYS_CONTROL_RING_STREAM,
                CMSettings.System.NAVIGATION_BAR_MENU_ARROW_KEYS,
        };

        /**
         * @hide
         */
        public static boolean isLegacySetting(String key) {
            return Arrays.asList(LEGACY_SYSTEM_SETTINGS).contains(key);
        }
    }

    /**
     * Secure settings, containing miscellaneous CM secure preferences. This
     * table holds simple name/value pairs. There are convenience
     * functions for accessing individual settings entries.
     */
    public static final class Secure {

        // region Secure Settings

        /**
         * Whether to enable "advanced mode" for the current user.
         * Boolean setting. 0 = no, 1 = yes.
         * @hide
         */
        public static final String ADVANCED_MODE = "advanced_mode";

        /**
         * The time in ms to keep the button backlight on after pressing a button.
         * A value of 0 will keep the buttons on for as long as the screen is on.
         * @hide
         */
        public static final String BUTTON_BACKLIGHT_TIMEOUT = "button_backlight_timeout";

        /**
         * The button brightness to be used while the screen is on or after a button press,
         * depending on the value of {@link BUTTON_BACKLIGHT_TIMEOUT}.
         * Valid value range is between 0 and {@link PowerManager#getMaximumButtonBrightness()}
         * @hide
         */
        public static final String BUTTON_BRIGHTNESS = "button_brightness";

        /**
         * A '|' delimited list of theme components to apply from the default theme on first boot.
         * Components can be one or more of the "mods_XXXXXXX" found in
         * {@link ThemesContract$ThemesColumns}.  Leaving this field blank assumes all components
         * will be applied.
         *
         * ex: mods_icons|mods_overlays|mods_homescreen
         *
         * @hide
         */
        public static final String DEFAULT_THEME_COMPONENTS = "default_theme_components";

        /**
         * Default theme to use.  If empty, use holo.
         * @hide
         */
        public static final String DEFAULT_THEME_PACKAGE = "default_theme_package";

        /**
         * Developer options - Navigation Bar show switch
         * @hide
         */
        public static final String DEV_FORCE_SHOW_NAVBAR = "dev_force_show_navbar";

        /**
         * The keyboard brightness to be used while the screen is on.
         * Valid value range is between 0 and {@link PowerManager#getMaximumKeyboardBrightness()}
         * @hide
         */
        public static final String KEYBOARD_BRIGHTNESS = "keyboard_brightness";

        /**
         * Default theme config name
         */
        public static final String NAME_THEME_CONFIG = "name_theme_config";

        /**
         * Custom navring actions
         * @hide
         */
        public static final String[] NAVIGATION_RING_TARGETS = new String[] {
                "navigation_ring_targets_0",
                "navigation_ring_targets_1",
                "navigation_ring_targets_2",
        };

        /**
         * String to contain power menu actions
         * @hide
         */
        public static final String POWER_MENU_ACTIONS = "power_menu_actions";

        /**
         * Whether to show the brightness slider in quick settings panel.
         * @hide
         */
        public static final String QS_SHOW_BRIGHTNESS_SLIDER = "qs_show_brightness_slider";

        /**
         * List of QS tile names
         * @hide
         */
        public static final String QS_TILES = "sysui_qs_tiles";

        /**
         * Use "main" tiles on the first row of the quick settings panel
         * 0 = no, 1 = yes
         * @hide
         */
        public static final String QS_USE_MAIN_TILES = "sysui_qs_main_tiles";

        /**
         * Global stats collection
         * @hide
         */
        public static final String STATS_COLLECTION = "stats_collection";

        /**
         * Boolean value whether to link ringtone and notification volume
         *
         * @hide
         */
        public static final String VOLUME_LINK_NOTIFICATION = "volume_link_notification";

        /**
         * Whether newly installed apps should run with privacy guard by default
         * @hide
         */
        public static final String PRIVACY_GUARD_DEFAULT = "privacy_guard_default";

        /**
         * The global recents long press activity chosen by the user.
         * This setting is stored as a flattened component name as
         * per {@link ComponentName#flattenToString()}.
         *
         * @hide
         */
        public static final String RECENTS_LONG_PRESS_ACTIVITY = "recents_long_press_activity";

        /**
         * What happens when the user presses the Home button when the
         * phone is ringing.<br/>
         * <b>Values:</b><br/>
         * 1 - Nothing happens. (Default behavior)<br/>
         * 2 - The Home button answer the current call.<br/>
         *
         * @hide
         */
        public static final String RING_HOME_BUTTON_BEHAVIOR = "ring_home_button_behavior";

        /**
         * RING_HOME_BUTTON_BEHAVIOR value for "do nothing".
         * @hide
         */
        public static final int RING_HOME_BUTTON_BEHAVIOR_DO_NOTHING = 0x1;

        /**
         * RING_HOME_BUTTON_BEHAVIOR value for "answer".
         * @hide
         */
        public static final int RING_HOME_BUTTON_BEHAVIOR_ANSWER = 0x2;

        /**
         * RING_HOME_BUTTON_BEHAVIOR default value.
         * @hide
         */
        public static final int RING_HOME_BUTTON_BEHAVIOR_DEFAULT =
                RING_HOME_BUTTON_BEHAVIOR_DO_NOTHING;

        /**
         * When the user has enable the option to have a "bug report" command
         * in the power menu.
         * @deprecated Use {@link android.provider.Settings.Global#BUGREPORT_IN_POWER_MENU} instead
         * @hide
         */
        @Deprecated
        public static final String BUGREPORT_IN_POWER_MENU = "bugreport_in_power_menu";

        /**
         * Performance profile
         * @hide
         */
        public static final String PERFORMANCE_PROFILE = "performance_profile";

        /**
         * App-based performance profile selection
         * @hide
         */
        public static final String APP_PERFORMANCE_PROFILES_ENABLED = "app_perf_profiles_enabled";

        /**
         * Launch actions for left/right lockscreen targets
         * @hide
         */
        public static final String LOCKSCREEN_TARGETS = "lockscreen_target_actions";

        /**
         * Whether to display a menu containing 'Wipe data', 'Force close' and other options
         * in the notification area and in the recent app list
         * @hide
         */
        public static final String DEVELOPMENT_SHORTCUT = "development_shortcut";

        /**
         * What happens when the user presses the Power button while in-call
         * and the screen is on.<br/>
         * <b>Values:</b><br/>
         * 1 - The Power button turns off the screen and locks the device. (Default behavior)<br/>
         * 2 - The Power button hangs up the current call.<br/>
         *
         * @hide
         */
        public static final String INCALL_POWER_BUTTON_BEHAVIOR = "incall_power_button_behavior";

        /**
         * INCALL_POWER_BUTTON_BEHAVIOR value for "turn off screen".
         * @hide
         */
        public static final int INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF = 0x1;

        /**
         * INCALL_POWER_BUTTON_BEHAVIOR value for "hang up".
         * @hide
         */
        public static final int INCALL_POWER_BUTTON_BEHAVIOR_HANGUP = 0x2;

        /**
         * INCALL_POWER_BUTTON_BEHAVIOR default value.
         * @hide
         */
        public static final int INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT =
                INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF;

        /**
         * Whether to display the ADB notification.
         * @hide
         */
        public static final String ADB_NOTIFY = "adb_notify";

        /**
         * The TCP/IP port to run ADB on, or -1 for USB
         * @hide
         */
        public static final String ADB_PORT = "adb_port";

        /**
         * The hostname for this device
         * @hide
         */
        public static final String DEVICE_HOSTNAME = "device_hostname";

        /**
         * Whether to allow killing of the foreground app by long-pressing the Back button
         * @hide
         */
        public static final String KILL_APP_LONGPRESS_BACK = "kill_app_longpress_back";

        /** Protected Components
         * @hide
         */
        public static final String PROTECTED_COMPONENTS = "protected_components";

        /**
         * Stored color matrix for LiveDisplay. This is used to allow co-existence with
         * display tuning done by DisplayAdjustmentUtils when hardware support isn't
         * available.
         * @hide
         */
        public static final String LIVE_DISPLAY_COLOR_MATRIX = "live_display_color_matrix";

        /**
         * Whether to include options in power menu for rebooting into recovery or bootloader
         * @hide
         */
        public static final String ADVANCED_REBOOT = "advanced_reboot";

        /**
         * This will be set to the system's current theme API version when ThemeService starts.
         * It is useful for when an upgrade from one version of CM to another occurs.
         * For example, after a user upgrades from CM11 to CM12, the value of this field
         * might be 19. ThemeService would then change the value to 21. This is useful
         * when an API change breaks a theme. Themeservice can identify old themes and
         * unapply them from the system.
         * @hide
         */
        public static final String THEME_PREV_BOOT_API_LEVEL = "theme_prev_boot_api_level";
        // endregion

        /**
         * @hide
         */
        public static final String[] LEGACY_SECURE_SETTINGS = new String[]{
                CMSettings.Secure.ADVANCED_MODE,
                CMSettings.Secure.BUTTON_BACKLIGHT_TIMEOUT,
                CMSettings.Secure.BUTTON_BRIGHTNESS,
                CMSettings.Secure.DEFAULT_THEME_COMPONENTS,
                CMSettings.Secure.DEFAULT_THEME_PACKAGE,
                CMSettings.Secure.DEV_FORCE_SHOW_NAVBAR,
                CMSettings.Secure.KEYBOARD_BRIGHTNESS,
                CMSettings.Secure.POWER_MENU_ACTIONS,
                CMSettings.Secure.STATS_COLLECTION,
                CMSettings.Secure.QS_SHOW_BRIGHTNESS_SLIDER,
                CMSettings.Secure.QS_TILES,
                CMSettings.Secure.QS_USE_MAIN_TILES,
                CMSettings.Secure.VOLUME_LINK_NOTIFICATION,
                CMSettings.Secure.NAVIGATION_RING_TARGETS[0],
                CMSettings.Secure.NAVIGATION_RING_TARGETS[1],
                CMSettings.Secure.NAVIGATION_RING_TARGETS[2],
                CMSettings.Secure.RECENTS_LONG_PRESS_ACTIVITY,
                CMSettings.Secure.ADB_NOTIFY,
                CMSettings.Secure.ADB_PORT,
                CMSettings.Secure.DEVICE_HOSTNAME,
                CMSettings.Secure.KILL_APP_LONGPRESS_BACK,
                CMSettings.Secure.PROTECTED_COMPONENTS,
                CMSettings.Secure.LIVE_DISPLAY_COLOR_MATRIX,
                CMSettings.Secure.ADVANCED_REBOOT,
                CMSettings.Secure.THEME_PREV_BOOT_API_LEVEL,
                CMSettings.Secure.LOCKSCREEN_TARGETS,
                CMSettings.Secure.RING_HOME_BUTTON_BEHAVIOR,
                CMSettings.Secure.PRIVACY_GUARD_DEFAULT,
                CMSettings.Secure.DEVELOPMENT_SHORTCUT,
                CMSettings.Secure.PERFORMANCE_PROFILE,
                CMSettings.Secure.APP_PERFORMANCE_PROFILES_ENABLED};

        /**
         * @hide
         */
        public static boolean isLegacySetting(String key) {
            return Arrays.asList(LEGACY_SECURE_SETTINGS).contains(key);
        }
    }

    /**
     * Global settings, containing miscellaneous CM global preferences. This
     * table holds simple name/value pairs. There are convenience
     * functions for accessing individual settings entries.
     */
    public static final class Global {
        // region Global Settings
        /**
         * Whether to wake the display when plugging or unplugging the charger
         *
         * @hide
         */
        public static final String WAKE_WHEN_PLUGGED_OR_UNPLUGGED =
                "wake_when_plugged_or_unplugged";

        /** {@hide} */
        public static final String
                BLUETOOTH_A2DP_SRC_PRIORITY_PREFIX = "bluetooth_a2dp_src_priority_";

        /**
         * Whether to sound when charger power is connected/disconnected
         * @hide
         */
        public static final String POWER_NOTIFICATIONS_ENABLED = "power_notifications_enabled";

        /**
         * Whether to vibrate when charger power is connected/disconnected
         * @hide
         */
        public static final String POWER_NOTIFICATIONS_VIBRATE = "power_notifications_vibrate";

        /**
         * URI for power notification sounds
         * @hide
         */
        public static final String POWER_NOTIFICATIONS_RINGTONE = "power_notifications_ringtone";

        /**
         * @hide
         */
        public static final String ZEN_DISABLE_DUCKING_DURING_MEDIA_PLAYBACK =
                "zen_disable_ducking_during_media_playback";

        /**
         * Whether the system auto-configure the priority of the wifi ap's or use
         * the manual settings established by the user.
         * <> 0 to autoconfigure, 0 to manual settings. Default is <> 0.
         * @hide
         */
        public static final String WIFI_AUTO_PRIORITIES_CONFIGURATION = "wifi_auto_priority";
        // endregion

        /**
         * @hide
         */
        public static final String[] LEGACY_GLOBAL_SETTINGS = new String[]{
                CMSettings.Global.WAKE_WHEN_PLUGGED_OR_UNPLUGGED,
                CMSettings.Global.POWER_NOTIFICATIONS_ENABLED,
                CMSettings.Global.POWER_NOTIFICATIONS_VIBRATE,
                CMSettings.Global.POWER_NOTIFICATIONS_RINGTONE,
                CMSettings.Global.ZEN_DISABLE_DUCKING_DURING_MEDIA_PLAYBACK,
                CMSettings.Global.WIFI_AUTO_PRIORITIES_CONFIGURATION};

        /**
         * @hide
         */
        public static boolean isLegacySetting(String key) {
            return Arrays.asList(LEGACY_GLOBAL_SETTINGS).contains(key);
        }
    }
}
