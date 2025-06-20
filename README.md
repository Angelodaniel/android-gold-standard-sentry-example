# Android Custom Metrics App

## Sentry Performance Demo

This minimal Android app demonstrates a powerful yet simple way to implement a centralized tracing system for Sentry performance monitoring. It automatically captures cold start, screen loads, and navigation timing using a custom `Tracer` class, making it incredibly easy to add robust performance monitoring to any activity with minimal boilerplate.

### What This App Captures

- **Cold Start (`app.start.flow`):**
  - Measures the entire application cold start, from the moment the process is created until the main screen (`HomeActivity`) is fully rendered.
  - This trace is initiated by `AppStartProvider` at the earliest possible moment and is managed by the central `Tracer`.

- **Screen Load (`ui.load`):**
  - For every `Activity`, a `ui.load` transaction is automatically created by the `Tracer`.
  - This transaction contains child spans for:
    - **TTID (`ui.load.initial_display`):** Time To Initial Display, measuring until the first layout pass is complete.
    - **TTFD (`ui.load.full_display`):** Time To Full Display, measuring until the activity's content is fully rendered. This span is finished by calling `Tracer.reportFullyDrawn()`.

- **Navigation Timing (`ui.load.sntr` & `ui.load.nts`):**
  - When navigating between activities using the `launchTraced` extension function, two spans are automatically created:
    - **SNTR (`ui.load.sntr`):** "Screen Navigation to Render", measures the total time from the user's tap until the destination screen's initial layout is complete.
    - **NTS (`ui.load.nts`):** "Navigation Timing Span", measures the time from the user's tap until the destination activity's `onCreate` is called.

- **Auto-instrumented Spans:**
  - Standard Sentry auto-instrumentation for OkHttp network requests, File I/O, etc., is also active.

### How It Works

1.  **`AppStartProvider`**: A `ContentProvider` that runs before the `Application` class. It initializes `SentryAndroid` and our custom `Tracer` to ensure tracing begins at the absolute start of the app process. It kicks off the `app.start.flow`.

2.  **`Tracer.kt`**: This is the core of the instrumentation. It centralizes all tracing logic so that individual activities don't need to contain any manual Sentry code.
    - It implements `ActivityLifecycleCallbacks` to listen for `onActivityCreated`, `onActivityResumed`, etc.
    - It automatically starts and stops `ui.load`, `TTID`, and `TTFD` spans for each activity based on these lifecycle events.
    - It provides the `launchTraced` extension function, which wraps `startActivity` to create navigation spans (`SNTR`, `NTS`).
    - `Tracer.reportFullyDrawn(activity)` is called from an activity when its content is ready, which finishes the corresponding `TTFD` span.

3.  **Activities (`HomeActivity`, `AutoTTIDTTFDWithNTSMeasurementActivity`, etc.)**: With the `Tracer` handling all the complexity, the activities become incredibly simple.
    - They contain **zero** manual Sentry span creation or management logic.
    - Adding tracing to a new screen is as simple as using `launchTraced(NextActivity::class.java)` for navigation.
    - They just need to signal when they are ready by calling `Tracer.reportFullyDrawn(this)`.

### Sentry Setup
- The Sentry SDK is initialized inside `AppStartProvider` to allow for capturing the earliest parts of the app start.
- The `Tracer` is registered as an `ActivityLifecycleCallbacks` in the `Application` class (`MyApp.kt`).
- The DSN is injected securely via `local.properties` and a manifest placeholder.

---

**This project demonstrates how to build a centralized and automated Sentry Android performance instrumentation system that is not only powerful but also incredibly easy to implement and maintain.** 