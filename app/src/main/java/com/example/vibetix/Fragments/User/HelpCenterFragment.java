package com.example.vibetix.Fragments.User;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.vibetix.R;

public class HelpCenterFragment extends Fragment {

    // FAQ data
    private static final String[][] FAQ_ITEMS = {
        {
            "Quy chế hoạt động",
            "VibeTix là nền tảng bán vé sự kiện trực tuyến, kết nối người mua và ban tổ chức sự kiện một cách an toàn và minh bạch.\n\n" +
            "• Người dùng đăng ký tài khoản miễn phí.\n" +
            "• Ban tổ chức cần được Admin phê duyệt trước khi tạo sự kiện.\n" +
            "• Mọi giao dịch đều được bảo mật theo chuẩn PCI-DSS.\n" +
            "• VibeTix không hoàn tiền sau khi vé đã được phát hành trừ trường hợp sự kiện bị hủy."
        },
        {
            "Chính sách bảo mật thông tin",
            "Chúng tôi cam kết bảo vệ thông tin cá nhân của bạn:\n\n" +
            "• Thông tin cá nhân chỉ được dùng để xử lý đơn hàng và thông báo sự kiện.\n" +
            "• Chúng tôi không chia sẻ dữ liệu với bên thứ ba mà không có sự đồng ý.\n" +
            "• Mật khẩu được mã hóa bcrypt — chúng tôi không thể xem mật khẩu của bạn.\n" +
            "• Bạn có thể yêu cầu xóa tài khoản bất cứ lúc nào."
        },
        {
            "Cơ chế giải quyết tranh chấp",
            "Nếu có tranh chấp liên quan đến đơn hàng hoặc sự kiện:\n\n" +
            "1. Liên hệ ban tổ chức sự kiện trong vòng 48h.\n" +
            "2. Nếu không được giải quyết, gửi khiếu nại qua support@vibetix.vn.\n" +
            "3. VibeTix sẽ xem xét và phản hồi trong 3-5 ngày làm việc.\n" +
            "4. Quyết định của VibeTix là quyết định cuối cùng trong phạm vi nền tảng."
        },
        {
            "Chính sách bảo mật thanh toán",
            "• Tất cả giao dịch được mã hóa SSL 256-bit.\n" +
            "• Thông tin thẻ không được lưu trữ trên máy chủ VibeTix.\n" +
            "• Hỗ trợ: MoMo, ZaloPay, VNPay, Visa/Mastercard, ATM nội địa.\n" +
            "• Đơn hàng pending sẽ tự động hủy sau 15 phút nếu chưa thanh toán."
        },
        {
            "Chính sách đổi trả và hoàn tiền",
            "• Vé đã mua KHÔNG được đổi hoặc hoàn tiền sau khi phát hành.\n" +
            "• Ngoại lệ: Sự kiện bị hủy bởi ban tổ chức → hoàn 100% trong 7 ngày.\n" +
            "• Sự kiện dời lịch → người dùng có thể giữ vé hoặc yêu cầu hoàn tiền trong 48h.\n" +
            "• Tính năng bán lại vé (Resale) cho phép bán vé cho người khác."
        },
        {
            "Điều khoản sử dụng cho khách hàng",
            "Bằng cách sử dụng VibeTix, bạn đồng ý:\n\n" +
            "• Không mua vé với mục đích đầu cơ, bán lại với giá cao hơn giá gốc quá 150%.\n" +
            "• Không chia sẻ mã QR vé với người khác khi bạn sẽ tham dự.\n" +
            "• Chịu trách nhiệm về tài khoản và mọi giao dịch phát sinh từ tài khoản.\n" +
            "• VibeTix có quyền khóa tài khoản vi phạm điều khoản."
        },
        {
            "Điều khoản sử dụng cho Ban tổ chức",
            "Ban tổ chức khi đăng ký trên VibeTix cam kết:\n\n" +
            "• Cung cấp thông tin sự kiện trung thực, chính xác.\n" +
            "• Tổ chức sự kiện đúng thời gian và địa điểm đã công bố.\n" +
            "• Hoàn tiền cho khách hàng nếu hủy sự kiện.\n" +
            "• VibeTix thu phí dịch vụ 5-10% trên giá trị vé bán được.\n" +
            "• Thanh toán cho ban tổ chức trong 7 ngày sau khi sự kiện kết thúc."
        },
        {
            "Phương thức thanh toán được hỗ trợ",
            "VibeTix hiện hỗ trợ các phương thức thanh toán:\n\n" +
            "💜 MoMo — Ví điện tử\n" +
            "💙 ZaloPay — Ví điện tử\n" +
            "🔵 VNPay — Cổng thanh toán QR\n" +
            "💳 Visa / Mastercard — Thẻ quốc tế\n" +
            "🏧 ATM nội địa — Thẻ ngân hàng Việt Nam\n\n" +
            "Tất cả giao dịch được xử lý tức thời và an toàn."
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_help_center, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        applyInsets(view);

        ImageView btnBack = view.findViewById(R.id.btnHelpBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getParentFragmentManager().getBackStackEntryCount() > 0)
                    getParentFragmentManager().popBackStack();
            });
        }

        LinearLayout container = view.findViewById(R.id.containerFaqItems);
        if (container != null) {
            buildFaqItems(container);
        }
    }

    private void applyInsets(View root) {
        View header = root.findViewById(R.id.layoutHelpHeader);
        if (header == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, top, 0, 0);
            return insets;
        });
    }

    private void buildFaqItems(LinearLayout container) {
        float dp = getResources().getDisplayMetrics().density;
        int marginDp = (int)(8 * dp);

        for (String[] item : FAQ_ITEMS) {
            String title   = item[0];
            String content = item[1];

            // Wrapper card
            LinearLayout card = new LinearLayout(requireContext());
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = marginDp;
            card.setLayoutParams(cardLp);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_faq_item);

            // Header row — title + chevron
            LinearLayout headerRow = new LinearLayout(requireContext());
            headerRow.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            int ph = (int)(16 * dp);
            int pv = (int)(14 * dp);
            headerRow.setPadding(ph, pv, ph, pv);

            TextView txtTitle = new TextView(requireContext());
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            txtTitle.setLayoutParams(titleLp);
            txtTitle.setText(title);
            txtTitle.setTextColor(0xFF1C1B1B);
            txtTitle.setTextSize(14f);
            headerRow.addView(txtTitle);

            TextView chevron = new TextView(requireContext());
            chevron.setText("›");
            chevron.setTextColor(0xFF808E92);
            chevron.setTextSize(20f);
            headerRow.addView(chevron);

            card.addView(headerRow);

            // Content — initially GONE
            TextView txtContent = new TextView(requireContext());
            LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            txtContent.setLayoutParams(contentLp);
            txtContent.setText(content);
            txtContent.setTextColor(0xFF808E92);
            txtContent.setTextSize(13f);
            txtContent.setLineSpacing(4 * dp, 1f);
            txtContent.setPadding(ph, 0, ph, pv);
            txtContent.setVisibility(View.GONE);
            card.addView(txtContent);

            // Toggle on click
            headerRow.setOnClickListener(v -> {
                boolean expanded = txtContent.getVisibility() == View.VISIBLE;
                txtContent.setVisibility(expanded ? View.GONE : View.VISIBLE);
                chevron.setText(expanded ? "›" : "∨");
                chevron.setTextColor(expanded ? 0xFF808E92 : 0xFF226CEB);
                txtTitle.setTextColor(expanded ? 0xFF1C1B1B : 0xFF226CEB);
            });

            container.addView(card);
        }
    }
}
