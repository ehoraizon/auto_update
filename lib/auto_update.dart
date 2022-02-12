import 'dart:async';
import 'dart:io';

import 'package:flutter/services.dart';
import './desktop.dart';
import './fetch_github.dart' as gb;

class AutoUpdate {
  static const MethodChannel _channel = MethodChannel('auto_update');

  static Future<String> getDocumentsFolder() async {
    if (Platform.isWindows) {
      return (await _channel.invokeMethod("getDocumentsFolder")).toString();
    }
    return "";
  }

  static Future<Map<dynamic, dynamic>> fetchGithub(
      String user, String packageName,
      {String fileType = ".exe"}) async {
    Map<dynamic, dynamic> res = {};
    if (Platform.isAndroid) {
      return await _channel.invokeMethod(
          "fetchGithub", {"user": user, "packageName": packageName});
    } else if (Platform.isWindows) {
      List<dynamic>? packageInfo =
          await _channel.invokeListMethod("getProductAndVersion");
      if (packageInfo != null) {
        res = await gb.fetchGithub(
          user,
          packageName,
          "application/octet-stream",
          packageInfo[1],
          packageInfo[0] + fileType,
        );
      }
      return res;
    }
    return res;
  }

  static Future<void> downloadAndUpdate(String url) async {
    if (Platform.isAndroid) {
      await _channel.invokeMethod("downloadAndUpdate", {"url": url});
    } else if (Platform.isWindows) {
      String? filePath = await downloadFile(
          Uri.parse(url),
          (await _channel.invokeMethod("getDownloadFolder")).toString() + "\\",
          url.split("/").last);
      if (filePath != null) {
        if ((await _channel
                .invokeMethod("runFileWindows", {"filePath": filePath})) >
            32) {
          exit(0);
        }
      }
    }
  }
}
