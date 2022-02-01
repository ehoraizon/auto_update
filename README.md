# Auto Update Plugin

This plugin for Flutter handles a native app update without an official application store.

# Features

| Feature | Android | iOS | macOS | Linux | Windows |
|---------|----------|------|--------|--------|--------- |
| download & update | ✅ 
| github releases | ✅ 

# Usage

### Import the pacakge with

```dart
    import 'package:auto_update/auto_update.dart';
```

### Check for new release in github

```dart
    Map<dynamic, dynamic> results = await AutoUpdate.fetchGithubApk("github_user", "package_name");
    if (results != null) {
        if (results['assetUrl'] == "up-to-date") {
            /* aplication is up-to-date */
        } else if (results['assetUrl'].isEmpty()) {
            /* package or user don't found */
        } else {
            /* update url found */
        }
    }
```

### Download and Update

In android, the package will ask the user for permissions if needed. If the permissions were required and granted, a second call to the function is necessary for the package to download and update to the new release.

```dart
    await AutoUpdate.downloadAndUpdate("https://release_raw_url");
```



