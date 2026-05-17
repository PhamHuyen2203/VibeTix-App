# VibeTix - Event Ticket Booking & Management App

VibeTix is an Android mobile application for event ticket booking and event management.  
The application combines ticket booking features for users and event management features for organizers.  
It also includes an admin role for managing users, event approvals, payment gateways, reports, categories, and promotions.

---

## Table of Contents

- [Introduction](#introduction)
- [Main Features](#main-features)
- [User Roles](#user-roles)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Setup Instructions](#setup-instructions)
- [Development Rules](#development-rules)
- [Suggested Development Order](#suggested-development-order)
- [Team Members](#team-members)

---

## Introduction

VibeTix is developed as a mobile application project using Android Studio, Java, and XML Layout.  
The system is inspired by event ticketing platforms such as Ticketbox, with additional event management features for organizers.

The application supports three main roles:

- User
- Organizer
- Admin

Each role has its own interface and feature flow.

---

## Main Features

### User Features

- Register and login
- Manage personal profile
- Search and explore events
- View event details
- Add events to wishlist
- Select ticket types
- Buy tickets
- View order status
- View purchased tickets
- Show QR ticket
- Receive notifications

### Organizer Features

- Register and login as organizer
- Create new events
- Upload event documents
- Submit events for approval
- Manage created events
- Edit or cancel events
- Manage event publication
- View ticket sales
- View sales reports
- Check in tickets using QR code

### Admin Features

- Manage users
- Verify organizer information
- Review event submissions
- Approve or reject events
- Manage payment gateways
- Manage orders and financial records
- Process financial settlements
- Manage event categories
- Manage promotions
- Generate and export system reports

---

## User Roles

| Role | Description |
|---|---|
| User | Searches events, buys tickets, manages orders, and uses QR tickets |
| Organizer | Creates events, manages publications, tracks sales, and checks in tickets |
| Admin | Manages users, verifies events, handles finance, reports, categories, and promotions |

---

## Technology Stack

- Android Studio
- Java
- XML Layout
- Firebase Authentication
- Cloud Firestore
- Firebase Storage
- RecyclerView
- Material Design Components
- Glide
- ZXing / ML Kit Barcode Scanner
- Git & GitHub

---

## Project Structure

```txt
app/src/main/java/com/example/vibetix
├── Activities
│   ├── SplashActivity.java
│   ├── AuthActivity.java
│   ├── UserMainActivity.java
│   ├── OrganizerMainActivity.java
│   └── AdminMainActivity.java
│
├── Fragments
│   ├── User
│   │   ├── HomeFragment.java
│   │   ├── SearchEventFragment.java
│   │   ├── EventDetailFragment.java
│   │   ├── WishlistFragment.java
│   │   ├── MyTicketsFragment.java
│   │   └── ProfileFragment.java
│   │
│   ├── Organizer
│   │   ├── OrganizerDashboardFragment.java
│   │   ├── CreateEventFragment.java
│   │   ├── ManageEventsFragment.java
│   │   ├── SalesReportFragment.java
│   │   └── CheckInTicketFragment.java
│   │
│   ├── Admin
│   │   ├── AdminDashboardFragment.java
│   │   ├── ManageUsersFragment.java
│   │   ├── ReviewEventSubmissionsFragment.java
│   │   ├── ManageCategoryFragment.java
│   │   ├── ManagePromotionFragment.java
│   │   └── SystemReportFragment.java
│   │
│   └── Shared
│       ├── LoginFragment.java
│       ├── RegisterFragment.java
│       └── ForgotPasswordFragment.java
│
├── Adapters
├── Models
├── Repositories
├── Firebase
├── Utils
└── Interfaces
```

---

## Setup Instructions

To run this project on a local machine:

### 1. Clone the repository

```bash
git clone https://github.com/PhamHuyen2203/VibeTix-App.git
```

### 2. Open project in Android Studio

Open Android Studio and choose:

```txt
File > Open > VibeTix-App
```

### 3. Wait for Gradle Sync

Wait until Android Studio finishes syncing Gradle files.

### 4. Add Firebase configuration file

If Firebase is used, add the Firebase configuration file to:

```txt
app/google-services.json
```

This file is ignored by Git for security reasons.

### 5. Run the app

Connect an Android device or start an emulator, then click:

```txt
Run
```

---

## Development Rules

- Use Java for application logic.
- Use XML for UI layout.
- Use Activities as main containers.
- Use Fragments for feature screens.
- Use RecyclerView Adapters for list screens.
- Use Model classes for data objects.
- Use Repository classes for Firebase or data operations.
- Do not write heavy data logic directly inside Activity or Fragment.
- Use meaningful file names such as `EventAdapter.java`, `User.java`, and `HomeFragment.java`.
- Keep Firebase collection names in `FirebaseCollections.java`.
- Keep app constants in `Constants.java`.

---

## Suggested Development Order

1. Initialize project structure
2. Create models
3. Create Firebase constants
4. Create utility classes
5. Build login and register screens
6. Build user home screen
7. Build event detail screen
8. Build wishlist feature
9. Build ticket booking flow
10. Build my tickets and QR ticket screens
11. Build organizer event management
12. Build admin event approval
13. Build reports and statistics

---