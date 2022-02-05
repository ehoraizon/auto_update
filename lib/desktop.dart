import 'dart:io';

Future<void> runFile(String filePath) async {
  await Process.run(filePath, [], includeParentEnvironment: false);
}

Future<String?> downloadFile(
    Uri uri, String downloadFolder, String fileName) async {
  final client = HttpClient();
  client.userAgent = "auto_update";
  final request = await client.getUrl(uri);
  final response = await request.close();

  File file = File(downloadFolder + fileName);

  if (await file.exists()) {
    await file.delete();
  }

  await file.create();

  if (response.statusCode == 200) {
    await response.pipe(file.openWrite());
    return file.absolute.path;
  }
}
