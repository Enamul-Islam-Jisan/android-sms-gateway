# AI Guide: Android SMS Gateway

This document provides a comprehensive overview of the Android SMS Gateway project to help AI assistants understand its structure, logic, and usages.

## Project Overview

The Android SMS Gateway is an application that turns an Android smartphone into an SMS gateway. It supports sending and receiving SMS/MMS messages via a REST API.

It can operate in two modes:
1. **Local Server:** A Ktor-based web server runs directly on the device, accessible within the local network.
2. **Cloud Server (Gateway):** The device connects to a remote cloud server via WebSockets for bidirectional communication, allowing it to be controlled from anywhere.

## Core Architecture

The project follows a modular architecture, with logic separated into distinct modules under `me.capcom.smsgateway.modules`.

### Key Modules

- **`localserver`:** Implements the local REST API using Ktor (Netty).
    - `WebService.kt`: The main Android service running the Ktor server.
    - `routes/`: Contains API route definitions (Messages, Webhooks, Logs, etc.).
- **`gateway`:** Handles communication with the cloud server.
    - `GatewayService.kt`: Manages the WebSocket connection and message processing.
- **`webhooks`:** Manages outgoing webhook notifications for various events.
    - `WebHooksService.kt`: Core logic for emitting events and queuing webhooks.
    - `workers/SendWebhookWorker.kt`: Background worker for retrying failed webhooks.
- **`messages`:** Handles outgoing SMS/MMS logic.
- **`receiver`:** Listens for incoming SMS/MMS and triggers events.
- **`settings`:** Centralized settings management using `SharedPreferences`.

### Technology Stack

- **Language:** Kotlin
- **Dependency Injection:** Koin
- **Database:** Room (for messages, webhooks queue, logs)
- **Networking:** Ktor (Client & Server), OkHttp
- **Background Tasks:** WorkManager
- **UI:** XML-based layouts with ViewBinding, ViewModel, and LiveData

## Logic Flow Examples

### Sending a Message (Local Server)
1. `POST /message` is received by `MessagesRoutes` in `localserver`.
2. The request is processed and a `Message` entity is created in the database.
3. `MessagesService` (in `messages` module) picks up the message and uses `SmsManager` or `MmsManager` to send it.
4. Status updates are emitted via `EventBus`.

### Receiving a Message
1. `MessagesReceiver` (in `receiver` module) receives `SMS_RECEIVED` or `WAP_PUSH_RECEIVED` broadcast.
2. The message is saved to the database.
3. `WebHooksService` is notified and queues a `sms:received` or `mms:received` webhook.
4. `SendWebhookWorker` attempts to deliver the webhook to registered URLs.

## Data Model

- **`AppDatabase`:** The main Room database.
    - `Message`: Outgoing messages and their states.
    - `IncomingMessage`: Received messages.
    - `WebHook`: Registered webhook endpoints.
    - `webhook_queue`: Pending and failed webhook deliveries.
    - `logs_entries`: Application logs.

## API Documentation

The Local Server API is documented via Swagger/OpenAPI. The schema can be found in `app/src/main/assets/api/swagger.json`.

### Common Endpoints
- `POST /message`: Send SMS/MMS.
- `GET /message/{id}`: Check message status.
- `POST /webhooks`: Register a new webhook.
- `GET /logs`: Retrieve application logs.

## Guidelines for AI Modifications

1. **Dependency Injection:** Always use Koin for injecting services and settings.
2. **Asynchronous Work:** Use Coroutines for I/O and long-running tasks. Use `WorkManager` for persistent background tasks.
3. **Events:** Use the internal `EventBus` (based on Kotlin Flows) for cross-module communication.
4. **Settings:** Add new settings to `SettingsHelper.kt` or create a module-specific settings class like `LocalServerSettings.kt`.
5. **Database:** When modifying entities, remember to bump the `AppDatabase` version and add an `AutoMigration` if possible.
6. **UI:** Follow the existing pattern of using `Fragments` with `ViewModels`. Use `ViewBinding` for accessing layout elements.

## Common File Locations

- **UI Fragments:** `app/src/main/java/me/capcom/smsgateway/ui/`
- **Modules Logic:** `app/src/main/java/me/capcom/smsgateway/modules/`
- **Database Entities:** `app/src/main/java/me/capcom/smsgateway/modules/*/db/`
- **Resources:** `app/src/main/res/`
- **Manifest:** `app/src/main/AndroidManifest.xml`
