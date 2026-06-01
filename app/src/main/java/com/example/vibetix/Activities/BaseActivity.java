package com.example.vibetix.Activities;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.vibetix.Utils.SessionManager;

public abstract class BaseActivity extends AppCompatActivity {
    protected SessionManager sessionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);
    }

    protected void checkLoginStatus() {
        if (!sessionManager.isLoggedIn()) {
            // Chuyển hướng về login nếu chưa đăng nhập
            // (Thực hiện bởi MainActivity hoặc tại đây nếu cần)
        }
    }
}
