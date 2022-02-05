import 'dart:convert';
import 'dart:io';

Future<Map<String, String>> fetchGithub(String user, String packageName,
    String type, String version, String appName) async {
  Map<String, String> results = {"assetUrl": ""};
  final client = HttpClient();
  client.userAgent = "auto_update";

  final request = await client.getUrl(Uri.parse(
      "https://api.github.com/repos/$user/$packageName/releases/latest"));
  final response = await request.close();

  if (response.statusCode == 200) {
    final contentAsString = await utf8.decodeStream(response);
    final Map<dynamic, dynamic> map = json.decode(contentAsString);
    // print(map);
    if (map["tag_name"] != null &&
        map["tag_name"] != version &&
        map["assets"] != null) {
      for (Map<dynamic, dynamic> asset in map["assets"]) {
        if ((asset["content_type"] != null && asset["content_type"] == type) &&
            (asset["name"] != null && asset["name"] == appName)) {
          results["assetUrl"] = asset["browser_download_url"] ?? '';
          results["body"] = map["body"] ?? '';
          results["tag"] = map["tag_name"] ?? '';
        }
      }
    }
  }

  return results;
}
