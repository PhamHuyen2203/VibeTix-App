import re

with open('app/src/main/res/values/strings.xml', 'r', encoding='utf-8') as f:
    content = f.read()

# find the first </resources>
idx = content.find('</resources>')
if idx != -1:
    content = content[:idx] + '''
    <string name="nav_dashboard">Dashboard</string>
    <string name="nav_events">Sự kiện</string>
    <string name="btn_cancel">Hủy</string>
    <string name="btn_save">Lưu</string>
    <string name="error_empty_fields">Vui lòng nhập đầy đủ thông tin</string>
    <string name="error_invalid_percentage">Phần trăm không hợp lệ</string>
    <string name="error_invalid_number">Số không hợp lệ</string>
    <string name="tab_categories">Danh mục</string>
    <string name="tab_venues">Địa điểm</string>
    <string name="add_category_title">Thêm danh mục</string>
    <string name="add_venue_title">Thêm địa điểm</string>
    <string name="menu_view_detail">Xem chi tiết</string>
    <string name="menu_edit">Chỉnh sửa</string>
    <string name="menu_manage_tickets">Quản lý vé</string>
    <string name="menu_submit">Gửi duyệt</string>
    <string name="menu_cancel_event">Hủy sự kiện</string>
</resources>
'''

with open('app/src/main/res/values/strings.xml', 'w', encoding='utf-8') as f:
    f.write(content)
print('Fixed strings.xml')
