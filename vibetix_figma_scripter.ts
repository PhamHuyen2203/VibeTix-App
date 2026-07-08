// VibeTix Figma Scripter — v4 FULL (25 screens)
// Paste into Figma Scripter plugin and Run.
// Image placeholders: Rectangle named "📷 label" — select → Fill → Solid → Image to swap.

const W = 780, H = 1688;
const GAP = 120;
const SB = 88;        // status bar height
const HDR_H = 200;    // total header (SB + AppBar)
const NAV_BAR = 62;   // bottom nav bar
const FAB_R = 52;     // FAB radius (extends above nav bar)
const NAV_H = NAV_BAR + FAB_R; // total nav area bottom reservation = 114
const CONTENT_TOP = HDR_H;
const CONTENT_BOT = H - NAV_H;
const CONTENT_H = CONTENT_BOT - CONTENT_TOP; // usable scroll area

const C = {
  primary:   '#2563EB',
  primary2:  '#1E50C8',
  accent:    '#06B6D4',
  success:   '#16A34A',
  warning:   '#F59E0B',
  error:     '#EF4444',
  white:     '#FFFFFF',
  bg:        '#F8FAFC',
  card:      '#FFFFFF',
  border:    '#E2E8F0',
  text:      '#0F172A',
  textSub:   '#64748B',
  textMuted: '#94A3B8',
  navBg:     '#FFFFFF',
  overlay:   '#00000080',
  chipBg:    '#EFF6FF',
  gray:      '#D0D5DD',
  orange:    '#F2994A',
};

// ── Helpers ─────────────────────────────────────────────────────────────────

function hex(h: string): RGB {
  const r = parseInt(h.slice(1,3),16)/255;
  const g = parseInt(h.slice(3,5),16)/255;
  const b = parseInt(h.slice(5,7),16)/255;
  return {r,g,b};
}
function solid(color: string): SolidPaint { return {type:'SOLID',color:hex(color)}; }
function alpha(color: string, a: number): SolidPaint { return {type:'SOLID',color:hex(color),opacity:a}; }

function mkFrame(name: string, x=0, y=0, w=W, h=H): FrameNode {
  const f = figma.createFrame();
  f.name = name; f.x = x; f.y = y; f.resize(w, h);
  f.fills = [solid(C.bg)];
  f.clipsContent = true;
  return f;
}

function mkRect(parent: BaseNode & ChildrenMixin, name: string, x: number, y: number, w: number, h: number, color=C.card, radius=0): RectangleNode {
  const r = figma.createRectangle();
  r.name = name; r.x = x; r.y = y; r.resize(w, h);
  r.fills = [solid(color)];
  if (radius) r.cornerRadius = radius;
  (parent as FrameNode).appendChild(r);
  return r;
}

function mkImg(parent: BaseNode & ChildrenMixin, label: string, x: number, y: number, w: number, h: number, radius=0): RectangleNode {
  const r = mkRect(parent, `📷 ${label}`, x, y, w, h, C.gray, radius);
  return r;
}

async function mkText(parent: BaseNode & ChildrenMixin, content: string, x: number, y: number, size=28, weight: 'Regular'|'Medium'|'SemiBold'='Regular', color=C.text, w=0): Promise<TextNode> {
  await figma.loadFontAsync({family:'Poppins', style:weight});
  const t = figma.createText();
  t.fontName = {family:'Poppins', style:weight};
  t.fontSize = size;
  t.fills = [solid(color)];
  t.characters = content;
  t.x = x; t.y = y;
  if (w > 0) { t.textAutoResize = 'HEIGHT'; t.resize(w, t.height); }
  (parent as FrameNode).appendChild(t);
  return t;
}

function mkCard(parent: BaseNode & ChildrenMixin, name: string, x: number, y: number, w: number, h: number): FrameNode {
  const f = figma.createFrame();
  f.name = name; f.x = x; f.y = y; f.resize(w, h);
  f.fills = [solid(C.card)];
  f.cornerRadius = 16;
  f.strokes = [solid(C.border)];
  f.strokeWeight = 1;
  (parent as BaseNode & ChildrenMixin as FrameNode).appendChild(f);
  return f;
}

async function mkBtn(parent: BaseNode & ChildrenMixin, label: string, x: number, y: number, w: number, h=88, bg=C.primary, fg=C.white): Promise<FrameNode> {
  const f = figma.createFrame();
  f.name = `btn_${label}`; f.x = x; f.y = y; f.resize(w, h);
  f.fills = [solid(bg)];
  f.cornerRadius = 16;
  (parent as FrameNode).appendChild(f);
  await mkText(f, label, 0, (h-40)/2, 32, 'SemiBold', fg, w);
  const t = f.children[0] as TextNode;
  t.textAlignHorizontal = 'CENTER';
  return f;
}

async function mkInput(parent: BaseNode & ChildrenMixin, label: string, placeholder: string, x: number, y: number, w: number): Promise<FrameNode> {
  const f = figma.createFrame();
  f.name = `input_${label}`; f.x = x; f.y = y; f.resize(w, 110);
  f.fills = [];
  (parent as FrameNode).appendChild(f);
  await mkText(f, label, 0, 0, 24, 'Medium', C.textSub, w);
  const box = figma.createRectangle();
  box.name = 'box'; box.x = 0; box.y = 36; box.resize(w, 74);
  box.fills = [solid(C.white)]; box.cornerRadius = 12;
  box.strokes = [solid(C.border)]; box.strokeWeight = 1;
  f.appendChild(box);
  await mkText(f, placeholder, 20, 52, 26, 'Regular', C.textMuted, w-40);
  return f;
}

// ── Header & Nav ─────────────────────────────────────────────────────────────

async function cloneHeader(target: FrameNode, sourceName = 'Homepage'): Promise<boolean> {
  try {
    const src = figma.currentPage.findOne(n => n.name === sourceName) as FrameNode | null;
    if (!src) return false;
    const hdr = src.findOne(n => n.name === 'Header - TopAppBar') as SceneNode | null;
    if (!hdr) return false;
    const cloned = hdr.clone();
    cloned.x = 0; cloned.y = 0;
    target.appendChild(cloned);
    return true;
  } catch { return false; }
}

async function drawSimpleHeader(f: FrameNode, title: string, showBack=false) {
  mkRect(f, 'hdr_bg', 0, 0, W, HDR_H, C.white);
  await mkText(f, 'VibeTix', 32, SB+28, 36, 'SemiBold', C.primary, 200);
  if (showBack) {
    await mkText(f, '←', 32, SB+24, 44, 'SemiBold', C.text);
    await mkText(f, title, 110, SB+28, 36, 'SemiBold', C.text, 500);
  }
  mkRect(f, 'hdr_divider', 0, HDR_H-1, W, 1, C.border);
}

async function drawBottomNav(f: FrameNode, active: 0|1|2|3|4 = 0) {
  const navY = H - NAV_BAR;
  mkRect(f, 'nav_bg', 0, navY, W, NAV_BAR, C.navBg);
  mkRect(f, 'nav_top_line', 0, navY, W, 1, C.border);

  const tabs = ['Trang chủ','Sự kiện','','Vé của tôi','Cá nhân'];
  const icons = ['⊙','◫','','⊟','○'];
  const tw = W / 5;
  for (let i = 0; i < 5; i++) {
    if (i === 2) continue; // FAB slot
    const tx = i * tw;
    const isActive = i === active;
    const col = isActive ? C.primary : C.textMuted;
    await mkText(f, icons[i], tx, navY+4, 32, 'Regular', col, tw);
    (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
    await mkText(f, tabs[i], tx, navY+38, 18, isActive?'SemiBold':'Regular', col, tw);
    (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  }

  // FAB
  const fabX = W/2 - FAB_R;
  const fabY = H - NAV_BAR - FAB_R;
  const fabCircle = figma.createEllipse();
  fabCircle.name = 'FAB';
  fabCircle.x = fabX; fabCircle.y = fabY;
  fabCircle.resize(FAB_R*2, FAB_R*2);
  fabCircle.fills = [solid(C.primary)];
  f.appendChild(fabCircle);
  await mkText(f, '+', W/2-18, fabY+12, 52, 'Regular', C.white);
}

// ── Screen builders ──────────────────────────────────────────────────────────

async function buildHome(x: number) {
  const f = mkFrame('01 Home', x);
  const ok = await cloneHeader(f);
  if (!ok) await drawSimpleHeader(f, '');
  await drawBottomNav(f, 0);

  let y = CONTENT_TOP + 24;
  // Banner
  mkImg(f, 'Banner Carousel', 32, y, W-64, 320, 20);
  y += 340;
  // Section: Stars
  await mkText(f, 'Nghệ sĩ nổi bật', 32, y, 30, 'SemiBold', C.text);
  y += 50;
  for (let i=0;i<4;i++) {
    mkImg(f, `Star ${i+1}`, 32+i*160, y, 120, 120, 60);
  }
  y += 150;
  // Section: Featured Events
  await mkText(f, 'Sự kiện nổi bật', 32, y, 30, 'SemiBold', C.text);
  y += 50;
  const ec1 = mkCard(f, 'event_card_1', 32, y, 340, 300);
  mkImg(ec1, 'Event poster 1', 0, 0, 340, 180, 12);
  await mkText(ec1, 'LiveFest 2025', 16, 196, 26, 'SemiBold', C.text, 308);
  await mkText(ec1, '📅 25/12/2025 • TP.HCM', 16, 228, 22, 'Regular', C.textSub, 308);
  await mkText(ec1, 'từ 500.000 ₫', 16, 258, 24, 'SemiBold', C.primary, 308);
  const ec2 = mkCard(f, 'event_card_2', 388, y, 340, 300);
  mkImg(ec2, 'Event poster 2', 0, 0, 340, 180, 12);
  await mkText(ec2, 'Rock Night Ha Noi', 16, 196, 26, 'SemiBold', C.text, 308);
  await mkText(ec2, '📅 01/01/2026 • Hà Nội', 16, 228, 22, 'Regular', C.textSub, 308);
  await mkText(ec2, 'từ 300.000 ₫', 16, 258, 24, 'SemiBold', C.primary, 308);
  y += 320;
  // Trending
  await mkText(f, 'Đang hot 🔥', 32, y, 30, 'SemiBold', C.text);
  y += 50;
  for (let i=0;i<3;i++) {
    const tc = mkCard(f, `trending_${i}`, 32, y, W-64, 120);
    mkImg(tc, `Trending ${i+1}`, 0, 0, 120, 120, 12);
    await mkText(tc, `Sự kiện trending ${i+1}`, 136, 16, 26, 'SemiBold', C.text, 380);
    await mkText(tc, `📅 ${10+i}/12/2025`, 136, 50, 22, 'Regular', C.textSub, 380);
    await mkText(tc, `${(i+2)*100}.000 ₫`, 136, 82, 24, 'SemiBold', C.primary, 380);
    y += 136;
  }
  // Resale section
  await mkText(f, 'Vé đang sang nhượng', 32, y, 30, 'SemiBold', C.text);
  y += 50;
  const rc = mkCard(f, 'resale_card', 32, y, W-64, 160);
  mkImg(rc, 'Resale event poster', 0, 0, 160, 160, 12);
  await mkText(rc, 'Vé chuyển nhượng — Sự kiện A', 176, 24, 26, 'SemiBold', C.text, 350);
  await mkText(rc, 'Người bán: Nguyễn Văn A', 176, 60, 22, 'Regular', C.textSub, 350);
  await mkText(rc, 'Xem ngay →', 176, 110, 26, 'SemiBold', C.accent, 200);
  y += 180;

  f.resize(W, y + NAV_H + 32);
}

async function buildEvents(x: number) {
  const f = mkFrame('02 Events', x);
  await drawSimpleHeader(f, 'Sự kiện', false);
  await drawBottomNav(f, 1);

  let y = CONTENT_TOP + 24;
  // Search bar
  const sb = mkRect(f, 'search_bar', 32, y, W-64, 80, C.white, 40);
  sb.strokes = [solid(C.border)]; sb.strokeWeight = 1;
  await mkText(f, '🔍  Tìm kiếm sự kiện...', 56, y+20, 26, 'Regular', C.textMuted, W-100);
  y += 100;
  // Filter chips
  const chips = ['Tất cả','Live Music','Workshop','Thể thao','Nghệ thuật'];
  let cx = 32;
  for (const chip of chips) {
    const cw = chip.length * 18 + 40;
    const cb = mkRect(f, `chip_${chip}`, cx, y, cw, 56, chip==='Tất cả'?C.primary:C.chipBg, 28);
    await mkText(f, chip, cx+20, y+10, 22, 'Medium', chip==='Tất cả'?C.white:C.primary, cw-40);
    cx += cw + 16;
  }
  y += 80;
  // Grid of events (2 col)
  await mkText(f, '128 sự kiện', 32, y, 24, 'Regular', C.textSub);
  y += 40;
  for (let row=0;row<4;row++) {
    for (let col=0;col<2;col++) {
      const ex = 32 + col*360;
      const ec = mkCard(f, `ev_${row*2+col}`, ex, y, 340, 300);
      mkImg(ec, `Event ${row*2+col+1}`, 0, 0, 340, 180, 12);
      await mkText(ec, `Sự kiện ${row*2+col+1}`, 16, 196, 24, 'SemiBold', C.text, 308);
      await mkText(ec, '📅 12/2025', 16, 228, 20, 'Regular', C.textSub, 308);
      await mkText(ec, '500.000 ₫', 16, 258, 22, 'SemiBold', C.primary, 308);
    }
    y += 316;
  }
  f.resize(W, y + NAV_H + 32);
}

async function buildEventDetail(x: number) {
  const f = mkFrame('03 Event Detail', x);
  await drawSimpleHeader(f, 'Chi tiết sự kiện', true);
  // No bottom nav on detail

  let y = CONTENT_TOP;
  mkImg(f, 'Event Banner/Poster', 0, y, W, 420);
  y += 420;

  const info = mkCard(f, 'detail_card', 32, y-40, W-64, 900);
  await mkText(info, 'LiveFest 2025 — Đêm nhạc cuối năm', 24, 16, 36, 'SemiBold', C.text, W-112);
  await mkText(info, '📅  Thứ 5, 25 tháng 12 năm 2025  •  19:00', 24, 80, 24, 'Regular', C.textSub, W-112);
  await mkText(info, '📍  SVĐ Quốc gia Mỹ Đình, Hà Nội', 24, 114, 24, 'Regular', C.textSub, W-112);

  mkRect(info, 'divider1', 24, 156, W-112, 1, C.border);
  await mkText(info, 'Ban tổ chức', 24, 168, 22, 'Medium', C.textMuted, 200);
  mkImg(info, 'Org Logo', 24, 196, 72, 72, 36);
  await mkText(info, 'Saigon Events Co.', 112, 208, 26, 'SemiBold', C.text, 300);
  await mkText(info, '✓ Đã xác minh', 112, 242, 22, 'Regular', C.success, 200);

  mkRect(info, 'divider2', 24, 284, W-112, 1, C.border);
  await mkText(info, 'Mô tả sự kiện', 24, 296, 28, 'SemiBold', C.text, W-112);
  await mkText(info, 'Đêm nhạc cuối năm với sự góp mặt của hàng chục nghệ sĩ nổi tiếng. Một đêm không thể bỏ lỡ cho những ai yêu âm nhạc. Hãy cùng chúng tôi chào đón năm mới tại sân khấu lớn nhất Hà Nội!', 24, 336, 24, 'Regular', C.text, W-112);

  mkRect(info, 'divider3', 24, 460, W-112, 1, C.border);
  await mkText(info, 'Loại vé', 24, 472, 28, 'SemiBold', C.text, W-112);
  // Ticket type rows
  const ttypes = [['Vé Thường','500.000 ₫','còn 200 vé'],['Vé VIP','1.200.000 ₫','còn 50 vé'],['Vé VVIP','2.500.000 ₫','còn 10 vé']];
  let ty = 516;
  for (const [name,price,qty] of ttypes) {
    mkRect(info, `tt_${name}`, 24, ty, W-112, 88, C.chipBg, 12);
    await mkText(info, name, 40, ty+12, 26, 'SemiBold', C.text, 300);
    await mkText(info, price, 40, ty+46, 24, 'SemiBold', C.primary, 200);
    await mkText(info, qty, W-250, ty+30, 22, 'Regular', C.success, 180);
    ty += 104;
  }

  mkRect(info, 'divider4', 24, ty+8, W-112, 1, C.border);
  await mkText(info, 'Nghệ sĩ tham gia', 24, ty+24, 28, 'SemiBold', C.text, W-112);
  for (let i=0;i<3;i++) {
    mkImg(info, `Star ${i+1}`, 24+i*140, ty+68, 112, 112, 56);
  }

  info.resize(W-64, ty+200);
  f.resize(W, y + ty + 280);

  // Sticky buy button
  await mkBtn(f, 'Mua vé ngay', 32, H-120, W-64);
}

async function buildSelectTicket(x: number) {
  const f = mkFrame('04 Select Ticket', x);
  await drawSimpleHeader(f, 'Chọn vé', true);

  let y = CONTENT_TOP + 32;
  await mkText(f, 'LiveFest 2025', 32, y, 32, 'SemiBold', C.text, W-64);
  y += 50;
  await mkText(f, '📅 25/12/2025  •  📍 Mỹ Đình, Hà Nội', 32, y, 24, 'Regular', C.textSub, W-64);
  y += 50;
  mkRect(f, 'divider', 32, y, W-64, 1, C.border);
  y += 24;
  await mkText(f, 'Chọn loại vé', 32, y, 30, 'SemiBold', C.text);
  y += 48;

  const ticketTypes = [
    {name:'Vé Thường', price:'500.000 ₫', qty:200, desc:'Khu vực đứng'},
    {name:'Vé VIP', price:'1.200.000 ₫', qty:50, desc:'Khu vực ngồi có ghế'},
    {name:'Vé VVIP', price:'2.500.000 ₫', qty:10, desc:'Hàng đầu + backstage pass'},
  ];
  for (const tt of ticketTypes) {
    const tc = mkCard(f, `ticket_sel_${tt.name}`, 32, y, W-64, 160);
    mkRect(tc, 'left_accent', 0, 0, 8, 160, C.primary, 4);
    await mkText(tc, tt.name, 24, 16, 28, 'SemiBold', C.text, 350);
    await mkText(tc, tt.desc, 24, 54, 22, 'Regular', C.textSub, 350);
    await mkText(tc, tt.price, 24, 92, 30, 'SemiBold', C.primary, 280);
    await mkText(tc, `còn ${tt.qty} vé`, 24, 128, 20, 'Regular', C.success, 200);
    // qty stepper
    mkRect(tc, 'minus', W-200, 50, 72, 72, C.chipBg, 36);
    await mkText(tc, '−', W-186, 64, 40, 'SemiBold', C.primary);
    await mkText(tc, '0', W-136, 64, 40, 'SemiBold', C.text);
    mkRect(tc, 'plus', W-88, 50, 72, 72, C.primary, 36);
    await mkText(tc, '+', W-72, 64, 40, 'SemiBold', C.white);
    y += 176;
  }

  // Summary
  y += 16;
  mkRect(f, 'summary_bg', 32, y, W-64, 120, C.chipBg, 16);
  await mkText(f, 'Tổng cộng', 56, y+16, 24, 'Regular', C.textSub, 300);
  await mkText(f, '0 ₫', 56, y+52, 36, 'SemiBold', C.text, 400);
  await mkText(f, '0 vé đã chọn', 56, y+92, 22, 'Regular', C.textMuted, 300);

  y += 148;
  await mkBtn(f, 'Tiếp tục →', 32, y, W-64);
  f.resize(W, y+120);
}

async function buildFillAttendee(x: number) {
  const f = mkFrame('05 Fill Attendee Info', x);
  await drawSimpleHeader(f, 'Thông tin người tham dự', true);

  // Step indicator
  mkRect(f, 'step_bg', 0, HDR_H, W, 60, C.white);
  const steps = ['Chọn vé','Thông tin','Thanh toán','Hoàn tất'];
  for (let i=0;i<4;i++) {
    const sw = W/4;
    mkRect(f, `step_line_${i}`, i*sw, HDR_H+50, sw, 4, i<=1?C.primary:C.border);
    await mkText(f, `${i+1}`, i*sw+sw/2-12, HDR_H+16, 22, i===1?'SemiBold':'Regular', i<=1?C.primary:C.textMuted, 24);
  }

  let y = HDR_H + 80;
  await mkText(f, 'Thông tin người đặt vé', 32, y, 30, 'SemiBold', C.text);
  y += 50;
  await mkInput(f, 'Họ và tên *', 'Nguyễn Văn A', 32, y, W-64); y += 130;
  await mkInput(f, 'Email *', 'nguyenvana@gmail.com', 32, y, W-64); y += 130;
  await mkInput(f, 'Số điện thoại *', '0901234567', 32, y, W-64); y += 130;

  mkRect(f, 'divider', 32, y, W-64, 1, C.border); y += 24;
  await mkText(f, 'Người tham dự (nếu khác)', 32, y, 30, 'SemiBold', C.text); y += 50;
  await mkInput(f, 'Họ và tên người tham dự', 'Nguyễn Thị B', 32, y, W-64); y += 130;
  await mkInput(f, 'Email người tham dự', 'b@gmail.com', 32, y, W-64); y += 130;

  mkRect(f, 'divider2', 32, y, W-64, 1, C.border); y += 24;
  await mkText(f, 'Mã giảm giá', 32, y, 30, 'SemiBold', C.text); y += 48;
  const discRow = mkRect(f, 'disc_row', 32, y, W-64, 80, C.white, 12);
  discRow.strokes = [solid(C.border)]; discRow.strokeWeight = 1;
  await mkText(f, 'Nhập mã voucher...', 56, y+24, 26, 'Regular', C.textMuted, W-200);
  await mkBtn(f, 'Áp dụng', W-188, y-4, 156, 88, C.accent);
  y += 104;

  // Order summary
  mkRect(f, 'order_sum', 32, y, W-64, 200, C.chipBg, 16);
  await mkText(f, 'Tóm tắt đơn hàng', 56, y+16, 26, 'SemiBold', C.text, 400);
  await mkText(f, '2x Vé Thường  500.000 ₫/vé', 56, y+56, 24, 'Regular', C.text, W-120);
  await mkText(f, '1x Vé VIP  1.200.000 ₫/vé', 56, y+88, 24, 'Regular', C.text, W-120);
  mkRect(f, 'sum_divider', 56, y+120, W-112, 1, C.border);
  await mkText(f, 'Tổng:', 56, y+132, 28, 'SemiBold', C.text, 200);
  await mkText(f, '2.200.000 ₫', W-280, y+132, 28, 'SemiBold', C.primary, 220);
  y += 224;

  await mkBtn(f, 'Xác nhận & Thanh toán →', 32, y, W-64);
  f.resize(W, y+120);
}

async function buildPayment(x: number) {
  const f = mkFrame('06 Simulated Payment', x);
  await drawSimpleHeader(f, 'Thanh toán', true);

  let y = CONTENT_TOP + 32;
  // Countdown
  mkRect(f, 'countdown_bg', 32, y, W-64, 120, '#FEF3C7', 16);
  await mkText(f, '⏱ Thời gian còn lại', 56, y+16, 24, 'Regular', C.warning, 400);
  await mkText(f, '14:32', 56, y+52, 52, 'SemiBold', '#B45309', 400);
  await mkText(f, 'Đơn hàng sẽ hết hạn sau thời gian trên', 56, y+100, 22, 'Regular', C.textSub, W-120);
  y += 140;

  // Order info
  await mkText(f, 'Chi tiết đơn hàng', 32, y, 30, 'SemiBold', C.text); y += 50;
  const rows = [['2x Vé Thường','1.000.000 ₫'],['1x Vé VIP','1.200.000 ₫'],['Phí xử lý','10.000 ₫'],['Giảm giá CODE10','-221.000 ₫'],['Tổng thanh toán','1.989.000 ₫']];
  for (const [l,v] of rows) {
    const isBold = l.startsWith('Tổng');
    await mkText(f, l, 32, y, 24, isBold?'SemiBold':'Regular', isBold?C.text:C.textSub, W/2);
    const tv = await mkText(f, v, W/2, y, 24, isBold?'SemiBold':'Regular', isBold?C.primary:C.text, W/2-32);
    (tv as TextNode).textAlignHorizontal = 'RIGHT';
    y += 40;
    if (l.startsWith('Giảm')) { mkRect(f,'sum_divider',32,y,W-64,1,C.border); y+=16; }
  }
  y += 24;

  // Payment method
  await mkText(f, 'Phương thức thanh toán', 32, y, 30, 'SemiBold', C.text); y += 50;
  const methods = [['💳 Thẻ tín dụng/ghi nợ',true],['🏦 Chuyển khoản ngân hàng',false],['📱 Ví điện tử (MoMo/ZaloPay)',false]];
  for (const [m, selected] of methods) {
    mkRect(f, `pay_${m}`, 32, y, W-64, 88, selected?C.chipBg:C.white, 12);
    (f.children[f.children.length-1] as RectangleNode).strokes = [solid(selected?C.primary:C.border)];
    (f.children[f.children.length-1] as RectangleNode).strokeWeight = selected?2:1;
    await mkText(f, m as string, 56, y+28, 26, selected?'SemiBold':'Regular', C.text, W-120);
    if (selected) await mkText(f,'✓', W-100, y+28, 30, 'SemiBold', C.primary);
    y += 104;
  }
  y += 16;

  // Card input (visible when credit card selected)
  mkRect(f, 'card_input_bg', 32, y, W-64, 240, C.white, 16);
  (f.children[f.children.length-1] as RectangleNode).strokes = [solid(C.border)];
  await mkText(f, 'Số thẻ', 56, y+16, 22, 'Medium', C.textSub, 300);
  mkRect(f, 'card_num_box', 56, y+44, W-112, 64, C.bg, 8);
  await mkText(f, '1234  5678  9012  3456', 72, y+60, 26, 'Regular', C.textMuted, 400);
  await mkText(f, 'Ngày hết hạn', 56, y+124, 22, 'Medium', C.textSub, 220);
  await mkText(f, 'CVV', W/2+16, y+124, 22, 'Medium', C.textSub, 120);
  mkRect(f, 'exp_box', 56, y+152, W/2-72, 64, C.bg, 8);
  mkRect(f, 'cvv_box', W/2+16, y+152, 160, 64, C.bg, 8);
  y += 264;
  y += 16;

  // CAPTCHA slider
  mkRect(f, 'captcha_bg', 32, y, W-64, 88, '#F0FDF4', 12);
  mkRect(f, 'captcha_slider_thumb', 44, y+20, 88, 48, C.success, 8);
  await mkText(f, '  →  Kéo để xác nhận thanh toán', 148, y+28, 24, 'Regular', C.success, W-200);
  y += 112;

  await mkBtn(f, '💳 Thanh toán ngay — 1.989.000 ₫', 32, y, W-64);
  f.resize(W, y+120);
}

async function buildPaymentSuccess(x: number) {
  const f = mkFrame('07 Payment Success', x);
  f.fills = [solid(C.white)];

  let y = 160;
  // Success icon
  const circle = figma.createEllipse();
  circle.name = 'success_circle'; circle.x = W/2-80; circle.y = y;
  circle.resize(160,160); circle.fills = [solid('#DCFCE7')]; f.appendChild(circle);
  await mkText(f, '✓', W/2-30, y+36, 80, 'SemiBold', C.success);
  y += 200;

  await mkText(f, 'Đặt vé thành công!', 0, y, 44, 'SemiBold', C.text, W);
  (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  y += 60;
  await mkText(f, 'Vé của bạn đã được xác nhận.\nKiểm tra email để nhận thông tin chi tiết.', 64, y, 26, 'Regular', C.textSub, W-128);
  (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  y += 100;

  // Order card
  const oc = mkCard(f, 'order_confirm_card', 32, y, W-64, 400);
  await mkText(oc, 'Mã đơn hàng', 24, 16, 22, 'Regular', C.textMuted, 300);
  await mkText(oc, '#VT-2025-001234', 24, 44, 30, 'SemiBold', C.text, 400);
  mkRect(oc, 'd1', 24, 88, W-112, 1, C.border);
  await mkText(oc, 'LiveFest 2025', 24, 100, 28, 'SemiBold', C.text, W-112);
  await mkText(oc, '📅 25/12/2025  19:00', 24, 136, 22, 'Regular', C.textSub, W-112);
  await mkText(oc, '📍 SVĐ Quốc gia Mỹ Đình, Hà Nội', 24, 164, 22, 'Regular', C.textSub, W-112);
  mkRect(oc, 'd2', 24, 200, W-112, 1, C.border);
  await mkText(oc, '2x Vé Thường + 1x Vé VIP', 24, 212, 24, 'Regular', C.text, W-112);
  await mkText(oc, 'Tổng đã thanh toán: 1.989.000 ₫', 24, 244, 26, 'SemiBold', C.primary, W-112);
  mkRect(oc, 'd3', 24, 284, W-112, 1, C.border);
  // QR preview
  mkImg(oc, 'QR Code (ticket)', W/2-80-32, 296, 160, 160);
  await mkText(oc, 'Quét QR để vào cổng', 24, 296+168, 22, 'Regular', C.textMuted, W-112);
  y += 424;
  y += 24;

  await mkBtn(f, 'Xem vé của tôi', 32, y, W-64);
  y += 104;
  await mkBtn(f, 'Về trang chủ', 32, y, W-64, 88, C.chipBg, C.primary);
  f.resize(W, y+120);
}

async function buildMyTickets(x: number) {
  const f = mkFrame('08 My Tickets', x);
  await drawSimpleHeader(f, 'Vé của tôi', false);
  await drawBottomNav(f, 3);

  let y = CONTENT_TOP + 16;
  // Tab bar
  const tabs = ['ĐÃ MUA','ĐANG BÁN','MEMBERSHIP'];
  const tw = W/3;
  for (let i=0;i<3;i++) {
    mkRect(f, `tab_bg_${i}`, i*tw, y, tw, 72, C.white);
    await mkText(f, tabs[i], i*tw, y+16, 22, i===0?'SemiBold':'Regular', i===0?C.primary:C.textMuted, tw);
    (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
    if (i===0) mkRect(f, 'tab_indicator', i*tw+20, y+64, tw-40, 4, C.primary, 2);
  }
  y += 80;
  // Sub-tabs
  mkRect(f, 'subtab_upcoming', 32, y, 200, 56, C.primary, 28);
  await mkText(f, 'Sắp diễn ra', 32, y+14, 22, 'SemiBold', C.white, 200);
  (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  mkRect(f, 'subtab_ended', 248, y, 160, 56, C.chipBg, 28);
  await mkText(f, 'Đã kết thúc', 248, y+14, 22, 'Regular', C.textSub, 160);
  (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  y += 80;

  // Ticket cards
  for (let i=0;i<3;i++) {
    const tc = mkCard(f, `myticket_${i}`, 32, y, W-64, 240);
    mkImg(tc, `Ticket event ${i+1} poster`, 0, 0, 160, 240, 12);
    await mkText(tc, `LiveFest 2025 - Vé ${i===0?'Thường':i===1?'VIP':'VVIP'}`, 176, 16, 26, 'SemiBold', C.text, W-240);
    await mkText(tc, '📅 25/12/2025  •  19:00', 176, 56, 22, 'Regular', C.textSub, W-240);
    await mkText(tc, '📍 SVĐ Mỹ Đình, Hà Nội', 176, 88, 22, 'Regular', C.textSub, W-240);
    mkRect(tc, 'status_chip', 176, 124, 120, 44, '#DCFCE7', 22);
    await mkText(tc, 'Còn hiệu lực', 184, 134, 20, 'Medium', C.success, 110);
    await mkText(tc, 'Xem QR vé', 176, 180, 22, 'SemiBold', C.primary, 160);
    await mkText(tc, 'Sang nhượng', 360, 180, 22, 'SemiBold', C.accent, 160);
    y += 256;
  }
  f.resize(W, y + NAV_H + 32);
}

async function buildProfile(x: number) {
  const f = mkFrame('09 Profile', x);
  await drawSimpleHeader(f, 'Cá nhân', false);
  await drawBottomNav(f, 4);

  let y = CONTENT_TOP + 32;
  // Avatar & info
  mkImg(f, 'User Avatar', W/2-64, y, 128, 128, 64);
  y += 144;
  await mkText(f, 'Nguyễn Văn A', 0, y, 36, 'SemiBold', C.text, W);
  (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  y += 48;
  await mkText(f, 'nguyenvana@gmail.com', 0, y, 24, 'Regular', C.textSub, W);
  (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  y += 56;

  // Organizer Hub button
  await mkBtn(f, '🎪 Trung tâm Tổ chức sự kiện', 32, y, W-64, 80, C.chipBg, C.primary);
  y += 100;

  // Menu items
  const items = ['Thông tin tài khoản','Bảo mật','Phương thức thanh toán','Trung tâm hỗ trợ','Ngôn ngữ: Tiếng Việt','Đăng xuất'];
  for (const item of items) {
    mkRect(f, `menu_${item}`, 32, y, W-64, 88, C.white, 12);
    (f.children[f.children.length-1] as RectangleNode).strokes = [solid(C.border)];
    (f.children[f.children.length-1] as RectangleNode).strokeWeight = 1;
    await mkText(f, item, 56, y+28, 26, item==='Đăng xuất'?'SemiBold':'Regular', item==='Đăng xuất'?C.error:C.text, W-140);
    await mkText(f, '›', W-96, y+28, 36, 'Regular', C.textMuted);
    y += 104;
  }
  f.resize(W, y + NAV_H + 32);
}

async function buildLogin(x: number) {
  const f = mkFrame('10 Login', x);
  f.fills = [solid(C.white)];

  let y = 120;
  mkImg(f, 'VibeTix Logo', W/2-80, y, 160, 160, 16);
  y += 196;
  await mkText(f, 'Chào mừng trở lại!', 32, y, 44, 'SemiBold', C.text, W-64);
  (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  y += 60;
  await mkText(f, 'Đăng nhập để tiếp tục với VibeTix', 32, y, 26, 'Regular', C.textSub, W-64);
  (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  y += 80;

  await mkInput(f, 'Email', 'you@example.com', 32, y, W-64); y += 130;
  await mkInput(f, 'Mật khẩu', '••••••••', 32, y, W-64); y += 130;
  await mkText(f, 'Quên mật khẩu?', W-220, y-20, 24, 'Medium', C.primary); y += 24;
  await mkBtn(f, 'Đăng nhập', 32, y, W-64); y += 104;
  mkRect(f, 'divider_or', 32, y+28, W/2-80, 1, C.border);
  await mkText(f, 'hoặc', W/2-30, y+12, 24, 'Regular', C.textMuted);
  mkRect(f, 'divider_or2', W/2+50, y+28, W/2-80, 1, C.border);
  y += 64;
  await mkBtn(f, '🌐 Tiếp tục với Google', 32, y, W-64, 88, C.bg, C.text); y += 104;
  await mkText(f, 'Chưa có tài khoản?  Đăng ký ngay', 32, y, 26, 'Regular', C.textSub, W-64);
  (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  f.resize(W, y+80);
}

// ──────────────── ORGANIZER SCREENS ──────────────────────────────────────────

async function buildOrganizerHub(x: number) {
  const f = mkFrame('11 Organizer Hub', x);
  await drawSimpleHeader(f, 'Trung tâm tổ chức', false);
  await drawBottomNav(f, 2);

  let y = CONTENT_TOP + 32;
  // Organizer profile strip
  mkRect(f, 'org_profile_strip', 32, y, W-64, 120, C.primary, 20);
  mkImg(f, 'Org Logo', 48, y+24, 72, 72, 36);
  await mkText(f, 'Saigon Events Co.', 140, y+24, 28, 'SemiBold', C.white, 400);
  await mkText(f, '✓ Đã xác minh  •  5 sự kiện', 140, y+62, 22, 'Regular', '#BFDBFE', 400);
  await mkText(f, 'Đổi hồ sơ ›', 140, y+94, 22, 'Medium', '#93C5FD', 200);
  y += 140;

  // Quick stats
  await mkText(f, 'Tổng quan', 32, y, 30, 'SemiBold', C.text); y += 48;
  const stats = [['5','Sự kiện'],['248','Vé bán ra'],['12,4M','Doanh thu']];
  const sw = (W-64)/3;
  for (let i=0;i<3;i++) {
    const sc = mkCard(f, `stat_${i}`, 32+i*sw, y, sw-8, 120);
    await mkText(sc, stats[i][0], 16, 16, 44, 'SemiBold', C.primary, sw-40);
    (sc.children[sc.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
    await mkText(sc, stats[i][1], 16, 68, 22, 'Regular', C.textSub, sw-40);
    (sc.children[sc.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  }
  y += 136;

  // Management menu
  await mkText(f, 'Quản lý', 32, y, 30, 'SemiBold', C.text); y += 48;
  const menus = [
    ['📋 Sự kiện của tôi','Xem & quản lý sự kiện'],
    ['➕ Tạo sự kiện mới','Bắt đầu tạo sự kiện'],
    ['🎫 Loại vé','Quản lý loại vé & giá'],
    ['💰 Đơn hàng','Xem đơn & doanh thu'],
    ['🏷️ Mã giảm giá','Tạo & quản lý voucher'],
    ['👥 Nhân viên','Phân quyền nhân viên'],
    ['📷 Quét QR Check-in','Xác nhận vé tại cổng'],
    ['⭐ Nghệ sĩ','Gắn nghệ sĩ vào sự kiện'],
    ['📣 Thông báo hàng loạt','Gửi tin tới người mua'],
  ];
  for (const [title, sub] of menus) {
    mkRect(f, `menu_${title}`, 32, y, W-64, 96, C.white, 12);
    (f.children[f.children.length-1] as RectangleNode).strokes = [solid(C.border)];
    await mkText(f, title, 56, y+14, 26, 'SemiBold', C.text, W-140);
    await mkText(f, sub, 56, y+52, 22, 'Regular', C.textSub, W-200);
    await mkText(f, '›', W-96, y+32, 36, 'Regular', C.textMuted);
    y += 112;
  }
  f.resize(W, y + NAV_H + 32);
}

async function buildMyEventsList(x: number) {
  const f = mkFrame('12 My Events List', x);
  await drawSimpleHeader(f, 'Sự kiện của tôi', true);

  let y = CONTENT_TOP + 32;
  // Filter bar
  const statuses = ['Tất cả','Nháp','Chờ duyệt','Đã duyệt','Đang diễn ra'];
  let cx = 32;
  for (const s of statuses) {
    const sw2 = s.length*17+32;
    mkRect(f, `stat_${s}`, cx, y, sw2, 56, s==='Tất cả'?C.primary:C.chipBg, 28);
    await mkText(f, s, cx+16, y+14, 20, s==='Tất cả'?'SemiBold':'Regular', s==='Tất cả'?C.white:C.textSub, sw2-32);
    cx += sw2+12;
  }
  y += 80;

  // Event list items
  const events2 = [
    {name:'LiveFest 2025',date:'25/12/2025',status:'Đã duyệt',statusColor:C.success,sold:248,total:500},
    {name:'Rock Night 2026',date:'01/01/2026',status:'Chờ duyệt',statusColor:C.warning,sold:0,total:200},
    {name:'Workshop Âm nhạc',date:'15/01/2026',status:'Nháp',statusColor:C.textMuted,sold:0,total:50},
  ];
  for (const ev of events2) {
    const ec = mkCard(f, `ev_${ev.name}`, 32, y, W-64, 220);
    mkImg(ec, `${ev.name} poster`, 0, 0, 180, 220, 12);
    await mkText(ec, ev.name, 196, 16, 28, 'SemiBold', C.text, W-296);
    await mkText(ec, `📅 ${ev.date}`, 196, 56, 22, 'Regular', C.textSub, W-296);
    mkRect(ec, 'status_chip', 196, 88, ev.status.length*18+24, 44, `${ev.statusColor}20`, 22);
    await mkText(ec, ev.status, 208, 98, 20, 'Medium', ev.statusColor, ev.status.length*18);
    await mkText(ec, `🎫 ${ev.sold}/${ev.total} vé bán`, 196, 144, 22, 'Regular', C.textSub, W-296);
    await mkText(ec, 'Quản lý ›', 196, 178, 22, 'SemiBold', C.primary, 160);
    y += 236;
  }
  await mkBtn(f, '+ Tạo sự kiện mới', 32, y+16, W-64);
  f.resize(W, y+136);
}

async function buildCreateEventStep1(x: number) {
  const f = mkFrame('13 Create Event — Step1 Basic Info', x);
  await drawSimpleHeader(f, 'Tạo sự kiện', true);

  // Step indicator
  mkRect(f, 'step_bg', 0, HDR_H, W, 60, C.white);
  const steps5 = ['Cơ bản','Lịch/Địa điểm','Vé','Media','Xem trước'];
  for (let i=0;i<5;i++) {
    const sw = W/5;
    mkRect(f, `step_bar_${i}`, i*sw, HDR_H+52, sw, 4, i===0?C.primary:C.border);
    await mkText(f, `${i+1}`, i*sw+sw/2-10, HDR_H+16, 20, i===0?'SemiBold':'Regular', i===0?C.primary:C.textMuted, 20);
    await mkText(f, steps5[i], i*sw, HDR_H+30, 18, 'Regular', i===0?C.primary:C.textMuted, sw);
    (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  }

  let y = HDR_H + 80;
  await mkText(f, 'Thông tin cơ bản', 32, y, 32, 'SemiBold', C.text); y += 52;
  await mkInput(f, 'Tên sự kiện *', 'VD: LiveFest 2025', 32, y, W-64); y += 130;

  // Category dropdown
  await mkText(f, 'Thể loại *', 32, y, 24, 'Medium', C.textSub); y += 36;
  mkRect(f, 'category_dropdown', 32, y, W-64, 74, C.white, 12);
  (f.children[f.children.length-1] as RectangleNode).strokes = [solid(C.border)];
  await mkText(f, 'Live Music', 56, y+20, 26, 'Regular', C.text, W-140);
  await mkText(f, '▾', W-96, y+20, 30, 'Regular', C.textMuted);
  y += 94;

  // Description
  await mkText(f, 'Mô tả sự kiện *', 32, y, 24, 'Medium', C.textSub); y += 36;
  mkRect(f, 'desc_box', 32, y, W-64, 240, C.white, 12);
  (f.children[f.children.length-1] as RectangleNode).strokes = [solid(C.border)];
  await mkText(f, 'Nhập mô tả chi tiết về sự kiện của bạn...', 52, y+24, 24, 'Regular', C.textMuted, W-100);
  y += 260;

  // Organizer selector
  await mkText(f, 'Tổ chức bởi *', 32, y, 24, 'Medium', C.textSub); y += 36;
  mkRect(f, 'org_select', 32, y, W-64, 74, C.chipBg, 12);
  mkImg(f, 'Org logo small', 52, y+11, 52, 52, 26);
  await mkText(f, 'Saigon Events Co.', 120, y+22, 26, 'SemiBold', C.text, 360);
  await mkText(f, '▾', W-96, y+22, 30, 'Regular', C.textMuted);
  y += 94;

  await mkBtn(f, 'Tiếp theo: Lịch & Địa điểm →', 32, y, W-64);
  f.resize(W, y+120);
}

async function buildCreateEventStep2(x: number) {
  const f = mkFrame('14 Create Event — Step2 Date Venue', x);
  await drawSimpleHeader(f, 'Tạo sự kiện', true);
  mkRect(f, 'step_bg', 0, HDR_H, W, 60, C.white);
  const steps5 = ['Cơ bản','Lịch/Địa điểm','Vé','Media','Xem trước'];
  for (let i=0;i<5;i++) {
    const sw = W/5;
    mkRect(f, `step_bar_${i}`, i*sw, HDR_H+52, sw, 4, i<=1?C.primary:C.border);
    await mkText(f, steps5[i], i*sw, HDR_H+30, 18, 'Regular', i<=1?C.primary:C.textMuted, sw);
    (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  }

  let y = HDR_H + 80;
  await mkText(f, 'Thời gian & Địa điểm', 32, y, 32, 'SemiBold', C.text); y += 52;

  // Date pickers
  const datePairs = [['Ngày bắt đầu *','25/12/2025'],['Giờ bắt đầu *','19:00'],['Ngày kết thúc *','25/12/2025'],['Giờ kết thúc *','23:00']];
  for (let i=0;i<4;i+=2) {
    const hw = (W-80)/2;
    for (let j=0;j<2;j++) {
      const [label, val] = datePairs[i+j];
      const bx = 32 + j*(hw+16);
      await mkText(f, label, bx, y, 22, 'Medium', C.textSub, hw); y2:
      mkRect(f, `dp_${label}`, bx, y+30, hw, 74, C.white, 12);
      (f.children[f.children.length-1] as RectangleNode).strokes = [solid(C.border)];
      await mkText(f, (j===0?'📅 ':'⏰ ')+val, bx+16, y+48, 24, 'Regular', C.text, hw-32);
    }
    y += 120;
  }

  // Venue selector
  y += 8;
  await mkText(f, 'Địa điểm *', 32, y, 24, 'Medium', C.textSub); y += 36;
  mkRect(f, 'venue_dropdown', 32, y, W-64, 74, C.white, 12);
  (f.children[f.children.length-1] as RectangleNode).strokes = [solid(C.border)];
  await mkText(f, '📍 SVĐ Quốc gia Mỹ Đình', 56, y+20, 26, 'Regular', C.text, W-140);
  await mkText(f, '▾', W-96, y+20, 30, 'Regular', C.textMuted);
  y += 94;

  // Map preview
  mkImg(f, 'Map Preview', 32, y, W-64, 280, 16);
  y += 300;

  await mkBtn(f, 'Tiếp theo: Loại vé →', 32, y, W-64);
  f.resize(W, y+120);
}

async function buildCreateEventStep3(x: number) {
  const f = mkFrame('15 Create Event — Step3 Ticket Types', x);
  await drawSimpleHeader(f, 'Tạo sự kiện', true);
  mkRect(f, 'step_bg', 0, HDR_H, W, 60, C.white);
  const steps5 = ['Cơ bản','Lịch/Địa điểm','Vé','Media','Xem trước'];
  for (let i=0;i<5;i++) {
    const sw = W/5;
    mkRect(f, `step_bar_${i}`, i*sw, HDR_H+52, sw, 4, i<=2?C.primary:C.border);
    await mkText(f, steps5[i], i*sw, HDR_H+30, 18, 'Regular', i<=2?C.primary:C.textMuted, sw);
    (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  }

  let y = HDR_H + 80;
  await mkText(f, 'Loại vé', 32, y, 32, 'SemiBold', C.text); y += 52;

  // Ticket type cards
  const ttypes2 = ['Vé Thường','Vé VIP'];
  for (const tt of ttypes2) {
    const tc = mkCard(f, `tt_create_${tt}`, 32, y, W-64, 300);
    await mkText(tc, tt, 24, 16, 28, 'SemiBold', C.text, W-112);
    await mkInput(tc, 'Tên loại vé', tt, 24, 56, W-112);
    await mkInput(tc, 'Giá vé (₫)', tt==='Vé Thường'?'500000':'1200000', 24, 166, (W-112)/2-8);
    await mkInput(tc, 'Số lượng', tt==='Vé Thường'?'500':'100', (W-112)/2+32, 166, (W-112)/2-8);
    const delBtn = mkRect(tc, 'del_btn', W-120, 16, 72, 48, '#FEE2E2', 8);
    await mkText(tc, '🗑', W-110, 20, 30, 'Regular', C.error);
    y += 316;
  }

  // Add type button
  mkRect(f, 'add_tt_btn', 32, y, W-64, 88, C.chipBg, 12);
  (f.children[f.children.length-1] as RectangleNode).strokes = [solid(C.primary)];
  (f.children[f.children.length-1] as RectangleNode).dashPattern = [8,4];
  await mkText(f, '+ Thêm loại vé', 0, y+24, 28, 'SemiBold', C.primary, W);
  (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  y += 112;

  await mkBtn(f, 'Tiếp theo: Media →', 32, y, W-64);
  f.resize(W, y+120);
}

async function buildCreateEventStep4(x: number) {
  const f = mkFrame('16 Create Event — Step4 Media', x);
  await drawSimpleHeader(f, 'Tạo sự kiện', true);
  mkRect(f, 'step_bg', 0, HDR_H, W, 60, C.white);
  const steps5 = ['Cơ bản','Lịch/Địa điểm','Vé','Media','Xem trước'];
  for (let i=0;i<5;i++) {
    const sw = W/5;
    mkRect(f, `step_bar_${i}`, i*sw, HDR_H+52, sw, 4, i<=3?C.primary:C.border);
    await mkText(f, steps5[i], i*sw, HDR_H+30, 18, 'Regular', i<=3?C.primary:C.textMuted, sw);
    (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  }

  let y = HDR_H + 80;
  await mkText(f, 'Hình ảnh sự kiện', 32, y, 32, 'SemiBold', C.text); y += 52;

  // Poster upload
  await mkText(f, 'Poster sự kiện *', 32, y, 24, 'Medium', C.textSub); y += 36;
  mkImg(f, 'Poster Upload Area (3:4)', 32, y, W-64, 400, 16);
  mkRect(f, 'poster_overlay', 32, y, W-64, 400, C.overlay, 16);
  await mkText(f, '📷\nTải lên poster\n(700×933px, tỉ lệ 3:4)', 0, y+148, 28, 'Medium', C.white, W);
  (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  y += 424;

  // Banner upload
  await mkText(f, 'Banner sự kiện', 32, y, 24, 'Medium', C.textSub); y += 36;
  mkImg(f, 'Banner Upload Area (16:9)', 32, y, W-64, 220, 16);
  mkRect(f, 'banner_overlay', 32, y, W-64, 220, C.overlay, 16);
  await mkText(f, '📷  Tải lên banner (1920×1080px)', 0, y+92, 26, 'Medium', C.white, W);
  (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  y += 244;

  // Additional photos
  await mkText(f, 'Ảnh thêm (tối đa 8)', 32, y, 24, 'Medium', C.textSub); y += 36;
  for (let i=0;i<3;i++) {
    mkImg(f, `Extra photo ${i+1}`, 32+i*220, y, 196, 196, 12);
  }
  mkRect(f, 'add_photo_btn', 32+3*220, y, 196, 196, C.chipBg, 12);
  (f.children[f.children.length-1] as RectangleNode).strokes = [solid(C.border)];
  await mkText(f, '+', 32+3*220, y+60, 72, 'Regular', C.textMuted, 196);
  (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  y += 220;

  await mkBtn(f, 'Tiếp theo: Xem trước →', 32, y, W-64);
  f.resize(W, y+120);
}

async function buildCreateEventStep5(x: number) {
  const f = mkFrame('17 Create Event — Step5 Review Submit', x);
  await drawSimpleHeader(f, 'Tạo sự kiện', true);
  mkRect(f, 'step_bg', 0, HDR_H, W, 60, C.white);
  const steps5 = ['Cơ bản','Lịch/Địa điểm','Vé','Media','Xem trước'];
  for (let i=0;i<5;i++) {
    const sw = W/5;
    mkRect(f, `step_bar_${i}`, i*sw, HDR_H+52, sw, 4, C.primary);
    await mkText(f, steps5[i], i*sw, HDR_H+30, 18, 'SemiBold', C.primary, sw);
    (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  }

  let y = HDR_H + 80;
  await mkText(f, 'Xem trước & Gửi duyệt', 32, y, 32, 'SemiBold', C.text); y += 52;

  // Preview card
  const pc = mkCard(f, 'preview_card', 32, y, W-64, 800);
  mkImg(pc, 'Poster preview', 0, 0, W-64, 320, 12);
  await mkText(pc, 'LiveFest 2025 — Đêm nhạc cuối năm', 24, 336, 32, 'SemiBold', C.text, W-112);
  await mkText(pc, '📅 25/12/2025 • 19:00 – 23:00', 24, 388, 24, 'Regular', C.textSub, W-112);
  await mkText(pc, '📍 SVĐ Quốc gia Mỹ Đình, Hà Nội', 24, 420, 24, 'Regular', C.textSub, W-112);
  mkRect(pc, 'div', 24, 460, W-112, 1, C.border);
  await mkText(pc, 'Vé:', 24, 472, 24, 'Medium', C.textSub, 80);
  await mkText(pc, 'Thường 500k • VIP 1,2M', 100, 472, 24, 'Regular', C.text, 450);
  mkRect(pc, 'div2', 24, 508, W-112, 1, C.border);
  await mkText(pc, 'Ban tổ chức:', 24, 520, 24, 'Medium', C.textSub, 200);
  await mkText(pc, 'Saigon Events Co.', 200, 520, 24, 'Regular', C.text, 350);
  // Status badge
  mkRect(pc, 'draft_badge', 24, 556, 140, 44, '#FEF9C3', 22);
  await mkText(pc, '📝 Nháp', 36, 568, 22, 'Medium', C.warning, 120);
  mkRect(pc, 'd3', 24, 616, W-112, 1, C.border);
  await mkText(pc, '⚠️  Sau khi gửi duyệt, sự kiện sẽ được admin kiểm tra trong 1–2 ngày làm việc.', 24, 628, 22, 'Regular', C.warning, W-112);
  y += 816;

  await mkBtn(f, '📤 Gửi duyệt', 32, y, W-64, 88, C.primary);
  y += 104;
  await mkBtn(f, 'Lưu nháp', 32, y, W-64, 88, C.bg, C.text);
  f.resize(W, y+120);
}

async function buildTicketTypeManagement(x: number) {
  const f = mkFrame('18 Ticket Type Management', x);
  await drawSimpleHeader(f, 'Loại vé — LiveFest 2025', true);

  let y = CONTENT_TOP + 32;
  await mkText(f, 'Loại vé sự kiện', 32, y, 30, 'SemiBold', C.text); y += 50;

  const tts = [{n:'Vé Thường',p:'500.000 ₫',sold:198,total:500},{n:'Vé VIP',p:'1.200.000 ₫',sold:48,total:100},{n:'Vé VVIP',p:'2.500.000 ₫',sold:2,total:10}];
  for (const tt of tts) {
    const tc = mkCard(f, `tt_${tt.n}`, 32, y, W-64, 180);
    await mkText(tc, tt.n, 24, 16, 28, 'SemiBold', C.text, 400);
    await mkText(tc, tt.p, 24, 56, 28, 'SemiBold', C.primary, 300);
    // Progress bar
    const pct = tt.sold/tt.total;
    mkRect(tc, 'prog_bg', 24, 100, W-112, 12, C.border, 6);
    mkRect(tc, 'prog_fill', 24, 100, Math.round((W-112)*pct), 12, C.primary, 6);
    await mkText(tc, `${tt.sold}/${tt.total} đã bán`, 24, 120, 22, 'Regular', C.textSub, 300);
    // Actions
    await mkBtn(tc, 'Sửa', W-280, 140, 120, 52, C.chipBg, C.primary);
    await mkBtn(tc, 'Xóa', W-152, 140, 120, 52, '#FEE2E2', C.error);
    y += 196;
  }

  await mkBtn(f, '+ Thêm loại vé', 32, y+16, W-64);
  f.resize(W, y+136);
}

async function buildOrderManagement(x: number) {
  const f = mkFrame('19 Order Management', x);
  await drawSimpleHeader(f, 'Đơn hàng — LiveFest 2025', true);

  let y = CONTENT_TOP + 24;
  // Stats row
  const ostats = [['248','Đơn hàng'],['1.240','Vé bán'],['124M','Doanh thu']];
  const osw = (W-64)/3;
  for (let i=0;i<3;i++) {
    const sc = mkCard(f, `ostat_${i}`, 32+i*osw, y, osw-8, 100);
    await mkText(sc, ostats[i][0], 16, 12, 36, 'SemiBold', C.primary, osw-40);
    (sc.children[sc.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
    await mkText(sc, ostats[i][1], 16, 56, 20, 'Regular', C.textSub, osw-40);
    (sc.children[sc.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  }
  y += 116;

  // Search
  mkRect(f, 'search_bar', 32, y, W-64, 72, C.white, 36);
  (f.children[f.children.length-1] as RectangleNode).strokes = [solid(C.border)];
  await mkText(f, '🔍  Tìm đơn hàng, email...', 56, y+18, 24, 'Regular', C.textMuted, W-120);
  y += 92;

  // Order list
  const orders = [
    {id:'#VT001234',email:'user@gmail.com',items:'2x Thường',amount:'1.000.000 ₫',status:'PAID'},
    {id:'#VT001235',email:'another@mail.vn',items:'1x VIP',amount:'1.200.000 ₫',status:'PAID'},
    {id:'#VT001236',email:'pending@test.com',items:'3x Thường',amount:'1.500.000 ₫',status:'PENDING'},
  ];
  const statusColors: {[k:string]:string} = {PAID:C.success,PENDING:C.warning};
  for (const ord of orders) {
    const oc = mkCard(f, `ord_${ord.id}`, 32, y, W-64, 140);
    await mkText(oc, ord.id, 24, 12, 26, 'SemiBold', C.text, 200);
    await mkText(oc, ord.email, 24, 46, 22, 'Regular', C.textSub, W-200);
    await mkText(oc, ord.items, 24, 78, 22, 'Regular', C.text, 300);
    await mkText(oc, ord.amount, W-300, 12, 26, 'SemiBold', C.primary, 260);
    mkRect(oc, 'st_chip', W-220, 50, 180, 44, statusColors[ord.status]+'20', 22);
    await mkText(oc, ord.status, W-208, 62, 20, 'SemiBold', statusColors[ord.status], 160);
    await mkText(oc, 'Xem chi tiết ›', 24, 106, 22, 'Medium', C.primary, 200);
    y += 156;
  }
  f.resize(W, y+80);
}

async function buildDiscountManagement(x: number) {
  const f = mkFrame('20 Discount Management', x);
  await drawSimpleHeader(f, 'Mã giảm giá', true);

  let y = CONTENT_TOP + 32;
  await mkBtn(f, '+ Tạo mã mới', W-236, y-16, 204, 72);
  await mkText(f, 'Voucher hiện tại', 32, y, 30, 'SemiBold', C.text); y += 50;

  const discounts = [
    {code:'EARLY10',type:'10%',usage:'45/100',status:'Đang hoạt động'},
    {code:'VIP500K',type:'500.000 ₫',usage:'8/20',status:'Đang hoạt động'},
    {code:'EXPIRED20',type:'20%',usage:'200/200',status:'Hết hạn'},
  ];
  for (const d of discounts) {
    const dc = mkCard(f, `dc_${d.code}`, 32, y, W-64, 150);
    mkRect(dc, 'left', 0, 0, 10, 150, d.status==='Đang hoạt động'?C.success:C.textMuted, 4);
    await mkText(dc, d.code, 28, 16, 30, 'SemiBold', C.text, 300);
    await mkText(dc, `Giảm ${d.type}`, 28, 56, 24, 'Regular', C.primary, 280);
    await mkText(dc, `Đã dùng: ${d.usage}`, 28, 90, 22, 'Regular', C.textSub, 280);
    // Usage bar
    const [used,total2] = d.usage.split('/').map(Number);
    mkRect(dc, 'pb', 28, 118, W-140, 10, C.border, 5);
    mkRect(dc, 'pf', 28, 118, Math.round((W-140)*used/total2), 10, d.status==='Đang hoạt động'?C.success:C.textMuted, 5);
    mkRect(dc, 'status_chip', W-260, 16, 216, 44, d.status==='Đang hoạt động'?'#DCFCE7':'#F1F5F9', 22);
    await mkText(dc, d.status, W-248, 28, 20, 'Medium', d.status==='Đang hoạt động'?C.success:C.textMuted, 200);
    y += 166;
  }
  f.resize(W, y+80);
}

async function buildStaffManagement(x: number) {
  const f = mkFrame('21 Staff Management', x);
  await drawSimpleHeader(f, 'Quản lý nhân viên', true);

  let y = CONTENT_TOP + 32;
  await mkBtn(f, '+ Thêm nhân viên', W-296, y-16, 264, 72);
  await mkText(f, 'Nhân viên sự kiện', 32, y, 30, 'SemiBold', C.text); y += 50;

  const staff = [
    {name:'Trần Thị B',email:'b@gmail.com',role:'manager'},
    {name:'Lê Văn C',email:'c@gmail.com',role:'check_in_staff'},
    {name:'Phạm Thị D',email:'d@gmail.com',role:'check_in_staff'},
  ];
  for (const s of staff) {
    const sc = mkCard(f, `staff_${s.name}`, 32, y, W-64, 120);
    mkImg(sc, `${s.name} avatar`, 16, 16, 88, 88, 44);
    await mkText(sc, s.name, 120, 16, 26, 'SemiBold', C.text, 350);
    await mkText(sc, s.email, 120, 50, 22, 'Regular', C.textSub, 350);
    mkRect(sc, 'role_chip', 120, 80, s.role==='manager'?160:240, 40, s.role==='manager'?C.chipBg:'#F0FDF4', 20);
    await mkText(sc, s.role==='manager'?'Quản lý':'Nhân viên check-in', 132, 90, 20, 'Medium', s.role==='manager'?C.primary:C.success, s.role==='manager'?136:220);
    await mkText(sc, '⋮', W-72, 40, 40, 'Regular', C.textMuted);
    y += 136;
  }
  f.resize(W, y+120);
}

async function buildQrScanner(x: number) {
  const f = mkFrame('22 QR Scanner Check-in', x);
  f.fills = [solid('#0F172A')];
  await drawSimpleHeader(f, 'Quét QR Check-in', true);
  (f.children[0] as RectangleNode).fills = [solid('#0F172A')];

  let y = CONTENT_TOP + 60;
  // Camera viewfinder
  mkImg(f, 'Camera Viewfinder', 32, y, W-64, W-64, 20);
  // Corner overlays
  const corners = [[32,y],[W-96,y],[32,y+W-96],[W-96,y+W-96]];
  for (const [cx2,cy] of corners) {
    mkRect(f, 'corner', cx2, cy, 64, 8, C.primary);
    mkRect(f, 'corner_v', cx2, cy, 8, 64, C.primary);
  }
  // Scan line
  mkRect(f, 'scan_line', 32, y+(W-64)/2, W-64, 3, C.primary);
  y += W-64+40;

  // Status
  mkRect(f, 'status_bg', 32, y, W-64, 120, '#1E293B', 20);
  await mkText(f, '📷 Đưa QR vé vào khung hình', 0, y+28, 26, 'Regular', C.textMuted, W);
  (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  await mkText(f, 'Đang tìm kiếm mã QR...', 0, y+68, 24, 'Regular', '#64748B', W);
  (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  y += 140;

  // Manual entry
  await mkText(f, 'hoặc nhập mã thủ công', 0, y, 22, 'Regular', C.textMuted, W);
  (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  y += 36;
  mkRect(f, 'manual_input', 32, y, W-180, 74, '#1E293B', 12);
  (f.children[f.children.length-1] as RectangleNode).strokes = [solid('#334155')];
  await mkText(f, 'Nhập mã vé...', 52, y+20, 24, 'Regular', '#475569', W-220);
  await mkBtn(f, 'Xác nhận', W-140, y-8, 108, 90, C.primary);
  y += 100;

  // Check-in result (success state shown)
  mkRect(f, 'result_bg', 32, y, W-64, 200, '#064E3B', 20);
  await mkText(f, '✓  VÉ HỢP LỆ', 0, y+24, 36, 'SemiBold', C.success, W);
  (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  await mkText(f, 'Nguyễn Văn A  •  Vé Thường', 0, y+72, 26, 'Regular', C.white, W);
  (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  await mkText(f, 'Đã check-in lúc 19:04', 0, y+108, 22, 'Regular', '#6EE7B7', W);
  (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  await mkText(f, '🎉  Chào mừng tới LiveFest 2025!', 0, y+148, 24, 'Regular', '#6EE7B7', W);
  (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
  f.resize(W, y+220);
}

async function buildEventStars(x: number) {
  const f = mkFrame('23 Event Stars Management', x);
  await drawSimpleHeader(f, 'Nghệ sĩ — LiveFest 2025', true);

  let y = CONTENT_TOP + 32;
  await mkBtn(f, '+ Thêm nghệ sĩ', W-248, y-16, 216, 72);
  await mkText(f, 'Nghệ sĩ tham gia', 32, y, 30, 'SemiBold', C.text); y += 50;

  const stars = [{name:'Sơn Tùng M-TP',role:'Headliner',order:1},{name:'Mỹ Tâm',role:'Main Act',order:2},{name:'Đen Vâu',role:'Supporting',order:3}];
  for (const s of stars) {
    const sc = mkCard(f, `star_${s.name}`, 32, y, W-64, 128);
    mkImg(sc, `${s.name} avatar`, 16, 16, 96, 96, 48);
    await mkText(sc, s.name, 128, 16, 28, 'SemiBold', C.text, 350);
    await mkText(sc, s.role, 128, 56, 22, 'Regular', C.primary, 280);
    await mkText(sc, `#${s.order} thứ tự hiển thị`, 128, 88, 20, 'Regular', C.textSub, 280);
    await mkText(sc, '☰', W-72, 44, 36, 'Regular', C.textMuted);
    y += 144;
  }
  // Search to add stars
  y += 16;
  await mkText(f, 'Tìm kiếm & thêm nghệ sĩ', 32, y, 26, 'SemiBold', C.text); y += 44;
  mkRect(f, 'star_search', 32, y, W-64, 72, C.white, 36);
  (f.children[f.children.length-1] as RectangleNode).strokes = [solid(C.border)];
  await mkText(f, '🔍  Tên nghệ sĩ...', 56, y+18, 24, 'Regular', C.textMuted, W-120);
  y += 92;
  // Search result
  const sr = mkCard(f, 'star_search_result', 32, y, W-64, 96);
  mkImg(sr, 'Star search result avatar', 16, 12, 72, 72, 36);
  await mkText(sr, 'Hoàng Thùy Linh', 104, 16, 26, 'SemiBold', C.text, 350);
  await mkText(sr, 'Pop • 125K followers', 104, 52, 22, 'Regular', C.textSub, 350);
  await mkBtn(sr, '+ Thêm', W-184, 24, 152, 56);
  f.resize(W, y+128);
}

async function buildBlastNotification(x: number) {
  const f = mkFrame('24 Blast Notification', x);
  await drawSimpleHeader(f, 'Thông báo hàng loạt', true);

  let y = CONTENT_TOP + 32;
  await mkText(f, 'Gửi thông báo tới người mua', 32, y, 30, 'SemiBold', C.text); y += 52;

  // Target selector
  await mkText(f, 'Đối tượng gửi', 32, y, 24, 'Medium', C.textSub); y += 36;
  const targets = ['Tất cả người mua vé','Chỉ Vé Thường','Chỉ Vé VIP & VVIP'];
  for (const t of targets) {
    mkRect(f, `tgt_${t}`, 32, y, W-64, 72, t===targets[0]?C.chipBg:C.white, 12);
    (f.children[f.children.length-1] as RectangleNode).strokes = [solid(t===targets[0]?C.primary:C.border)];
    mkRect(f, `radio_${t}`, 52, y+22, 28, 28, C.white, 14);
    (f.children[f.children.length-1] as RectangleNode).strokes = [solid(t===targets[0]?C.primary:C.border)];
    (f.children[f.children.length-1] as RectangleNode).strokeWeight = t===targets[0]?3:1;
    await mkText(f, t, 96, y+20, 24, t===targets[0]?'SemiBold':'Regular', C.text, W-160);
    y += 88;
  }
  y += 16;

  // Message
  await mkText(f, 'Tiêu đề thông báo *', 32, y, 24, 'Medium', C.textSub); y += 36;
  mkRect(f, 'notif_title', 32, y, W-64, 72, C.white, 12);
  (f.children[f.children.length-1] as RectangleNode).strokes = [solid(C.border)];
  await mkText(f, 'VD: Thông báo lịch trình...', 52, y+20, 24, 'Regular', C.textMuted, W-120);
  y += 92;

  await mkText(f, 'Nội dung *', 32, y, 24, 'Medium', C.textSub); y += 36;
  mkRect(f, 'notif_body', 32, y, W-64, 280, C.white, 12);
  (f.children[f.children.length-1] as RectangleNode).strokes = [solid(C.border)];
  await mkText(f, 'Nhập nội dung thông báo muốn gửi tới người mua vé...', 52, y+24, 24, 'Regular', C.textMuted, W-120);
  y += 300;

  // Preview
  mkRect(f, 'preview_bg', 32, y, W-64, 180, '#F1F5F9', 16);
  await mkText(f, '👁 Xem trước thông báo', 56, y+16, 24, 'Medium', C.textSub, 360);
  mkRect(f, 'notif_preview', 56, y+52, W-112, 108, C.white, 12);
  (f.children[f.children.length-1] as RectangleNode).strokes = [solid(C.border)];
  mkImg(f, 'App icon in notification', 72, y+68, 64, 64, 8);
  await mkText(f, 'LiveFest 2025', 148, y+68, 22, 'SemiBold', C.text, 400);
  await mkText(f, 'Thông báo lịch trình chính thức...', 148, y+98, 20, 'Regular', C.textSub, 400);
  y += 200;

  await mkBtn(f, '📣 Gửi thông báo ngay', 32, y, W-64);
  f.resize(W, y+120);
}

// ──────────────── ADMIN SCREENS ───────────────────────────────────────────────

async function buildAdminDashboard(x: number) {
  const f = mkFrame('25 Admin Dashboard', x);
  await drawSimpleHeader(f, 'Admin Dashboard', false);

  let y = CONTENT_TOP + 32;
  await mkText(f, 'Tổng quan hệ thống', 32, y, 32, 'SemiBold', C.text); y += 52;

  // Metric cards (2x2)
  const metrics = [['1,248','Người dùng','↑12%'],['56','Sự kiện','↑4%'],['8,432','Vé bán','↑23%'],['412M','Doanh thu','↑18%']];
  const mw = (W-80)/2;
  for (let i=0;i<4;i++) {
    const mx = 32 + (i%2)*(mw+16);
    const my = y + Math.floor(i/2)*136;
    const mc = mkCard(f, `metric_${i}`, mx, my, mw, 120);
    await mkText(mc, metrics[i][0], 16, 12, 40, 'SemiBold', C.primary, mw-40);
    await mkText(mc, metrics[i][1], 16, 60, 22, 'Regular', C.textSub, mw-80);
    await mkText(mc, metrics[i][2], mw-80, 60, 22, 'SemiBold', C.success, 64);
  }
  y += 296;

  // Chart placeholder
  await mkText(f, 'Doanh thu 30 ngày gần nhất', 32, y, 28, 'SemiBold', C.text); y += 44;
  mkImg(f, 'Revenue Chart (MPAndroidChart)', 32, y, W-64, 320, 16);
  y += 340;

  // Quick actions
  await mkText(f, 'Thao tác nhanh', 32, y, 28, 'SemiBold', C.text); y += 44;
  const actions = [['📋 Duyệt sự kiện','3 đang chờ'],['✅ Duyệt tổ chức','1 đang chờ'],['🏷️ Danh mục','Quản lý categories'],['🎯 Voucher Global','Tạo mã toàn hệ thống']];
  for (const [title2, sub2] of actions) {
    mkRect(f, `action_${title2}`, 32, y, W-64, 88, C.white, 12);
    (f.children[f.children.length-1] as RectangleNode).strokes = [solid(C.border)];
    await mkText(f, title2, 56, y+14, 26, 'SemiBold', C.text, W-140);
    await mkText(f, sub2, 56, y+50, 22, 'Regular', C.textSub, W-200);
    await mkText(f, '›', W-80, y+26, 36, 'Regular', C.textMuted);
    y += 104;
  }
  f.resize(W, y+80);
}

async function buildEventApproval(x: number) {
  const f = mkFrame('26 Admin Event Approval', x);
  await drawSimpleHeader(f, 'Duyệt sự kiện', true);

  let y = CONTENT_TOP + 32;
  // Tab bar
  const atabs = ['Chờ duyệt (3)','Đã duyệt','Từ chối'];
  const tw2 = W/3;
  for (let i=0;i<3;i++) {
    mkRect(f, `atab_${i}`, i*tw2, y, tw2, 72, C.white);
    await mkText(f, atabs[i], i*tw2, y+18, 22, i===0?'SemiBold':'Regular', i===0?C.primary:C.textMuted, tw2);
    (f.children[f.children.length-1] as TextNode).textAlignHorizontal = 'CENTER';
    if (i===0) mkRect(f, 'atab_indicator', i*tw2+24, y+64, tw2-48, 4, C.primary, 2);
  }
  y += 88;

  // Pending events
  const pendingEvs = [
    {name:'Jazz Night 2026',org:'Music Box Co.',date:'10/01/2026'},
    {name:'Art Workshop Series',org:'Creative Hub',date:'15/01/2026'},
    {name:'Sport Triathlon HCM',org:'Sport Vietnam',date:'20/02/2026'},
  ];
  for (const ev of pendingEvs) {
    const ec = mkCard(f, `pending_${ev.name}`, 32, y, W-64, 280);
    mkImg(ec, `${ev.name} poster`, 0, 0, W-64, 160, 12);
    await mkText(ec, ev.name, 24, 172, 28, 'SemiBold', C.text, W-112);
    await mkText(ec, `🏢 ${ev.org}  •  📅 ${ev.date}`, 24, 208, 22, 'Regular', C.textSub, W-112);
    mkRect(ec, 'status_chip', 24, 208, 180, 0, C.bg); // spacer
    await mkBtn(ec, '✓ Duyệt', 24, 232, (W-96)/2-8, 56, '#DCFCE7', C.success);
    await mkBtn(ec, '✗ Từ chối', (W-64)/2+8, 232, (W-96)/2-8, 56, '#FEE2E2', C.error);
    y += 296;
  }
  f.resize(W, y+80);
}

// ─── Main ────────────────────────────────────────────────────────────────────

async function main() {
  await figma.loadFontAsync({family:'Poppins', style:'Regular'});
  await figma.loadFontAsync({family:'Poppins', style:'Medium'});
  await figma.loadFontAsync({family:'Poppins', style:'SemiBold'});

  const screens: Array<{fn: (x:number)=>Promise<void>, name:string}> = [
    {fn: buildHome,                  name:'01 Home'},
    {fn: buildEvents,                name:'02 Events'},
    {fn: buildEventDetail,           name:'03 Event Detail'},
    {fn: buildSelectTicket,          name:'04 Select Ticket'},
    {fn: buildFillAttendee,          name:'05 Fill Attendee'},
    {fn: buildPayment,               name:'06 Payment'},
    {fn: buildPaymentSuccess,        name:'07 Payment Success'},
    {fn: buildMyTickets,             name:'08 My Tickets'},
    {fn: buildProfile,               name:'09 Profile'},
    {fn: buildLogin,                 name:'10 Login'},
    {fn: buildOrganizerHub,          name:'11 Organizer Hub'},
    {fn: buildMyEventsList,          name:'12 My Events List'},
    {fn: buildCreateEventStep1,      name:'13 Create Event 1'},
    {fn: buildCreateEventStep2,      name:'14 Create Event 2'},
    {fn: buildCreateEventStep3,      name:'15 Create Event 3'},
    {fn: buildCreateEventStep4,      name:'16 Create Event 4'},
    {fn: buildCreateEventStep5,      name:'17 Create Event 5'},
    {fn: buildTicketTypeManagement,  name:'18 Ticket Types'},
    {fn: buildOrderManagement,       name:'19 Orders'},
    {fn: buildDiscountManagement,    name:'20 Discounts'},
    {fn: buildStaffManagement,       name:'21 Staff'},
    {fn: buildQrScanner,             name:'22 QR Scanner'},
    {fn: buildEventStars,            name:'23 Event Stars'},
    {fn: buildBlastNotification,     name:'24 Blast'},
    {fn: buildAdminDashboard,        name:'25 Admin Dashboard'},
    {fn: buildEventApproval,         name:'26 Admin Approval'},
  ];

  let xOffset = 0;
  for (const {fn} of screens) {
    await fn(xOffset);
    xOffset += W + GAP;
  }

  figma.viewport.scrollAndZoomIntoView(figma.currentPage.children);
  figma.notify(`✅ VibeTix: ${screens.length} màn hình đã được tạo!`);
}

main().catch(err => figma.notify('❌ Lỗi: ' + err.message));
