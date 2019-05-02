package technology.silverwing.com.thumbscanner.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.Length;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;

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
import technology.silverwing.com.thumbscanner.networkResponce.LoginResponce;
import technology.silverwing.com.thumbscanner.utility.PreferenceManager;
import technology.silverwing.com.thumbscanner.utility.Tools;

public class MainActivity extends AppCompatActivity implements Validator.ValidationListener {

    @NotEmpty
    @Length(min = 6, max = 20, message = "Enter valid userName")
    @BindView(R.id.text_edit_user)
    TextInputEditText text_edit_user;

    @NotEmpty
    @Length(min = 2, max = 20, message = "Enter valid password")
    @BindView(R.id.text_edit_pass)
    TextInputEditText text_edit_pass;

    Validator validator;

    private ProgressDialog dialog;
    PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Tools.setSystemBarColor(this, android.R.color.white);
        Tools.setSystemBarLight(this);

        ButterKnife.bind(this);

        preferenceManager = new PreferenceManager(this);

        validator = new Validator(this);
        validator.setValidationListener(this);

        dialog = new ProgressDialog(this);

    }

    @OnClick(R.id.btn_sub)
    public void btn_sub() {
        validator.validate();
    }

    @Override
    public void onValidationSucceeded() {


        CallNetWork();

    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null;
    }

    private void CallNetWork() {

        if (isNetworkConnected()) {

            dialog.setMessage("please wait.");
            dialog.show();


            RestCall call = RestClient.createService(RestCall.class);

            call.getLogin(Objects.requireNonNull(text_edit_user.getText()).toString(),Objects.requireNonNull(text_edit_pass.getText()).toString())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<List<LoginResponce>>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {

                            if (dialog.isShowing()) {
                                dialog.dismiss();
                            }


                        }

                        @Override
                        public void onNext(List<LoginResponce> loginResponces) {

                            if (dialog.isShowing()) {
                                dialog.dismiss();
                            }

                            if (!loginResponces.get(0).getSuccess().toString().equalsIgnoreCase("False")){
                                preferenceManager.setLoginSession();
                                preferenceManager.setRegisteredUserId(loginResponces.get(0).getSuccess().toString());
                                startActivity(new Intent(MainActivity.this, FormActivity.class));
                                finish();

                            }else {

                                final AlertDialog.Builder bui = new AlertDialog.Builder(MainActivity.this);
                                bui.setTitle("Access Denial!");
                                bui.setMessage("You wrong UserName or Password");
                                bui.setCancelable(true);
                                bui.setPositiveButton("retry", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                      dialog.dismiss();

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

                    CallNetWork();

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
}
