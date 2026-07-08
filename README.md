# VibeTix — Your Event Starts Here

VibeTix is an Android event-ticketing application inspired by Ticketbox, enabling users to discover, purchase, and manage tickets for live events across Vietnam. The app supports three distinct roles: **Customer**, **Organizer**, and **Admin**, each with a dedicated interface and feature set.

---

## Overview

| Item | Detail |
|---|---|
| Platform | Android (Java 21) |
| Min SDK | API 26 (Android 8.0 Oreo) |
| Target SDK | API 34 (Android 14) |
| Compile SDK | API 36 |
| Backend | Firebase (Auth + Firestore + Storage) |
| Firebase Project | `mobile-61a6c` — asia-southeast1 |
| Package | `com.example.vibetix` |
| Build System | Gradle 9.1.1 with version catalog |

---

## User Roles

| Role | Description |
|---|---|
| **Customer** | Discover events, purchase tickets, view QR tickets, manage transfers/resale |
| **Organizer** | Create and manage events, ticket types, staff, orders, QR check-in |
| **Admin** | Approve events and organizers, manage categories, venues, and global discounts |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| UI | XML Layouts + Material Design Components 3 |
| Navigation | Fragment transactions via `getSupportFragmentManager()` |
| Authentication | Firebase Authentication (Email/Password) |
| Database | Cloud Firestore (16 collections) |
| File Storage | Firebase Storage |
| Image Loading | Glide 4.16 (`DiskCacheStrategy.ALL`) |
| QR Codes | ZXing Android Embedded 4.3 + ML Kit Barcode Scanning 17.3 |
| Camera | CameraX 1.4 (core, camera2, lifecycle, view) |
| Charts | MPAndroidChart 3.1 |
| Lists | RecyclerView 1.3 + ViewPager2 1.1 |
| Serialization | Gson 2.10.1 |

---

## Architecture

```
Activity  →  Fragment  →  Repository  →  Firebase
```

| Layer | Role |
|---|---|
| **Activity** | Navigation container per user role |
| **Fragment** | Individual screens (35 fragments total) |
| **Repository** | All Firebase read/write logic (15 repositories) |
| **Model** | Plain Java POJOs — field names match Firestore document fields exactly |
| **Adapter** | One dedicated class per RecyclerView list (23 adapters) |

**Key rules:**
- Firestore queries never go inside Activity or Fragment — always in a Repository
- All Firestore collection name strings live in `FirebaseCollections.java`
- All role names, status strings, and SharedPreferences keys live in `Constants.java`
- All user-visible text is in `res/values/strings.xml` with Vietnamese in `res/values-vi/strings.xml`

---

## Features

### Customer
- Browse and search events with filters (city, category, price range, date)
- Full ticket purchase flow: select ticket type → fill attendee info → simulated payment → QR ticket issued
- View purchased tickets with QR code; upcoming and ended tabs
- Peer-to-peer ticket transfer and resale via `ticket_transfers` collection
- Follow artists and view featured performers on homepage
- Homepage: banner carousel, featured stars, featured/trending events, resale section

### Organizer
- Multi-step event creation: basic info → date & venue → ticket types → media upload → review & submit
- Manage ticket types, pricing, sale windows, and available quantity
- View orders and revenue breakdown per event
- Assign staff roles (manager / check-in staff) per event
- QR code check-in scanner powered by CameraX + ML Kit
- Link artists/performers to events with display order
- Send blast push notifications to all ticket buyers of an event
- Create and manage event-scoped discount codes

### Admin
- Approve or reject event submissions from organizers
- Approve organizer account registrations
- Manage event categories, venues, and global discount codes
- System-wide dashboard with user, event, ticket, and revenue metrics

---

## Firebase Collections

| Collection | Description |
|---|---|
| `users` | User accounts, roles, and organizer link |
| `organizers` | Organizer brand profiles (1:N per user) |
| `events` | Event listings with status, pricing, and metadata |
| `ticket_types` | Ticket tiers, pricing, quantities, and sale windows per event |
| `orders` | Purchase order headers |
| `order_items` | Line items within each order |
| `user_tickets` | Individually issued tickets with `ticket_code` (QR) and status |
| `ticket_transfers` | Peer-to-peer resale/transfer requests with expiry |
| `stars` | Artist and performer profiles |
| `event_stars` | Many-to-many: performers linked to events |
| `user_star_follows` | User follow relationships with artists |
| `discounts` | Promotional codes (percentage or fixed, global or event-scoped) |
| `event_staff` | Staff role assignments per event |
| `notifications` | In-app notification records per user |
| `categories` | Event category taxonomy |
| `venues` | Venue master data (name, address, city, capacity) |

**Status flows:**
- Event: `DRAFT → PENDING → APPROVED → ONGOING → COMPLETED` (or `CANCELLED`)
- Order: `PENDING → PAID` (or `CANCELLED`)
- User ticket: `valid → used / expired / transferred / cancelled`
- Transfer: `pending → accepted / rejected / expired / cancelled`

---

## Project Structure

```
app/src/main/
├── java/com/example/vibetix/
│   ├── Activities/
│   │   ├── Auth/           AuthActivity
│   │   ├── User/           UserMainActivity, EventDetailActivity,
│   │   │                   CreateOrganizerActivity
│   │   ├── Organizer/      EventHubActivity, CreateEditEventActivity,
│   │   │                   QrScannerActivity, OrderManagementActivity,
│   │   │                   TicketTypeManagementActivity,
│   │   │                   DiscountManagementActivity,
│   │   │                   StaffManagementActivity,
│   │   │                   EventStarManagementActivity,
│   │   │                   OrganizerBlastActivity,
│   │   │                   OrganizerNotificationsActivity, ...
│   │   └── Admin/          AdminMainActivity, CreateDiscountActivity
│   │
│   ├── Fragments/
│   │   ├── Auth/           LoginFragment, RegisterFragment
│   │   ├── User/           HomeFragment, EventsFragment, SearchFragment,
│   │   │                   EventDetailFragment, SelectTicketFragment,
│   │   │                   FillAttendeeInfoFragment, SimulatedPaymentFragment,
│   │   │                   PaymentSuccessFragment, MyTicketsFragment,
│   │   │                   ProfileFragment, AccountInfoFragment,
│   │   │                   SecurityFragment, HelpCenterFragment,
│   │   │                   OrganizerHubFragment, OrganizerRegisterFragment,
│   │   │                   OrganizerProfileFragment, AllEventsFragment,
│   │   │                   CreateEventFragment, PaymentMethodsFragment,
│   │   │                   TicketQrDialogFragment, PassTicketDialogFragment,
│   │   │                   SliderCaptchaDialogFragment, DateFilterDialog,
│   │   │                   EventFilterDialog
│   │   ├── Organizer/      MyEventsListFragment, OrganizerProfileFragment
│   │   └── Admin/          DashboardFragment, EventApprovalFragment,
│   │                       CategoryManagerFragment, MasterDataFragment,
│   │                       GlobalDiscountFragment, OrganizerApprovalFragment,
│   │                       VenueManagerFragment
│   │
│   ├── Adapters/           23 RecyclerView adapter classes
│   ├── Models/             22 data classes (Event, User, Order, TicketType,
│   │                       UserTicket, TicketTransfer, Star, Organizer, ...)
│   ├── Repositories/       15 Firebase repository classes
│   ├── Firebase/           FirebaseCollections.java, FirestoreHelper.java,
│   │                       HomepageLoader.java
│   └── Utils/              Constants.java, SessionManager.java,
│                           NotificationPopupHelper.java
│
└── res/
    ├── layout/             Activity and Fragment XML layouts
    ├── drawable/           Vector icons (ic_*), backgrounds (bg_*),
    │                       gradients (gradient_*)
    ├── values/             colors.xml, strings.xml, themes.xml, dimens.xml
    ├── values-vi/          strings.xml (Vietnamese locale)
    └── font/               Poppins (Regular, Medium, SemiBold)
```

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/PhamHuyen2203/VibeTix-App.git
```

### 2. Open in Android Studio

```
File → Open → VibeTix-App
```

Wait for Gradle sync to complete.

### 3. Add Firebase configuration

Place `google-services.json` (obtained from the Firebase Console, project `mobile-61a6c`) into the `app/` directory. This file is excluded from version control.

### 4. Run the app

Connect a physical Android device (API 26+) or start an emulator with Google Play Services, then click **Run**.

---

## Team

Developed as a final project for the **Mobile Application Development** course  
**University of Economics and Law (UEL)** — Ho Chi Minh City National University, Vietnam
