/**
 * Preference Interface.
 * All iptables "communication" is handled by this class.
 * <p>
 * Copyright (C) 2011-2012  Umakanthan Chandran
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Umakanthan Chandran
 * @version 1.0
 */

package dev.ukanth.ufirewall.preferences;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.List;

import dev.ukanth.ufirewall.Api;
import dev.ukanth.ufirewall.R;
import dev.ukanth.ufirewall.events.LogChangeEvent;
import dev.ukanth.ufirewall.events.RulesEvent;
import dev.ukanth.ufirewall.events.RxEvent;
import dev.ukanth.ufirewall.service.LogService;
import dev.ukanth.ufirewall.service.RootCommand;
import dev.ukanth.ufirewall.util.FingerprintUtil;
import dev.ukanth.ufirewall.util.G;
import haibison.android.lockpattern.LockPatternActivity;

import static dev.ukanth.ufirewall.util.G.isDonate;
import static haibison.android.lockpattern.LockPatternActivity.ACTION_COMPARE_PATTERN;
import static haibison.android.lockpattern.LockPatternActivity.EXTRA_PATTERN;

public class PreferencesActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final boolean ALWAYS_SIMPLE_PREFS = false;
    private Toolbar mToolBar;
    private static final int REQ_ENTER_PATTERN = 9755;
    private static final int LOCK_VERIFICATION = 1212;

    RxEvent rxEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // set language
        Api.updateLanguage(getApplicationContext(), G.locale());
        super.onCreate(savedInstanceState);
        prepareLayout();
        subscribe();

        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            Object data = bundle.get("validate");
            if(data != null){
                String check = (String) data;
                if(check.equals("yes")) {
                    passCheck();
                }
            }
        }
    }

    private void subscribe() {
        rxEvent = new RxEvent();
        rxEvent.subscribe(event -> {
            if (event instanceof RulesEvent) {
                ruleChangeApplyRules((RulesEvent) event);
            } else if (event instanceof LogChangeEvent) {
                logDmesgChangeApplyRules((LogChangeEvent) event);
            }
        });
    }

    private void ruleChangeApplyRules(RulesEvent rulesEvent) {
        final Context context = rulesEvent.ctx;
        Api.applySavedIptablesRules(context, false, new RootCommand()
                .setFailureToast(R.string.error_apply)
                .setCallback(new RootCommand.Callback() {
                    @Override
                    public void cbFunc(RootCommand state) {
                        if (state.exitCode == 0) {
                            Log.i(Api.TAG, "Rules applied successfully during preference change");
                        } else {
                            // error details are already in logcat
                            Log.i(Api.TAG, "Error applying rules during preference change");
                        }
                    }
                }));
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }


    private void prepareLayout() {
        ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        View content = root.getChildAt(0);
        LinearLayout toolbarContainer = (LinearLayout) View.inflate(this, R.layout.activity_prefs, null);

        root.removeAllViews();
        toolbarContainer.addView(content);
        root.addView(toolbarContainer);

        mToolBar = (Toolbar) toolbarContainer.findViewById(R.id.toolbar);
        mToolBar.setTitle(getTitle() + " " + getString(R.string.preferences));
        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        // Allow super to try and create a view first
        final View result = super.onCreateView(name, context, attrs);
        if (result != null) {
            return result;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // If we're running pre-L, we need to 'inject' our tint aware Views in place of the
            // standard framework versions
            switch (name) {
                case "EditText":
                    return new AppCompatEditText(this, attrs);
                case "Spinner":
                    return new AppCompatSpinner(this, attrs);
                case "CheckBox":
                    return new AppCompatCheckBox(this, attrs);
                case "RadioButton":
                    return new AppCompatRadioButton(this, attrs);
                case "CheckedTextView":
                    return new AppCompatCheckedTextView(this, attrs);
            }
        }

        return null;
    }


    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onPause() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (UIPreferenceFragment.class.getName().equals(fragmentName)
                || RulesPreferenceFragment.class.getName().equals(fragmentName)
                || LogPreferenceFragment.class.getName().equals(fragmentName)
                || ExpPreferenceFragment.class.getName().equals(fragmentName)
                || CustomBinaryPreferenceFragment.class.getName().equals(
                fragmentName)
                || SecPreferenceFragment.class.getName().equals(fragmentName)
                || MultiProfilePreferenceFragment.class.getName().equals(fragmentName)
                || WidgetPreferenceFragment.class.getName().equals(fragmentName)
                || LanguagePreferenceFragment.class.getName().equals(fragmentName)) {
            return (true);
        }

        return (false);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preferences_headers, target);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this) && !isSimplePreferences(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Determines whether the simplified settings UI should be shown. This is
     * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
     * doesn't have newer APIs like {@link PreferenceFragment}, or the device
     * doesn't have an extra-large screen. In these cases, a single-pane
     * "simplified" settings UI should be shown.
     */
    private static boolean isSimplePreferences(Context context) {
        return ALWAYS_SIMPLE_PREFS
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
                || !isXLargeTablet(context);
    }

    public void logDmesgChangeApplyRules(LogChangeEvent logChangeEvent) {
        if (logChangeEvent != null) {
            final Context context = logChangeEvent.ctx;
            final Intent logIntent = new Intent(context, LogService.class);
            if (G.enableLogService()) {
                //restart service
                context.stopService(logIntent);
                Api.cleanupUid();
                context.startService(logIntent);
            } else {
                //log service disabled
                context.stopService(logIntent);
                Api.cleanupUid();
            }
        }

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Context ctx = getApplicationContext();


        if (key.equals("showUid") || key.equals("disableIcons") || key.equals("enableVPN")
                || key.equals("enableLAN") || key.equals("enableRoam")
                || key.equals("locale") || key.equals("showFilter")) {
            G.reloadProfile();
        }

        if (key.equals("ip_path") || key.equals("dns_value")) {
            rxEvent.publish(new RulesEvent("", ctx));
        }

        if (key.equals("logDmesg")) {
            rxEvent.publish(new LogChangeEvent("", ctx));
        }

        if (key.equals("activeNotification")) {
            boolean enabled = sharedPreferences.getBoolean(key, false);
            if (enabled) {
                Api.showNotification(Api.isEnabled(ctx), ctx);
            } else {
                NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(33341);
            }
        }

        if (key.equals("enableLogService")) {
            boolean enabled = sharedPreferences.getBoolean(key, false);
            if (enabled) {
                Api.setLogTarget(ctx, true);
                Intent intent = new Intent(ctx, LogService.class);
                ctx.stopService(intent);
                Api.cleanupUid();
                ctx.startService(intent);
            } else {
                Api.setLogTarget(ctx, false);
                Intent intent = new Intent(ctx, LogService.class);
                ctx.stopService(intent);
                Api.cleanupUid();
            }
        }
        if (key.equals("enableMultiProfile")) {
            G.reloadProfile();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void deviceCheck() {
        if (Build.VERSION.SDK_INT >= 21) {
            if ((G.isDoKey(getApplicationContext()) || isDonate())) {
                KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                if (keyguardManager.isKeyguardSecure()) {
                    Intent createConfirmDeviceCredentialIntent = keyguardManager.createConfirmDeviceCredentialIntent(null, null);
                    if (createConfirmDeviceCredentialIntent != null) {
                        try {
                            startActivityForResult(createConfirmDeviceCredentialIntent, LOCK_VERIFICATION);
                        } catch (ActivityNotFoundException e) {
                        }
                    }
                } else {
                    Toast.makeText(this, getText(R.string.android_version), Toast.LENGTH_SHORT).show();
                }
            } else {
                Api.donateDialog(PreferencesActivity.this, true);
            }
        }
    }
    private boolean passCheck() {
        if (G.enableDeviceCheck()) {
            deviceCheck();
        } else {
            switch (G.protectionLevel()) {
                case "p0":
                    return true;
                case "p1":
                    final String oldpwd = G.profile_pwd();
                    if (oldpwd.length() == 0) {
                        return true;
                    } else {
                        // Check the password
                        requestPassword();
                    }
                    break;
                case "p2":
                    final String pwd = G.sPrefs.getString("LockPassword", "");
                    if (pwd.length() == 0) {
                        return true;
                    } else {
                        requestPassword();
                    }
                    break;
                case "p3":
                    if (FingerprintUtil.isAndroidSupport() && G.isFingerprintEnabled()) {
                        requestFingerprint();
                    }
            }
        }
        return false;
    }

    private void requestFingerprint() {
        FingerprintUtil.FingerprintDialog dialog = new FingerprintUtil.FingerprintDialog(this);
        dialog.setOnFingerprintFailureListener(new FingerprintUtil.OnFingerprintFailure() {
            @Override
            public void then() {
                PreferencesActivity.this.finish();
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });
        dialog.show();
    }

    private void requestPassword() {
        switch (G.protectionLevel()) {
            case "p1":
                new MaterialDialog.Builder(PreferencesActivity.this).cancelable(false)
                        .title(R.string.pass_titleget).autoDismiss(false)
                        .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                        .positiveText(R.string.submit)
                        .negativeText(R.string.Cancel)
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                PreferencesActivity.this.finish();
                                android.os.Process.killProcess(android.os.Process.myPid());
                            }
                        })
                        .input(R.string.enterpass, R.string.password_empty, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(MaterialDialog dialog, CharSequence input) {
                                String pass = input.toString();
                                boolean isAllowed = false;
                                if (G.isEnc()) {
                                    String decrypt = Api.unhideCrypt("AFW@LL_P@SSWORD_PR0T3CTI0N", G.profile_pwd());
                                    if (decrypt != null) {
                                        if (decrypt.equals(pass)) {
                                            isAllowed = true;
                                        }
                                    }
                                } else {
                                    if (pass.equals(G.profile_pwd())) {
                                        isAllowed = true;
                                    }
                                }
                                if (isAllowed) {
                                    dialog.dismiss();
                                } else {
                                    Api.toast(PreferencesActivity.this, getString(R.string.wrong_password));
                                }


                            }
                        }).show();
                break;
            case "p2":
                Intent intent = new Intent(ACTION_COMPARE_PATTERN, null, getApplicationContext(), LockPatternActivity.class);
                String savedPattern = G.sPrefs.getString("LockPassword", "");
                intent.putExtra(EXTRA_PATTERN, savedPattern.toCharArray());
                startActivityForResult(intent, REQ_ENTER_PATTERN);
                break;
        }

    }
}
