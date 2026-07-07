/**
 * VibeTix App — Figma Screen Generator v3
 * ✅ Header  → clone từ frame "Homepage" > "Header - TopAppBar" (pixel-perfect)
 * ✅ Bottom nav → vẽ lại đúng 5 tab + FAB "+" ở giữa
 *
 * YÊU CẦU: Frame "Homepage" phải đang có mặt trên cùng page trước khi chạy.
 *
 * CÁCH THAY ẢNH: chọn Rectangle tên bắt đầu bằng 📷
 *   → Fill (bên phải) → click ô màu → đổi Solid → Image → upload ảnh
 */

// ─── Constants ────────────────────────────────────────────────────────────────
const W        = 780;   // frame width
const H        = 1688;  // frame height
const GAP      = 120;   // khoảng cách giữa các frame
const SB       = 88;    // status bar height (được bao gồm trong header clone)
const HDR_H    = 200;   // tổng chiều cao header (SB + AppBar) — content bắt đầu từ đây
const NAV_H    = 124;   // chiều cao vùng bottom nav (62 bar + 62 overflow cho FAB)
const NAV_BAR  = 62;    // chiều cao thực của thanh nav (khớp Figma)
const FAB_R    = 52;    // bán kính FAB circle
const CT       = HDR_H; // content top y
const CB       = H - NAV_H; // content bottom y

// ─── Colors ───────────────────────────────────────────────────────────────────
const C = {
  primary:  hex('#2563EB'),
  accent:   hex('#06B6D4'),
  success:  hex('#16A34A'),
  warning:  hex('#D97706'),
  error:    hex('#DC2626'),
  white:    hex('#FFFFFF'),
  bg:       hex('#F5F5F5'),
  card:     hex('#FFFFFF'),
  border:   hex('#EEF0F5'),
  divider:  hex('#F0F0F0'),
  textPri:  hex('#1C1B1B'),
  textSec:  hex('#808E92'),
  textHint: hex('#B0B8C1'),
  overlay:  hex('#000000'),
  chipBlu:  hex('#EBF2FF'),
  greenBg:  hex('#DCFCE7'),
  redBg:    hex('#FEE2E2'),
  amberBg:  hex('#FEF3C7'),
};

function hex(h: string): RGB {
  return { r: parseInt(h.slice(1,3),16)/255, g: parseInt(h.slice(3,5),16)/255, b: parseInt(h.slice(5,7),16)/255 };
}
function solid(c: RGB, o = 1): SolidPaint { return { type: 'SOLID', color: c, opacity: o }; }

// ─── Font ─────────────────────────────────────────────────────────────────────
const _fc = new Set<string>();
async function loadFont(w: string) {
  if (_fc.has(w)) return 'Poppins';
  try { await figma.loadFontAsync({ family: 'Poppins', style: w }); _fc.add(w); return 'Poppins'; }
  catch { const fb = w === 'SemiBold' ? 'Semi Bold' : w; await figma.loadFontAsync({ family: 'Inter', style: fb }); return 'Inter'; }
}

// ─── Primitives ───────────────────────────────────────────────────────────────
function mkFrame(name: string, x: number, y: number, w: number, h: number, fill = C.bg): FrameNode {
  const f = figma.createFrame();
  f.name = name; f.x = x; f.y = y; f.resize(w, h);
  f.fills = [solid(fill)]; f.clipsContent = true;
  return f;
}

function mkRect(p: FrameNode | GroupNode, name: string, x: number, y: number, w: number, h: number,
                fill: RGB, radius = 0, opacity = 1): RectangleNode {
  const n = figma.createRectangle();
  n.name = name; n.x = x; n.y = y; n.resize(w, h);
  n.fills = [solid(fill, opacity)];
  if (radius) n.cornerRadius = radius;
  (p as FrameNode).appendChild(n);
  return n;
}

/** Image placeholder — rectangle xám, tên bắt đầu "📷" — đổi Fill → Image để thay ảnh */
function mkImg(p: FrameNode | GroupNode, label: string, x: number, y: number, w: number, h: number, radius = 0): RectangleNode {
  const n = figma.createRectangle();
  n.name = `📷 ${label}`; n.x = x; n.y = y; n.resize(w, h);
  n.fills = [solid(hex('#D0D5DD'))];
  if (radius) n.cornerRadius = radius;
  (p as FrameNode).appendChild(n);
  return n;
}

async function mkText(p: FrameNode | GroupNode, content: string, x: number, y: number,
                      size: number, fill: RGB,
                      weight: 'Regular' | 'Medium' | 'SemiBold' | 'Bold' = 'Regular',
                      wrapW = 0, name = ''): Promise<TextNode> {
  const fam = await loadFont(weight);
  const fbStyle = weight === 'SemiBold' ? 'Semi Bold' : weight;
  const n = figma.createText();
  n.name = name || content.slice(0, 24);
  try { n.fontName = { family: 'Poppins', style: weight }; }
  catch { n.fontName = { family: 'Inter', style: fbStyle }; }
  n.fontSize = size; n.fills = [solid(fill)];
  n.x = x; n.y = y;
  if (wrapW > 0) { n.textAutoResize = 'HEIGHT'; n.resize(wrapW, size + 4); }
  n.characters = content;
  (p as FrameNode).appendChild(n);
  return n;
}

function mkDiv(p: FrameNode | GroupNode, y: number, indent = 0): RectangleNode {
  return mkRect(p, 'divider', indent, y, W - indent * 2, 2, C.divider);
}

function mkPill(p: FrameNode | GroupNode, x: number, y: number, w: number, h: number,
                fill: RGB, opacity = 1, name = 'pill'): RectangleNode {
  return mkRect(p, name, x, y, w, h, fill, h / 2, opacity);
}

// ─── CLONE HEADER từ frame "Homepage" ─────────────────────────────────────────
/**
 * Tìm "Header - TopAppBar" trong frame "Homepage" và clone vào frame mới.
 * Đảm bảo pixel-perfect vì dùng đúng node từ file Figma có sẵn.
 */
async function cloneHeader(targetFrame: FrameNode, sourceName = 'Homepage'): Promise<boolean> {
  const src = figma.currentPage.findOne(n => n.name === sourceName) as FrameNode | null;
  if (!src) {
    print(`⚠️  Không tìm thấy frame "${sourceName}". Vẽ header dự phòng...`);
    // fallback: vẽ header đơn giản
    mkRect(targetFrame, 'statusBar', 0, 0, W, SB, C.primary);
    mkRect(targetFrame, 'appBar', 0, SB, W, HDR_H - SB, C.primary);
    await mkText(targetFrame, 'VibeTix', 40, SB + 32, 32, C.white, 'Bold', 0, 'logoFallback');
    return false;
  }

  const hdr = src.findOne(n => n.name === 'Header - TopAppBar');
  if (!hdr) {
    print('⚠️  Không tìm thấy "Header - TopAppBar" trong Homepage. Kiểm tra lại tên layer.');
    return false;
  }

  const cloned = hdr.clone();
  cloned.x = 0;
  cloned.y = 0;
  // resize chiều rộng cho khớp frame (nếu cần)
  if ('resize' in cloned && (cloned as FrameNode).width !== W) {
    try { (cloned as FrameNode).resize(W, (cloned as FrameNode).height); } catch {}
  }
  targetFrame.appendChild(cloned);
  return true;
}

// ─── BOTTOM NAV — 5 tab + FAB "+" ở giữa ─────────────────────────────────────
async function drawBottomNav(f: FrameNode, active: 0 | 1 | 2 | 3 | 4 = 0) {
  // nền + border trên
  mkRect(f, 'navBg',     0, H - NAV_BAR, W, NAV_BAR, C.white);
  mkRect(f, 'navBorder', 0, H - NAV_BAR, W, 2, C.border);
  // home indicator bar (iOS style)
  mkRect(f, 'homeIndicator', W/2 - 67, H - 10, 134, 6, C.textPri, 3, 0.2);

  const tabs = [
    { icon: '⌂',  label: 'Trang chủ',   fab: false },
    { icon: '▦',  label: 'Sự kiện',     fab: false },
    { icon: '+',  label: 'Tạo sự kiện', fab: true  },
    { icon: '▣',  label: 'Vé của tôi',  fab: false },
    { icon: '☺',  label: 'Cá nhân',    fab: false  },
  ];

  const tw = W / 5;
  for (let i = 0; i < 5; i++) {
    const t = tabs[i];
    const cx = Math.round(i * tw + tw / 2);
    const col = i === active ? C.primary : C.textSec;

    if (t.fab) {
      // FAB circle — nổi lên trên thanh nav
      const fabY = H - NAV_BAR - FAB_R + 10;
      mkRect(f, 'fabShadow', cx - FAB_R + 4, fabY + 4, FAB_R * 2, FAB_R * 2, C.overlay, FAB_R, 0.15);
      mkRect(f, 'fabCircle', cx - FAB_R, fabY, FAB_R * 2, FAB_R * 2, C.primary, FAB_R);
      await mkText(f, '+', cx - 18, fabY + FAB_R - 22, 44, C.white, 'Bold', 0, 'fabPlus');
      await mkText(f, t.label, cx - 52, H - NAV_BAR + 36, 18, C.textSec, 'Regular', 104, `navLbl${i}`);
    } else {
      await mkText(f, t.icon, cx - 18, H - NAV_BAR + 10, 30, col, 'Regular', 0, `navIcon${i}`);
      await mkText(f, t.label, cx - 48, H - NAV_BAR + 46, 18, col, i === active ? 'SemiBold' : 'Regular', 96, `navLbl${i}`);
      if (i === active) mkRect(f, 'activeBar', i * tw, H - NAV_BAR, tw, 3, C.primary);
    }
  }
}

// ─── Search bar ───────────────────────────────────────────────────────────────
async function drawSearchBar(f: FrameNode, y: number) {
  mkRect(f, 'searchBg',     32, y, W - 64, 88, C.white, 22);
  mkRect(f, 'searchBorder', 32, y, W - 64, 88, C.border, 22);
  await mkText(f, '🔍  Tìm kiếm sự kiện...', 72, y + 28, 26, C.textHint, 'Regular', 0, 'searchPh');
}

// ─── Event card ───────────────────────────────────────────────────────────────
async function drawEventCard(f: FrameNode, x: number, y: number, w: number, h: number,
                             title: string, date: string, venue: string,
                             price: string, badge = '', priceColor = C.primary) {
  mkRect(f, 'cardBg', x, y, w, h, C.card, 24);
  const posterH = Math.round(h * 0.54);
  mkImg(f, `poster_${title.slice(0, 18)}`, x, y, w, posterH, 24);
  mkRect(f, 'posterFade', x, y + posterH - 48, w, 48, C.overlay, 0, 0.3);

  if (badge) {
    mkPill(f, x + 14, y + 14, badge.length * 13 + 28, 44, badge === 'Free' ? C.success : C.primary, 1, 'badge');
    await mkText(f, badge, x + 28, y + 23, 20, C.white, 'SemiBold', 0, 'badgeTxt');
  }

  const by = y + posterH + 14;
  await mkText(f, title, x + 18, by, 24, C.textPri, 'SemiBold', w - 36, 'cardTitle');
  await mkText(f, '📅 ' + date, x + 18, by + 56, 20, C.textSec, 'Regular', 0, 'cardDate');
  await mkText(f, '📍 ' + venue, x + 18, by + 84, 20, C.textSec, 'Regular', w - 36, 'cardVenue');
  const priceBg = priceColor === C.success ? C.greenBg : C.chipBlu;
  mkRect(f, 'priceBg', x + 18, by + 120, price.length * 13 + 28, 40, priceBg, 20);
  await mkText(f, price, x + 32, by + 129, 20, priceColor, 'SemiBold', 0, 'cardPrice');
}

// ═══════════════════════════════════════════════════════════════════════════════
// SCREEN 01 — HOME
// ═══════════════════════════════════════════════════════════════════════════════
async function s01_Home(ox: number) {
  const f = mkFrame('01 · Home', ox, 0, W, H);
  await cloneHeader(f); // ← clone từ Homepage
  let y = CT + 24;

  // Banner carousel
  mkImg(f, 'banner_event_poster', 32, y, W - 64, 360, 24);
  mkRect(f, 'bannerGrad', 32, y + 220, W - 64, 140, C.overlay, 24, 0.5);
  mkPill(f, 56, y + 20, 160, 48, C.success, 1, 'ongoingBadge');
  await mkText(f, 'Ongoing', 76, y + 30, 22, C.white, 'SemiBold', 0, 'ongoingTxt');
  await mkText(f, 'VinhVerse Concert', 56, y + 252, 28, C.white, 'Bold', W - 112, 'bannerTitle');
  await mkText(f, '09/06/2026  ·  📍 Hồ Chí Minh', 56, y + 292, 22, hex('#DDECFF'), 'Regular', 0, 'bannerMeta');
  // dots
  for (let d = 0; d < 6; d++) mkRect(f, `dot${d}`, W/2 - 44 + d*18, y + 344, d===0?32:12, 10, d===0?C.white:hex('#AAAAAA'), 5, d===0?1:0.5);
  y += 392;

  // Featured Stars
  await mkText(f, '⭐  Featured Stars', 32, y, 30, C.textPri, 'Bold', 0, 'starsHdr');
  y += 50;
  const stars = ['Mỹ Tâm', 'Sơn Tùng\nMTP', 'Noo Phước\nThịnh', 'Hòa Minzy', 'Đạt Việt'];
  for (let i = 0; i < 5; i++) {
    const sx = 32 + i * 140;
    mkImg(f, `star_avatar_${stars[i].replace('\n', '_')}`, sx, y, 104, 104, 52);
    mkRect(f, 'verDot', sx + 76, y + 76, 26, 26, C.primary, 13);
    await mkText(f, '✓', sx + 80, y + 79, 16, C.white, 'Bold', 0, `verIcon${i}`);
    await mkText(f, stars[i].replace('\n', ' '), sx - 4, y + 112, 18, C.textSec, 'Regular', 112, `starName${i}`);
  }
  y += 188;

  // Sự kiện đặc biệt
  await mkText(f, 'Sự kiện đặc biệt', 32, y, 30, C.textPri, 'Bold', 0, 'sec1Hdr');
  await mkText(f, 'Xem thêm ›', W - 168, y + 4, 24, C.primary, 'Medium', 0, 'sec1More');
  y += 48;
  const cw = (W - 80) / 2;
  const ch = 380;
  await drawEventCard(f, 32, y, cw, ch, 'Mừng Ngày Hội Non Sông - Võ Hạ Trâm', '01/05/2026', 'Hà Nội', 'From 150.000đ', 'Ongoing');
  await drawEventCard(f, 32 + cw + 16, y, cw, ch, 'Private Show in Fantasy - Quốc Thiên', '16/05/2026', 'Hà Nội', 'From 200.000đ', 'Ongoing');
  y += ch + 28;

  // Sự kiện xu hướng
  await mkText(f, '🔥  Sự kiện xu hướng', 32, y, 30, C.textPri, 'Bold', 0, 'trendHdr');
  await mkText(f, 'Xem thêm ›', W - 168, y + 4, 24, C.primary, 'Medium', 0, 'trendMore');
  y += 48;
  // top 3 row (horizontal scroll)
  const trendW = 320;
  const trendH = 220;
  for (let i = 0; i < 3; i++) {
    const tx = 32 + i * (trendW + 16);
    mkImg(f, `trend_poster_${i}`, tx, y, trendW, trendH, 20);
    mkRect(f, `trendGrad${i}`, tx, y + trendH - 72, trendW, 72, C.overlay, 20, 0.6);
    mkRect(f, `rankBadge${i}`, tx + 14, y + 14, 44, 44, C.primary, 22);
    await mkText(f, String(i + 1), tx + 24, y + 22, 28, C.white, 'Bold', 0, `rankTxt${i}`);
    await mkText(f, ['Osun Fest', 'FFWS SEA Spring', 'VBA Star 2025'][i], tx + 14, y + trendH - 60, 22, C.white, 'SemiBold', trendW - 28, `trendTitle${i}`);
  }
  y += trendH + 28;

  // Resale Ticket banner
  mkRect(f, 'resaleBg', 32, y, W - 64, 200, C.chipBlu, 24);
  mkRect(f, 'resaleBorder', 32, y, W - 64, 200, C.primary, 24, 0.15);
  await mkText(f, 'Resale Ticket', 56, y + 20, 28, C.primary, 'Bold', 0, 'resaleHdr');
  await mkText(f, 'VÉ BÁN LẠI', 56, y + 54, 20, C.textSec, 'Regular', 0, 'resaleSub');
  await mkText(f, 'Xem thêm ›', 56, y + 84, 22, C.primary, 'Medium', 0, 'resaleMore');
  mkImg(f, 'resale_mascot_bibi', W - 220, y + 16, 168, 168, 12);
  y += 224;

  // Category: Nhạc sống
  await mkText(f, 'Nhạc sống', 32, y, 30, C.textPri, 'Bold', 0, 'catHdr');
  await mkText(f, 'Xem thêm ›', W - 168, y + 4, 24, C.primary, 'Medium', 0, 'catMore');
  y += 48;
  await drawEventCard(f, 32, y, cw, ch, 'Charm Melody Minh Show - Biển Tình', '15/06/2026', 'Hồ Chí Minh', 'From 300.000đ');
  await drawEventCard(f, 32 + cw + 16, y, cw, ch, 'Đêm Nhạc Xưa - Hoài Niệm 2026', '20/06/2026', 'Hà Nội', 'From 200.000đ');

  await drawBottomNav(f, 0);
  figma.currentPage.appendChild(f);
  return f;
}

// ═══════════════════════════════════════════════════════════════════════════════
// SCREEN 02 — EVENTS
// ═══════════════════════════════════════════════════════════════════════════════
async function s02_Events(ox: number) {
  const f = mkFrame('02 · Events', ox, 0, W, H);
  await cloneHeader(f);
  let y = CT + 24;

  await drawSearchBar(f, y); y += 108;

  // Category chips
  const cats = ['Tất cả', 'Âm nhạc', 'Workshop', 'Thể thao', 'Stage & Arts', 'Tour'];
  let cx = 32;
  for (let i = 0; i < cats.length; i++) {
    const cw2 = cats[i].length * 15 + 44;
    mkRect(f, `chip${i}`, cx, y, cw2, 60, i === 0 ? C.primary : C.white, 30);
    if (i !== 0) mkRect(f, `chipStr${i}`, cx, y, cw2, 60, C.border, 30);
    await mkText(f, cats[i], cx + 22, y + 16, 22, i === 0 ? C.white : C.textSec, i === 0 ? 'SemiBold' : 'Regular', 0, `chipTxt${i}`);
    cx += cw2 + 12;
  }
  y += 80;

  // Filter row
  mkRect(f, 'filterBg1', 32, y, 220, 60, C.white, 30); mkRect(f, 'filterStr1', 32, y, 220, 60, C.border, 30);
  await mkText(f, '≡  Mới nhất  ▾', 56, y + 16, 22, C.textPri, 'Medium', 0, 'filter1');
  mkRect(f, 'filterBg2', 268, y, 260, 60, C.white, 30); mkRect(f, 'filterStr2', 268, y, 260, 60, C.border, 30);
  await mkText(f, '📍 Tất cả tỉnh thành ▾', 288, y + 16, 22, C.textPri, 'Medium', 0, 'filter2');
  y += 84;

  const ew = (W - 80) / 2, eh = 400;
  const evs = [
    { t: 'Escape Room - Căn Nhà Ma Quái', d: '21/09/2026', v: 'Hồ Chí Minh', p: 'From 150.000đ', b: 'Ongoing' },
    { t: 'Ký Ức Hội An Memories Show', d: '09/09/2026', v: 'Hội An', p: 'From 200.000đ', b: 'Ongoing' },
    { t: 'Rồi 30 Năm Sau - Trương Hùng Minh', d: '06/09/2026', v: 'Việt Nam', p: 'From 150.000đ', b: '' },
    { t: 'Sinh Nhật Triệu Đô - Thế Giới Trẻ', d: '03/09/2026', v: 'Hồ Chí Minh', p: 'Free', b: '' },
  ];
  for (let i = 0; i < evs.length; i++) {
    const ev = evs[i];
    const ex = i % 2 === 0 ? 32 : 32 + ew + 16;
    const ey = y + Math.floor(i / 2) * (eh + 20);
    await drawEventCard(f, ex, ey, ew, eh, ev.t, ev.d, ev.v, ev.p, ev.b, ev.p === 'Free' ? C.success : C.primary);
  }

  await drawBottomNav(f, 1);
  figma.currentPage.appendChild(f);
  return f;
}

// ═══════════════════════════════════════════════════════════════════════════════
// SCREEN 03 — EVENT DETAIL
// ═══════════════════════════════════════════════════════════════════════════════
async function s03_EventDetail(ox: number) {
  const f = mkFrame('03 · Event Detail', ox, 0, W, H, C.white);

  // Hero full-bleed poster
  mkImg(f, 'event_detail_hero_poster', 0, 0, W, 500);
  mkRect(f, 'heroGrad', 0, 320, W, 180, C.overlay, 0, 0.65);
  mkRect(f, 'sbOverlay', 0, 0, W, SB, C.overlay, 0, 0.35);
  await mkText(f, '9:41', 32, 26, 26, C.white, 'SemiBold', 0, 'time');
  mkRect(f, 'backBtn',  32, SB + 24, 76, 76, C.white, 38, 0.2);
  await mkText(f, '←', 50, SB + 32, 36, C.white, 'Bold', 0, 'back');
  mkRect(f, 'shareBtn', W - 108, SB + 24, 76, 76, C.white, 38, 0.2);
  await mkText(f, '⤴', W - 92, SB + 32, 36, C.white, 'Bold', 0, 'share');
  await mkText(f, 'Private Show in Fantasy\n— Quốc Thiên', 40, 370, 36, C.white, 'Bold', W - 80, 'heroTitle');

  let y = 518;
  mkPill(f, 32, y, 200, 52, C.success, 1, 'statusBadge');
  await mkText(f, '● Ongoing', 52, y + 14, 24, C.white, 'SemiBold', 0, 'statusTxt');
  mkPill(f, W - 228, y, 196, 52, C.chipBlu, 1, 'interestBtn');
  await mkText(f, '🔔 Quan tâm', W - 212, y + 15, 22, C.primary, 'Medium', 0, 'interestTxt');
  y += 72;

  for (const m of [
    { icon: '📅', txt: 'Thứ Tư, 16 tháng 05, 2026 · 19:30' },
    { icon: '📍', txt: 'Diamond Plaza - 34 Lê Duẩn, Q.1, TP.HCM' },
    { icon: '🏢', txt: 'Fantasy Entertainment' },
  ]) {
    await mkText(f, m.icon, 32, y, 32, C.textSec, 'Regular', 0, 'mIcon');
    await mkText(f, m.txt, 80, y, 26, C.textSec, 'Regular', W - 112, 'mTxt');
    y += 48;
  }
  y += 8; mkDiv(f, y, 32); y += 32;

  // Organizer
  mkImg(f, 'organizer_logo', 32, y, 88, 88, 16);
  await mkText(f, 'Fantasy Entertainment', 140, y + 6, 26, C.textPri, 'SemiBold', W - 200, 'orgName');
  await mkText(f, '✔ Đã xác minh · 5 sự kiện', 140, y + 42, 22, C.success, 'Regular', 0, 'orgMeta');
  mkPill(f, 140, y + 72, 192, 44, C.chipBlu, 1, 'followBtn');
  await mkText(f, '+ Theo dõi', 160, y + 83, 20, C.primary, 'SemiBold', 0, 'followTxt');
  y += 120; mkDiv(f, y, 32); y += 32;

  // Description
  await mkText(f, 'Mô tả sự kiện', 32, y, 28, C.textPri, 'Bold', 0, 'descHdr'); y += 44;
  await mkText(f, 'Đêm nhạc Private Show in Fantasy hứa hẹn mang đến trải nghiệm độc đáo với Quốc Thiên cùng những tiết mục đặc sắc trên sân khấu huyền ảo, hoành tráng và đầy cảm xúc...', 32, y, 24, C.textSec, 'Regular', W - 64, 'descBody');
  await mkText(f, 'Xem thêm', 32, y + 120, 24, C.primary, 'SemiBold', 0, 'readMore');
  y += 172; mkDiv(f, y, 32); y += 32;

  // Ticket types
  await mkText(f, 'Loại vé', 32, y, 28, C.textPri, 'Bold', 0, 'ticketsHdr'); y += 48;
  const tts = [
    { name: 'VIP — Hàng đầu',  price: '2.000.000 ₫', qty: 'Còn 30 vé', avail: true,  active: true  },
    { name: 'ZONE A',           price: '1.000.000 ₫', qty: 'Còn 85 vé', avail: true,  active: false },
    { name: 'ZONE B',           price: '500.000 ₫',   qty: 'Hết vé',    avail: false, active: false },
  ];
  for (const tt of tts) {
    mkRect(f, 'ttCard', 32, y, W - 64, 108, tt.active ? C.chipBlu : C.card, 20);
    if (tt.active) mkRect(f, 'ttAccent', 32, y, 8, 108, C.primary, 4);
    await mkText(f, tt.name,  60, y + 14, 26, C.textPri, 'SemiBold', W - 300, 'ttN');
    await mkText(f, tt.price, 60, y + 52, 28, tt.avail ? C.primary : C.textSec, 'Bold', 0, 'ttP');
    await mkText(f, tt.qty,   60, y + 84, 20, C.textHint, 'Regular', 0, 'ttQ');
    const sbg = tt.avail ? C.greenBg : C.redBg;
    mkPill(f, W - 220, y + 30, 180, 48, sbg, 1, 'stBg');
    await mkText(f, tt.avail ? 'On Sale' : 'Hết vé', W - 206, y + 43, 22, tt.avail ? C.success : C.error, 'SemiBold', 0, 'stTxt');
    y += 124;
  }

  // Fixed buy button
  mkRect(f, 'buyBg', 0, H - NAV_H - 104, W, 104, C.white);
  mkRect(f, 'buyDiv', 0, H - NAV_H - 104, W, 2, C.border);
  mkRect(f, 'buyBtn', 32, H - NAV_H - 88, W - 64, 84, C.primary, 20);
  await mkText(f, 'Chọn vé  →', W/2 - 76, H - NAV_H - 64, 30, C.white, 'Bold', 0, 'buyTxt');

  figma.currentPage.appendChild(f);
  return f;
}

// ═══════════════════════════════════════════════════════════════════════════════
// SCREEN 04 — MY TICKETS
// ═══════════════════════════════════════════════════════════════════════════════
async function s04_MyTickets(ox: number) {
  const f = mkFrame('04 · My Tickets', ox, 0, W, H);
  await cloneHeader(f);

  // Main tabs
  const tabY = CT;
  mkRect(f, 'mainTabBg', 0, tabY, W, 84, C.white);
  const mtabs = ['MUA', 'BÁN LẠI', 'MEMBERSHIP'];
  for (let i = 0; i < 3; i++) {
    const tw = W / 3;
    await mkText(f, mtabs[i], i*tw + tw/2 - mtabs[i].length*9, tabY + 26, 24,
      i===0 ? C.primary : C.textSec, i===0 ? 'Bold' : 'Regular', 0, `mTab${i}`);
    if (i===0) mkRect(f, 'tabInd', 0, tabY+80, W/3, 4, C.primary);
  }

  // Sub tabs
  let y = tabY + 84 + 20;
  const subs = ['Sắp diễn ra', 'Đã kết thúc'];
  let sx = 32;
  for (let i = 0; i < 2; i++) {
    const sw = subs[i].length * 14 + 48;
    mkRect(f, `subTab${i}`, sx, y, sw, 60, i===0 ? C.primary : C.white, 30);
    if (i!==0) mkRect(f, `subStr${i}`, sx, y, sw, 60, C.border, 30);
    await mkText(f, subs[i], sx + 24, y + 15, 22, i===0 ? C.white : C.textSec, i===0 ? 'SemiBold' : 'Regular', 0, `subLbl${i}`);
    sx += sw + 14;
  }
  y += 84;

  // Ticket cards
  const tickets = [
    { event: 'Private Show in Fantasy — Quốc Thiên', date: '16/05/2026', venue: 'Diamond Plaza, Q.1', type: 'VIP — Hàng đầu', code: '#A1B2C3D4' },
    { event: 'Ký Ức Hội An — Memories Show', date: '09/09/2026', venue: 'Phố Cổ Hội An', type: 'ZONE A', code: '#E5F6A7B8' },
    { event: 'Infinity Racing — Go-Kart', date: '31/08/2026', venue: 'Vincom Mega Mall', type: 'STANDARD', code: '#C9D0E1F2' },
  ];
  for (const t of tickets) {
    mkRect(f, 'tktCard', 32, y, W - 64, 272, C.card, 24);
    mkRect(f, 'tktAccent', 32, y, 10, 272, C.primary, 0);
    mkImg(f, `ticket_thumb_${t.code}`, 50, y + 18, 108, 108, 14);
    await mkText(f, t.event, 180, y + 14, 24, C.textPri, 'Bold', W - 260, 'tEvt');
    await mkText(f, '📅 ' + t.date, 180, y + 66, 20, C.textSec, 'Regular', 0, 'tDate');
    await mkText(f, '📍 ' + t.venue, 180, y + 94, 20, C.textSec, 'Regular', W - 260, 'tVen');
    await mkText(f, '🎫 ' + t.type, 180, y + 124, 22, C.primary, 'SemiBold', 0, 'tType');
    mkImg(f, `qr_${t.code}`, W - 170, y + 18, 130, 130, 12);
    mkDiv(f, y + 194, 50);
    await mkText(f, t.code, 50, y + 210, 20, C.textHint, 'Regular', 0, 'tCode');
    mkRect(f, 'sellBtn', W - 280, y + 202, 232, 52, C.chipBlu, 26);
    await mkText(f, 'Bán lại vé', W - 258, y + 215, 22, C.primary, 'SemiBold', 0, 'sellTxt');
    y += 292;
  }

  await drawBottomNav(f, 3);
  figma.currentPage.appendChild(f);
  return f;
}

// ═══════════════════════════════════════════════════════════════════════════════
// SCREEN 05 — PROFILE
// ═══════════════════════════════════════════════════════════════════════════════
async function s05_Profile(ox: number) {
  const f = mkFrame('05 · Profile', ox, 0, W, H);

  // Blue header bg (thay header clone bằng bg riêng vì profile có layout khác)
  mkRect(f, 'profileHeaderBg', 0, 0, W, 460, C.primary);
  await mkText(f, '9:41', 32, 26, 26, C.white, 'SemiBold', 0, 'time');
  await mkText(f, '▪▪▪  ▲  ⬜', W - 172, 26, 22, C.white, 'Regular', 0, 'statusIcons');
  await mkText(f, 'Cá nhân', 32, SB + 28, 32, C.white, 'Bold', 0, 'pageTitle');

  // Avatar
  mkImg(f, 'user_avatar', W/2 - 76, SB + 80, 152, 152, 76);
  mkRect(f, 'avatarRing', W/2 - 80, SB + 76, 160, 160, 80, 0, 0.3);
  await mkText(f, 'Nguyễn Văn A', W/2 - 96, SB + 248, 32, C.white, 'Bold', 192, 'pName');
  await mkText(f, 'user@example.com', W/2 - 96, SB + 292, 22, hex('#CCE8F4'), 'Regular', 192, 'pEmail');

  // Stats card
  const scY = 424;
  mkRect(f, 'statsCard', 40, scY, W - 80, 120, C.white, 24);
  await mkText(f, '2', W/4 - 12, scY + 18, 44, C.primary, 'Bold', 0, 's1v');
  await mkText(f, 'Yêu thích', W/4 - 44, scY + 74, 22, C.textSec, 'Regular', 88, 's1l');
  mkRect(f, 'statDiv', W/2, scY + 22, 2, 76, C.border);
  await mkText(f, '7', 3*W/4 - 12, scY + 18, 44, C.primary, 'Bold', 0, 's2v');
  await mkText(f, 'Theo dõi', 3*W/4 - 40, scY + 74, 22, C.textSec, 'Regular', 80, 's2l');

  let y = scY + 148;
  mkRect(f, 'settingsCard', 32, y, W - 64, 680, C.white, 24);
  await mkText(f, 'CÀI ĐẶT & BẢO MẬT', 56, y + 26, 20, C.textSec, 'Bold', 0, 'setHdr');
  let ry = y + 68;
  const rows = [
    { icon: '👤', label: 'Thông tin tài khoản', right: 'arrow' },
    { icon: '🏢', label: 'Ban tổ chức của tôi',  right: 'arrow' },
    { icon: '🔐', label: 'Bảo mật & Mật khẩu',   right: 'arrow' },
    { icon: '🌐', label: 'Ngôn ngữ ứng dụng',     right: 'chip', chip: '🇬🇧 English' },
    { icon: '🔔', label: 'Thông báo',             right: 'toggle' },
    { icon: '❓', label: 'Trung tâm trợ giúp',    right: 'arrow' },
  ];
  for (const r of rows) {
    await mkText(f, r.icon, 56, ry + 10, 32, C.textSec, 'Regular', 0, 'rIcon');
    await mkText(f, r.label, 108, ry + 13, 26, C.textPri, 'Regular', W - 280, 'rLabel');
    if (r.right === 'arrow') await mkText(f, '›', W - 80, ry + 10, 40, C.textSec, 'Regular', 0, 'rArrow');
    if (r.right === 'chip') {
      mkRect(f, 'langChip', W - 228, ry + 4, 188, 52, C.chipBlu, 26);
      await mkText(f, r.chip!, W - 212, ry + 16, 22, C.primary, 'SemiBold', 0, 'langTxt');
    }
    if (r.right === 'toggle') {
      mkRect(f, 'togBg',    W - 136, ry + 10, 88, 44, C.primary, 22);
      mkRect(f, 'togKnob',  W - 100, ry + 14, 36, 36, C.white, 18);
    }
    mkDiv(f, ry + 72, 56);
    ry += 84;
  }
  y += 696;
  mkRect(f, 'logoutBtn', 32, y, W - 64, 88, C.error, 20);
  await mkText(f, 'Đăng xuất', W/2 - 56, y + 26, 30, C.white, 'Bold', 0, 'logoutTxt');

  await drawBottomNav(f, 4);
  figma.currentPage.appendChild(f);
  return f;
}

// ═══════════════════════════════════════════════════════════════════════════════
// SCREEN 06 — LOGIN
// ═══════════════════════════════════════════════════════════════════════════════
async function s06_Login(ox: number) {
  const f = mkFrame('06 · Login', ox, 0, W, H, C.white);

  mkRect(f, 'loginHeaderBg', 0, 0, W, 480, C.primary);
  mkRect(f, 'loginFade', 0, 360, W, 120, C.white, 0, 0.0);

  // Logo
  mkRect(f, 'logoBg', W/2 - 40, 96, 80, 80, C.white, 20, 0.2);
  await mkText(f, '🎫', W/2 - 26, 108, 52, C.white, 'Regular', 0, 'logoIcon');
  await mkText(f, 'VibeTix', W/2 - 64, 204, 42, C.white, 'Bold', 0, 'logoTxt');
  await mkText(f, 'Your event starts here', W/2 - 112, 260, 24, hex('#CCE8F4'), 'Regular', 224, 'tagline');
  await mkText(f, 'Chào mừng trở lại! 👋', W/2 - 120, 316, 30, C.white, 'Bold', 240, 'welcome');
  await mkText(f, 'Đăng nhập để tiếp tục', W/2 - 96, 360, 24, hex('#CCE8F4'), 'Regular', 192, 'sub');

  let y = 504;
  await mkText(f, 'Email', 32, y, 24, C.textSec, 'Medium', 0, 'emailLbl'); y += 34;
  mkRect(f, 'emailField', 32, y, W - 64, 90, C.bg, 20);
  mkRect(f, 'emailStr',   32, y, W - 64, 90, C.border, 20);
  await mkText(f, '✉', 52, y + 27, 36, C.textSec, 'Regular', 0, 'emailIcon');
  await mkText(f, 'Nhập email của bạn', 100, y + 30, 26, C.textHint, 'Regular', 0, 'emailPh');
  y += 114;

  await mkText(f, 'Mật khẩu', 32, y, 24, C.textSec, 'Medium', 0, 'passLbl'); y += 34;
  mkRect(f, 'passField', 32, y, W - 64, 90, C.bg, 20);
  mkRect(f, 'passStr',   32, y, W - 64, 90, C.border, 20);
  await mkText(f, '🔒', 52, y + 27, 36, C.textSec, 'Regular', 0, 'passIcon');
  await mkText(f, 'Nhập mật khẩu', 100, y + 30, 26, C.textHint, 'Regular', 0, 'passPh');
  await mkText(f, '👁', W - 84, y + 30, 34, C.textSec, 'Regular', 0, 'eye');
  y += 114;

  await mkText(f, 'Quên mật khẩu?', W - 188, y, 24, C.primary, 'Medium', 0, 'forgot'); y += 52;
  mkRect(f, 'loginBtn', 32, y, W - 64, 98, C.primary, 20);
  await mkText(f, 'Đăng nhập', W/2 - 68, y + 29, 34, C.white, 'Bold', 0, 'loginBtnTxt');
  y += 120;

  mkRect(f, 'divL', 32, y + 28, W/2 - 72, 2, C.border);
  await mkText(f, 'hoặc', W/2 - 36, y + 6, 26, C.textSec, 'Regular', 0, 'orTxt');
  mkRect(f, 'divR', W/2 + 36, y + 28, W/2 - 68, 2, C.border);
  y += 72;

  mkRect(f, 'googleBtn', 32, y, W - 64, 98, C.white, 20);
  mkRect(f, 'googleStr', 32, y, W - 64, 98, C.border, 20);
  await mkText(f, 'G', 56, y + 26, 46, C.error, 'Bold', 0, 'gLogo');
  await mkText(f, 'Đăng nhập với Google', 112, y + 30, 28, C.textPri, 'Medium', 0, 'gTxt');
  y += 120;

  await mkText(f, 'Chưa có tài khoản?', 32, y, 26, C.textSec, 'Regular', 0, 'noAcc');
  await mkText(f, 'Đăng ký ngay', 288, y, 26, C.primary, 'Bold', 0, 'regLink');

  figma.currentPage.appendChild(f);
  return f;
}

// ═══════════════════════════════════════════════════════════════════════════════
// SCREEN 07 — PAYMENT SUCCESS
// ═══════════════════════════════════════════════════════════════════════════════
async function s07_PaymentSuccess(ox: number) {
  const f = mkFrame('07 · Payment Success', ox, 0, W, H, C.white);
  await cloneHeader(f);
  let y = CT + 56;

  mkRect(f, 'sucRing',   W/2 - 80, y,    160, 160, C.success, 80, 0.1);
  mkRect(f, 'sucCircle', W/2 - 56, y+24, 112, 112, C.success, 56, 0.2);
  await mkText(f, '✓', W/2 - 28, y + 32, 72, C.success, 'Bold', 0, 'checkIcon');
  y += 188;
  await mkText(f, 'Đặt vé thành công! 🎉', W/2 - 152, y, 36, C.textPri, 'Bold', 304, 'sucTitle'); y += 60;
  await mkText(f, 'E-ticket đã gửi đến email và lưu trong Vé của tôi.', W/2 - 144, y, 24, C.textSec, 'Regular', 288, 'sucDesc'); y += 100;

  mkRect(f, 'confirmCard', 32, y, W - 64, 304, C.card, 24);
  mkRect(f, 'confAccent', 32, y, 10, 304, C.success);
  mkImg(f, 'confirm_event_thumb', 52, y + 16, 108, 108, 14);
  await mkText(f, 'Private Show in Fantasy — Quốc Thiên', 184, y + 14, 26, C.textPri, 'Bold', W - 256, 'cEvt');
  await mkText(f, '📅 16/05/2026 · 19:30', 184, y + 72, 22, C.textSec, 'Regular', 0, 'cDate');
  await mkText(f, '📍 Diamond Plaza, Q.1', 184, y + 100, 22, C.textSec, 'Regular', 0, 'cVen');
  await mkText(f, '🎫 2 × VIP — Hàng đầu', 184, y + 128, 22, C.textSec, 'Regular', 0, 'cQty');
  mkDiv(f, y + 210, 52);
  await mkText(f, 'Tổng cộng', 52, y + 228, 26, C.textSec, 'Regular', 0, 'totLbl');
  await mkText(f, '4.000.000 ₫', W - 208, y + 224, 28, C.primary, 'Bold', 0, 'totAmt');
  mkPill(f, 52, y + 266, 188, 52, C.greenBg, 1, 'paidBg');
  await mkText(f, '✓ Đã thanh toán', 68, y + 279, 22, C.success, 'SemiBold', 0, 'paidTxt');
  y += 328;

  await mkText(f, 'Mã QR vé của bạn', W/2 - 88, y, 24, C.textSec, 'Regular', 0, 'qrHdr'); y += 44;
  mkImg(f, 'ticket_qr_code', W/2 - 108, y, 216, 216, 16);
  await mkText(f, 'Mã: #A1B2C3D4', W/2 - 68, y + 228, 22, C.textHint, 'Regular', 0, 'qrCode');
  y += 272;

  mkRect(f, 'viewBtn', 32, y, W - 64, 96, C.primary, 20);
  await mkText(f, 'Xem vé của tôi  →', W/2 - 96, y + 28, 30, C.white, 'Bold', 0, 'viewTxt');
  y += 116;
  mkRect(f, 'homeBtn', 32, y, W - 64, 96, C.white, 20);
  mkRect(f, 'homeStr', 32, y, W - 64, 96, C.border, 20);
  await mkText(f, 'Về trang chủ', W/2 - 68, y + 28, 30, C.textPri, 'Medium', 0, 'homeTxt');

  figma.currentPage.appendChild(f);
  return f;
}

// ═══════════════════════════════════════════════════════════════════════════════
// SCREEN 08 — ORGANIZER HUB
// ═══════════════════════════════════════════════════════════════════════════════
async function s08_OrganizerHub(ox: number) {
  const f = mkFrame('08 · Organizer Hub', ox, 0, W, H);
  await cloneHeader(f);
  let y = CT + 24;

  // Organizer info card
  mkRect(f, 'orgCard', 32, y, W - 64, 188, C.card, 24);
  mkRect(f, 'orgAccent', 32, y, 10, 188, C.primary);
  mkImg(f, 'organizer_brand_logo', 52, y + 22, 120, 120, 16);
  await mkText(f, 'Công ty Sự kiện ABC', 196, y + 22, 28, C.textPri, 'Bold', W - 264, 'orgN');
  mkPill(f, 196, y + 62, 224, 50, C.greenBg, 1, 'verPill');
  await mkText(f, '✔ Đã xác minh', 212, y + 74, 22, C.success, 'SemiBold', 0, 'verTxt');
  await mkText(f, '3 sự kiện · 1.247 vé đã bán', 196, y + 132, 22, C.textSec, 'Regular', 0, 'orgStats');
  y += 212;

  // Quick actions 4×2
  await mkText(f, 'Quản lý nhanh', 32, y, 28, C.textPri, 'Bold', 0, 'qaHdr'); y += 48;
  const actions = [
    { icon: '📋', label: 'Sự kiện' },  { icon: '🎫', label: 'Loại vé' },
    { icon: '💰', label: 'Đơn hàng' }, { icon: '🏷', label: 'Mã giảm giá' },
    { icon: '👥', label: 'Nhân viên' }, { icon: '📷', label: 'Quét QR' },
    { icon: '⭐', label: 'Nghệ sĩ' },  { icon: '📢', label: 'Blast' },
  ];
  const cw2 = (W - 64) / 4;
  for (let i = 0; i < actions.length; i++) {
    const col = i % 4, row = Math.floor(i / 4);
    const ax = 32 + col * cw2 + cw2/2 - 52;
    const ay = y + row * 188;
    mkRect(f, `aCard${i}`, ax, ay, 104, 104, C.chipBlu, 20);
    await mkText(f, actions[i].icon, ax + 26, ay + 22, 52, C.primary, 'Regular', 0, `aIcon${i}`);
    await mkText(f, actions[i].label, ax - 8, ay + 114, 20, C.textSec, 'Regular', 120, `aLbl${i}`);
  }
  y += 400;

  mkDiv(f, y, 32); y += 32;
  await mkText(f, 'Sự kiện gần đây', 32, y, 28, C.textPri, 'Bold', 0, 'reHdr');
  await mkText(f, 'Xem tất cả ›', W - 188, y + 4, 24, C.primary, 'Medium', 0, 'reMore');
  y += 52;
  const evs = [
    { n: 'Starlight Tour 2026',         d: '21/07/2026', s: '245/500', st: 'APPROVED', sc: C.success, sb: C.greenBg },
    { n: 'Workshop Nhiếp ảnh cơ bản',  d: '15/08/2026', s: '0/100',   st: 'PENDING',  sc: C.warning, sb: C.amberBg },
    { n: 'Đêm nhạc Acoustic Café',     d: '10/09/2026', s: '0/200',   st: 'DRAFT',    sc: C.textSec, sb: C.border },
  ];
  for (const ev of evs) {
    mkRect(f, 'evCard', 32, y, W - 64, 136, C.card, 20);
    mkImg(f, `orgThumb_${ev.n.slice(0,10)}`, 52, y + 18, 88, 88, 12);
    await mkText(f, ev.n, 164, y + 14, 26, C.textPri, 'SemiBold', W - 280, 'evN');
    await mkText(f, '📅 ' + ev.d, 164, y + 58, 20, C.textSec, 'Regular', 0, 'evD');
    await mkText(f, '🎫 ' + ev.s + ' vé', 164, y + 86, 20, C.textSec, 'Regular', 0, 'evS');
    const sw = ev.st.length * 13 + 36;
    mkPill(f, W - sw - 44, y + 40, sw, 48, ev.sb, 1, 'stBg');
    await mkText(f, ev.st, W - sw - 28, y + 53, 20, ev.sc, 'Bold', 0, 'stTxt');
    y += 156;
  }

  mkRect(f, 'createBtn', 32, H - NAV_H - 116, W - 64, 90, C.primary, 20);
  await mkText(f, '+ Tạo sự kiện mới', W/2 - 108, H - NAV_H - 90, 30, C.white, 'Bold', 0, 'createTxt');

  await drawBottomNav(f, 2);
  figma.currentPage.appendChild(f);
  return f;
}

// ═══════════════════════════════════════════════════════════════════════════════
// MAIN
// ═══════════════════════════════════════════════════════════════════════════════
async function main() {
  figma.currentPage.name = 'VibeTix — App Screens';
  print('🚀 VibeTix Screen Generator v3');
  print('   ✅ Header clone từ frame "Homepage" > "Header - TopAppBar"');
  print('   ✅ Bottom nav: 5 tab + FAB "+" ở giữa');
  print('');

  const screens = [
    { fn: s01_Home,           name: '01 Home' },
    { fn: s02_Events,         name: '02 Events' },
    { fn: s03_EventDetail,    name: '03 Event Detail' },
    { fn: s04_MyTickets,      name: '04 My Tickets' },
    { fn: s05_Profile,        name: '05 Profile' },
    { fn: s06_Login,          name: '06 Login' },
    { fn: s07_PaymentSuccess, name: '07 Payment Success' },
    { fn: s08_OrganizerHub,   name: '08 Organizer Hub' },
  ];

  const frames: FrameNode[] = [];
  for (let i = 0; i < screens.length; i++) {
    const x = i * (W + GAP);
    print(`  [${i+1}/${screens.length}] Đang tạo ${screens[i].name}...`);
    const fr = await screens[i].fn(x);
    frames.push(fr);
  }

  figma.viewport.scrollAndZoomIntoView(frames);
  print('');
  print('✅ Xong! Đã tạo ' + frames.length + ' màn hình.');
  print('');
  print('📷 THAY ẢNH: chọn Rectangle tên bắt đầu 📷 → Fill → Solid → Image → upload');
  print('⌨️  Ctrl+Shift+H = fit tất cả vào màn hình Figma');
}

main().catch(e => print('❌ Lỗi: ' + String(e)));
