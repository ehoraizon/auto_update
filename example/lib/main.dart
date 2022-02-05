import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:auto_update/auto_update.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  Map<dynamic, dynamic> _packageUpdateUrl = {};

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    Map<dynamic, dynamic> updateUrl;
    // Platform messages may fail, so we use a try/catch PlatformException.
    // We also handle the message potentially returning null.
    try {
      updateUrl = await AutoUpdate.fetchGithub("user", "packageName");
    } on PlatformException {
      updateUrl = {'assetUrl': 'Failed to get the url of the new release.'};
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _packageUpdateUrl = updateUrl;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Auto Update Example'),
        ),
        body: Center(
          child: Column(
            children: [
              Text('Update url: ${_packageUpdateUrl['assetUrl']}\n'),
              IconButton(
                  onPressed: () async {
                    if (_packageUpdateUrl['assetUrl'].isNotEmpty &&
                        _packageUpdateUrl['assetUrl'] != "up-to-date" &&
                        (_packageUpdateUrl['assetUrl'] as String)
                            .contains("https://")) {
                      try {
                        await AutoUpdate.downloadAndUpdate(
                            _packageUpdateUrl['assetUrl']);
                      } on PlatformException {
                        setState(() {
                          _packageUpdateUrl['assetUrl'] = "Unable to download";
                        });
                      }
                    }
                  },
                  icon: const Icon(Icons.download_for_offline_outlined))
            ],
          ),
        ),
      ),
    );
  }
}
