# VibeTix — CLAUDE.md

VibeTix is an Android event-ticketing app inspired by Ticketbox, built with Java + XML + Firebase.
Tagline: **"Your event starts here."**
Three user roles: **Customer**, **Organizer**, **Admin**.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java (Java 21) |
| UI | XML Layouts + Material Design Components |
| Navigation | Jetpack Navigation Component (fragment-based) |
| Backend | Firebase Auth + Cloud Firestore + Firebase Storage |
| Image loading | Glide 4.16 |
| QR codes | ZXing Android Embedded 4.3 |
| Lists | RecyclerView + ViewPager2 |
| Build | Gradle with version catalog (`gradle/libs.versions.toml`) |
| Min SDK | 26 (Android 8.0) · Target/Compile SDK 36 |

---

## Architecture

**Pattern:** Activity → Fragment → Repository → Firebase

```
Activities/          Main navigation containers per role
Fragments/User/      Customer-facing screens
Fragments/Organizer/ Organizer screens
Fragments/Admin/     Admin screens (stub — not yet built)
Fragments/Shared/    Auth screens (Login, Register)
Adapters/            RecyclerView adapters (one per list type)
Models/              Plain Java data classes matching Firestore docs
Repositories/        All Firebase read/write logic lives here
Firebase/            FirebaseCollections.java constants + helpers
Utils/               Constants.java, NotificationPopupHelper.java
Interfaces/          Callback interfaces (currently empty)
```

**Rules:**
- Never put Firestore queries or business logic directly in an Activity or Fragment — use a Repository.
- Keep all Firestore collection name strings in `FirebaseCollections.java`.
- Keep role names, status strings, and SharedPreferences keys in `Constants.java`.
- Activities are containers; Fragments are screens.
- One Adapter class per RecyclerView list type.
- Model classes must match Firestore document field names exactly (Firestore uses them for serialization).

---

## Firebase Collections

Defined in `Firebase/FirebaseCollections.java`:

```
users               organizers          events
event_documents     event_submissions   categories
ticket_types        orders              tickets
payments            wishlists           notifications
payment_gateways    financial_settlements promotions
system_reports
```

**Event status flow:** `DRAFT → PENDING → APPROVED/REJECTED → PUBLISHED → CANCELLED`
**Order status:** `PENDING → PAID / CANCELLED`

---

## User Roles

| Constant | Value |
|---|---|
| `Constants.ROLE_CUSTOMER` | `"customer"` |
| `Constants.ROLE_ORGANIZER` | `"organizer"` |
| `Constants.ROLE_ADMIN` | `"admin"` |

Role is stored in the Firestore `users` collection and read at login to route to the correct Activity.

---

## Navigation Flow

```
MainActivity (splash/router)
  ├── not logged in → AuthActivity
  │     ├── LoginFragment
  │     └── RegisterFragment
  └── logged in → UserMainActivity  (customer nav)
        ├── HomeFragment
        ├── SearchFragment / EventsFragment
        ├── EventDetailFragment → SelectTicketFragment
        │     → FillAttendeeInfoFragment → SimulatedPaymentFragment
        │     → PaymentSuccessFragment
        ├── MyTicketsFragment (+ TicketQrDialogFragment)
        └── ProfileFragment → AccountInfoFragment / SecurityFragment / etc.
```

OrganizerMainActivity and AdminMainActivity are planned but not yet wired up.

---

## Design System (Styleguide)

### Typeface
**Poppins** for all text. Import via Google Fonts in `res/font/`.

### Color Palette

**Primary**
- Blue Primary: `#2563EB`
- Teal/Cyan Accent: `#06B6D4`
- Primary light/dark shades as needed

**Secondary**
- Dark Blue shades (navigation, backgrounds)
- Yellow / Green / Red for accent actions

**Gradients**
- Blue gradient (banners, hero areas)
- Cyan gradient
- Orange gradient (warm CTAs)

**Text**
- Primary text: Black `#000000`
- Inverse text: White `#FFFFFF`

**State Colors**
| State | Color |
|---|---|
| Info | Blue |
| Success | Green |
| Warning | Yellow/Orange |
| Error | Red |

### Typography Scale (Poppins)

| Style | Size | Weight |
|---|---|---|
| Heading 01 | 32sp | Semi Bold |
| Heading 02 | 28sp | Semi Bold |
| Heading 03 | 22sp | Semi Bold |
| Heading 04 | 20sp | Semi Bold |
| Heading 05 | 18sp | Semi Bold |
| Heading 06 | 16sp | Semi Bold |
| Body 01 | 16sp | Regular |
| Body 02 | 14sp | Medium |
| Body 03 | 14sp | Regular |
| Button | 16sp | Semi Bold |
| Caption | 12sp–10sp | Medium/Regular |

### Brand Assets
- Logo: blue location-pin + ticket icon lockup — `ic_vibetix_logo`
- App icon: `app_icon` drawable
- Mascot: **Bibi** — a friendly blue ticket character, "the spirit of every great experience"
- Tagline: *"Your event starts here"*

---

## Naming Conventions

| Type | Pattern | Example |
|---|---|---|
| Activity | `*Activity.java` | `AuthActivity.java` |
| Fragment | `*Fragment.java` | `HomeFragment.java` |
| Adapter | `*Adapter.java` | `EventAdapter.java` |
| Model | singular noun | `Event.java`, `User.java` |
| Repository | `*Repository.java` | `EventRepository.java` |
| Item layout | `item_*.xml` | `item_event_card.xml` |
| Fragment layout | `fragment_*.xml` | `fragment_home.xml` |
| Dialog layout | `dialog_*.xml` | `dialog_ticket_qr.xml` |
| Drawable: background | `bg_*.xml` | `bg_btn_primary.xml` |
| Drawable: icon | `ic_*.xml` | `ic_auth_email.xml` |
| Drawable: gradient | `gradient_*.xml` | `gradient_banner_overlay.xml` |

Use snake_case for all resource IDs and file names. Use PascalCase for all Java class names.

---

## Current Implementation Status

### Done
- Authentication UI (Login / Register)
- Home screen with banner carousel, categories, featured/trending events
- Event browsing, search, filter (date + category dialogs)
- Event detail page
- Ticket selection + attendee info form
- Simulated payment flow + success screen
- My Tickets screen with QR code dialog
- User profile + account / security / payment methods / help screens
- Organizer registration + profile
- Firebase Firestore integration (events, categories, venues, orders, tickets)
- 10 adapters, 10 models, 3 repositories

### In Progress / Stub
- Admin fragments directory exists but is empty
- Organizer fragments directory exists but is empty (Organizer screens are under Fragments/User currently)
- Interfaces directory is empty

### Not Yet Built
- `OrganizerMainActivity` and `AdminMainActivity` (Activities planned per README)
- Admin dashboard (ManageUsers, ReviewSubmissions, ManageCategories, ManagePromotions, SystemReports)
- Organizer dashboard (ManageEvents, SalesReport, CheckInTicket)
- Real payment gateway integration (currently simulated)
- Wishlist feature (model exists, UI not wired)
- ForgotPassword screen
- Push notifications
- Event check-in (QR scanner via ZXing already in dependencies)

---

## Development Guidelines

1. **Java only** — no Kotlin. All source files are `.java`.
2. **XML layouts only** — no Jetpack Compose.
3. **Fragment transactions** via Jetpack Navigation or `getSupportFragmentManager()` — match whichever the screen already uses.
4. **Firestore callbacks** use `OnSuccessListener` / `OnFailureListener` — keep async handling in Repository, pass results to Fragment via interface or direct callback lambda.
5. **Image loading** always uses Glide — never load bitmaps manually.
6. **RecyclerView lists** always use a dedicated Adapter class — do not inline adapters anonymously.
7. **SharedPreferences** keys must be defined in `Constants.java` before use.
8. **String resources** for all user-visible text — put in `res/values/strings.xml` (and `res/values-vi/strings.xml` for Vietnamese).
9. **Do not hardcode colors** in Java code — reference `R.color.*` defined in `colors.xml`.
10. **ProGuard** is enabled for release builds — add keep rules when adding new libraries.

---

## Build & Run

```bash
# Clone
git clone https://github.com/PhamHuyen2203/VibeTix-App.git

# Open in Android Studio, wait for Gradle sync
# Add app/google-services.json (not committed — obtain from Firebase Console)
# Run on device or emulator (API 26+)
```

Firebase project ID: `mobile-61a6c` (asia-southeast1 region)

---

## Package

`com.example.vibetix`
