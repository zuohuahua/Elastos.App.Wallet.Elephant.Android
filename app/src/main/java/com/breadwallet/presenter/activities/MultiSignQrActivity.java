package com.breadwallet.presenter.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.camera.ScanQRActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.qrcode.QRUtils;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;
import com.google.gson.Gson;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class MultiSignQrActivity extends BRActivity {

    private final String TAG = "MultiSignQrActivity";
    private ImageView mQRCodeIv;
    private Bitmap mBitmap = null;
    private ArrayList<Bitmap> mBitmaps;

    private String mTransaction;
    private String mTxid;

    private Handler mHandler = null;
    private int mIndex = 0;
    private int mTotal = 0;
    private final static int INTERVAL = 500;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_sign_qr);

        Intent intent = getIntent();
        mTransaction = intent.getStringExtra("tx");
        mTxid = intent.getStringExtra("txid");

        initView();
        if (!StringUtil.isNullOrEmpty(mTransaction)) {
            fixView();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mHandler != null) {
            mHandler.removeCallbacks(runnable);
            mHandler = null;
        }
    }


    private void initView() {
        mQRCodeIv = findViewById(R.id.multisign_qr_iv);
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        TextView shareJson = findViewById(R.id.multisign_qr_share_json);
        TextView passOrSent = findViewById(R.id.multisign_pass_or_sent);

        if (!StringUtil.isNullOrEmpty(mTxid)) {
            mQRCodeIv.setVisibility(View.INVISIBLE);
            shareJson.setVisibility(View.INVISIBLE);
            passOrSent.setText(R.string.multisign_send_succeeded);
        } else if (StringUtil.isNullOrEmpty(mTransaction)) {
            mQRCodeIv.setVisibility(View.INVISIBLE);
            shareJson.setVisibility(View.INVISIBLE);
            passOrSent.setVisibility(View.INVISIBLE);
        } else {
            shareJson.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareJsonFile();
                }
            });

            passOrSent.setText(R.string.multisign_pass_next);
        }
    }

    private String getUrl() throws UnsupportedEncodingException {
        return "elaphant://multitx?AppName=" + BRConstants.ELAPHANT_APP_NAME +
                "&AppID=" + BRConstants.ELAPHANT_APP_ID +
                "&PublicKey=" + BRConstants.ELAPHANT_APP_PUBLICKEY +
                "&DID=" + BRConstants.ELAPHANT_APP_DID +
                "&Tx=" + URLEncoder.encode(mTransaction, "utf-8");
    }

    private void fixView() {
        try {
            String url = getUrl();
            Log.d(TAG, "url: " + url);

            mTotal = (int) Math.ceil((double) url.length() / (double) 500);
            Log.d(TAG, "qrcount: " + mTotal);

            if (mTotal > 1) {
                mBitmaps = new ArrayList<>(mTotal);
                String md5str = UiUtils.getStringMd5(url);
                Log.d(TAG, "md5: " + md5str);
                for (int i = 0; i < mTotal; i++) {
                    ScanQRActivity.MultiPartQrcode qr = new ScanQRActivity.MultiPartQrcode();
                    qr.name = "MultiQrContent";
                    qr.total = mTotal;
                    qr.index = i;

                    int start = i * 500;
                    if (start > url.length()) break;
                    int end = (i + 1) * 500;
                    if (end > url.length()) end = url.length();
                    qr.data = url.substring(start, end);
                    qr.md5 = md5str;

                    String qrstr = new Gson().toJson(qr);
                    Log.d(TAG, "qrstr: " + qrstr);
                    Bitmap bitmap = QRUtils.generateQRBitmap(this, qrstr);
                    mBitmaps.add(i, bitmap);
                }
                mHandler = new Handler();
                mIndex = 0;
                changeQrcode();
            } else {
                mBitmap = QRUtils.generateQRBitmap(this, url);
                mQRCodeIv.setImageBitmap(mBitmap);
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private void shareJsonFile() {
        try {
            File imagePath = new File(getCacheDir(), "images");
            imagePath.mkdirs();

            FileOutputStream fOut = new FileOutputStream(imagePath + "/tx.elasign");
            OutputStreamWriter outWriter = new OutputStreamWriter(fOut);
            outWriter.write(getUrl());
            outWriter.close();
            fOut.flush();
            fOut.close();

            share("tx.elasign");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void share(final String fileName) {
        File cachePath = new File(getCacheDir(), "images");
        File newFile = new File(cachePath, fileName);
        Uri contentUri = FileProvider.getUriForFile(this, "com.elastos.wallet.imageprovider", newFile);
        if (contentUri == null) return;

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (fileName.contains(".elasign")) {
            shareIntent.setDataAndType(contentUri, "text/plain");
        } else {
            shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
        }

        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        startActivity(Intent.createChooser(shareIntent, "Share"));
    }

    private void changeQrcode() {
        mQRCodeIv.setImageBitmap(mBitmaps.get(mIndex));
        mIndex++;
        if (mIndex >= mTotal) mIndex = 0;
        mHandler.postDelayed(runnable, INTERVAL);
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            changeQrcode();
        }
    };
}
