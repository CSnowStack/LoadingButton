package com.cq.csnowstack;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.cq.loadingbutton.LoadingButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final LoadingButton lbtn= (LoadingButton) findViewById(R.id.lbtn);
        lbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this,"loading",Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.failed).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lbtn.setLoadingFailed();
            }
        });

        findViewById(R.id.success).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lbtn.setLoadingSuccess();
            }
        });

        lbtn.setLoadingEndListener(new LoadingButton.loadingEndListener() {
            @Override
            public void onLoadingEndListener(boolean loadingSuccess) {
                Toast.makeText(MainActivity.this,"Loading End  "+loadingSuccess,Toast.LENGTH_SHORT).show();
            }
        });
    }
}
