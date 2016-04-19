/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (C) 2013-2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera;

import java.util.Locale;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.Camera.Parameters;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewPropertyAnimator;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import com.android.camera.CameraPreference.OnPreferenceChangedListener;
import com.android.camera.ui.CameraRootView;
import com.android.camera.ui.CountdownTimerPopup;
import com.android.camera.ui.ListSubMenu;
import com.android.camera.ui.ListMenu;
import com.android.camera.ui.RotateLayout;
import com.android.camera.ui.RotateImageView;
import com.android.camera.ui.RotateTextToast;
import org.codeaurora.snapcam.R;
import android.widget.HorizontalScrollView;
import android.view.ViewGroup;
import com.android.camera.util.CameraUtil;
import java.util.Locale;

public class PhotoMenu extends MenuController
        implements ListMenu.Listener,
        CountdownTimerPopup.Listener,
        ListSubMenu.Listener {
    private static String TAG = "PhotoMenu";

    private final String mSettingOff;
    private final String mSettingOn;

    private String[] mOtherKeys1;
    private String[] mOtherKeys2;
    private ListMenu mListMenu;
    private View mPreviewMenu;
    private static final int POPUP_NONE = 0;
    private static final int POPUP_FIRST_LEVEL = 1;
    private static final int POPUP_SECOND_LEVEL = 2;
    private static final int POPUP_IN_ANIMATION_SLIDE = 3;
    private static final int POPUP_IN_ANIMATION_FADE = 4;
    private static final int PREVIEW_MENU_NONE = 0;
    private static final int PREVIEW_MENU_IN_ANIMATION = 1;
    private static final int PREVIEW_MENU_ON = 2;
    private static final int MODE_SCENE = 0;
    private static final int MODE_FILTER = 1;
    private static final int DEVELOPER_MENU_TOUCH_COUNT = 10;
    private int mSceneStatus;
    private View mHdrSwitcher;
    private View mFrontBackSwitcher;
    private View mSceneModeSwitcher;
    private View mFilterModeSwitcher;
    private View mCameraSwitcher;
    private View mSettingMenu;
    private View mPreviewThumbnail;
    private PhotoUI mUI;
    private int mPopupStatus;
    private int mPreviewMenuStatus;
    private ListSubMenu mListSubMenu;
    private CameraActivity mActivity;
    private String mPrevSavedCDS;
    private boolean mIsTNREnabled = false;
    private boolean mIsCDSUpdated = false;
    private int privateCounter = 0;
    private static final int ANIMATION_DURATION = 300;
    private int previewMenuSize;
    private Rect mTmpRect = new Rect();

    public PhotoMenu(CameraActivity activity, PhotoUI ui) {
        super(activity);
        mUI = ui;
        mSettingOff = activity.getString(R.string.setting_off_value);
        mSettingOn = activity.getString(R.string.setting_on_value);
        mActivity = activity;
        mFrontBackSwitcher = ui.getRootView().findViewById(R.id.front_back_switcher);
        mHdrSwitcher = ui.getRootView().findViewById(R.id.hdr_switcher);
        mSceneModeSwitcher = ui.getRootView().findViewById(R.id.scene_mode_switcher);
        mFilterModeSwitcher = ui.getRootView().findViewById(R.id.filter_mode_switcher);
        mSettingMenu = ui.getRootView().findViewById(R.id.menu);
        mCameraSwitcher = ui.getRootView().findViewById(R.id.camera_switcher);
        mPreviewThumbnail = ui.getRootView().findViewById(R.id.preview_thumb);
    }

    public void initialize(PreferenceGroup group) {
        super.initialize(group);
        mListSubMenu = null;
        mListMenu = null;
        mPopupStatus = POPUP_NONE;
        mPreviewMenuStatus = POPUP_NONE;
        final Resources res = mActivity.getResources();
        Locale locale = res.getConfiguration().locale;
        // The order is from left to right in the menu.

        initSceneModeButton(mSceneModeSwitcher);
        initFilterModeButton(mFilterModeSwitcher);
        mHdrSwitcher.setVisibility(View.INVISIBLE);

        mFrontBackSwitcher.setVisibility(View.INVISIBLE);
        // HDR.
        if (group.findPreference(CameraSettings.KEY_CAMERA_HDR) != null) {
            mHdrSwitcher.setVisibility(View.VISIBLE);
            initSwitchItem(CameraSettings.KEY_CAMERA_HDR, mHdrSwitcher);
        } else {
            mHdrSwitcher.setVisibility(View.INVISIBLE);
        }

        mOtherKeys1 = new String[] {
                CameraSettings.KEY_SELFIE_FLASH,
                CameraSettings.KEY_FLASH_MODE,
                CameraSettings.KEY_RECORD_LOCATION,
                CameraSettings.KEY_PICTURE_SIZE,
                CameraSettings.KEY_JPEG_QUALITY,
                CameraSettings.KEY_TIMER,
                CameraSettings.KEY_CAMERA_SAVEPATH,
                CameraSettings.KEY_LONGSHOT,
                CameraSettings.KEY_FACE_DETECTION,
                CameraSettings.KEY_ISO,
                CameraSettings.KEY_EXPOSURE,
                CameraSettings.KEY_WHITE_BALANCE,
                CameraSettings.KEY_FOCUS_MODE,
                CameraSettings.KEY_FOCUS_TIME,
                CameraSettings.KEY_SHUTTER_SPEED,
                CameraSettings.KEY_REDEYE_REDUCTION,
                CameraSettings.KEY_POWER_SHUTTER,
                CameraSettings.KEY_MAX_BRIGHTNESS
        };

        mOtherKeys2 = new String[] {
                CameraSettings.KEY_SELFIE_FLASH,
                CameraSettings.KEY_FLASH_MODE,
                CameraSettings.KEY_RECORD_LOCATION,
                CameraSettings.KEY_PICTURE_SIZE,
                CameraSettings.KEY_JPEG_QUALITY,
                CameraSettings.KEY_TIMER,
                CameraSettings.KEY_CAMERA_SAVEPATH,
                CameraSettings.KEY_LONGSHOT,
                CameraSettings.KEY_FACE_DETECTION,
                CameraSettings.KEY_ISO,
                CameraSettings.KEY_EXPOSURE,
                CameraSettings.KEY_WHITE_BALANCE,
                CameraSettings.KEY_FOCUS_MODE,
                CameraSettings.KEY_FOCUS_TIME,
                CameraSettings.KEY_SHUTTER_SPEED,
                CameraSettings.KEY_REDEYE_REDUCTION,
                CameraSettings.KEY_POWER_SHUTTER,
                CameraSettings.KEY_MAX_BRIGHTNESS,
                CameraSettings.KEY_AUTO_HDR,
                CameraSettings.KEY_HDR_MODE,
                CameraSettings.KEY_HDR_NEED_1X,
                CameraSettings.KEY_CDS_MODE,
                CameraSettings.KEY_TNR_MODE,
                CameraSettings.KEY_HISTOGRAM,
                CameraSettings.KEY_ZSL,
                CameraSettings.KEY_TIMER_SOUND_EFFECTS,
                CameraSettings.KEY_FACE_RECOGNITION,
                CameraSettings.KEY_SELECTABLE_ZONE_AF,
                CameraSettings.KEY_PICTURE_FORMAT,
                CameraSettings.KEY_SATURATION,
                CameraSettings.KEY_CONTRAST,
                CameraSettings.KEY_SHARPNESS,
                CameraSettings.KEY_AUTOEXPOSURE,
                CameraSettings.KEY_ANTIBANDING,
                CameraSettings.KEY_DENOISE,
                CameraSettings.KEY_ADVANCED_FEATURES,
                CameraSettings.KEY_AE_BRACKET_HDR,
                CameraSettings.KEY_MANUAL_EXPOSURE,
                CameraSettings.KEY_MANUAL_WB,
                CameraSettings.KEY_MANUAL_FOCUS
        };

        initSwitchItem(CameraSettings.KEY_CAMERA_ID, mFrontBackSwitcher);
    }

    @Override
    // Hit when an item in a popup gets selected
    public void onListPrefChanged(ListPreference pref) {
        animateFadeOut(mListSubMenu, 2);
        onSettingChanged(pref);
        ((ListMenu) mListMenu).resetHighlight();
    }

    public boolean handleBackKey() {
        if (mPreviewMenuStatus == PREVIEW_MENU_ON) {
            animateSlideOut(mPreviewMenu);
            return true;
        }
        if (mPopupStatus == POPUP_NONE)
            return false;
        if (mPopupStatus == POPUP_FIRST_LEVEL) {
            animateSlideOut(mListMenu, 1);
        } else if (mPopupStatus == POPUP_SECOND_LEVEL) {
            animateFadeOut(mListSubMenu, 2);
            ((ListMenu) mListMenu).resetHighlight();
        }
        return true;
    }

    public void closeSceneMode() {
        mUI.removeSceneModeMenu();
    }

    public void tryToCloseSubList() {
        if (mListMenu != null)
            ((ListMenu) mListMenu).resetHighlight();

        if (mPopupStatus == POPUP_SECOND_LEVEL) {
            mUI.dismissLevel2();
            mPopupStatus = POPUP_FIRST_LEVEL;
        }
    }

    private void animateFadeOut(final ListView v, final int level) {
        if (v == null || mPopupStatus == POPUP_IN_ANIMATION_FADE)
            return;
        mPopupStatus = POPUP_IN_ANIMATION_FADE;

        ViewPropertyAnimator vp = v.animate();
        vp.alpha(0f).setDuration(ANIMATION_DURATION);
        vp.setListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (level == 1) {
                    mUI.dismissLevel1();
                    initializePopup();
                    mPopupStatus = POPUP_NONE;
                    mUI.cleanupListview();
                }
                else if (level == 2) {
                    mUI.dismissLevel2();
                    mPopupStatus = POPUP_FIRST_LEVEL;
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (level == 1) {
                    mUI.dismissLevel1();
                    initializePopup();
                    mPopupStatus = POPUP_NONE;
                    mUI.cleanupListview();
                }
                else if (level == 2) {
                    mUI.dismissLevel2();
                    mPopupStatus = POPUP_FIRST_LEVEL;
                }

            }
        });
        vp.start();
    }

    private void animateSlideOut(final ListView v, final int level) {
        if (v == null || mPopupStatus == POPUP_IN_ANIMATION_SLIDE)
            return;
        mPopupStatus = POPUP_IN_ANIMATION_SLIDE;

        ViewPropertyAnimator vp = v.animate();
        int sign = mUI.isRtl() ? -1 : 1;
        switch (mUI.getOrientation()) {
            case 0:
                vp.translationXBy(sign * -v.getWidth());
                break;
            case 90:
                vp.translationYBy(sign * 2 * v.getHeight());
                break;
            case 180:
                vp.translationXBy(sign * 2 * v.getWidth());
                break;
            case 270:
                vp.translationYBy(sign * -v.getHeight());
                break;
        }
        vp.setListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (level == 1) {
                    mUI.dismissLevel1();
                    initializePopup();
                    mPopupStatus = POPUP_NONE;
                    mUI.cleanupListview();
                }
                else if (level == 2) {
                    mUI.dismissLevel2();
                    mPopupStatus = POPUP_FIRST_LEVEL;
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (level == 1) {
                    mUI.dismissLevel1();
                    initializePopup();
                    mPopupStatus = POPUP_NONE;
                    mUI.cleanupListview();
                }
                else if (level == 2) {
                    mUI.dismissLevel2();
                    mPopupStatus = POPUP_FIRST_LEVEL;
                }

            }
        });
        vp.setDuration(ANIMATION_DURATION).start();
    }

    public void animateFadeIn(final ListView v) {
        ViewPropertyAnimator vp = v.animate();
        vp.alpha(1f).setDuration(ANIMATION_DURATION);
        vp.start();
    }

    public void animateSlideIn(final View v, int delta, boolean forcePortrait) {
        int orientation = mUI.getOrientation();
        if (!forcePortrait)
            orientation = 0;

        ViewPropertyAnimator vp = v.animate();
        int sign = mUI.isRtl() ? -1 : 1;
        float dest;

        switch (orientation) {
            case 0:
                dest = v.getX();
                v.setX(sign * (dest - delta));
                vp.translationX(dest);
                break;
            case 90:
                dest = v.getY();
                v.setY(sign * (dest + delta));
                vp.translationY(dest);
                break;
            case 180:
                dest = v.getX();
                v.setX(sign * (dest + delta));
                vp.translationX(dest);
                break;
            case 270:
                dest = v.getY();
                v.setY(sign * (dest - delta));
                vp.translationY(dest);
                break;
        }
        vp.setDuration(ANIMATION_DURATION).start();
    }

    public void animateSlideOutPreviewMenu() {
        if (mPreviewMenu == null)
            return;
        animateSlideOut(mPreviewMenu);
    }

    private void animateSlideOut(final View v) {
        if (v == null || mPreviewMenuStatus == PREVIEW_MENU_IN_ANIMATION)
            return;
        mPreviewMenuStatus = PREVIEW_MENU_IN_ANIMATION;

        ViewPropertyAnimator vp = v.animate()
                .translationXBy(-v.getWidth() * (mUI.isRtl() ? -1 : 1))
                .setDuration(ANIMATION_DURATION);
        vp.setListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                closeSceneMode();
                mPreviewMenuStatus = PREVIEW_MENU_NONE;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                closeSceneMode();
                mPreviewMenuStatus = PREVIEW_MENU_NONE;

            }
        });
        vp.start();
    }

    private void buttonSetEnabled(View v, boolean enable) {
        v.setEnabled(enable);
        if (v instanceof ViewGroup) {
            View v2 = ((ViewGroup) v).getChildAt(0);
            if (v2 != null)
                v2.setEnabled(enable);

        }

    }

    public boolean isOverMenu(MotionEvent ev) {
        if (mPopupStatus == POPUP_NONE
                || mPopupStatus == POPUP_IN_ANIMATION_SLIDE
                || mPopupStatus == POPUP_IN_ANIMATION_FADE)
            return false;
        return isOverView(mUI.getMenuLayout(), ev);
    }

    public boolean isOverPreviewMenu(MotionEvent ev) {
        if (mPreviewMenuStatus != PREVIEW_MENU_ON)
            return false;
        return isOverView(mUI.getPreviewMenuLayout(), ev);
    }

    private boolean isOverView(View view, MotionEvent ev) {
        if (view == null)
            return false;
        view.getHitRect(mTmpRect);
        return mTmpRect.contains((int) ev.getX(), (int) ev.getY());
    }

    public boolean isMenuBeingShown() {
        return mPopupStatus != POPUP_NONE;
    }

    public boolean isMenuBeingAnimated() {
        return mPopupStatus == POPUP_IN_ANIMATION_SLIDE || mPopupStatus == POPUP_IN_ANIMATION_FADE;
    }

    public boolean isPreviewMenuBeingShown() {
        return mPreviewMenuStatus == PREVIEW_MENU_ON;
    }

    public boolean isPreviewMenuBeingAnimated() {
        return mPreviewMenuStatus == PREVIEW_MENU_IN_ANIMATION;
    }

    public boolean sendTouchToPreviewMenu(MotionEvent ev) {
        return mUI.sendTouchToPreviewMenu(ev);
    }

    public boolean sendTouchToMenu(MotionEvent ev) {
        return mUI.sendTouchToMenu(ev);
    }

    @Override
    public void overrideSettings(final String... keyvalues) {
        if (mListMenu != null) {
            ListPreference pref_tnr = mPreferenceGroup.findPreference(CameraSettings.KEY_TNR_MODE);
            ListPreference pref_cds = mPreferenceGroup.findPreference(CameraSettings.KEY_CDS_MODE);

            String tnr = (pref_tnr != null) ? pref_tnr.getValue() : null;
            String cds = (pref_cds != null) ? pref_cds.getValue() : null;

            if (mPrevSavedCDS == null && cds != null) {
                mPrevSavedCDS = cds;
            }

            if ((tnr != null) && !mActivity.getString(R.string.
                    pref_camera_tnr_default).equals(tnr)) {
                mListMenu.setPreferenceEnabled(CameraSettings.KEY_CDS_MODE, false);
                mListMenu.overrideSettings(CameraSettings.KEY_CDS_MODE,
                        mActivity.getString(R.string.pref_camera_cds_value_off));
                mIsTNREnabled = true;
                if (!mIsCDSUpdated) {
                    if (cds != null) {
                        mPrevSavedCDS = cds;
                    }
                    mIsCDSUpdated = true;
                }
            } else if (tnr != null) {
                mListMenu.setPreferenceEnabled(CameraSettings.KEY_CDS_MODE, true);
                if (mIsTNREnabled && mPrevSavedCDS != cds) {
                    mListMenu.overrideSettings(CameraSettings.KEY_CDS_MODE, mPrevSavedCDS);
                    mIsTNREnabled = false;
                    mIsCDSUpdated = false;
                }
            }
        }
        for (int i = 0; i < keyvalues.length; i += 2) {
            if (keyvalues[i].equals(CameraSettings.KEY_SCENE_MODE)) {
                buttonSetEnabled(mSceneModeSwitcher, keyvalues[i + 1] == null);
            }
        }
        super.overrideSettings(keyvalues);
        if ((mListMenu == null))
            initializePopup();
        mListMenu.overrideSettings(keyvalues);
    }

    protected void initializePopup() {
        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        ListMenu popup1 = (ListMenu) inflater.inflate(
                R.layout.list_menu, null, false);

        popup1.setSettingChangedListener(this);

        String[] keys = mOtherKeys1;
        if (mActivity.isDeveloperMenuEnabled())
            keys = mOtherKeys2;
        popup1.initialize(mPreferenceGroup, keys);
        if (mActivity.isSecureCamera()) {
            // Prevent location preference from getting changed in secure camera
            // mode
            popup1.setPreferenceEnabled(CameraSettings.KEY_RECORD_LOCATION, false);
        }
        mListMenu = popup1;

        ListPreference pref = mPreferenceGroup.findPreference(
                CameraSettings.KEY_SCENE_MODE);
        updateFilterModeIcon(pref, mPreferenceGroup.findPreference(CameraSettings.KEY_CAMERA_HDR));
        String sceneMode = (pref != null) ? pref.getValue() : null;
        pref = mPreferenceGroup.findPreference(CameraSettings.KEY_FACE_DETECTION);
        String faceDetection = (pref != null) ? pref.getValue() : null;
        pref = mPreferenceGroup.findPreference(CameraSettings.KEY_ZSL);
        String zsl = (pref != null) ? pref.getValue() : null;
        pref = mPreferenceGroup.findPreference(CameraSettings.KEY_AUTO_HDR);
        String autohdr = (pref != null) ? pref.getValue() : null;
        if (((sceneMode != null) && !Parameters.SCENE_MODE_AUTO.equals(sceneMode))
                || ((autohdr != null) && autohdr.equals("enable"))) {
            popup1.setPreferenceEnabled(CameraSettings.KEY_FOCUS_MODE, false);
            popup1.setPreferenceEnabled(CameraSettings.KEY_AUTOEXPOSURE, false);
            popup1.setPreferenceEnabled(CameraSettings.KEY_SATURATION, false);
            popup1.setPreferenceEnabled(CameraSettings.KEY_CONTRAST, false);
            popup1.setPreferenceEnabled(CameraSettings.KEY_SHARPNESS, false);
            popup1.setPreferenceEnabled(CameraSettings.KEY_COLOR_EFFECT, false);
            popup1.setPreferenceEnabled(CameraSettings.KEY_FLASH_MODE, false);
            popup1.setPreferenceEnabled(CameraSettings.KEY_WHITE_BALANCE, false);
            popup1.setPreferenceEnabled(CameraSettings.KEY_EXPOSURE, false);
        }
        if ((autohdr != null) && autohdr.equals("enable")) {
            popup1.setPreferenceEnabled(CameraSettings.KEY_SCENE_MODE, false);
        }
        if ((zsl != null) && Parameters.ZSL_ON.equals(zsl)) {
            popup1.setPreferenceEnabled(CameraSettings.KEY_FOCUS_MODE, false);
            popup1.setPreferenceEnabled(CameraSettings.KEY_MANUAL_EXPOSURE, false);
            popup1.setPreferenceEnabled(CameraSettings.KEY_MANUAL_WB, false);
            popup1.setPreferenceEnabled(CameraSettings.KEY_MANUAL_FOCUS, false);
        }
        if ((faceDetection != null) && !Parameters.FACE_DETECTION_ON.equals(faceDetection)) {
            popup1.setPreferenceEnabled(CameraSettings.KEY_FACE_RECOGNITION, false);
        }
        popup1.setPreferenceEnabled(CameraSettings.KEY_ZSL, !mUI.isCountingDown());

        pref = mPreferenceGroup.findPreference(CameraSettings.KEY_ADVANCED_FEATURES);
        String advancedFeatures = (pref != null) ? pref.getValue() : null;

        String ubiFocusOn = mActivity.getString(R.string.
                pref_camera_advanced_feature_value_ubifocus_on);
        String reFocusOn = mActivity.getString(R.string.
                pref_camera_advanced_feature_value_refocus_on);
        String chromaFlashOn = mActivity.getString(R.string.
                pref_camera_advanced_feature_value_chromaflash_on);
        String optiZoomOn = mActivity.getString(R.string.
                pref_camera_advanced_feature_value_optizoom_on);
        String fssrOn = mActivity.getString(R.string.
                pref_camera_advanced_feature_value_FSSR_on);
        String truePortraitOn = mActivity.getString(R.string.
                pref_camera_advanced_feature_value_trueportrait_on);
        String multiTouchFocusOn = mActivity.getString(R.string.
                pref_camera_advanced_feature_value_multi_touch_focus_on);

        if ((zsl != null) && Parameters.ZSL_OFF.equals(zsl)) {
            popup1.overrideSettings(CameraSettings.KEY_ADVANCED_FEATURES,
                    mActivity.getString(R.string.pref_camera_advanced_feature_default));

            popup1.setPreferenceEnabled(CameraSettings.KEY_ADVANCED_FEATURES, false);
            if (mHdrSwitcher.getVisibility() == View.VISIBLE) {
                buttonSetEnabled(mHdrSwitcher, true);
            }
        } else {
            if ((advancedFeatures != null) && (advancedFeatures.equals(ubiFocusOn) ||
                    advancedFeatures.equals(chromaFlashOn) ||
                    advancedFeatures.equals(reFocusOn) ||
                    advancedFeatures.equals(optiZoomOn) ||
                    advancedFeatures.equals(fssrOn) ||
                    advancedFeatures.equals(truePortraitOn) ||
                    advancedFeatures.equals(multiTouchFocusOn))) {
                popup1.setPreferenceEnabled(CameraSettings.KEY_FOCUS_MODE, false);
                popup1.setPreferenceEnabled(CameraSettings.KEY_FLASH_MODE, false);
                popup1.setPreferenceEnabled(CameraSettings.KEY_AE_BRACKET_HDR, false);
                popup1.setPreferenceEnabled(CameraSettings.KEY_REDEYE_REDUCTION, false);
                popup1.setPreferenceEnabled(CameraSettings.KEY_EXPOSURE, false);
                popup1.setPreferenceEnabled(CameraSettings.KEY_COLOR_EFFECT, false);
                popup1.setPreferenceEnabled(CameraSettings.KEY_SCENE_MODE, false);

                setPreference(CameraSettings.KEY_CAMERA_HDR, mSettingOff);
                if (mHdrSwitcher.getVisibility() == View.VISIBLE) {
                    buttonSetEnabled(mHdrSwitcher, false);
                }
            } else {
                if (mHdrSwitcher.getVisibility() == View.VISIBLE) {
                    buttonSetEnabled(mHdrSwitcher, true);
                }
            }
        }

        if (mListener != null) {
            mListener.onSharedPreferenceChanged();
        }
    }

    private void updateFilterModeIcon(ListPreference scenePref, ListPreference hdrPref) {
        if (scenePref == null || hdrPref == null) return;
        if ((notSame(scenePref, CameraSettings.KEY_SCENE_MODE, Parameters.SCENE_MODE_AUTO))
                || (notSame(hdrPref, CameraSettings.KEY_CAMERA_HDR, mSettingOff))) {
            buttonSetEnabled(mFilterModeSwitcher, false);
        } else {
            buttonSetEnabled(mFilterModeSwitcher, true);
        }
    }

    public void initSwitchItem(final String prefKey, View switcher) {
        final IconListPreference pref =
                (IconListPreference) mPreferenceGroup.findPreference(prefKey);
        if (pref == null)
            return;

        int[] iconIds = pref.getLargeIconIds();
        int resid = -1;
        int index = pref.findIndexOfValue(pref.getValue());
        if (!pref.getUseSingleIcon() && iconIds != null) {
            // Each entry has a corresponding icon.
            resid = iconIds[index];
        } else {
            // The preference only has a single icon to represent it.
            resid = pref.getSingleIcon();
        }
        ((ImageView) switcher).setImageResource(resid);
        switcher.setVisibility(View.VISIBLE);
        mPreferences.add(pref);
        mPreferenceMap.put(pref, switcher);
        switcher.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                IconListPreference pref = (IconListPreference) mPreferenceGroup
                        .findPreference(prefKey);
                if (pref == null)
                    return;
                int index = pref.findIndexOfValue(pref.getValue());
                CharSequence[] values = pref.getEntryValues();
                index = (index + 1) % values.length;
                pref.setValueIndex(index);
                ((ImageView) v).setImageResource(
                        ((IconListPreference) pref).getLargeIconIds()[index]);
                if (prefKey.equals(CameraSettings.KEY_CAMERA_ID))
                    mListener.onCameraPickerClicked(index);
                reloadPreference(pref);
                onSettingChanged(pref);
            }
        });
    }

    public void initSceneModeButton(View button) {
        button.setVisibility(View.INVISIBLE);
        final IconListPreference pref = (IconListPreference) mPreferenceGroup
                .findPreference(CameraSettings.KEY_SCENE_MODE);
        if (pref == null)
            return;
        updateSceneModeIcon(pref);
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                addSceneMode();
                ViewGroup menuLayout = mUI.getPreviewMenuLayout();
                if (menuLayout != null) {
                    mUI.adjustOrientation();
                    animateSlideIn(menuLayout, previewMenuSize, false);
                }
            }
        });
    }

    public void addModeBack() {
        if (mSceneStatus == MODE_SCENE) {
            addSceneMode();
        }
        if (mSceneStatus == MODE_FILTER) {
            addFilterMode();
        }
    }

    public void addSceneMode() {
        final IconListPreference pref = (IconListPreference) mPreferenceGroup
                .findPreference(CameraSettings.KEY_SCENE_MODE);
        if (pref == null)
            return;

        int rotation = CameraUtil.getDisplayRotation(mActivity);
        boolean mIsDefaultToPortrait = CameraUtil.isDefaultToPortrait(mActivity);
        if (!mIsDefaultToPortrait) {
            rotation = (rotation + 90) % 360;
        }

        CharSequence[] entries = pref.getEntries();

        int[] thumbnails = pref.getThumbnailIds();

        Resources r = mActivity.getResources();
        int height = (int) (r.getDimension(R.dimen.scene_mode_height) + 2
                * r.getDimension(R.dimen.scene_mode_padding) + 1);
        int width = (int) (r.getDimension(R.dimen.scene_mode_width) + 2
                * r.getDimension(R.dimen.scene_mode_padding) + 1);

        int gridRes = 0;
        boolean portrait = (rotation == 0) || (rotation == 180);
        int size = height;
        if (portrait) {
            gridRes = R.layout.vertical_grid;
            size = width;
        } else {
            gridRes = R.layout.horiz_grid;
        }
        previewMenuSize = size;
        mUI.hideUI();
        mPreviewMenuStatus = PREVIEW_MENU_ON;
        mSceneStatus = MODE_SCENE;

        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        FrameLayout basic = (FrameLayout) inflater.inflate(
                gridRes, null, false);

        mUI.dismissSceneModeMenu();
        mUI.setPreviewMenuLayout(basic);
        ViewGroup.LayoutParams params = null;
        CameraRootView rootView = mUI.getRootView();
        if (portrait) {
            params = new ViewGroup.LayoutParams(size, LayoutParams.MATCH_PARENT);
        } else {
            params = new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, size);

            int rootViewBottom = rootView.getClientRectForOrientation(rotation).bottom;
            basic.setY(rootViewBottom - size);
        }
        basic.setLayoutParams(params);
        rootView.addView(basic);

        LinearLayout layout = (LinearLayout) basic.findViewById(R.id.layout);
        Rect insets = rootView.getInsetsForOrientation(rotation);
        layout.setPadding(insets.left, insets.top, insets.right, insets.bottom);

        final View[] views = new View[entries.length];
        int init = pref.getCurrentIndex();
        for (int i = 0; i < entries.length; i++) {
            RotateLayout layout2 = (RotateLayout) inflater.inflate(
                    R.layout.scene_mode_view, null, false);

            ImageView imageView = (ImageView) layout2.findViewById(R.id.image);
            TextView label = (TextView) layout2.findViewById(R.id.label);
            final int j = i;

            layout2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pref.setValueIndex(j);
                    onSettingChanged(pref);
                    updateSceneModeIcon(pref);
                    for (View v1 : views) {
                        v1.setActivated(v1 == v);
                    }
                }
            });

            views[j] = layout2;
            layout2.setActivated(i == init);
            imageView.setImageResource(thumbnails[i]);
            label.setText(entries[i]);
            layout.addView(layout2);
        }
        mPreviewMenu = basic;
    }

    public void updateSceneModeIcon(IconListPreference pref) {
        int[] thumbnails = pref.getThumbnailIds();
        int ind = pref.getCurrentIndex();
        if (ind == -1)
            ind = 0;
        ((ImageView) mSceneModeSwitcher).setImageResource(thumbnails[ind]);
    }

    public void initFilterModeButton(View button) {
        button.setVisibility(View.INVISIBLE);
        final IconListPreference pref = (IconListPreference) mPreferenceGroup
                .findPreference(CameraSettings.KEY_COLOR_EFFECT);
        if (pref == null)
            return;

        int[] iconIds = pref.getLargeIconIds();
        int resid = -1;
        // The preference only has a single icon to represent it.
        resid = pref.getSingleIcon();
        ((ImageView) button).setImageResource(resid);
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                addFilterMode();
                ViewGroup menuLayout = mUI.getPreviewMenuLayout();
                if (menuLayout != null) {
                    mUI.adjustOrientation();
                    animateSlideIn(menuLayout, previewMenuSize, false);
                }
            }
        });
    }

    public void addFilterMode() {
        final IconListPreference pref = (IconListPreference) mPreferenceGroup
                .findPreference(CameraSettings.KEY_COLOR_EFFECT);
        if (pref == null)
            return;

        int rotation = CameraUtil.getDisplayRotation(mActivity);
        boolean mIsDefaultToPortrait = CameraUtil.isDefaultToPortrait(mActivity);
        if (!mIsDefaultToPortrait) {
            rotation = (rotation + 90) % 360;
        }
        CharSequence[] entries = pref.getEntries();

        Resources r = mActivity.getResources();
        int height = (int) (r.getDimension(R.dimen.filter_mode_height) + 2
                * r.getDimension(R.dimen.filter_mode_padding) + 1);
        int width = (int) (r.getDimension(R.dimen.filter_mode_width) + 2
                * r.getDimension(R.dimen.filter_mode_padding) + 1);

        int gridRes = 0;
        boolean portrait = (rotation == 0) || (rotation == 180);
        int size = height;
        if (portrait) {
            gridRes = R.layout.vertical_grid;
            size = width;
        } else {
            gridRes = R.layout.horiz_grid;
        }
        previewMenuSize = size;
        mUI.hideUI();
        mPreviewMenuStatus = PREVIEW_MENU_ON;
        mSceneStatus = MODE_FILTER;

        int[] thumbnails = pref.getThumbnailIds();

        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        FrameLayout basic = (FrameLayout) inflater.inflate(
                gridRes, null, false);

        mUI.dismissSceneModeMenu();
        mUI.setPreviewMenuLayout(basic);
        ViewGroup.LayoutParams params = null;
        CameraRootView rootView = mUI.getRootView();
        if (portrait) {
            params = new ViewGroup.LayoutParams(size, LayoutParams.MATCH_PARENT);
        } else {
            params = new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, size);

            int rootViewBottom = rootView.getClientRectForOrientation(rotation).bottom;
            basic.setY(rootViewBottom - size);
        }
        basic.setLayoutParams(params);
        rootView.addView(basic);

        LinearLayout layout = (LinearLayout) basic.findViewById(R.id.layout);
        Rect insets = rootView.getInsetsForOrientation(rotation);
        layout.setPadding(insets.left, insets.top, insets.right, insets.bottom);

        final View[] views = new View[entries.length];
        int init = pref.getCurrentIndex();
        for (int i = 0; i < entries.length; i++) {
            RotateLayout layout2 = (RotateLayout) inflater.inflate(
                    R.layout.filter_mode_view, null, false);
            ImageView imageView = (ImageView) layout2.findViewById(R.id.image);
            final int j = i;

            layout2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pref.setValueIndex(j);
                    onSettingChanged(pref);
                    for (View v1 : views) {
                        v1.setActivated(v1 == v);
                    }
                }
            });

            views[j] = layout2;
            layout2.setActivated(i == init);
            TextView label = (TextView) layout2.findViewById(R.id.label);
            imageView.setImageResource(thumbnails[i]);
            label.setText(entries[i]);
            layout.addView(layout2);
        }
        mPreviewMenu = basic;
    }

    public void openFirstLevel() {
        if (isMenuBeingShown() || mUI.isCameraControlsAnimating())
            return;
        if (mListMenu == null || mPopupStatus != POPUP_FIRST_LEVEL) {
            initializePopup();
            mPopupStatus = POPUP_FIRST_LEVEL;
        }
        mUI.showPopup(mListMenu, 1, true);

    }

    public void popupDismissed(boolean dismissAll) {
        if (!dismissAll && mPopupStatus == POPUP_SECOND_LEVEL) {
            initializePopup();
            mPopupStatus = POPUP_FIRST_LEVEL;
            mUI.showPopup(mListMenu, 1, false);
            if (mListMenu != null)
                mListMenu = null;

        } else {
            initializePopup();
        }

    }

    @Override
    // Hit when an item in the first-level popup gets selected, then bring up
    // the second-level popup
    public void onPreferenceClicked(ListPreference pref) {
        onPreferenceClicked(pref, 0);
    }

    public void onPreferenceClicked(ListPreference pref, int y) {
        if (!mActivity.isDeveloperMenuEnabled()) {
            if (pref.getKey().equals(CameraSettings.KEY_REDEYE_REDUCTION)) {
                privateCounter++;
                if (privateCounter >= DEVELOPER_MENU_TOUCH_COUNT) {
                    mActivity.enableDeveloperMenu();
                    SharedPreferences prefs = PreferenceManager
                            .getDefaultSharedPreferences(mActivity);
                    prefs.edit().putBoolean(CameraSettings.KEY_DEVELOPER_MENU, true).apply();
                    RotateTextToast.makeText(mActivity,
                            "Camera developer option is enabled now", Toast.LENGTH_SHORT).show();
                }
            } else {
                privateCounter = 0;
            }
        }

        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        ListSubMenu basic = (ListSubMenu) inflater.inflate(
                R.layout.list_sub_menu, null, false);
        basic.initialize(pref, y);
        basic.setSettingChangedListener(this);
        basic.setAlpha(0f);
        mListSubMenu = basic;
        mUI.removeLevel2();
        if (mPopupStatus == POPUP_SECOND_LEVEL) {
            mUI.showPopup(mListSubMenu, 2, false);
        } else {
            mUI.showPopup(mListSubMenu, 2, true);
        }
        mPopupStatus = POPUP_SECOND_LEVEL;
    }

    public void onListMenuTouched() {
        mUI.removeLevel2();
    }

    public void closeAllView() {
        if (mUI != null)
            mUI.removeLevel2();

        if (mListMenu != null) {
            animateSlideOut(mListMenu, 1);
        }
        animateSlideOutPreviewMenu();
    }

    public void closeView() {
        if (mUI != null)
            mUI.removeLevel2();

        if (mListMenu != null && mPopupStatus != POPUP_NONE)
            animateSlideOut(mListMenu, 1);
    }

    // Return true if the preference has the specified key but not the value.
    private static boolean notSame(ListPreference pref, String key, String value) {
        return (key.equals(pref.getKey()) && !value.equals(pref.getValue()));
    }

    // Return true if the preference has the specified key and the value.
    private static boolean same(ListPreference pref, String key, String value) {
        return (key.equals(pref.getKey()) && value.equals(pref.getValue()));
    }

    public void setPreference(String key, String value) {
        ListPreference pref = mPreferenceGroup.findPreference(key);
        if (pref != null && !value.equals(pref.getValue())) {
            pref.setValue(value);
            reloadPreferences();
        }
    }

    @Override
    public void onSettingChanged(ListPreference pref) {
        // Reset the scene mode if HDR is set to on. Reset HDR if scene mode is
        // set to non-auto.
        if (same(pref, CameraSettings.KEY_SCENE_MODE, Parameters.SCENE_MODE_HDR)) {
            ListPreference hdrPref =
                    mPreferenceGroup.findPreference(CameraSettings.KEY_CAMERA_HDR);
            if (hdrPref != null && same(hdrPref, CameraSettings.KEY_CAMERA_HDR, mSettingOff)) {
                setPreference(CameraSettings.KEY_CAMERA_HDR, mSettingOn);
            }
        } else if (notSame(pref, CameraSettings.KEY_SCENE_MODE, Parameters.SCENE_MODE_HDR)) {
            ListPreference hdrPref =
                    mPreferenceGroup.findPreference(CameraSettings.KEY_CAMERA_HDR);
            if (hdrPref != null && notSame(hdrPref, CameraSettings.KEY_CAMERA_HDR, mSettingOff)) {
                setPreference(CameraSettings.KEY_CAMERA_HDR, mSettingOff);
            }
        } else if (same(pref, CameraSettings.KEY_CAMERA_HDR, mSettingOff)) {
            ListPreference scenePref =
                    mPreferenceGroup.findPreference(CameraSettings.KEY_SCENE_MODE);
            if (scenePref != null && notSame(scenePref, CameraSettings.KEY_SCENE_MODE, Parameters.SCENE_MODE_AUTO)) {
                setPreference(CameraSettings.KEY_SCENE_MODE, Parameters.SCENE_MODE_AUTO);
            }
            updateSceneModeIcon((IconListPreference) scenePref);
	} else if (same(pref, CameraSettings.KEY_CAMERA_HDR, mSettingOn)) {
            ListPreference scenePref =
                    mPreferenceGroup.findPreference(CameraSettings.KEY_SCENE_MODE);
            if (scenePref != null && notSame(scenePref, CameraSettings.KEY_SCENE_MODE, Parameters.SCENE_MODE_HDR)) {
                setPreference(CameraSettings.KEY_SCENE_MODE, Parameters.SCENE_MODE_HDR);
            }
            updateSceneModeIcon((IconListPreference) scenePref);
	} else if (notSame(pref,CameraSettings.KEY_AE_BRACKET_HDR,"Off")) {
            RotateTextToast.makeText(mActivity,
                           R.string.flash_aebracket_message,Toast.LENGTH_SHORT).show();
            setPreference(CameraSettings.KEY_FLASH_MODE,Parameters.FLASH_MODE_OFF);
        } else if (notSame(pref,CameraSettings.KEY_FLASH_MODE,"Off")) {
            ListPreference aePref =
                      mPreferenceGroup.findPreference(CameraSettings.KEY_AE_BRACKET_HDR);
            if (aePref != null) {
                if (notSame(aePref,CameraSettings.KEY_AE_BRACKET_HDR,"Off")) {
                   RotateTextToast.makeText(mActivity,
                                  R.string.flash_aebracket_message,Toast.LENGTH_SHORT).show();
                }
            }
        } else if (notSame(pref, CameraSettings.KEY_LONGSHOT, mSettingOff)) {
            ListPreference advancefeaturePref =
                    mPreferenceGroup.findPreference(CameraSettings.KEY_ADVANCED_FEATURES);
            if (advancefeaturePref != null) {
                if (notSame(advancefeaturePref, CameraSettings.KEY_ADVANCED_FEATURES,
                        mActivity.getString(R.string.pref_camera_advanced_feature_default))) {
                    RotateTextToast.makeText(mActivity, R.string.longshot_enable_message,
                            Toast.LENGTH_LONG).show();
                }
                setPreference(CameraSettings.KEY_ADVANCED_FEATURES,
                        mActivity.getString(R.string.pref_camera_advanced_feature_default));
            }
        } else if (notSame(pref, CameraSettings.KEY_ADVANCED_FEATURES,
                mActivity.getString(R.string.pref_camera_advanced_feature_default))) {
            ListPreference longshotPref =
                    mPreferenceGroup.findPreference(CameraSettings.KEY_LONGSHOT);
            if (longshotPref != null ) {
                if (notSame(longshotPref, CameraSettings.KEY_LONGSHOT, mSettingOff)) {
                    RotateTextToast.makeText(mActivity, R.string.advance_feature_enable_msg,
                            Toast.LENGTH_LONG).show();
                }
                setPreference(CameraSettings.KEY_LONGSHOT, mSettingOff);
            }
        }

        String refocusOn = mActivity.getString(R.string
                .pref_camera_advanced_feature_value_refocus_on);
        if (notSame(pref, CameraSettings.KEY_SCENE_MODE, refocusOn)) {
            ListPreference lp = mPreferenceGroup
                    .findPreference(CameraSettings.KEY_ADVANCED_FEATURES);
            if (lp != null && refocusOn.equals(lp.getValue())) {
                setPreference(CameraSettings.KEY_ADVANCED_FEATURES,
                        mActivity.getString(R.string.pref_camera_advanced_feature_default));
            }
        }
        updateFilterModeIcon(pref, pref);
        super.onSettingChanged(pref);
    }

    public int getOrientation() {
        return mUI == null ? 0 : mUI.getOrientation();
    }

    public void hideCameraControls(boolean hide) {
        final int status = (hide) ? View.INVISIBLE : View.VISIBLE;
        mSettingMenu.setVisibility(status);
        mFrontBackSwitcher.setVisibility(status);
        mHdrSwitcher.setVisibility(status);
        mSceneModeSwitcher.setVisibility(status);
        mFilterModeSwitcher.setVisibility(status);
        mCameraSwitcher.setVisibility(status);
        mPreviewThumbnail.setVisibility(status);
    }
}
