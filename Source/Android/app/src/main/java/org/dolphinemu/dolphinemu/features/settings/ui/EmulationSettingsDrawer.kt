package org.dolphinemu.dolphinemu.features.settings.ui

import android.content.res.Configuration
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.navigation.NavigationView
import com.google.android.material.slider.Slider
import org.dolphinemu.dolphinemu.NativeLibrary
import org.dolphinemu.dolphinemu.R
import org.dolphinemu.dolphinemu.activities.EmulationActivity
import org.dolphinemu.dolphinemu.overlay.InputOverlay
import org.dolphinemu.dolphinemu.utils.Rumble

class EmulationSettingsDrawer(
    private val activity: EmulationActivity,
    private val drawerLayout: DrawerLayout,
    private val navigationView: NavigationView
) {
    private var handler: Handler = Handler(Looper.getMainLooper())
    private var heapInfoRunnable: Runnable? = null
    private var runningSettings: IntArray? = null

    init {
        setupNavigationView()
        setupHeapInfo()
        loadRunningSettings()
    }

    private fun loadRunningSettings() {
        runningSettings = NativeLibrary.getRunningSettings()
    }

    private fun setupNavigationView() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawer(GravityCompat.END)

            when (menuItem.itemId) {
                R.id.nav_screenshot -> {
                    NativeLibrary.SaveScreenShot()
                    true
                }
                R.id.nav_quicksave -> {
                    NativeLibrary.SaveState(0, false)
                    true
                }
                R.id.nav_quickload -> {
                    NativeLibrary.LoadState(0)
                    true
                }
                R.id.nav_savestates -> {
                    activity.showStateSaves()
                    true
                }
                R.id.nav_edit_controls -> {
                    activity.editControlsPlacement()
                    true
                }
                R.id.nav_toggle_controls -> {
                    activity.toggleControls()
                    true
                }
                R.id.nav_adjust_controls -> {
                    activity.adjustControls()
                    true
                }
                R.id.nav_settings -> {
                    showSettingsBottomSheet()
                    true
                }
                R.id.nav_exit -> {
                    activity.exitEmulation()
                    true
                }
                else -> false
            }
        }
    }

    private fun showSettingsBottomSheet() {
        val bottomSheet = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_emulation_settings, null)
        val dialog = BottomSheetDialog(activity)
        dialog.setContentView(bottomSheet)

        // Load settings in the same order as RunningSettingDialog
        loadRunningSettings()

        // Graphics Settings (following the order in RunningSettingDialog)
        setupSwitch(bottomSheet, R.id.switch_show_fps, 0)        // Show FPS
        setupSwitch(bottomSheet, R.id.switch_skip_efb, 1)        // Skip EFB Access
        setupSwitch(bottomSheet, R.id.switch_efb_texture, 2)     // EFB Copy Method
        setupSwitch(bottomSheet, R.id.switch_vi_skip, 3)         // VI Skip
        setupSwitch(bottomSheet, R.id.switch_ignore_format, 4)   // Ignore Format Changes
        setupSwitch(bottomSheet, R.id.switch_arbitrary_mipmap, 5) // Arbitrary Mipmap
        setupSwitch(bottomSheet, R.id.switch_immediate_xfb, 6)   // Immediate XFB

        // Display Scale
        setupSlider(
            bottomSheet,
            R.id.seekbar_display_scale,
            R.id.text_display_scale_value,
            7,
            200,
            runningSettings!![7]
        ) { progress -> updateSetting(7, progress) }

        // Core Settings
        setupSwitch(bottomSheet, R.id.switch_sync_skip_idle, 8)  // Sync on Skip Idle
        setupSwitch(bottomSheet, R.id.switch_overclock_enable, 9) // Overclock Enable
        setupSwitch(bottomSheet, R.id.switch_jit_follow_branch, 11) // JIT Follow Branch

        // Preference Settings
        setupSwitch(bottomSheet, R.id.switch_phone_rumble, 100)  // Phone Rumble
        setupSwitch(bottomSheet, R.id.switch_touch_pointer, 101) // Touch Pointer
        setupSwitch(bottomSheet, R.id.switch_pointer_recenter, 102) // Pointer Recenter
        setupSwitch(bottomSheet, R.id.switch_joystick_rel, 103)  // Joystick Relative

        // IR Settings for Wii games
        val irContainer = bottomSheet.findViewById<View>(R.id.ir_settings_container)
        if (!activity.isGameCubeGame && runningSettings!!.size > 14) {
            // IR Touch Mode Radio Group
            setupRadioGroup(bottomSheet, R.id.radio_group_ir_mode, R.id.radio_ir_click, R.id.radio_ir_stick)

            setupSlider(
                bottomSheet,
                R.id.seekbar_ir_pitch,
                R.id.text_ir_pitch_value,
                12,
                50,
                runningSettings!![12]
            ) { progress -> updateSetting(12, progress) }

            setupSlider(
                bottomSheet,
                R.id.seekbar_ir_yaw,
                R.id.text_ir_yaw_value,
                13,
                50,
                runningSettings!![13]
            ) { progress -> updateSetting(13, progress) }

            setupSlider(
                bottomSheet,
                R.id.seekbar_ir_vertical,
                R.id.text_ir_vertical_value,
                14,
                50,
                runningSettings!![14]
            ) { progress -> updateSetting(14, progress) }
        } else {
            irContainer.visibility = View.GONE
        }

        setupDialogBehavior(dialog, bottomSheet)
    }

    private fun setupDialogBehavior(dialog: BottomSheetDialog, bottomSheet: View) {
        val isLandscape = activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (isLandscape) {
            val displayMetrics = activity.resources.displayMetrics
            bottomSheet.layoutParams.height = (displayMetrics.heightPixels * 0.9).toInt()
        }

        val behavior = BottomSheetBehavior.from(bottomSheet.parent as View)
        behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            peekHeight = ViewGroup.LayoutParams.WRAP_CONTENT
            isHideable = true
            skipCollapsed = true

            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        dialog.dismiss()
                    }
                }
                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })
        }

        dialog.show()
    }

    private fun setupSwitch(root: View, switchId: Int, settingIndex: Int) {
        val switch = root.findViewById<MaterialSwitch>(switchId)

        when (settingIndex) {
            100 -> { // Phone Rumble
                val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
                switch.isChecked = prefs.getBoolean(EmulationActivity.RUMBLE_PREF_KEY, true)
                switch.setOnCheckedChangeListener { _, isChecked ->
                    prefs.edit().putBoolean(EmulationActivity.RUMBLE_PREF_KEY, isChecked).apply()
                    Rumble.setPhoneRumble(activity, isChecked)
                }
            }
            101 -> { // Touch Pointer
                val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
                switch.isChecked = prefs.getBoolean(InputOverlay.POINTER_PREF_KEY, true)
                switch.setOnCheckedChangeListener { _, isChecked ->
                    prefs.edit().putBoolean(InputOverlay.POINTER_PREF_KEY, isChecked).apply()
                    activity.setTouchPointer(if (isChecked) 1 else 0)
                }
            }
            102 -> { // Pointer Recenter
                val gameId = activity.selectedGameId
                val prefId = if (gameId!!.length > 3) gameId.substring(0, 3) else gameId
                val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
                switch.isChecked = prefs.getBoolean(InputOverlay.RECENTER_PREF_KEY + "_" + prefId, true)
                switch.setOnCheckedChangeListener { _, isChecked ->
                    prefs.edit().putBoolean(InputOverlay.RECENTER_PREF_KEY + "_" + prefId, isChecked).apply()
                    InputOverlay.sIRRecenter = isChecked
                }
            }
            103 -> { // Joystick Relative
                val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
                switch.isChecked = prefs.getBoolean(InputOverlay.RELATIVE_PREF_KEY, true)
                switch.setOnCheckedChangeListener { _, isChecked ->
                    prefs.edit().putBoolean(InputOverlay.RELATIVE_PREF_KEY, isChecked).apply()
                    InputOverlay.sJoystickRelative = isChecked
                }
            }
            else -> {
                // Handle regular emulation settings
                switch.isChecked = runningSettings!![settingIndex] > 0
                switch.setOnCheckedChangeListener { _, isChecked ->
                    updateSetting(settingIndex, if (isChecked) 1 else 0)
                }
            }
        }
    }

    private fun setupSlider(
        root: View,
        sliderId: Int,
        valueTextId: Int,
        defaultValue: Int,
        maxValue: Int,
        currentValue: Int,
        callback: (Int) -> Unit
    ) {
        val slider = root.findViewById<Slider>(sliderId)
        val valueText = root.findViewById<TextView>(valueTextId)

        slider.valueFrom = 0f
        slider.valueTo = maxValue.toFloat()
        slider.value = currentValue.toFloat()
        valueText.text = "${currentValue}%"

        slider.addOnChangeListener { _, value, fromUser ->
            val progress = value.toInt()
            valueText.text = "${progress}%"
            if (fromUser) {
                callback(progress)
            }
        }
    }

    private fun updateSetting(index: Int, value: Int) {
        if (runningSettings!![index] != value) {
            runningSettings!![index] = value
            NativeLibrary.setRunningSettings(runningSettings)
            if (index == 7) { // Display scale
                activity.updateTouchPointer()
            }
        }
    }

    private fun setupHeapInfo() {
        val infoText = navigationView.getHeaderView(0).findViewById<TextView>(R.id.text_info)

        heapInfoRunnable = object : Runnable {
            override fun run() {
                val heapSize = Debug.getNativeHeapAllocatedSize() shr 20
                infoText.text = String.format("%dMB", heapSize)
                handler.postDelayed(this, 1000)
            }
        }

        heapInfoRunnable?.let { handler.post(it) }
    }

    fun cleanup() {
        heapInfoRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun setupRadioGroup(root: View, groupId: Int, clickId: Int, stickId: Int) {
        val radioGroup = root.findViewById<RadioGroup>(groupId)
        val clickRadio = root.findViewById<RadioButton>(clickId)
        val stickRadio = root.findViewById<RadioButton>(stickId)

        clickRadio.setText(R.string.touch_ir_click)
        stickRadio.setText(R.string.touch_ir_stick)

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val value = if (checkedId == clickId) 0 else 1
            updateSetting(101, value) // Touch pointer mode setting index
        }
    }
}