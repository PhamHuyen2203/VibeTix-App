package com.example.vibetix.Activities.Admin;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vibetix.Models.Discount;
import com.example.vibetix.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CreateDiscountActivity extends AppCompatActivity {

    private EditText etCode, etTitle, etDesc, etValue, etMaxDiscount, etMinOrder;
    private RadioGroup rgType;
    private TextView tvStartDate, tvExpiryDate;
    private Button btnSave;

    private Calendar startCalendar = Calendar.getInstance();
    private Calendar expiryCalendar = Calendar.getInstance();
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_discount);

        initViews();
        setupDateTimePickers();

        btnSave.setOnClickListener(v -> saveDiscount());
    }

    private void initViews() {
        etCode = findViewById(R.id.et_discount_code);
        etTitle = findViewById(R.id.et_discount_title);
        etDesc = findViewById(R.id.et_discount_desc);
        etValue = findViewById(R.id.et_discount_value);
        etMaxDiscount = findViewById(R.id.et_max_discount);
        etMinOrder = findViewById(R.id.et_min_order);
        rgType = findViewById(R.id.rg_discount_type);
        tvStartDate = findViewById(R.id.tv_start_date);
        tvExpiryDate = findViewById(R.id.tv_expiry_date);
        btnSave = findViewById(R.id.btn_save_discount);
    }

    private void setupDateTimePickers() {
        tvStartDate.setOnClickListener(v -> showDateTimePicker(startCalendar, tvStartDate));
        tvExpiryDate.setOnClickListener(v -> showDateTimePicker(expiryCalendar, tvExpiryDate));
    }

    private void showDateTimePicker(Calendar calendar, TextView targetView) {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                targetView.setText(dateTimeFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveDiscount() {
        String code = etCode.getText().toString().trim();
        String title = etTitle.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();
        String valueStr = etValue.getText().toString().trim();
        String maxDiscountStr = etMaxDiscount.getText().toString().trim();
        String minOrderStr = etMinOrder.getText().toString().trim();

        if (code.isEmpty() || title.isEmpty() || valueStr.isEmpty() || 
            tvStartDate.getText().equals(getString(R.string.hint_start_date)) || 
            tvExpiryDate.getText().equals(getString(R.string.hint_expiry_date))) {
            Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        if (expiryCalendar.before(startCalendar)) {
            Toast.makeText(this, R.string.error_date_range, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double value = Double.parseDouble(valueStr);
            double maxDiscount = maxDiscountStr.isEmpty() ? 0 : Double.parseDouble(maxDiscountStr);
            double minOrder = minOrderStr.isEmpty() ? 0 : Double.parseDouble(minOrderStr);

            int selectedTypeId = rgType.getCheckedRadioButtonId();
            String type = (selectedTypeId == R.id.rb_percentage) ? "percentage" : "fixed";

            if (type.equals("percentage") && (value <= 0 || value > 100)) {
                Toast.makeText(this, R.string.error_invalid_percentage, Toast.LENGTH_SHORT).show();
                return;
            }

            Discount discount = new Discount();
            discount.setCode(code.toUpperCase());
            discount.setTitle(title);
            discount.setDescription(desc);
            discount.setType(type);
            discount.setValue(value);
            discount.setMaxDiscount(maxDiscount);
            discount.setMinOrderValue(minOrder);
            discount.setStartDate(startCalendar.getTimeInMillis());
            discount.setExpiryDate(expiryCalendar.getTimeInMillis());
            discount.setCreatorType("admin");
            discount.setScope("global");
            discount.setEventId(null);
            discount.setActive(true);

            // Logic to send to Backend/Firestore
            Toast.makeText(this, R.string.msg_discount_created, Toast.LENGTH_LONG).show();
            finish();

        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.error_invalid_number, Toast.LENGTH_SHORT).show();
        }
    }
}
