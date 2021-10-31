package com.github.gouravkhunger.jekyllex.ui.settings

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import com.github.gouravkhunger.fontize.Fontize
import com.github.gouravkhunger.fontize.changeFont
import com.github.gouravkhunger.jekyllex.R
import com.github.gouravkhunger.jekyllex.databinding.ActivitySettingsBinding
import com.github.gouravkhunger.jekyllex.ui.auth.AuthActivity
import com.google.android.material.snackbar.Snackbar
import okhttp3.internal.toHexString
import vadiole.colorpicker.ColorModel
import vadiole.colorpicker.ColorPickerDialog

private const val TITLE_TAG = "settingsActivityTitle"

class SettingsActivity :
    AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private lateinit var settingsBinding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsBinding = ActivitySettingsBinding.inflate(layoutInflater)
        setTheme(R.style.Theme_JekyllEx)
        setContentView(settingsBinding.root)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(settingsBinding.settingsLayout.id, HeaderFragment())
                .commit()
        } else {
            settingsBinding.settingToolbarText.text = savedInstanceState.getCharSequence(TITLE_TAG)
        }

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                settingsBinding.settingToolbarText.text = getText(R.string.settings)
            }
        }

        setSupportActionBar(settingsBinding.toolbarSettings)
        supportActionBar?.setHomeButtonEnabled(true)
        settingsBinding.toolbarSettings.setNavigationIcon(R.drawable.ic_back)
        settingsBinding.toolbarSettings.setNavigationOnClickListener {
            onBackPressed()
        }
        settingsBinding.toolbarSettings.applyFont()
    }

    override fun onBackPressed() {
        finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, title)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.popBackStackImmediate()) {
            return true
        }
        return super.onSupportNavigateUp()
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        // Instantiate the new Fragment
        val args = pref.extras
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            pref.fragment
        ).apply {
            arguments = args
            setTargetFragment(caller, 0)
        }

        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
            .replace(settingsBinding.settingsLayout.id, fragment)
            .addToBackStack(null)
            .commit()

        settingsBinding.settingToolbarText.text = pref.title

        return true
    }

    class HeaderFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.header_preferences, rootKey)
            val defaultNumberPref = findPreference("default_load_count") as SeekBarPreference?
            var initialCount = defaultNumberPref?.value

            defaultNumberPref?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue as Int != initialCount) {
                    initialCount = newValue
                    Snackbar.make(
                        requireActivity().findViewById(R.id.settingsActivityRoot),
                        "Restart the app to apply changes.",
                        Snackbar.LENGTH_LONG
                    ).setAction("Restart") {
                        requireActivity().finishAffinity()
                        startActivity(Intent(requireActivity(), AuthActivity::class.java))
                    }.show()
                }
                true
            }
        }
    }

    class FontFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.font_preferences, rootKey)

            val fontPref = findPreference("font") as ListPreference?
            var oldValue = fontPref!!.value

            fontPref.setOnPreferenceChangeListener { _, newValue ->
                if (newValue != oldValue) {
                    Snackbar.make(
                        requireActivity().findViewById(R.id.settingsActivityRoot),
                        "Restart the app to apply changes.",
                        Snackbar.LENGTH_LONG
                    ).setAction("Restart") {
                        requireActivity().finishAffinity()
                        startActivity(Intent(requireActivity(), AuthActivity::class.java))
                    }.also {
                        it.changeFont(requireContext())
                    }.show()

                    when (newValue) {
                        "josefin" -> Fontize(requireContext()).updateFont(R.font.josefinsans)
                        "libre" -> Fontize(requireContext()).updateFont(R.font.libre_baskerville)
                    }
                }

                oldValue = newValue.toString()

                true
            }
        }
    }

    class ColourFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.color_preferences, rootKey)

            val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext())

            var initialPrimaryAppColor = sharedPref.getString("primaryAppColor", "#000000")
            val primaryAppColorPref = findPreference("primaryAppColor") as Preference?
            primaryAppColorPref!!.summary = initialPrimaryAppColor

            var initialPrimaryTextColor = sharedPref.getString("primaryTextColor", "#ffffff")
            val primaryTextColorPref = findPreference("primaryTextColor") as Preference?
            primaryTextColorPref!!.summary = initialPrimaryTextColor

            primaryAppColorPref.setOnPreferenceClickListener {
                val colorPicker: ColorPickerDialog = ColorPickerDialog.Builder()
                    .setInitialColor(Color.parseColor(initialPrimaryAppColor))
                    .setColorModel(ColorModel.RGB)
                    .setColorModelSwitchEnabled(true)
                    .setButtonOkText(android.R.string.ok)
                    .setButtonCancelText(android.R.string.cancel)
                    .onColorSelected { color: Int ->
                        val colorHex = "#${color.toHexString().substring(2)}"

                        if (initialPrimaryAppColor != colorHex) {
                            initialPrimaryAppColor = colorHex
                            sharedPref.edit().putString("primaryAppColor", colorHex).apply()
                            primaryAppColorPref.summary = initialPrimaryAppColor

                            Snackbar.make(
                                requireActivity().findViewById(R.id.settingsActivityRoot),
                                "Restart the app to apply changes.",
                                Snackbar.LENGTH_LONG
                            ).setAction("Restart") {
                                requireActivity().finishAffinity()
                                startActivity(Intent(requireActivity(), AuthActivity::class.java))
                            }.also {
                                it.changeFont(requireContext())
                            }.show()
                        }
                    }
                    .create()

                colorPicker.show(childFragmentManager, "color_picker")

                true
            }

            primaryTextColorPref.setOnPreferenceClickListener {
                val colorPicker: ColorPickerDialog = ColorPickerDialog.Builder()
                    .setInitialColor(Color.parseColor(initialPrimaryTextColor))
                    .setColorModel(ColorModel.RGB)
                    .setColorModelSwitchEnabled(true)
                    .setButtonOkText(android.R.string.ok)
                    .setButtonCancelText(android.R.string.cancel)
                    .onColorSelected { color: Int ->
                        val colorHex = "#${color.toHexString().substring(2)}"

                        if (initialPrimaryTextColor != colorHex) {
                            initialPrimaryTextColor = colorHex
                            sharedPref.edit().putString("primaryTextColor", colorHex).apply()
                            primaryTextColorPref.summary = initialPrimaryTextColor

                            Snackbar.make(
                                requireActivity().findViewById(R.id.settingsActivityRoot),
                                "Restart the app to apply changes.",
                                Snackbar.LENGTH_LONG
                            ).setAction("Restart") {
                                requireActivity().finishAffinity()
                                startActivity(Intent(requireActivity(), AuthActivity::class.java))
                            }.also {
                                it.changeFont(requireContext())
                            }.show()
                        }
                    }
                    .create()

                colorPicker.show(childFragmentManager, "color_picker")

                true
            }
        }
    }
}
