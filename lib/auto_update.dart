import 'dart:async';
import 'dart:io';

import 'package:flutter/services.dart';
import './windows.dart';
import './desktop.dart';
import './fetch_github.dart' as gb;

class AutoUpdate {
  static const MethodChannel _channel = MethodChannel('auto_update');

  static Future<Map<dynamic, dynamic>> fetchGithub(
      String user, String packageName) async {
    if (Platform.isAndroid) {
      return await _channel.invokeMethod(
          "fetchGithub", {"user": user, "packageName": packageName});
    } else if (Platform.isWindows) {
      PackageInfo packageInfo = PackageInfo(Platform.resolvedExecutable);
      var res = await gb.fetchGithub(
        user,
        packageName,
        "application/octet-stream",
        packageInfo.productVersion,
        packageInfo.productName + ".msix",
      );
      return res;
    }
    return {};
  }

  static Future<void> downloadAndUpdate(String url) async {
    if (Platform.isAndroid) {
      await _channel.invokeMethod("downloadAndUpdate", {"url": url});
    } else if (Platform.isWindows) {
      String? filePath = await downloadFile(
          Uri.parse(url), getDownloadFolder(), url.split("/").last);
      if (filePath != null) {
        if (runFileWindows(filePath) > 32) {
          exit(0);
        }
      }
    }
  }
}
