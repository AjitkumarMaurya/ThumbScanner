package technology.silverwing.com.thumbscanner.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TextInputEditText;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mantra.mfs100.FingerData;
import com.mantra.mfs100.MFS100;
import com.mantra.mfs100.MFS100Event;
import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.Length;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import technology.silverwing.com.thumbscanner.R;
import technology.silverwing.com.thumbscanner.network.RestCall;
import technology.silverwing.com.thumbscanner.network.RestClient;
import technology.silverwing.com.thumbscanner.networkResponce.CommonResponce;
import technology.silverwing.com.thumbscanner.utility.PreferenceManager;

public class FormActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, Validator.ValidationListener, MFS100Event {

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;

    @BindView(R.id.nav_view)
    NavigationView navigationView;

    @NotEmpty
    @Length(min = 12, max = 12, message = "Enter valid aadhaar no")
    @BindView(R.id.text_edit_aadhaar)
    TextInputEditText text_edit_aadhaar;

    @NotEmpty
    @Length(min = 6, max = 15, message = "Enter valid rashan no")
    @BindView(R.id.text_edit_rashan)
    TextInputEditText text_edit_rashan;

    @NotEmpty
    @Length(min = 6, max = 15, message = "Enter valid code")
    @BindView(R.id.text_edit_userCode)
    TextInputEditText text_edit_userCode;

    @BindView(R.id.tv_left_text)
    TextView tv_left_text;

    @BindView(R.id.tv_right_text)
    TextView tv_right_text;

    @BindView(R.id.iv_left)
    ImageView iv_left;

    @BindView(R.id.iv_right)
    ImageView iv_right;


    Validator validator;

    private enum ScannerAction {
        Capture, Verify
    }

    int flg = 1;
    byte[] Enroll_Template;
    byte[] Verify_Template;
    private FingerData lastCapFingerData = null;
    ScannerAction scannerAction = ScannerAction.Capture;

    int timeout = 10000;
    MFS100 mfs100 = null;

    String LeftImg = "left", RightImg = "right";

    private boolean isCaptureRunning = false;

    private ProgressDialog dialog;

    PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        validator = new Validator(this);
        validator.setValidationListener(this);


        preferenceManager = new PreferenceManager(this);
        dialog = new ProgressDialog(this);
        if (preferenceManager.getLoginSession()) {
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.addDrawerListener(toggle);
            toggle.syncState();

            navigationView.setNavigationItemSelectedListener(this);

            mfs100 = new MFS100(FormActivity.this);
            mfs100.SetApplicationContext(FormActivity.this);

        } else {
            startActivity(new Intent(FormActivity.this, MainActivity.class));
        }

    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
            final AlertDialog.Builder bui = new AlertDialog.Builder(this);
            bui.setTitle("Are you sure to exit?");
            bui.setMessage("Thanks for using this app.");
            bui.setCancelable(false);
            bui.setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    finishAffinity();

                }
            });
            bui.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog alert11 = bui.create();
            alert11.show();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_logout) {

            final AlertDialog.Builder bui = new AlertDialog.Builder(this);
            bui.setTitle("Are you sure to logout ?");
            bui.setMessage("Thanks for using this app.");
            bui.setCancelable(false);
            bui.setPositiveButton("Logout", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    preferenceManager.clearPreferences();
                    startActivity(new Intent(FormActivity.this, MainActivity.class));
                    finish();

                }
            });
            bui.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            AlertDialog alert11 = bui.create();
            alert11.show();


        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    @OnClick(R.id.lin_right)
    public void lin_right() {

        flg = 2;
        InitScanner();

    }

    @OnClick(R.id.lin_left)
    public void lin_left() {

        flg = 1;
        InitScanner();


    }


    @OnClick(R.id.btn_submit)
    public void btn_submit() {

        validator.validate();


    }

    @Override
    public void onValidationSucceeded() {

        if (LeftImg.length() > 10 || RightImg.length() > 10) {

            CallNetwork();

        } else {

            final AlertDialog.Builder bui = new AlertDialog.Builder(this);
            bui.setTitle("Scanner!");
            bui.setMessage("Add at least one fingar thumb scan");
            bui.setCancelable(true);
            bui.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    dialog.dismiss();
                }
            });
            AlertDialog alert11 = bui.create();
            alert11.show();

        }


    }

    @Override
    public void onValidationFailed(List<ValidationError> errors) {
        for (ValidationError error : errors) {
            View view = error.getView();
            String message = error.getCollatedErrorMessage(this);

            // Display error messages ;)
            if (view instanceof TextInputEditText) {
                ((TextInputEditText) view).setError(message);
            } else {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        }

    }


    private void InitScanner() {
        try {
            int ret = mfs100.Init();
            if (ret != 0) {
                SetTextOnUIThread(mfs100.GetErrorMsg(ret));
            } else {
                SetTextOnUIThread("Init success");
                String info = "Serial: " + mfs100.GetDeviceInfo().SerialNo()
                        + " Make: " + mfs100.GetDeviceInfo().Make()
                        + " Model: " + mfs100.GetDeviceInfo().Model()
                        + "\nCertificate: " + mfs100.GetCertification();
                SetTextOnUIThread(info);


                scannerAction = ScannerAction.Capture;
                if (!isCaptureRunning) {
                    StartSyncCapture();
                }

            }
        } catch (Exception ex) {
            Toast.makeText(this, "Init failed, unhandled exception",
                    Toast.LENGTH_LONG).show();
            SetTextOnUIThread("Init failed, unhandled exception");
        }
    }

    private void StartSyncCapture() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                SetTextOnUIThread("");
                isCaptureRunning = true;
                try {
                    final FingerData fingerData = new FingerData();
                    int ret = mfs100.AutoCapture(fingerData, timeout, true);
                    Log.e("StartSyncCapture.RET", "" + ret);
                    if (ret != 0) {
                        SetTextOnUIThread(mfs100.GetErrorMsg(ret));
                    } else {
                        lastCapFingerData = fingerData;
                        final Bitmap bitmap = BitmapFactory.decodeByteArray(fingerData.FingerImage(), 0,

                                fingerData.FingerImage().length);

                        FormActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (fingerData.Quality() > 65) {

                                    if (flg == 1) {
                                        iv_left.setImageBitmap(bitmap);
                                        tv_left_text.setVisibility(View.GONE);
                                        iv_left.setVisibility(View.VISIBLE);
                                        LeftImg = encodeTobase64(bitmap);

                                    }
                                    if (flg == 2) {
                                        iv_right.setImageBitmap(bitmap);
                                        tv_right_text.setVisibility(View.GONE);
                                        iv_right.setVisibility(View.VISIBLE);
                                        RightImg = encodeTobase64(bitmap);

                                    }
                                } else {
                                    if (flg == 1) {
                                        iv_left.setVisibility(View.GONE);

                                        tv_left_text.setText("retry to add left thumb");
                                    }
                                    if (flg == 2) {
                                        iv_right.setVisibility(View.GONE);

                                        tv_right_text.setText("retry to add right thumb");
                                    }

                                }
                            }
                        });

                        SetTextOnUIThread("Capture Success");
                        String log = "\nQuality: " + fingerData.Quality()
                                + "\nNFIQ: " + fingerData.Nfiq()
                                + "\nWSQ Compress Ratio: "
                                + fingerData.WSQCompressRatio()
                                + "\nImage Dimensions (inch): "
                                + fingerData.InWidth() + "\" X "
                                + fingerData.InHeight() + "\""
                                + "\nImage Area (inch): " + fingerData.InArea()
                                + "\"" + "\nResolution (dpi/ppi): "
                                + fingerData.Resolution() + "\nGray Scale: "
                                + fingerData.GrayScale() + "\nBits Per Pixal: "
                                + fingerData.Bpp() + "\nWSQ Info: "
                                + fingerData.WSQInfo();
                        SetTextOnUIThread(log);
                        SetData2(fingerData);
                    }
                } catch (Exception ex) {
                    SetTextOnUIThread("Error");
                } finally {
                    isCaptureRunning = false;
                }
            }
        }).start();
    }

    private void StopCapture() {
        try {
            mfs100.StopAutoCapture();
        } catch (Exception e) {
            SetTextOnUIThread("Error");
        }
    }

    private void SetTextOnUIThread(String s) {

        Log.e("@@", s);
    }

    public void SetData2(FingerData fingerData) {
        if (scannerAction.equals(ScannerAction.Capture)) {
            Enroll_Template = new byte[fingerData.ISOTemplate().length];
            System.arraycopy(fingerData.ISOTemplate(), 0, Enroll_Template, 0,
                    fingerData.ISOTemplate().length);
        } else if (scannerAction.equals(ScannerAction.Verify)) {
            Verify_Template = new byte[fingerData.ISOTemplate().length];
            System.arraycopy(fingerData.ISOTemplate(), 0, Verify_Template, 0,
                    fingerData.ISOTemplate().length);
            int ret = mfs100.MatchISO(Enroll_Template, Verify_Template);
            if (ret < 0) {
                SetTextOnUIThread("Error: " + ret + "(" + mfs100.GetErrorMsg(ret) + ")");
            } else {
                if (ret >= 1400) {
                    SetTextOnUIThread("Finger matched with score: " + ret);
                } else {
                    SetTextOnUIThread("Finger not matched, score: " + ret);
                }
            }
        }

        WriteFile("Raw.raw", fingerData.RawData());
        WriteFile("Bitmap.bmp", fingerData.FingerImage());
        WriteFile("ISOTemplate.iso", fingerData.ISOTemplate());
    }


    private void WriteFile(String filename, byte[] bytes) {
        try {
            String path = Environment.getExternalStorageDirectory()
                    + "//FingerData";
            File file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }
            path = path + "//" + filename;
            file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream stream = new FileOutputStream(path);
            stream.write(bytes);
            stream.close();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }


    @Override
    protected void onDestroy() {
        if (mfs100 != null) {
            mfs100.Dispose();
        }
        super.onDestroy();
    }

    protected void onStop() {
        UnInitScanner();
        super.onStop();
    }


    private void UnInitScanner() {
        try {
            int ret = mfs100.UnInit();
            if (ret != 0) {
                try {
                    SetTextOnUIThread(mfs100.GetErrorMsg(ret));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                SetTextOnUIThread("Uninit Success");
                SetTextOnUIThread("Uninit Success");
                lastCapFingerData = null;
            }
        } catch (Exception e) {
            Log.e("UnInitScanner.EX", e.toString());
        }
    }

    @Override
    public void OnDeviceAttached(int vid, int pid, boolean hasPermission) {
        int ret;
        if (!hasPermission) {
            SetTextOnUIThread("Permission denied");
            return;
        }
        if (vid == 1204 || vid == 11279) {
            if (pid == 34323) {
                ret = mfs100.LoadFirmware();
                if (ret != 0) {
                    SetTextOnUIThread(mfs100.GetErrorMsg(ret));
                } else {
                    SetTextOnUIThread("Load firmware success");
                }
            } else if (pid == 4101) {
                String key = "Without Key";
                ret = mfs100.Init();
                if (ret == 0) {
                    SetTextOnUIThread(key);
                } else {
                    SetTextOnUIThread(mfs100.GetErrorMsg(ret));
                }

            }
        }
    }


    @Override
    public void OnDeviceDetached() {

        UnInitScanner();
        SetTextOnUIThread("Device removed");

    }

    @Override
    public void OnHostCheckFailed(String s) {

    }


    public static String encodeTobase64(Bitmap image) {
        Bitmap immagex = image;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        immagex.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        String imageEncoded = Base64.encodeToString(b, Base64.DEFAULT);
        return imageEncoded;
    }


    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null;
    }

    public void CallNetwork() {

        if (isNetworkConnected()) {

            dialog.setMessage("please wait Uploading..");
            dialog.show();


            RestCall call = RestClient.createService(RestCall.class);

            call.sendData(Objects.requireNonNull(text_edit_aadhaar.getText()).toString(), Objects.requireNonNull(text_edit_rashan.getText()).toString(), Objects.requireNonNull(text_edit_userCode.getText()).toString(), preferenceManager.getRegisteredUserId(), RightImg, LeftImg)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<List<CommonResponce>>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {

                            if (dialog.isShowing()) {
                                dialog.dismiss();
                                Toast.makeText(FormActivity.this, ""+e.getMessage(), Toast.LENGTH_LONG).show();
                            }

                        }

                        @Override
                        public void onNext(List<CommonResponce> commonResponces) {

                            if (dialog.isShowing()) {
                                dialog.dismiss();
                            }

                            if (!commonResponces.get(0).getSuccess().toString().equalsIgnoreCase("False")) {

                                final AlertDialog.Builder bui = new AlertDialog.Builder(FormActivity.this);
                                bui.setTitle("Uploaded  Successful!");
                                bui.setMessage("Data Added to server successful");
                                bui.setCancelable(true);
                                bui.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        Intent intent = getIntent();
                                        finish();
                                        startActivity(intent);

                                    }
                                });
                                AlertDialog alert11 = bui.create();
                                alert11.show();


                            } else {

                                final AlertDialog.Builder bui = new AlertDialog.Builder(FormActivity.this);
                                bui.setTitle("Access Denial!");
                                bui.setMessage("You wrong UserName or Password");
                                bui.setCancelable(true);
                                bui.setPositiveButton("retry", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        CallNetwork();
                                    }
                                });
                                AlertDialog alert11 = bui.create();
                                alert11.show();

                            }

                        }
                    });


        } else {
            final AlertDialog.Builder bui = new AlertDialog.Builder(this);
            bui.setTitle("Offline!");
            bui.setMessage("You are offline. turn on data connection");
            bui.setCancelable(true);
            bui.setPositiveButton("retry", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    CallNetwork();

                }
            });
            AlertDialog alert11 = bui.create();
            alert11.show();

        }

    }

}
