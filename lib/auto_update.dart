import 'dart:async';

import 'package:flutter/services.dart';

class AutoUpdate {
  static const MethodChannel _channel = MethodChannel('auto_update');

  static Future<Map<dynamic, dynamic>> fetchGithubApk(
      String user, String packageName) async {
    return await _channel.invokeMethod(
        "fetchGithubApk", {"user": user, "packageName": packageName});
  }

  static Future<void> downloadAndUpdate(String url) async {
    await _channel.invokeMethod("downloadAndUpdate", {"url": url});
  }
}
