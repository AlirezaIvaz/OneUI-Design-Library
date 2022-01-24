package de.dlyt.yanndroid.oneuiexample;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import de.dlyt.yanndroid.oneui.dialog.AlertDialog;
import de.dlyt.yanndroid.oneui.dialog.ClassicColorPickerDialog;
import de.dlyt.yanndroid.oneui.dialog.DetailedColorPickerDialog;
import de.dlyt.yanndroid.oneui.dialog.ProgressDialog;
import de.dlyt.yanndroid.oneui.layout.DrawerLayout;
import de.dlyt.yanndroid.oneui.layout.ToolbarLayout;
import de.dlyt.yanndroid.oneui.menu.MenuItem;
import de.dlyt.yanndroid.oneui.menu.PopupMenu;
import de.dlyt.yanndroid.oneui.sesl.support.ViewSupport;
import de.dlyt.yanndroid.oneui.sesl.utils.ReflectUtils;
import de.dlyt.yanndroid.oneui.utils.CustomButtonClickListener;
import de.dlyt.yanndroid.oneui.utils.ThemeUtil;
import de.dlyt.yanndroid.oneui.view.Snackbar;
import de.dlyt.yanndroid.oneui.widget.BottomNavigationView;
import de.dlyt.yanndroid.oneuiexample.base.BaseThemeActivity;
import de.dlyt.yanndroid.oneuiexample.utils.TabsManager;

public class MainActivity extends BaseThemeActivity {
    private String[] mTabsTagName;
    private String[] mTabsTitleName;
    private String[] mTabsClassName;

    private String sharedPrefName;

    private Context mContext;
    private FragmentManager mFragmentManager;
    private Fragment mFragment;
    private TabsManager mTabsManager;

    private DrawerLayout drawerLayout;
    private BottomNavigationView bnvLayout;
    private PopupMenu bnvPopupMenu;

    private ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mUseAltTheme = false;

        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_main);

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> drawerLayout.onSearchModeVoiceInputResult(result));

        init();
    }

    @Override
    public void attachBaseContext(Context context) {
        // pre-OneUI
        if (Build.VERSION.SDK_INT <= 28) {
            super.attachBaseContext(ThemeUtil.createDarkModeContextWrapper(context));
        } else
            super.attachBaseContext(context);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // pre-OneUI
        if (Build.VERSION.SDK_INT <= 28) {
            Resources res = getResources();
            res.getConfiguration().setTo(ThemeUtil.createDarkModeConfig(mContext, newConfig));
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (bnvLayout != null) {
            bnvLayout.setResumeStatus(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (bnvLayout != null) {
            bnvLayout.setResumeStatus(true);
        }
    }

    private void init() {
        ViewSupport.semSetRoundedCorners(getWindow().getDecorView(), 0);

        drawerLayout = findViewById(R.id.drawer_view);
        bnvLayout = findViewById(R.id.main_samsung_tabs);

        sharedPrefName = "mainactivity_tabs";
        mTabsTagName = getResources().getStringArray(R.array.mainactivity_tab_tag);
        mTabsTitleName = getResources().getStringArray(R.array.mainactivity_tab_title);
        mTabsClassName = getResources().getStringArray(R.array.mainactivity_tab_class);

        mTabsManager = new TabsManager(mContext, sharedPrefName);
        mTabsManager.initTabPosition();

        mFragmentManager = getSupportFragmentManager();

        //DrawerLayout
        drawerLayout.setDrawerButtonOnClickListener(v -> startActivity(new Intent().setClass(mContext, AboutActivity.class)));
        drawerLayout.setDrawerButtonTooltip(getText(R.string.app_info));
        drawerLayout.setButtonBadges(ToolbarLayout.N_BADGE, DrawerLayout.N_BADGE);

        drawerLayout.getAppBarLayout().addOnOffsetChangedListener((layout, verticalOffset) -> {
            int totalScrollRange = layout.getTotalScrollRange();
            int inputMethodWindowVisibleHeight = (int) ReflectUtils.genericInvokeMethod(InputMethodManager.class, mContext.getSystemService(INPUT_METHOD_SERVICE), "getInputMethodWindowVisibleHeight");
            LinearLayout nothingLayout = findViewById(R.id.nothing_layout);
            if (nothingLayout != null) {
                if (totalScrollRange != 0) {
                    nothingLayout.setTranslationY(((float) (Math.abs(verticalOffset) - totalScrollRange)) / 2.0f);
                } else {
                    nothingLayout.setTranslationY(((float) (Math.abs(verticalOffset) - inputMethodWindowVisibleHeight)) / 2.0f);
                }
            }
        });

        drawerLayout.inflateToolbarMenu(R.menu.main);
        drawerLayout.getToolbarMenu().findItem(R.id.theme_toggle).setTitle(mUseOUI4Theme ? "Switch to OneUI 3 Theme" : "Switch to OneUI 4 Theme");
        drawerLayout.setOnToolbarMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.search:
                    drawerLayout.showSearchMode();
                    item.setBadge(item.getBadge() + 1);
                    break;
                case R.id.info:
                    startActivity(new Intent().setClass(mContext, AboutActivity.class));
                    item.setBadge(item.getBadge() + 1);
                    break;
                case R.id.theme_toggle:
                    switchOUITheme();
                    break;
            }

            return true;
        });
        drawerLayout.setSearchModeListener(new ToolbarLayout.SearchModeListener() {
            @Override
            public void onKeyboardSearchClick(CharSequence s) {
                Toast.makeText(mContext, s, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onVoiceInputClick(Intent intent) {
                activityResultLauncher.launch(intent);
            }
        });

        //BottomNavigationLayout
        Drawable icon = getDrawable(R.drawable.ic_samsung_drawer);
        icon.setColorFilter(getResources().getColor(R.color.sesl_tablayout_text_color), PorterDuff.Mode.SRC_IN);
        for (String s : mTabsTitleName) {
            bnvLayout.addTab(bnvLayout.newTab().setText(s));
        }
        bnvLayout.addTabCustomButton(icon, new CustomButtonClickListener(bnvLayout) {
            @Override
            public void onClick(View v) {
                popupView(v);
            }
        });

        bnvLayout.addOnTabSelectedListener(new BottomNavigationView.OnTabSelectedListener() {
            public void onTabSelected(BottomNavigationView.Tab tab) {
                int tabPosition = tab.getPosition();
                mTabsManager.setTabPosition(tabPosition);
                setCurrentItem();
            }

            public void onTabUnselected(BottomNavigationView.Tab tab) {
            }

            public void onTabReselected(BottomNavigationView.Tab tab) {
            }
        });
        bnvLayout.updateWidget(this);
        setCurrentItem();
    }

    private void setCurrentItem() {
        if (bnvLayout.isEnabled()) {
            int tabPosition = mTabsManager.getCurrentTab();
            BottomNavigationView.Tab tab = bnvLayout.getTabAt(tabPosition);
            if (tab != null) {
                tab.select();
                setFragment(tabPosition);

                if (tabPosition == 0) {
                    // MainActivityFirstFragment
                    drawerLayout.setSubtitle("Design");
                    drawerLayout.setNavigationButtonVisible(true);
                    drawerLayout.getToolbarMenu().findItem(R.id.search).setVisible(true);
                    ((androidx.drawerlayout.widget.DrawerLayout) drawerLayout.findViewById(R.id.drawerLayout)).setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED);
                } else {
                    // MainActivitySecondFragment
                    drawerLayout.setSubtitle("Preferences");
                    drawerLayout.setNavigationButtonVisible(false);
                    drawerLayout.getToolbarMenu().findItem(R.id.search).setVisible(false);
                    ((androidx.drawerlayout.widget.DrawerLayout) drawerLayout.findViewById(R.id.drawerLayout)).setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                }

            }
        }
    }

    private void setFragment(int tabPosition) {
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        String tabName = mTabsTagName[tabPosition];
        Fragment fragment = mFragmentManager.findFragmentByTag(tabName);
        if (mFragment != null) {
            transaction.hide(mFragment);
        }
        if (fragment != null) {
            mFragment = (Fragment) fragment;
            transaction.show(fragment);
        } else {
            try {
                mFragment = (Fragment) Class.forName(mTabsClassName[tabPosition]).newInstance();
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            transaction.add(R.id.fragment_container, mFragment, tabName);
        }
        transaction.commit();
    }

    // onClick
    public void classicColorPickerDialog(View view) {
        ClassicColorPickerDialog mClassicColorPickerDialog;
        SharedPreferences sharedPreferences = getSharedPreferences("ThemeColor", Context.MODE_PRIVATE);
        String stringColor = sharedPreferences.getString("color", "0381fe");

        int currentColor = Color.parseColor("#" + stringColor);

        try {
            mClassicColorPickerDialog = new ClassicColorPickerDialog(this,
                    new ClassicColorPickerDialog.OnColorSetListener() {
                        @Override
                        public void onColorSet(int i) {
                            if (currentColor != i)
                                ThemeUtil.setColor(MainActivity.this, Color.red(i), Color.green(i), Color.blue(i));
                        }
                    },
                    currentColor);
            mClassicColorPickerDialog.show();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public void detailedColorPickerDialog(View view) {
        DetailedColorPickerDialog mDetailedColorPickerDialog;
        SharedPreferences sharedPreferences = getSharedPreferences("ThemeColor", Context.MODE_PRIVATE);
        String stringColor = sharedPreferences.getString("color", "0381fe");

        int currentColor = Color.parseColor("#" + stringColor);

        try {
            mDetailedColorPickerDialog = new DetailedColorPickerDialog(this,
                    new DetailedColorPickerDialog.OnColorSetListener() {
                        @Override
                        public void onColorSet(int i) {
                            if (currentColor != i)
                                ThemeUtil.setColor(MainActivity.this, Color.red(i), Color.green(i), Color.blue(i));
                        }
                    },
                    currentColor);
            mDetailedColorPickerDialog.show();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public void standardDialog(View view) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Title")
                .setMessage("Message")
                .setNeutralButton("Maybe", null)
                .setNegativeButton("No", (dialogInterface, i) -> new Handler().postDelayed(dialogInterface::dismiss, 700))
                .setPositiveButton("Yes", (dialogInterface, i) -> new Handler().postDelayed(dialogInterface::dismiss, 700))
                .setNegativeButtonColor(getResources().getColor(R.color.sesl_functional_red))
                .setPositiveButtonColor(getResources().getColor(R.color.sesl_functional_green))
                .setPositiveButtonProgress(true)
                .setNegativeButtonProgress(true)
                .create();
        dialog.show();
    }

    public void singleChoiceDialog(View view) {
        CharSequence[] charSequences = {"Choice1", "Choice2", "Choice3"};
        new AlertDialog.Builder(this)
                .setTitle("Title")
                .setNeutralButton("Maybe", null)
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", null)
                .setSingleChoiceItems(charSequences, 0, null)
                .show();
    }

    public void multiChoiceDialog(View view) {
        CharSequence[] charSequences = {"Choice1", "Choice2", "Choice3"};
        boolean[] booleans = {true, false, true};
        new AlertDialog.Builder(this)
                .setTitle("Title")
                .setNeutralButton("Maybe", null)
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", null)
                .setMultiChoiceItems(charSequences, booleans, null)
                .show();
    }

    public void progressDialog(View view) {
        ProgressDialog dialog = new ProgressDialog(mContext);
        dialog.setIndeterminate(true);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setTitle("Title");
        dialog.setMessage("ProgressDialog");
        dialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "Cancel", (DialogInterface.OnClickListener) null);
        dialog.setOnCancelListener(dialog12 -> progressDialogCircleOnly(view));
        dialog.show();
    }

    private void progressDialogCircleOnly(View view) {
        ProgressDialog dialog = new ProgressDialog(mContext);
        dialog.setProgressStyle(ProgressDialog.STYLE_CIRCLE_ONLY);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnCancelListener(dialog1 -> Snackbar.make(view, "Text label", Snackbar.LENGTH_SHORT).setAction("Action", new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        }).show());
        dialog.show();
    }

    private void popupView(View view) {
        if (bnvPopupMenu == null) {
            bnvPopupMenu = new PopupMenu(view);
            bnvPopupMenu.setGroupDividerEnabled(true);
            bnvPopupMenu.inflate(R.menu.bnv_menu);
            bnvPopupMenu.setAnimationStyle(R.style.BottomMenuPopupAnimStyle);
            bnvPopupMenu.setPopupMenuListener(new PopupMenu.PopupMenuListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    item.setBadge(item.getBadge() + 1);
                    return true;
                }

                @Override
                public void onMenuItemUpdate(MenuItem menuItem) {

                }
            });
        }
        int xoff = bnvPopupMenu.getPopupMenuWidth() - view.getWidth() + 7;
        if (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            bnvPopupMenu.show(xoff, 0);
        } else {
            bnvPopupMenu.show(-xoff, 0);
        }
    }
}
