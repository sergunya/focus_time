# Focus Time No-Op Plugin

A minimal no-operation plugin for GoLand 2025.2 that demonstrates proper plugin structure and follows JetBrains development guidelines.

## Overview

This plugin is designed to perform no operations and serves as:
- An educational example of proper JetBrains plugin structure
- A template for plugin development
- A demonstration of modern Gradle-based IntelliJ Platform Plugin SDK usage

## Features

- ✅ Proper Gradle-based project structure
- ✅ Follows JetBrains plugin development guidelines
- ✅ Compatible with GoLand 2025.2
- ✅ No operational functionality (by design)
- ✅ Installable from local disk

## Development

### Prerequisites

- Java 17 or higher
- GoLand 2025.2 or compatible IntelliJ-based IDE

### Building the Plugin

1. Clone or download this repository
2. Open terminal in the project root
3. Run the build command:

```bash
./gradlew buildPlugin
```

The built plugin will be available in `build/distributions/focus-time-plugin-1.0.0.zip`

### Installing the Plugin

1. Open GoLand 2025.2
2. Go to `File | Settings | Plugins`
3. Click the gear icon and select "Install Plugin from Disk..."
4. Navigate to `build/distributions/focus-time-plugin-1.0.0.zip`
5. Select the file and click "OK"
6. Restart GoLand to activate the plugin
 
 ### Feature: Gopher Terminal Cursor
 
 - Replaces the blinking cursor in the built-in Terminal with a fast Gopher icon.
 - Toggle via Settings: `Focus Time: Gopher Terminal Cursor` or action: `Toggle Gopher Terminal Cursor`.
 - Asset path: `src/main/resources/icons/gopher_fast.png` (drop your PNG here; transparency recommended).

### Verifying Installation

After restart:
1. Go to `File | Settings | Plugins`
2. Look for "Focus Time No-Op Plugin" in the installed plugins list
3. Verify it's enabled (checkbox should be checked)

## Project Structure

```
focus-time-plugin/
├── build.gradle.kts              # Gradle build configuration
├── gradle.properties             # Project properties
├── settings.gradle.kts            # Gradle settings
├── src/main/
│   ├── kotlin/com/focustime/nopplugin/
│   │   └── NoOpPlugin.kt         # Minimal plugin class
│   └── resources/META-INF/
│       └── plugin.xml            # Plugin descriptor
├── gradle/wrapper/               # Gradle wrapper files
├── gradlew                       # Gradle wrapper script (Unix)
└── README.md                     # This file
```

## Plugin Configuration

The plugin is configured in `src/main/resources/META-INF/plugin.xml`:

- **ID**: `com.focustime.nopplugin`
- **Name**: Focus Time No-Op Plugin
- **Version**: 1.0.0
- **Compatibility**: GoLand 2025.2 (build 252+)

## Development Guidelines

This plugin follows JetBrains' official development guidelines:

- Uses modern Gradle-based IntelliJ Platform Plugin SDK
- Targets specific IntelliJ Platform version (2025.2)
- Includes proper plugin descriptor with required metadata
- Uses appropriate project structure and naming conventions
- Compatible with plugin marketplace standards

## License

This project is intended for educational purposes and plugin development reference.

## Contributing

This is a minimal example plugin. For real plugin development, extend the structure with actual functionality while maintaining the established patterns.
