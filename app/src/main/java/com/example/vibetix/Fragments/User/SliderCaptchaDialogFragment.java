package com.example.vibetix.Fragments.User;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.vibetix.R;
import com.example.vibetix.Views.PuzzleCaptchaView;

public class SliderCaptchaDialogFragment extends DialogFragment {

    public interface OnVerifiedListener {
        void onVerified();
    }

    private OnVerifiedListener listener;

    public void setOnVerifiedListener(OnVerifiedListener l) {
        this.listener = l;
    }

    public static SliderCaptchaDialogFragment newInstance() {
        return new SliderCaptchaDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, R.style.Theme_VibeTix_Dialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_captcha, container, false);
    }

    private Handler countdownHandler;
    private Runnable countdownRunnable;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        PuzzleCaptchaView puzzleView = view.findViewById(R.id.puzzleCaptchaView);
        SeekBar seekBar = view.findViewById(R.id.seekBarCaptcha);
        TextView txtResult = view.findViewById(R.id.txtCaptchaResult);
        View btnClose = view.findViewById(R.id.btnCaptchaClose);
        View btnReload = view.findViewById(R.id.btnCaptchaReload);
        View layoutLoading = view.findViewById(R.id.layoutCaptchaLoading);
        TextView txtCountdown = view.findViewById(R.id.txtCaptchaCountdown);

        // Disable seekbar until puzzle is ready and countdown finishes
        seekBar.setEnabled(false);

        puzzleView.setOnPuzzleReadyListener(() -> {
            if (!isAdded()) return;
            startCountdown(layoutLoading, txtCountdown, seekBar);
        });

        btnClose.setOnClickListener(v -> dismiss());

        btnReload.setOnClickListener(v -> {
            seekBar.setProgress(0);
            seekBar.setEnabled(false);
            if (layoutLoading != null) layoutLoading.setVisibility(View.VISIBLE);
            if (txtCountdown != null) txtCountdown.setText("");
            puzzleView.regenerate();
            txtResult.setVisibility(View.INVISIBLE);
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    puzzleView.setProgress(progress / 100f);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
                txtResult.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                if (puzzleView.isAligned()) {
                    txtResult.setText(getString(R.string.str_captcha_success));
                    txtResult.setTextColor(0xFF22C55E);
                    txtResult.setVisibility(View.VISIBLE);
                    seekBar.setEnabled(false);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (isAdded()) {
                            dismiss();
                            if (listener != null) listener.onVerified();
                        }
                    }, 600);
                } else {
                    txtResult.setText(getString(R.string.str_captcha_fail));
                    txtResult.setTextColor(0xFFF04438);
                    txtResult.setVisibility(View.VISIBLE);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        sb.setProgress(0);
                        puzzleView.setProgress(0f);
                        txtResult.setVisibility(View.INVISIBLE);
                    }, 700);
                }
            }
        });
    }

    private void startCountdown(View layoutLoading, TextView txtCountdown, SeekBar seekBar) {
        if (countdownHandler != null) countdownHandler.removeCallbacks(countdownRunnable);
        countdownHandler = new Handler(Looper.getMainLooper());
        final int[] count = {3};
        txtCountdown.setText(String.valueOf(count[0]));
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) return;
                count[0]--;
                if (count[0] > 0) {
                    txtCountdown.setText(String.valueOf(count[0]));
                    countdownHandler.postDelayed(this, 1000);
                } else {
                    layoutLoading.setVisibility(View.GONE);
                    seekBar.setEnabled(true);
                }
            }
        };
        countdownHandler.postDelayed(countdownRunnable, 1000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}

